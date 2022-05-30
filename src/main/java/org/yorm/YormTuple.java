package org.yorm;

import java.lang.reflect.Method;
import org.yorm.util.DbType;

public record YormTuple(String dbFieldName, String objectFieldName, DbType type,
                        int size, boolean isNullable, boolean isPrimaryKey, Boolean isAutoincrement, Method method) {

}
