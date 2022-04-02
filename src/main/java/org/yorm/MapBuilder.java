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
            logger.error(e.getMessage());
            throw new YormException("Error mapping record " + dbTable, e);
        }

        if(logger.isDebugEnabled()) {
            logger.debug("Record:{} mapped to table:{}", recordClass.getName(), dbTable);
            for (var tuple : tuples) {
                logger.debug("  Field:{} mapped to column:{} type:{} nullable:{}", tuple.objectName(), tuple.dbFieldName(), tuple.type(), tuple.isNull());
            }
        }



        return new YormTable(dbTable, tuples, (Constructor<Record>) recordClass.getConstructors()[0]);
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
                getSize(description.type()), getNullable(description.isNull()),
                description.key(), description.defaultValue(), description.extra(), methodOptional.get());
            tuples.add(tuple);
        }
        return tuples;
    }

    private List<Description> getDescription(Connection connection, String table) throws SQLException {
        List<Description> descriptionList = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        List<String> primaryKeysColumns = new ArrayList<>();
        try (ResultSet rsColumn = metaData.getPrimaryKeys(null, null, table)) {
            while (rsColumn.next()){
                primaryKeysColumns.add(rsColumn.getString("COLUMN_NAME"));
            }
        }

        List<String> uniqueIndexColumns = new ArrayList<>();
        List<String> nonUniqueIndexColumns = new ArrayList<>();
        try (ResultSet rsIndex = metaData.getIndexInfo(null, null, table, false, false)) {
            while (rsIndex.next()){
                String indexName = rsIndex.getString("INDEX_NAME");
                String columnName = rsIndex.getString("COLUMN_NAME");
                Boolean nonUnique = rsIndex.getBoolean("NON_UNIQUE");
                Short ordinalPosition = rsIndex.getShort("ORDINAL_POSITION");
                //trying to imitate mysql info from KEY in SHOW COLUMNS: https://dev.mysql.com/doc/refman/8.0/en/show-columns.html
                //basically:
                //Key: Whether the column is indexed:
                //If Key is empty, the column either is not indexed or is indexed only as a secondary column in a multiple-column, nonunique index.
                //If Key is PRI, the column is a PRIMARY KEY or is one of the columns in a multiple-column PRIMARY KEY.
                //If Key is UNI, the column is the first column of a UNIQUE index.
                //If Key is MUL, the column is the first column of a nonunique index in which multiple occurrences of a given value are permitted within the column.
                //If more than one of the Key values applies to a given column of a table, Key displays the one with the highest priority, in the order PRI, UNI, MUL.
                //
                //so I need information only about the first column of every index
                if(ordinalPosition == 1){
                    if(nonUnique){
                        nonUniqueIndexColumns.add(columnName);
                    }else{
                        uniqueIndexColumns.add(columnName);
                    }
                }

            }
        }

        try (ResultSet rsColumn = metaData.getColumns(null,null, table, null)) {
            while (rsColumn.next()) {
                String columnName = rsColumn.getString("COLUMN_NAME");
                String type = rsColumn.getString("DATA_TYPE");
                String size = rsColumn.getString("COLUMN_SIZE");
                String isNull = rsColumn.getString("IS_NULLABLE");
                String key = calculateKey(columnName, primaryKeysColumns, uniqueIndexColumns, nonUniqueIndexColumns);
                String defaultValue = rsColumn.getString(5);
                String extra = rsColumn.getString(6);
                descriptionList.add(new Description(columnName, type, size, isNull, key, defaultValue, extra));
            }
        } catch (SQLException e) {
            logger.error("Error mapping table {}", table, e);
        }
        return descriptionList;
    }

    private String calculateKey(String columnName, List<String> privateKeyColumns, List<String> uniqueIndexColumns, List<String> nonUniqueIndexColumns ){
        if(privateKeyColumns.contains(columnName)){
            return "PRI";
        }
        if(uniqueIndexColumns.contains(columnName)){
            return "UNI";
        }
        if(nonUniqueIndexColumns.contains(columnName)){
            return "MUL";
        }
        return "";
    }

    private record Description(String columnName, String type, String size, String isNull, String key, String defaultValue, String extra) {

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

    private int getSize(String type) {
        int pos = type.indexOf('(');
        if (pos == -1) {
            return -1;
        }
        int pos2 = type.indexOf(')');
        return Integer.parseInt(type.substring(pos + 1, pos2));
    }

    private boolean getNullable(String isNull) {
        return isNull.toLowerCase(Locale.ROOT).contains("null");
    }

}

