package ru.skqwk.scheduler.sandbox.props;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Range {
    private final double lowerBound;
    private final double upperBound;
}
