package ru.skqwk.scheduler.sandbox.util;

public class GanttRecord {
    public final String taskId;
    public final int taskIndex;      // Y-координата (какая строка)
    public final int requestIdx;     // номер запроса в задании
    public final double startSec;    // начало выполнения
    public final double finishSec;   // конец выполнения
    public final double arrivedSec;  // время поступления запроса
    public final double durationActualSec;   // finish - arrived
    public final double durationExecutionSec; // finish - start

    public GanttRecord(String taskId, int taskIndex, int requestIdx,
                       double startSec, double finishSec, double arrivedSec,
                       double durationActualSec, double durationExecutionSec) {
        this.taskId = taskId;
        this.taskIndex = taskIndex;
        this.requestIdx = requestIdx;
        this.startSec = startSec;
        this.finishSec = finishSec;
        this.arrivedSec = arrivedSec;
        this.durationActualSec = durationActualSec;
        this.durationExecutionSec = durationExecutionSec;
    }
}
