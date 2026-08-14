package ru.skqwk.scheduler.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ru.skqwk.scheduler.algo.Algos;
import ru.skqwk.scheduler.sandbox.actor.TalkerActorWithQueue;
import ru.skqwk.scheduler.sandbox.actor.impl.DefaultActorContext;
import ru.skqwk.scheduler.sandbox.actor.impl.FastRequestCounterActor;
import ru.skqwk.scheduler.sandbox.actor.impl.TaskDequeActor;
import ru.skqwk.scheduler.sandbox.actor.impl.UtilizationActor;
import ru.skqwk.scheduler.sandbox.actor.impl.scheduler.FirstComeFirstServedActor;
import ru.skqwk.scheduler.sandbox.actor.impl.scheduler.LeastAttainedServiceSchedulerActor;
import ru.skqwk.scheduler.sandbox.actor.impl.scheduler.RoundRobinSchedulerActor;
import ru.skqwk.scheduler.sandbox.actor.message.Message;
import ru.skqwk.scheduler.sandbox.clock.DefaultClockProvider;
import ru.skqwk.scheduler.sandbox.props.DefaultProps;
import ru.skqwk.scheduler.sandbox.props.Props;
import ru.skqwk.scheduler.sandbox.task.Task;
import ru.skqwk.scheduler.sandbox.task.meta.RequestMetadata;
import ru.skqwk.scheduler.sandbox.util.AccumulatedDuration;
import ru.skqwk.scheduler.sandbox.util.CsvTaskParser;
import ru.skqwk.scheduler.sandbox.util.CustomDateConverter;
import ru.skqwk.scheduler.sandbox.util.GanttDataConverter;
import ru.skqwk.scheduler.sandbox.util.GanttRecord;
import ru.skqwk.scheduler.sandbox.util.RequestExecutor;
import ru.skqwk.scheduler.sandbox.util.ValueRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ExperimentRunner {
    private static final DecimalFormat DF = new DecimalFormat("0.000",
            DecimalFormatSymbols.getInstance(Locale.US));

    /**
     * Интервал сбора метрик
     */
    private static final Duration INTERVAL_METRIC_RECORD = Duration.of(10, ChronoUnit.MINUTES);

    /**
     * Физическое время в одном шаге симуляции
     */
    private static final int MS_IN_ONE_TICK = 800;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        String experimentsPath = "dataset-generator\\experiments\\4";
        Path root = Paths.get(experimentsPath);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            System.err.println("Invalid experiments folder: " + experimentsPath);
            System.exit(1);
        }

        List<Path> datasetFiles;
        try (Stream<Path> walk = Files.walk(root)) {
            datasetFiles = walk.filter(p -> p.endsWith("dataset.csv"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to walk through experiments folder", e);
            return;
        }

        log.info("Found {} dataset.csv files", datasetFiles.size());

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (Path datasetPath : datasetFiles) {
            executor.submit(() -> processDataset(datasetPath));
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(24, TimeUnit.HOURS)) {
                log.warn("Some tasks did not finish within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("All experiments processed.");
    }

    private static void processDataset(Path datasetPath) {
        Path runDir = datasetPath.getParent();
        Path scenarioDir = runDir.getParent();
        Path metaPath = scenarioDir.resolve("meta.json");
        if (!Files.exists(metaPath)) {
            log.error("meta.json not found in {}", scenarioDir);
            return;
        }

        double rpm = readRpmFromMeta(metaPath);
        if (Double.isNaN(rpm)) {
            log.error("Could not parse rpm from {}", metaPath);
            return;
        }

        log.info("Processing {} with RPM={}", datasetPath, rpm);

        Props props = new DefaultProps(rpm, INTERVAL_METRIC_RECORD, MS_IN_ONE_TICK);

        Algos[] algorithms = {Algos.LAS, Algos.RR, Algos.FCFS};
        for (Algos algo : algorithms) {
            try {
                runSingleAlgorithm(datasetPath, algo, props);
            } catch (Exception e) {
                log.error("Failed to run {} on {}", algo, datasetPath, e);
            }
        }
    }

    private static void runSingleAlgorithm(Path datasetPath, Algos algo, Props props) {
        Path runDir = datasetPath.getParent();
        Path outputCsv = runDir.resolve(algo.name() + ".csv");

        if (Files.exists(outputCsv)) {
            log.info("Output {} already exists, skipping", outputCsv);
            return;
        }

        log.info("Running {} on {}", algo, datasetPath);

        DefaultClockProvider clockProvider = new DefaultClockProvider(props.getMsInOneTick());
        CustomDateConverter.setClockProvider(clockProvider);
        LocalDateTime started = LocalDateTime.now(clockProvider.getClock());

        // Используем новый метод парсера, работающий с абсолютным путём
        CsvTaskParser parser = new CsvTaskParser();
        List<Task> tasks;
        try {
            tasks = parser.parseFromPath(datasetPath, started);
        } catch (IOException e) {
            log.error("Failed to parse CSV: {}", datasetPath, e);
            return;
        }
        if (tasks == null || tasks.isEmpty()) {
            log.error("No tasks parsed from {}", datasetPath);
            return;
        }

        TaskDequeActor actor = new TaskDequeActor(tasks);
        RequestExecutor requestExecutor = new RequestExecutor(actor, props);
        DefaultActorContext actorContext = new DefaultActorContext();

        LinkedList<ValueRecord> loadRecords = new LinkedList<>();

        Map<String, LinkedList<ValueRecord>> records = new HashMap<>();

        actorContext.addActor(actor);
        actorContext.addActor(new FastRequestCounterActor(props));
        actorContext.addActor(createScheduler(algo, requestExecutor));
        actorContext.addActor(new UtilizationActor(loadRecords, records, props, requestExecutor));

        Queue<Message> messageQueue = new LinkedList<>();
        MainActorSandbox sandbox = new MainActorSandbox(actorContext, messageQueue, clockProvider);
        AccumulatedDuration duration = new AccumulatedDuration();

        sandbox.run(180_000_000, duration);

        Map<String, List<RequestMetadata>> history = requestExecutor.getHistory();
        List<GanttRecord> ganttRecords = GanttDataConverter.convertToGanttFormat(history, started);
        saveTaskDetailsCsv(ganttRecords, outputCsv);

        records.forEach((key, value) -> saveStatistics(runDir.resolve(algo + "_" + key + "_cv.csv"), value));
        saveStatistics(runDir.resolve(algo + "_load.csv"), loadRecords);

        log.info("Finished {} on {}, result saved to {}", algo, datasetPath, outputCsv);
    }

    private static TalkerActorWithQueue createScheduler(Algos mode, RequestExecutor requestExecutor) {
        return switch (mode) {
            case LAS -> new LeastAttainedServiceSchedulerActor(requestExecutor);
            case FCFS -> new FirstComeFirstServedActor(requestExecutor);
            case RR -> new RoundRobinSchedulerActor(requestExecutor);
        };
    }

    private static double readRpmFromMeta(Path metaPath) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(metaPath.toFile());
            JsonNode rpmNode = root.get("rpm");
            if (rpmNode == null || !rpmNode.isNumber()) {
                return Double.NaN;
            }
            return rpmNode.asDouble();
        } catch (Exception e) {
            log.error("Failed to parse rpm from " + metaPath, e);
            return Double.NaN;
        }
    }

    private static void saveTaskDetailsCsv(List<GanttRecord> records, Path outputPath) {
        Map<String, List<GanttRecord>> tasksMap = records.stream()
                .collect(Collectors.groupingBy(r -> r.taskId));

        List<String> sortedTaskIds = tasksMap.keySet().stream()
                .sorted((id1, id2) -> {
                    try {
                        return Integer.compare(Integer.parseInt(id1), Integer.parseInt(id2));
                    } catch (NumberFormatException e) {
                        return id1.compareTo(id2);
                    }
                })
                .collect(Collectors.toList());

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("task_id,start_sec,finish_sec,duration_sec,requests_count\n");
            for (String taskId : sortedTaskIds) {
                List<GanttRecord> taskRecords = tasksMap.get(taskId);
                double creationTime = taskRecords.stream().mapToDouble(r -> r.arrivedSec).min().orElse(0);
                double lastFinishTime = taskRecords.stream().mapToDouble(r -> r.finishSec).max().orElse(0);
                double timeInSystem = lastFinishTime - creationTime;
                writer.write(String.format("%s,%s,%s,%s,%d\n",
                        taskId,
                        DF.format(creationTime),
                        DF.format(lastFinishTime),
                        DF.format(timeInSystem),
                        taskRecords.size()));
            }
        } catch (IOException e) {
            log.error("Failed to write CSV to " + outputPath, e);
        }
    }

    private static void saveStatistics(Path resultFile, List<ValueRecord> cvRecords) {
        try (BufferedWriter writer = Files.newBufferedWriter(resultFile)) {
            writer.write(ValueRecord.getCsvHeader() + "\n");

            for (ValueRecord record : cvRecords) {
                writer.write(record.toCsvString() + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}