package org.yorm.db.operations;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import org.yorm.YormTuple;

public class FilterPredicates {

    private FilterPredicates() {
    }

    public static Predicate<YormTuple> getId() {
        return yt -> yt.key() != null
            && yt.dbFieldName().equalsIgnoreCase("id");
    }

    public static Predicate<YormTuple> filterAutoIncrementKey() {
        return yt -> yt.key() != null
            && yt.key().equalsIgnoreCase("PRI")
            && yt.isAutoIncrement();
    }

    public static Predicate<Method> getMethod(String fieldName) {
        return m -> m.getName().equalsIgnoreCase(fieldName);
    }

    public static Predicate<YormTuple> filterOutPrimaryKeys() {
        return yt -> yt.key() == null
            || yt.key().isEmpty()
            || !(yt.key().equalsIgnoreCase("PRI") && yt.isAutoIncrement());
    }

    public static Predicate<YormTuple> filterKeepKeys() {
        return yt -> yt.key() != null && !yt.key().isEmpty();
    }

}
