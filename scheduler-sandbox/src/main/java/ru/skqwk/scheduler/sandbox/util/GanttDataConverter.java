package ru.skqwk.scheduler.sandbox.util;

import ru.skqwk.scheduler.sandbox.task.meta.RequestMetadata;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class GanttDataConverter {

    /**
     * Преобразует историю выполнения в формат для построения диаграммы Ганта
     *
     * @param history         карта: taskId -> список метаданных запросов
     * @param experimentStart время начала эксперимента (для нормализации времени)
     * @return список записей для графика
     */
    public static List<GanttRecord> convertToGanttFormat(
            Map<String, List<RequestMetadata>> history,
            LocalDateTime experimentStart) {

        List<GanttRecord> records = new ArrayList<>();

        // Получаем все уникальные taskId и сортируем их (например, по времени первого запроса)
        List<String> sortedTaskIds = history.entrySet().stream()
                .sorted(Comparator.comparing(e ->
                        e.getValue().stream()
                                .map(RequestMetadata::getStart)
                                .min(LocalDateTime::compareTo)
                                .orElse(LocalDateTime.MAX)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Для каждого задания
        for (int taskIndex = 0; taskIndex < sortedTaskIds.size(); taskIndex++) {
            String taskId = sortedTaskIds.get(taskIndex);
            List<RequestMetadata> requests = history.get(taskId);

            // Сортируем запросы по времени начала
            requests.sort(Comparator.comparing(RequestMetadata::getStart));

            // Для каждого запроса в задании
            for (int requestIdx = 0; requestIdx < requests.size(); requestIdx++) {
                RequestMetadata req = requests.get(requestIdx);

                // Нормализуем время относительно начала эксперимента (в секундах)
                double startSec = Duration.between(experimentStart, req.getStart()).toMillis() / 1000.0;
                double finishSec = Duration.between(experimentStart, req.getFinish()).toMillis() / 1000.0;
                double arrivedSec = Duration.between(experimentStart, req.getArrived()).toMillis() / 1000.0;

                records.add(new GanttRecord(
                        taskId,
                        taskIndex,           // Y-координата для графика
                        requestIdx,
                        startSec,
                        finishSec,
                        arrivedSec,
                        req.getActualDurationInSeconds(),
                        req.getExecutionDurationInSeconds()
                ));
            }
        }

        return records;
    }

    /**
     * Преобразует в CSV строку
     */
    public static String toCsv(List<GanttRecord> records) {
        // Создаем формат с точкой в качестве десятичного разделителя
        DecimalFormat df = new DecimalFormat("0.000",
                DecimalFormatSymbols.getInstance(Locale.US));

        StringBuilder sb = new StringBuilder();
        sb.append("task_id,task_index,request_idx,start_sec,finish_sec,arrived_sec,")
                .append("duration_actual_sec,duration_execution_sec\n");

        for (GanttRecord r : records) {
            sb.append(r.taskId).append(',')
                    .append(r.taskIndex).append(',')
                    .append(r.requestIdx).append(',')
                    .append(df.format(r.startSec)).append(',')
                    .append(df.format(r.finishSec)).append(',')
                    .append(df.format(r.arrivedSec)).append(',')
                    .append(df.format(r.durationActualSec)).append(',')
                    .append(df.format(r.durationExecutionSec)).append('\n');
        }

        return sb.toString();
    }
}
