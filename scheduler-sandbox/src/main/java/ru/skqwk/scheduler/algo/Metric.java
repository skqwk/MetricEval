package ru.skqwk.scheduler.algo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Metric {
    private double actual;
    private double potential;

    public static Metric of(int actual, int payload) {
        Metric metric = new Metric();
        metric.setActual(actual);
        metric.setPotential(payload);

        return metric;
    }


    public static Metric ofAverageDuration(double actual, double payload) {
        Metric metric = new Metric();
        metric.setActual(60 / actual);
        metric.setPotential(60 / payload);

        return metric;
    }
}
