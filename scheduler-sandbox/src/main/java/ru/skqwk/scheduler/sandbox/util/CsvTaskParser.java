package ru.skqwk.scheduler.sandbox.util;

import ru.skqwk.scheduler.sandbox.task.DefaultTask;
import ru.skqwk.scheduler.sandbox.task.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CsvTaskParser {
    /**
     * Парсит CSV файл из абсолютного пути файловой системы.
     *
     * @param csvPath путь к CSV файлу
     * @param started время начала эксперимента
     * @return список задач
     * @throws IOException если ошибка чтения
     */
    public List<Task> parseFromPath(Path csvPath, LocalDateTime started) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            return parseFromReader(reader, started);
        }
    }

    // Выделим общую логику парсинга из Reader
    private List<Task> parseFromReader(BufferedReader reader, LocalDateTime started) throws IOException {
        String line;
        boolean isFirstLine = true;
        List<TaskData> taskData = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                continue; // Skip header
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            taskData.add(parseLine(line));
        }
        AtomicInteger counter = new AtomicInteger(1);
        return taskData.stream()
                .map((task) -> createTask(task, started, counter))
                .collect(Collectors.toList());
    }

    // Модифицируем существующий метод parse, чтобы он использовал общий парсер
    public List<Task> parse(String csvFilePath, LocalDateTime started)  {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                this.getClass().getClassLoader().getResourceAsStream(csvFilePath)))) {
            return parseFromReader(reader, started);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Task> parseCsvFile(String csvFilePath, LocalDateTime createdAt) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(csvFilePath)))) {
            String line;
            boolean isFirstLine = true;

            List<TaskData> taskData = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                taskData.add(parseLine(line));
            }

            AtomicInteger counter = new AtomicInteger(1);

            return taskData.stream()
                    .map((task) -> createTask(task, createdAt, counter))
                    .collect(Collectors.toList());
        }
    }

    private TaskData parseLine(String line) {
        // Split on first comma only (since profile contains commas)
        int firstCommaIndex = line.indexOf(',');
        if (firstCommaIndex == -1) {
            throw new IllegalArgumentException("Invalid CSV line: " + line);
        }

        String arrivalTimeStr = line.substring(0, firstCommaIndex).trim();
        String profileStr = line.substring(firstCommaIndex + 1).trim();

        // Parse arrival time
        double arrivalTimeSeconds = Double.parseDouble(arrivalTimeStr);

        // Parse profile array
        List<Double> profile = parseProfileArray(profileStr);

        return new TaskData(arrivalTimeSeconds, profile);
    }

    private List<Double> parseProfileArray(String profileStr) {
        List<Double> result = new ArrayList<>();

        if (profileStr.isEmpty()) {
            return result;
        }

        // Remove surrounding quotes if present
        String cleaned = profileStr;
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        // Remove square brackets if present
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        cleaned = cleaned.trim();

        // Handle empty array
        if (cleaned.isEmpty()) {
            return result;
        }

        // Split by commas and parse each number
        String[] numbers = cleaned.split(",");
        for (String numStr : numbers) {
            numStr = numStr.trim();
            if (!numStr.isEmpty()) {
                try {
                    result.add(Double.parseDouble(numStr));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number format: " + numStr, e);
                }
            }
        }

        return result;
    }

    private Task createTask(TaskData taskData, LocalDateTime createdAt, AtomicInteger counter) {
        // Convert Double profile to Integer for the task
        List<Integer> intProfile = taskData.profile.stream()
                .map(Double::intValue)
                .collect(Collectors.toList());

        DefaultTask task = new DefaultTask();
        task.setPlan(intProfile);

        // Calculate creation time based on arrival time
        LocalDateTime creationTime = createdAt
                .plus((long) (taskData.arrivalTimeSeconds * 1000), ChronoUnit.MILLIS);
        task.setCreatedAt(creationTime);
        task.setId(String.valueOf(counter.getAndIncrement()));

        return task;
    }

    private static class TaskData {
        private final double arrivalTimeSeconds;
        private final List<Double> profile;

        public TaskData(double arrivalTimeSeconds, List<Double> profile) {
            this.arrivalTimeSeconds = arrivalTimeSeconds;
            this.profile = profile;
        }
    }
}