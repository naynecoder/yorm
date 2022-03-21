package org.yorm.records;

import java.time.LocalDateTime;

public record Person(int id, String name, String email, LocalDateTime lastLogin, int companyId) {

}
