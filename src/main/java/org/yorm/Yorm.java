package org.yorm;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.yorm.db.QueryBuilder;
import org.yorm.exception.YormException;

public class Yorm {

    private final Map<String, YormTable> map = new HashMap<>();
    private final MapBuilder mapBuilder;
    private final QueryBuilder queryBuilder;
    private final DataSource ds;

    public Yorm(DataSource ds) {
        this.ds = ds;
        this.mapBuilder = new MapBuilder(ds);
        this.queryBuilder = new QueryBuilder(ds);
    }

    public long save(Record recordObj) throws YormException {
        String objectName = getRecordName(recordObj);
        YormTable yormTable = getTable(objectName, recordObj.getClass());
        long result = 0;
        try {
            result = queryBuilder.save(ds, recordObj, yormTable);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new YormException("Error while saving record:" + recordObj, e);
        }
        return result;
    }

    public long insert(Record recordObj) throws YormException {
        String objectName = getRecordName(recordObj);
        YormTable yormTable = getTable(objectName, recordObj.getClass());
        return queryBuilder.insert(ds, recordObj, yormTable);
    }

    public <T extends Record> void insert(List<T> recordListObj) throws YormException {
        if (recordListObj.isEmpty()) {
            return;
        }
        Record recordObj = recordListObj.get(0);
        String objectName = getRecordName(recordObj);
        YormTable yormTable = getTable(objectName, recordObj.getClass());
        queryBuilder.bulkInsert(ds, recordListObj, yormTable);

    }

    public void update(Record recordObj) throws YormException {
        String objectName = getRecordName(recordObj);
        YormTable yormTable = getTable(objectName, recordObj.getClass());
        queryBuilder.update(ds, recordObj, yormTable);
    }

    public <T extends Record> T find(Class<T> recordObject, long id) throws YormException {
        String objectName = getClassName(recordObject);
        YormTable yormTable = getTable(objectName, recordObject);
        return queryBuilder.get(ds, yormTable, id);
    }

    public <T extends Record> List<T> find(Class<T> referenceObject, Record filterObject) throws YormException {
        String filterObjectName = getRecordName(filterObject);
        String referenceObjectName = getClassName(referenceObject);
        YormTable yormTableFilter = getTable(filterObjectName, filterObject.getClass());
        YormTable yormTableObject = getTable(referenceObjectName, referenceObject);
        List<T> result;
        try {
            result = queryBuilder.get(ds, filterObject, yormTableFilter, yormTableObject);
        } catch (InvocationTargetException | IllegalAccessException | YormException e) {
            throw new YormException("Error while finding records with reference:" + referenceObject + " and filter:" + filterObject, e);
        }
        return result;
    }

    public <T extends Record> List<T> find(Class<T> referenceObject) throws YormException {
        String referenceObjectName = getClassName(referenceObject);
        YormTable yormTable = getTable(referenceObjectName, referenceObject);
        return queryBuilder.get(ds, yormTable);
    }

    public <T extends Record> List<T> find(List<T> list) throws YormException {
        List<T> result = new ArrayList<>();
        if (list.isEmpty()) {
            return result;
        }
        Record recordObj = list.get(0);
        String objectName = getRecordName(recordObj);
        YormTable yormTable = getTable(objectName, recordObj.getClass());
        try {
            result = queryBuilder.get(ds, list, yormTable);
        } catch (InvocationTargetException | IllegalAccessException | YormException e) {
            throw new YormException("Error while finding records with list", e);
        }
        return result;
    }

    private <T extends Record> String getClassName(Class<T> clazz) {
        return clazz.getSimpleName().toLowerCase(Locale.ROOT);
    }

    private String getRecordName(Record recordObj) {
        return recordObj.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    public <T extends Record> boolean delete(Class<T> recordObject, long id) throws YormException {
        String objectName = getClassName(recordObject);
        YormTable yormTable = getTable(objectName, recordObject);
        return queryBuilder.delete(ds, yormTable, id);
    }

    private <T extends Record> YormTable getTable(String objectName, Class<T> recordObject) throws YormException {
        YormTable yormTable = map.get(objectName);
        if (yormTable == null) {
            yormTable = mapBuilder.buildMap(recordObject);
        }
        map.put(objectName, yormTable);
        return yormTable;
    }

}
