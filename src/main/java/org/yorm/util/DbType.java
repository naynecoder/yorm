package org.yorm.util;

import java.util.Locale;

public enum DbType {
    TINYINT,
    SMALLINT,
    MEDIUMINT,
    INT,
    INTEGER,
    BIGINT,
    FLOAT,
    DOUBLE,
    DECIMAL,
    BIT,
    DATE,
    DATETIME,
    TIMESTAMP,
    CHAR,
    VARCHAR;

    public static DbType getType(String str) {
        int pos = str.indexOf('(');
        if (pos > -1) {
            str = str.substring(0, pos);
        }
        return valueOf(str.toUpperCase(Locale.ROOT));
    }


}
