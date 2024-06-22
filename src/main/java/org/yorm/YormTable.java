package org.yorm;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;

import org.yorm.exception.YormException;

public record YormTable(
    String dbTable,
    List<YormTuple> tuples,
    Constructor<Record> constructor,
    String concatenatedFieldNames,
    String selectAllFromTable,
    boolean hasPrimaryKey
) {

    public YormTable(String dbTable, List<YormTuple> tuples, Constructor<Record> constructor) throws YormException {
        this(
            dbTable,
            tuples,
            constructor,
            tuples.stream().map(YormTuple::dbFieldName).collect(Collectors.joining(", ")),
            tuples.stream().map(YormTuple::dbFieldName).collect(Collectors.joining(", ", "SELECT ", " FROM " + dbTable)),
            tuples.stream().anyMatch(YormTuple::isPrimaryKey)
        );
    }

    public YormTuple getTupleWithDBFieldName(String fieldName) throws YormException {
        return this.tuples().stream().filter(tuple -> tuple.dbFieldName().equals(fieldName))
                             .findFirst()
                             .orElseThrow(() -> new YormException("Field not found: " + fieldName));
    }

    public YormTuple getTupleWithObjectFieldName(String fieldName) throws YormException {
        return this.tuples().stream().filter(tuple -> tuple.objectFieldName().equals(fieldName))
                   .findFirst()
                   .orElseThrow(() -> new YormException("Field not found: " + fieldName));
    }


}
