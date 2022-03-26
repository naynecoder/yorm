package org.yorm.db.operations;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.yorm.YormTable;
import org.yorm.YormTuple;
import org.yorm.db.FieldValue;
import org.yorm.exception.YormException;
import org.yorm.util.DbType;

public class QueryGet {

    private QueryGet() {
    }

    public static <T extends Record> List<T> getAll(DataSource ds, YormTable yormTable) throws YormException {
        List<YormTuple> tuples = yormTable.tuples();
        StringBuilder query = new StringBuilder("SELECT ");
        List<T> resultList = new ArrayList<>();
        query.append(String.join(",", tuples.stream().map(YormTuple::dbFieldName).toList()))
            .append(" FROM ")
            .append(yormTable.dbTable());
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                Object[] values = new Object[tuples.size()];
                int params = 0;
                loopResults(tuples, rs, values, params);
                resultList.add((T) yormTable.constructor().newInstance(values));
            }
        } catch (SQLException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new YormException("Error while getting all records from table:" + yormTable.dbTable(), e);
        }
        return resultList;
    }

    public static <T extends Record> T getById(DataSource ds, YormTable yormTable, int id) throws YormException {
        List<YormTuple> tuples = yormTable.tuples();
        StringBuilder query = new StringBuilder("SELECT ");
        Object result = null;
        query.append(String.join(",", tuples.stream().map(YormTuple::dbFieldName).toList()))
            .append(" FROM ")
            .append(yormTable.dbTable())
            .append(" WHERE id=?");
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                Object[] values = new Object[tuples.size()];
                int params = 0;
                loopResults(tuples, rs, values, params);
                result = yormTable.constructor().newInstance(values);
            }


        } catch (SQLException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new YormException("Error while getting record with id:" + id + " from table:" + yormTable.dbTable(), e);
        }
        return (T) result;
    }

    public static <T extends Record> List<T> getByForeignId(DataSource ds, YormTable yormTable, String fieldName, int id) throws YormException {
        List<T> resultList = new ArrayList<>();
        List<YormTuple> tuples = yormTable.tuples();
        StringBuilder query = new StringBuilder("SELECT ");
        query.append(String.join(",", tuples.stream().map(YormTuple::dbFieldName).toList()))
            .append(" FROM ")
            .append(yormTable.dbTable())
            .append(" WHERE " + fieldName + "=?");
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                Object[] values = new Object[tuples.size()];
                int params = 0;
                loopResults(tuples, rs, values, params);
                Object result = yormTable.constructor().newInstance(values);
                resultList.add((T) result);
            }


        } catch (SQLException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new YormException("Error while deleting record with foreign id:" + id + " from table:" + yormTable.dbTable(), e);
        }
        return resultList;
    }

    public static <T extends Record> List<T> getFiltering(DataSource ds, YormTable yormTable, List<FieldValue> filteringList) throws YormException {
        String op = " ? OR";
        List<T> resultList = new ArrayList<>();
        List<YormTuple> tuples = yormTable.tuples();
        StringBuilder query = new StringBuilder("SELECT ");
        query.append(String.join(",", tuples.stream().map(YormTuple::dbFieldName).toList()))
            .append(" FROM ")
            .append(yormTable.dbTable());
        if (!filteringList.isEmpty()) {
            query.append(" WHERE ")
                .append(String.join(" ", filteringList.stream().map(fv -> fv.fieldName() + " " + fv.whereOperator().getOperor() + op).toList()));
            query.deleteCharAt(query.length() - 1);
            query.deleteCharAt(query.length() - 1);
        }
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            int paramIndex = 1;
            for (FieldValue fieldValue : filteringList) {
                Object obj = fieldValue.value();
                DbType type = fieldValue.dbType();
                switch (type) {
                    case TINYINT -> preparedStatement.setBoolean(paramIndex, (boolean) obj);
                    case SMALLINT, MEDIUMINT, INT, INTEGER, BIT -> preparedStatement.setInt(paramIndex, (int) obj);
                    case BIGINT -> preparedStatement.setLong(paramIndex, (long) obj);
                    case VARCHAR, CHAR -> preparedStatement.setString(paramIndex, (String) obj);
                    case DOUBLE -> preparedStatement.setDouble(paramIndex, (double) obj);
                    case FLOAT -> preparedStatement.setFloat(paramIndex, (float) obj);
                    case DECIMAL -> preparedStatement.setBigDecimal(paramIndex, (BigDecimal) obj);
                    case DATE -> preparedStatement.setDate(paramIndex, Date.valueOf((LocalDate) obj));
                    case DATETIME, TIMESTAMP -> preparedStatement.setTimestamp(paramIndex, Timestamp.valueOf((LocalDateTime) obj));
                    default -> throw new YormException("Couldn't find type for " + fieldValue.fieldName());
                }
                paramIndex++;
            }
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                Object[] values = new Object[tuples.size()];
                int params = 0;
                loopResults(tuples, rs, values, params);
                Object result = yormTable.constructor().newInstance(values);
                resultList.add((T) result);
            }
        } catch (SQLException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new YormException("Error while filtering records with filtering list:" + filteringList + " from table:" + yormTable.dbTable(), e);
        }
        return resultList;
    }

    private static void loopResults(List<YormTuple> tuples, ResultSet rs, Object[] values, int params) throws SQLException, YormException {
        for (YormTuple tuple : tuples) {
            String tableFieldName = tuple.dbFieldName();
            switch (tuple.type()) {
                case TINYINT:
                    boolean tiny = rs.getBoolean(tableFieldName);
                    values[params++] = tiny;
                    break;
                case SMALLINT, MEDIUMINT, INT, INTEGER, BIT:
                    int ii = rs.getInt(tableFieldName);
                    values[params++] = ii;
                    break;
                case BIGINT:
                    long ll = rs.getLong(tableFieldName);
                    values[params++] = ll;
                    break;
                case VARCHAR, CHAR:
                    String str = rs.getString(tableFieldName);
                    values[params++] = str;
                    break;
                case DOUBLE:
                    double dd = rs.getDouble(tableFieldName);
                    values[params++] = dd;
                    break;
                case FLOAT:
                    float ff = rs.getFloat(tableFieldName);
                    values[params++] = ff;
                    break;
                case DECIMAL:
                    BigDecimal bb = rs.getBigDecimal(tableFieldName);
                    values[params++] = bb;
                    break;
                case DATE:
                    Date date = rs.getDate(tableFieldName);
                    values[params++] = date.toLocalDate();
                    break;
                case TIMESTAMP, DATETIME:
                    Timestamp ts = rs.getTimestamp(tableFieldName);
                    values[params++] = ts.toLocalDateTime();
                    break;
                default:
                    throw new YormException("Couldn't find type for " + tuple.dbFieldName());
            }
        }
    }

}
