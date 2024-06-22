package org.yorm;

import java.lang.reflect.Method;

import org.yorm.util.DbType;
import org.yorm.util.RowRecordConverter;

public record YormTuple(
        String dbFieldName,
        String objectFieldName,
        DbType type,
        int size,
        boolean isNullable,
        boolean isPrimaryKey,
        boolean isAutoincrement,
        Method method,
        RowRecordConverter.Converter<?> serializer,
        RowRecordConverter.Converter<?> deserializer
) {
}
