package org.yorm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yorm.db.operations.FilterPredicates;
import org.yorm.exception.YormException;
import org.yorm.util.DbType;
import org.yorm.util.Levenshtein;

public class MapBuilder {

    DataSource ds;
    private static Logger logger = LoggerFactory.getLogger(MapBuilder.class);

    public MapBuilder(DataSource ds) {
        this.ds = ds;
    }

    public <T extends Record> YormTable buildMap(Class<T> recordObject) throws YormException {
        String recordName = recordObject.getSimpleName().toLowerCase(Locale.ROOT);
        Field[] objectFields = recordObject.getDeclaredFields();
        List<Method> methods = Arrays.asList(recordObject.getMethods());
        String query = "DESCRIBE " + recordName;
        List<YormTuple> tuples = new ArrayList<>();
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            List<Description> descriptionList = getDescription(preparedStatement);
            tuples = populateMap(objectFields, descriptionList, methods);
        } catch (SQLException | YormException e) {
            logger.error(e.getMessage());
            throw new YormException("Error mapping record " + recordName);
        }
        return new YormTable(recordName, tuples, (Constructor<Record>) recordObject.getConstructors()[0], concatenateFieldNames(tuples));
    }

    private List<YormTuple> populateMap(Field[] objectFields, List<Description> descriptionList, List<Method> methods) throws YormException {
        List<YormTuple> tuples = new ArrayList<>();
        Set<String> alreadyUsedObjectFields = new HashSet<>();
        for (Description description : descriptionList) {
            String objectField = findClosest(objectFields, description.fieldName());
            if (alreadyUsedObjectFields.contains(objectField)) {
                throw new YormException("Mismatch mapping methods to database with field " + description.fieldName());
            }
            Optional<Method> methodOptional = methods.stream().filter(FilterPredicates.getMethod(objectField)).findFirst();
            if (methodOptional.isEmpty()) {
                throw new YormException("Couldn't find method that matches object field " + objectField);
            }
            alreadyUsedObjectFields.add(objectField);
            var tuple = new YormTuple(description.fieldName(), objectField, DbType.getType(description.type()),
                getSize(description.type()), getNullable(description.isNull()),
                description.key(), description.defaultValue(), description.extra(), methodOptional.get());
            tuples.add(tuple);
        }
        return tuples;
    }

    private List<Description> getDescription(PreparedStatement preparedStatement) {
        List<Description> descriptionList = new ArrayList<>();
        try (ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                String fieldName = rs.getString(1);
                String type = rs.getString(2);
                String isNull = rs.getString(3);
                String key = rs.getString(4);
                String defaultValue = rs.getString(5);
                String extra = rs.getString(6);
                descriptionList.add(new Description(fieldName, type, isNull, key, defaultValue, extra));
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return descriptionList;
    }

    private record Description(String fieldName, String type, String isNull, String key, String defaultValue, String extra) {

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

    private String concatenateFieldNames(List<YormTuple> tuples) {
        return String.join(",", tuples.stream().map(YormTuple::dbFieldName).toList());
    }
}

