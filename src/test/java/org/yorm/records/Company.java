package org.yorm.records;

import java.time.LocalDate;

public record Company(long id, String name, String countryCode, LocalDate date, float debt, boolean isActive) {

}
