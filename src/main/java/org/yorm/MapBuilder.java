package org.yorm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yorm.exception.YormException;
import org.yorm.util.DbType;
import org.yorm.util.Levenshtein;

import static org.yorm.util.RowRecordConverter.converterFor;

public class MapBuilder {

    private final DataSource ds;
    private static Logger logger = LoggerFactory.getLogger(MapBuilder.class);

    public MapBuilder(DataSource ds) {
        this.ds = ds;
    }

    public <T extends Record> YormTable buildMap(Class<T> recordClass) throws YormException {
        String recordClassName = recordClass.getSimpleName().toLowerCase(Locale.ROOT);
        String dbTable = recordClassName;
        Field[] objectFields = recordClass.getDeclaredFields();
        Map<String, Method> methods = Arrays.stream(recordClass.getDeclaredMethods())
                                            .collect(Collectors.toMap(
                                                    method -> method.getName().toLowerCase(),
                                                    method -> method
                                            ));
        List<YormTuple> tuples;
        try (Connection connection = ds.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            dbTable = getMatchingTableName(metaData, recordClassName);
            List<Description> descriptionList = getDescription(metaData, dbTable);
            tuples = populateMap(objectFields, descriptionList, methods);
        } catch (SQLException | YormException e) {
            throw new YormException("Error mapping record " + recordClassName, e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Record:{} mapped to table:{}", recordClass.getName(), dbTable);
            for (var tuple : tuples) {
                logger.debug("  Field:{} mapped to column:{} type:{} nullable:{} primaryKey:{} autoIncrement:{}",
                    tuple.objectFieldName(), tuple.dbFieldName(), tuple.type(), tuple.isNullable(), tuple.isPrimaryKey(), tuple.isAutoincrement());
            }
        }
        Constructor<Record> constructor = findMatchingConstructor(recordClass, tuples);
        tuples = sortParametersForConstructor(tuples, constructor);
        return new YormTable(dbTable, tuples, constructor);
    }

    private <T extends Record> Constructor<Record> findMatchingConstructor(Class<T> recordClass, List<YormTuple> tuples) throws YormException {
        Constructor<Record>[] constructors = (Constructor<Record>[]) recordClass.getConstructors();
        if (logger.isDebugEnabled()) {
            logger.debug("Found {} constructors for record:{}", constructors.length, recordClass.getName());
        }
        for (Constructor<Record> constructor : constructors) {
            Parameter[] params = constructor.getParameters();
            if (logger.isDebugEnabled()) {
                logger.debug("Found constructor that takes {} parameters. We need one that takes {}.", params.length, tuples.size());
            }
            if (params.length == tuples.size()) {
                return constructor;
            }
        }
        throw new YormException("Couldn't find a constructor that matches the number of fields in the record: " + recordClass.getName());
    }

    private List<YormTuple> sortParametersForConstructor(List<YormTuple> tuples, Constructor<Record> constructor) throws YormException {
        Parameter[] params = constructor.getParameters();
        List<YormTuple> sortedList = new ArrayList<>();
        for (Parameter param : params) {
            String name = param.getName();
            YormTuple mt = tuples.stream().filter(t -> t.objectFieldName().equals(name)).findFirst()
                .orElseThrow(() -> new YormException("Couldn't find a field name that matches the constructor: " + name));
            sortedList.add(mt);
        }
        return sortedList;
    }

    private String getMatchingTableName(DatabaseMetaData metaData, String recordClassName) throws SQLException {
        ResultSet resultSet = metaData.getTables(null, null, null, new String[]{"TABLE", "VIEW"});
        List<String> tables = new ArrayList<>();
        while (resultSet.next()) {
            tables.add(resultSet.getString("TABLE_NAME"));
        }
        return findClosest(tables, recordClassName);
    }

    private List<YormTuple> populateMap(Field[] objectFields, List<Description> descriptionList, Map<String, Method> methods) throws YormException {
        List<YormTuple> tuples = new ArrayList<>();
        Set<String> alreadyUsedObjectFields = new HashSet<>();
        if (logger.isDebugEnabled()) {
            logger.debug("Matching fields {} to columns: {}", objectFields, descriptionList);
        }
        for (Description description : descriptionList) {

            List<String> objectFieldNames = Arrays.stream(objectFields).map(Field::getName).toList();
            String objectField = findClosest(objectFieldNames, description.columnName());

            if (alreadyUsedObjectFields.contains(objectField)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Column named \"{}\" mapped to \"{}\", which is already used. Ignoring.", description.columnName(), objectField);
                }
                continue;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Column named \"{}\" mapped to \"{}\". Forming YormTuple.", description.columnName(), objectField);
            }

            Method method = methods.get(objectField.toLowerCase());

            alreadyUsedObjectFields.add(objectField);
            var tuple = new YormTuple(
                    description.columnName(),
                    objectField,
                    DbType.getType(description.type()),
                    Integer.parseInt(description.size()),
                    yesNoToBoolean(description.isNullable()),
                    description.isPrimaryKey(),
                    yesNoToBoolean(description.isAutoincrement()),
                    method,
                    converterFor(method.getReturnType(), DbType.getType(description.type()).javaType),
                    converterFor(DbType.getType(description.type()).javaType, method.getReturnType())
            );
            tuples.add(tuple);

        }
        return tuples;
    }

    private boolean yesNoToBoolean(String str) throws YormException {
        if (str == null || str.isBlank()) {
            return false;
        }
        if ("YES".equalsIgnoreCase(str)) {
            return true;
        }
        if ("NO".equalsIgnoreCase(str)) {
            return false;
        }
        throw new YormException("Invalid value " + str + ". Must be YES or NO or null or blank");

    }

    private List<Description> getDescription(DatabaseMetaData metaData, String table) throws SQLException {
        List<Description> descriptionList = new ArrayList<>();
        List<String> primaryKeysColumns = new ArrayList<>();
        try (ResultSet rsColumn = metaData.getPrimaryKeys(null, null, table)) {
            while (rsColumn.next()) {
                primaryKeysColumns.add(rsColumn.getString("COLUMN_NAME"));
            }
        }
        try (ResultSet rsColumn = metaData.getColumns(null, null, table, null)) {
            while (rsColumn.next()) {
                String columnName = rsColumn.getString("COLUMN_NAME");
                String typeName = rsColumn.getString("TYPE_NAME");
                String type;
                if ("ENUM".equals(typeName)) {
                    type = String.valueOf(Types.VARCHAR);
                } else {
                    type = rsColumn.getString("DATA_TYPE");
                }
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

    private String findClosest(List<String> candidates, String target) {
        String closest = null;
        int distance = 100;
        for (String field : candidates) {
            int tempDist = Levenshtein.calculate(cleanName(field), cleanName(target));
            if (logger.isDebugEnabled()) {
                logger.debug("Distance between \"{}\" and \"{}\" is {}. Distance to beat is {}.", field, target, tempDist, distance);
            }
            if (tempDist < distance) {
                closest = field;
                distance = tempDist;
            }
        }
        return closest;
    }

    private String cleanName(String str) {
        return str.toLowerCase(Locale.ROOT).replace("_", "");
    }

}

