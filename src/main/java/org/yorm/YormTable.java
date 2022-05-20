package org.yorm;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;
import org.yorm.db.ReflectionUtil;
import org.yorm.exception.YormException;

public record YormTable(
    String dbTable,
    List<YormTuple> tuples,
    Constructor<Record> constructor,
    String concatenatedFieldNames,
    String selectAllFromTable,
    ReflectionUtil reflectionUtil,
    boolean hasPrimaryKey
) {

    public YormTable(String dbTable, List<YormTuple> tuples, Constructor<Record> constructor) throws YormException {
        this(
            dbTable,
            tuples,
            constructor,
            tuples.stream().map(YormTuple::dbFieldName).collect(Collectors.joining(", ")),
            tuples.stream().map(YormTuple::dbFieldName).collect(Collectors.joining(", ", "SELECT ", " FROM " + dbTable)),
            new ReflectionUtil(constructor, tuples),
            tuples.stream().anyMatch(YormTuple::isPrimaryKey)
        );
    }


}
