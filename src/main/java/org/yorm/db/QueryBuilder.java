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
import org.yorm.db.operations.WhereOperator;
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
            long id = (long) yormTuple.method().invoke(obj);
            if (id == 0) {
                idInsert = QuerySave.insert(ds, obj, yormTable);
            } else {
                QuerySave.update(ds, obj, yormTable);
            }
        }
        return idInsert;
    }

    public boolean delete(DataSource ds, YormTable yormTable, long id) throws YormException {
        return QueryDelete.delete(ds, yormTable, id);
    }

    public <T extends Record> T get(DataSource ds, YormTable yormTable, long id) throws YormException {
        return QueryFind.findById(ds, yormTable, id);
    }

    public <T extends Record> List<T> get(DataSource ds, Record filterObject, YormTable yormTableFilter, YormTable yormTableObject)
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
        long id = (long) optionalFilteringTupleId.get().method().invoke(filterObject);
        return QueryFind.findByForeignId(ds, yormTableObject, foreignKey, id);
    }

    public <T extends Record> List<T> get(DataSource ds, YormTable yormTable) throws YormException {
        return QueryFind.findAll(ds, yormTable);
    }

    public <T extends Record> List<T> get(DataSource ds, List<T> list, YormTable yormTable) throws InvocationTargetException, IllegalAccessException, YormException {
        List<FieldValue> fieldValueList = getFieldValues(list, yormTable);
        return QueryFind.findFiltering(ds, yormTable, fieldValueList);
    }

    private <T extends Record> List<FieldValue> getFieldValues(List<T> list, YormTable yormTable) throws IllegalAccessException, InvocationTargetException, YormException {
        List<FieldValue> fieldValueList = new ArrayList<>();
        List<YormTuple> yormTuples = yormTable.tuples();
        for (Record obj : list) {
            for (YormTuple yormTuple : yormTuples) {
                Method method = yormTuple.method();
                Object value = method.invoke(obj);
                if (value != null) {
                    mapValues(fieldValueList, yormTuple, value);
                }
            }
        }
        return fieldValueList;
    }

    private void mapValues(List<FieldValue> fieldValueList, YormTuple yormTuple, Object value) throws YormException {
        String dbFieldName = yormTuple.dbFieldName();
        DbType type = yormTuple.type();
        switch (type) {
            case VARCHAR, CHAR -> {
                String valueStr = (String) value;
                if (!valueStr.isEmpty()) {
                    fieldValueList.add(new FieldValue(dbFieldName, type, "%" + valueStr + "%", WhereOperator.LIKE));
                }
            }
            case SMALLINT, INTEGER, BIT -> {
                if(value instanceof Integer) {
                    int valueInt = (int) value;
                    boolean idWithoutValue = dbFieldName.contains("id") && valueInt < 1;
                    if (!idWithoutValue) {
                        fieldValueList.add(new FieldValue(dbFieldName, type, valueInt, WhereOperator.EQUALS));
                    }
                } else if (value instanceof Long) {
                    long valueLong = (long) value;
                    boolean idWithoutValue = dbFieldName.contains("id") && valueLong < 1;
                    if (!idWithoutValue) {
                        fieldValueList.add(new FieldValue(dbFieldName, type, valueLong, WhereOperator.EQUALS));
                    }
                } else {
                    throw new YormException("Incompatible value:" + value + " of class:" + value.getClass().getName() + " for column type:" + type);
                }
            }
            case BIGINT -> {
                long valueLong = (long) value;
                boolean idLongWithoutValue = dbFieldName.contains("id") && valueLong < 1;
                if (!idLongWithoutValue) {
                    fieldValueList.add(new FieldValue(dbFieldName, type, valueLong, WhereOperator.EQUALS));
                }
            }
            default -> fieldValueList.add(new FieldValue(dbFieldName, type, value, WhereOperator.EQUALS));
        }
    }

}
