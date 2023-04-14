package org.yorm.records;

import java.time.LocalDateTime;

public record Person(long id, String name, String email, LocalDateTime lastLogin, int companyId) {

    public Person(String name) {
        this(0, name, null, null, 0);
    }

}
