package org.yorm.records;

import java.time.LocalDate;

public record Company(int id, String name, String countryCode, LocalDate date, boolean isActive) {

}
