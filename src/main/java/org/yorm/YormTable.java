package org.yorm;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;
import org.yorm.db.ReflectionUtil;
import org.yorm.exception.YormException;

public record YormTable<T extends Record>(
    String dbTable,
    List<YormTuple> tuples,
    Constructor<T> constructor,
    String concatenatedFieldNames,
    String selectAllFromTable,
    ReflectionUtil<T> reflectionUtil
) {

    public YormTable(String dbTable, List<YormTuple> tuples, Constructor<T> constructor) throws YormException {
        this(
            dbTable,
            tuples,
            constructor,
            tuples.stream().map(YormTuple::dbFieldName).collect(Collectors.joining(", ")),
            tuples.stream().map(YormTuple::dbFieldName).collect(Collectors.joining(", ", "SELECT ", " FROM " + dbTable)),
            new ReflectionUtil<>(constructor, tuples)
        );
    }


}
