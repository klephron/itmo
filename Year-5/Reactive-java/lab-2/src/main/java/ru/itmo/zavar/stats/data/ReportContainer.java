package ru.itmo.zavar.stats.data;

public record ReportContainer<V>(V iterationValue, V streamApiValue, V customCollectorValue, long iterationElapsedTime,
                                 long streamApiElapsedTime, long customCollectorElapsedTime) {
}
