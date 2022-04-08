package org.yorm.db.operations.select;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.sql.DataSource;
import org.yorm.YormTable;
import org.yorm.YormTuple;
import org.yorm.db.FilteringFieldValue;
import org.yorm.db.operations.QueryFind;
import org.yorm.db.operations.operators.ComparisonOperator;
import org.yorm.db.operations.operators.WhereOperator;
import org.yorm.exception.YormException;

public class Select<T extends Record> {

    private YormTable<T> yormTable;
    private List<FilteringFieldValue> list = new ArrayList<>();
    private DataSource ds;

    public Select(DataSource ds, YormTable yormTable) {
        this.ds = ds;
        this.yormTable = yormTable;
    }

    public <U> SelectComparison<T, U> where(Function<T, U> getter) throws YormException {
        return where(getter, WhereOperator.EMPTY);
    }

    public <U> SelectComparison<T, U> and(Function<T, U> getter) throws YormException {
        return where(getter, WhereOperator.AND);
    }

    public <U> SelectComparison<T, U> or(Function<T, U> getter) throws YormException {
        return where(getter, WhereOperator.OR);
    }

    public List<T> find() throws YormException {
        return QueryFind.findFiltering(ds, yormTable, list);
    }


    private <U> SelectComparison<T, U> where(Function<T, U> getter, WhereOperator whereOperator)
        throws YormException {
        String getterName = this.yormTable.reflectionUtil().getFunctionName(getter);
        YormTuple currentTuple = getTuple(getterName);
        return new SelectComparison<>() {
            public Select<T> equalTo(U value) {
                FilteringFieldValue filteringFieldValue = new FilteringFieldValue(currentTuple.dbFieldName(), currentTuple.type(), value, ComparisonOperator.EQUALS, whereOperator);
                list.add(filteringFieldValue);
                return Select.this;
            }

            public Select<T> greaterThan(U value) {
                FilteringFieldValue filteringFieldValue = new FilteringFieldValue(currentTuple.dbFieldName(), currentTuple.type(), value, ComparisonOperator.GREATER_THAN, whereOperator);
                list.add(filteringFieldValue);
                return Select.this;
            }

            public Select<T> lessThan(U value) {
                FilteringFieldValue filteringFieldValue = new FilteringFieldValue(currentTuple.dbFieldName(), currentTuple.type(), value, ComparisonOperator.LESS_THAN, whereOperator);
                list.add(filteringFieldValue);
                return Select.this;
            }

            public Select<T> notEqualTo(U value) {
                FilteringFieldValue filteringFieldValue = new FilteringFieldValue(currentTuple.dbFieldName(), currentTuple.type(), value, ComparisonOperator.NOT_EQUALS,
                    whereOperator);
                list.add(filteringFieldValue);
                return Select.this;
            }

            public Select<T> like(U value) {
                String valueStr = "%" + value + "%";
                FilteringFieldValue filteringFieldValue = new FilteringFieldValue(currentTuple.dbFieldName(), currentTuple.type(), valueStr, ComparisonOperator.LIKE,
                    whereOperator);
                list.add(filteringFieldValue);
                return Select.this;
            }
        };
    }

    private YormTuple getTuple(String fieldName) throws YormException {
        return this.yormTable.tuples().stream().filter(tuple -> tuple.dbFieldName().equals(fieldName))
            .findFirst()
            .orElseThrow(() -> new YormException("Field not found: " + fieldName));
    }

    public interface SelectComparison<T extends Record, U> {

        Select<T> equalTo(U value);

        Select<T> notEqualTo(U value);

        Select<T> like(U value);

        Select<T> greaterThan(U value);

        Select<T> lessThan(U value);
    }
}
