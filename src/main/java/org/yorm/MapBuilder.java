package org.yorm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yorm.db.operations.FilterPredicates;
import org.yorm.exception.YormException;
import org.yorm.util.DbType;
import org.yorm.util.Levenshtein;

public class MapBuilder {

    private final DataSource ds;
    private static Logger logger = LoggerFactory.getLogger(MapBuilder.class);

    public MapBuilder(DataSource ds) {
        this.ds = ds;
    }

    public <T extends Record> YormTable buildMap(Class<T> recordClass) throws YormException {
        String dbTable = recordClass.getSimpleName().toLowerCase(Locale.ROOT);
        Field[] objectFields = recordClass.getDeclaredFields();
        List<Method> methods = Arrays.asList(recordClass.getMethods());
        List<YormTuple> tuples;
        try (Connection connection = ds.getConnection()) {
            List<Description> descriptionList = getDescription(connection, dbTable);
            tuples = populateMap(objectFields, descriptionList, methods);
        } catch (SQLException | YormException e) {
            throw new YormException("Error mapping record " + dbTable, e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Record:{} mapped to table:{}", recordClass.getName(), dbTable);
            for (var tuple : tuples) {
                logger.debug("  Field:{} mapped to column:{} type:{} nullable:{} primaryKey:{} autoIncrement:{}",
                    tuple.objectName(), tuple.dbFieldName(), tuple.type(), tuple.isNullable(), tuple.isPrimaryKey(), tuple.isAutoincrement());
            }
        }
        Constructor<Record> constructor = (Constructor<Record>) recordClass.getConstructors()[0];
        return new YormTable(dbTable, tuples, constructor);
    }

    private List<YormTuple> populateMap(Field[] objectFields, List<Description> descriptionList, List<Method> methods) throws YormException {
        List<YormTuple> tuples = new ArrayList<>();
        Set<String> alreadyUsedObjectFields = new HashSet<>();
        for (Description description : descriptionList) {
            String objectField = findClosest(objectFields, description.columnName());
            if (alreadyUsedObjectFields.contains(objectField)) {
                throw new YormException("Mismatch mapping methods to database with field " + description.columnName());
            }
            Optional<Method> methodOptional = methods.stream().filter(FilterPredicates.getMethod(objectField)).findFirst();
            if (methodOptional.isEmpty()) {
                throw new YormException("Couldn't find method that matches object field " + objectField);
            }
            alreadyUsedObjectFields.add(objectField);
            var tuple = new YormTuple(description.columnName(), objectField, DbType.getType(description.type()),
                Integer.parseInt(description.size()), yesNoToBoolean(description.isNullable()),
                description.isPrimaryKey(), yesNoToBoolean(description.isAutoincrement()), methodOptional.get());
            tuples.add(tuple);
        }
        return tuples;
    }

    private Boolean yesNoToBoolean(String str) throws YormException {
        if (str == null || str.isBlank()) {
            return null;
        }
        if ("YES".equalsIgnoreCase(str)) {
            return true;
        }
        if ("NO".equalsIgnoreCase(str)) {
            return false;
        }
        throw new YormException("Invalid value " + str + ". Must be YES or NO or null or blank");

    }

    private List<Description> getDescription(Connection connection, String table) throws SQLException {
        List<Description> descriptionList = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        List<String> primaryKeysColumns = new ArrayList<>();
        try (ResultSet rsColumn = metaData.getPrimaryKeys(null, null, table)) {
            while (rsColumn.next()) {
                primaryKeysColumns.add(rsColumn.getString("COLUMN_NAME"));
            }
        }
        try (ResultSet rsColumn = metaData.getColumns(null, null, table, null)) {
            while (rsColumn.next()) {
                String columnName = rsColumn.getString("COLUMN_NAME");
                String type = rsColumn.getString("DATA_TYPE");
                String size = rsColumn.getString("COLUMN_SIZE");
                String isNull = rsColumn.getString("IS_NULLABLE");
                String isAutoincrement = rsColumn.getString("IS_AUTOINCREMENT");
                boolean isPrimaryKey = primaryKeysColumns.contains(columnName);
                descriptionList.add(new Description(columnName, type, size, isNull, isPrimaryKey, isAutoincrement));
            }
        } catch (SQLException e) {
            logger.error("Error mapping table {}", table, e);
        }
        return descriptionList;
    }


    private record Description(String columnName, String type, String size, String isNullable, Boolean isPrimaryKey, String isAutoincrement) {

    }

    private String findClosest(Field[] fields, String fieldName) {
        String closest = null;
        int distance = 100;
        for (Field field : fields) {
            int tempDist = Levenshtein.calculate(cleanName(field.getName()), cleanName(fieldName));
            if (tempDist < distance) {
                closest = field.getName();
                distance = tempDist;
            }
        }
        return closest;
    }

    private String cleanName(String str) {
        return str.toLowerCase(Locale.ROOT).replace("_", "");
    }

}

