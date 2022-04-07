package org.yorm;

import java.lang.reflect.Method;
import org.yorm.util.DbType;

public record YormTuple(String dbFieldName, String objectName, DbType type,
                        int size, boolean isNull, String key, String defaultValue,
                        String extra, boolean isAutoIncrement, Method method) {

}
