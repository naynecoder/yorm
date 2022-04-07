package org.yorm.db.operations;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import org.yorm.YormTuple;

public class FilterPredicates {

    private FilterPredicates(){}

    public static Predicate<YormTuple> getId() {
        return yt -> yt.isPrimaryKey() && yt.dbFieldName().equalsIgnoreCase("id");
    }

    public static Predicate<YormTuple> filterAutoIncrementKey() {
        return yt -> yt.isPrimaryKey() && yt.isAutoincrement();
    }

    public static Predicate<Method> getMethod(String fieldName) {
        return m -> m.getName().equalsIgnoreCase(fieldName);
    }

    public static Predicate<YormTuple> filterOutPrimaryKeys() {
        return yt -> !yt.isPrimaryKey();
    }

    public static Predicate<YormTuple> filterKeepKeys() {
        return YormTuple::isPrimaryKey;
    }

}
