package org.yorm.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.yorm.YormTable;
import org.yorm.YormTuple;
import org.yorm.db.operations.FilterPredicates;
import org.yorm.db.operations.QueryDelete;
import org.yorm.db.operations.QueryFind;
import org.yorm.db.operations.QuerySave;
import org.yorm.db.operations.operators.ComparisonOperator;
import org.yorm.db.operations.operators.WhereOperator;
import org.yorm.exception.YormException;
import org.yorm.util.DbType;

public class QueryBuilder {

    DataSource ds;

    public QueryBuilder(DataSource ds) {
        this.ds = ds;
    }

    public long insert(DataSource ds, Record obj, YormTable yormTable) throws YormException {
        return QuerySave.forceInsert(ds, obj, yormTable);
    }

    public void update(DataSource ds, Record obj, YormTable yormTable) throws YormException {
        QuerySave.update(ds, obj, yormTable);
    }

    public <T extends Record> void bulkInsert(DataSource ds, List<T> list, YormTable yormTable) throws YormException {
        QuerySave.bulkInsert(ds, list, yormTable);
    }

    public long save(DataSource ds, Record obj, YormTable yormTable) throws InvocationTargetException, IllegalAccessException, YormException {
        Optional<YormTuple> idField = yormTable.tuples().stream().filter(FilterPredicates.filterAutoIncrementKey()).findFirst();
        long idInsert = 0;
        if (idField.isEmpty()) {
            idInsert = QuerySave.forceInsert(ds, obj, yormTable);
        } else {
            YormTuple yormTuple = idField.get();
            if (emptyId(obj, yormTuple)) {
                idInsert = QuerySave.insert(ds, obj, yormTable);
            } else {
                QuerySave.update(ds, obj, yormTable);
            }
        }
        return idInsert;
    }

    private boolean emptyId(Record obj, YormTuple yormTuple) throws InvocationTargetException, IllegalAccessException {
        Object idObject = yormTuple.method().invoke(obj);
        if (idObject == null) {
            return true;
        }
        if (yormTuple.method().getReturnType().getName().equalsIgnoreCase("long")) {
            return (long) idObject == 0;
        }
        if (yormTuple.method().getReturnType().getName().equalsIgnoreCase("int")) {
            return (int) idObject == 0;
        }
        return false;
    }

    public boolean delete(DataSource ds, YormTable yormTable, long id) throws YormException {
        return QueryDelete.delete(ds, yormTable, id);
    }

    public <T extends Record> T find(DataSource ds, YormTable yormTable, long id) throws YormException {
        return QueryFind.findById(ds, yormTable, id);
    }

    public <T extends Record> List<T> find(DataSource ds, Record filterObject, YormTable yormTableFilter, YormTable yormTableObject)
        throws InvocationTargetException, IllegalAccessException, YormException {
        String foreignKeyFilter = yormTableFilter.dbTable() + "_id";
        String foreignKey = yormTableFilter.dbTable() + "_id";
        Optional<YormTuple> yormTuple = yormTableObject.tuples().stream().filter(t -> t.dbFieldName().equalsIgnoreCase(foreignKeyFilter)).findFirst();
        if (yormTuple.isEmpty()) {
            String foreignKeySecondAttempt = "id_" + yormTableFilter.dbTable();
            yormTuple = yormTableObject.tuples().stream().filter(t -> t.dbFieldName().equalsIgnoreCase(foreignKeySecondAttempt)).findFirst();
            foreignKey = foreignKeySecondAttempt;
        }
        if (yormTuple.isEmpty()) {
            return new ArrayList<>();
        }
        Optional<YormTuple> optionalFilteringTupleId = yormTableFilter.tuples().stream().filter(FilterPredicates.getId()).findFirst();
        if (optionalFilteringTupleId.isEmpty()) {
            return new ArrayList<>();
        }
        int id = (int) optionalFilteringTupleId.get().method().invoke(filterObject);
        return QueryFind.findByForeignId(ds, yormTableObject, foreignKey, id);
    }

    public <T extends Record> List<T> find(DataSource ds, YormTable yormTable) throws YormException {
        return QueryFind.findAll(ds, yormTable);
    }

    public <T extends Record> List<T> find(DataSource ds, List<T> list, YormTable yormTable, WhereOperator whereOperator)
        throws InvocationTargetException, IllegalAccessException, YormException {
        List<FilteringFieldValue> filteringFieldValueList = getFieldValues(list, yormTable, whereOperator);
        return QueryFind.findFiltering(ds, yormTable, filteringFieldValueList);
    }

    public <T extends Record> List<T> find(DataSource ds, List<T> list, YormTable yormTable) throws InvocationTargetException, IllegalAccessException, YormException {
        List<FilteringFieldValue> filteringFieldValueList = getFieldValues(list, yormTable, WhereOperator.OR);
        return QueryFind.findFiltering(ds, yormTable, filteringFieldValueList);
    }

    private <T extends Record> List<FilteringFieldValue> getFieldValues(List<T> list, YormTable yormTable, WhereOperator whereOperator)
        throws IllegalAccessException, InvocationTargetException {
        List<FilteringFieldValue> filteringFieldValueList = new ArrayList<>();
        List<YormTuple> yormTuples = yormTable.tuples();
        for (Record obj : list) {
            for (YormTuple yormTuple : yormTuples) {
                Method method = yormTuple.method();
                Object value = method.invoke(obj);
                if (value != null) {
                    mapValues(filteringFieldValueList, yormTuple, value, whereOperator);
                }
            }
        }
        return filteringFieldValueList;
    }

    private void mapValues(List<FilteringFieldValue> filteringFieldValueList, YormTuple yormTuple, Object value, WhereOperator whereOperator) {
        String dbFieldName = yormTuple.dbFieldName();
        DbType type = yormTuple.type();
        if (type.equals(DbType.CHAR) || type.equals(DbType.VARCHAR)) {
            String valueStr = (String) value;
            if (!valueStr.isEmpty()) {
                filteringFieldValueList.add(new FilteringFieldValue(dbFieldName, type, "%" + valueStr + "%", ComparisonOperator.LIKE, whereOperator));
            }
        } else {
            filteringFieldValueList.add(new FilteringFieldValue(dbFieldName, type, value, ComparisonOperator.EQUALS, whereOperator));
        }
    }
}


