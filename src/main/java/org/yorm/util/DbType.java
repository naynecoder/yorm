package org.yorm.util;

import org.yorm.exception.YormException;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public enum DbType {
    //TODO: include all Types.whatever
    TINYINT(Types.TINYINT, boolean.class),
    BOOLEAN(Types.BOOLEAN, boolean.class),
    SMALLINT(Types.SMALLINT, int.class),
    INTEGER(Types.INTEGER, int.class),
    // TODO: Should this be an actual BigInteger?
    BIGINT(Types.BIGINT, long.class),
    FLOAT(Types.FLOAT, float.class),
    DOUBLE(Types.DOUBLE, double.class),
    REAL(Types.REAL, float.class),
    DECIMAL(Types.DECIMAL, BigDecimal.class),
    BIT(Types.BIT, boolean.class),
    DATE(Types.DATE, LocalDate.class),
    TIME(Types.TIME, LocalTime.class),
    TIMESTAMP(Types.TIMESTAMP, LocalDateTime.class),
    CHAR(Types.CHAR, char.class),
    VARCHAR(Types.VARCHAR, String.class),
    TEXT(Types.LONGVARCHAR, String.class);

    public final int sqlType;
    public final Class<?> javaType;

    DbType(int sqlType, Class<?> javaType) {
        this.sqlType = sqlType;
        this.javaType = javaType;
    }

    private static final Map<Integer, DbType> MAP = new HashMap<>();

    static {
        Arrays.stream(values()).forEach(t -> MAP.put(t.sqlType, t));
    }

    public static DbType getType(String str) throws YormException {
        int sqlType = Integer.parseInt(str);
        DbType dbType = MAP.get(sqlType);
        if (dbType == null) {
            throw new YormException("There is no DbType for sqlType:" + sqlType);
        }
        return dbType;
    }


}
