package org.yorm.util;

import org.yorm.exception.YormException;

import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum DbType {
    //TODO: include all Types.whatever
    TINYINT(Types.TINYINT),
    SMALLINT(Types.SMALLINT),
    INTEGER(Types.INTEGER),
    BIGINT(Types.BIGINT),
    FLOAT(Types.FLOAT),
    DOUBLE(Types.DOUBLE),
    REAL(Types.REAL),
    DECIMAL(Types.DECIMAL),
    BIT(Types.BIT),
    DATE(Types.DATE),
    TIMESTAMP(Types.TIMESTAMP),
    CHAR(Types.CHAR),
    VARCHAR(Types.VARCHAR);

    private final int sqlType;

    DbType(int sqlType) {
        this.sqlType = sqlType;
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
