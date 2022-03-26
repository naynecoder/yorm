package org.yorm;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;

public record YormTable(
        String dbTable,
        List<YormTuple> tuples,
        Constructor<Record> constructor,
        String concatenatedFieldNames,
        String selectAllFromTable
) {

    public YormTable(String dbTable, List<YormTuple> tuples, Constructor<Record> constructor) {
        this(
                dbTable,
                tuples,
                constructor,
                tuples.stream().map(YormTuple::dbFieldName).collect(Collectors.joining(", ")),
                tuples.stream().map(YormTuple::dbFieldName).collect(Collectors.joining(", ", "SELECT ", " FROM " + dbTable))
        );
    }


}
