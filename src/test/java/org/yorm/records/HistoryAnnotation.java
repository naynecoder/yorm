package org.yorm.records;

import java.time.LocalTime;

public record HistoryAnnotation(String subject, float amount, LocalTime annotationTime, String content) {

}
