package org.yorm.db.operations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.yorm.YormTable;
import org.yorm.YormTuple;
import org.yorm.exception.YormException;

public class QuerySave {

    private QuerySave() {
    }

    public static <T extends Record> void bulkInsert(DataSource ds, List<T> objList, YormTable yormTable) throws YormException {
        String op = "?,";
        List<YormTuple> tuples = yormTable.getTuples();
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(yormTable.getDbTable())
            .append(" (")
            .append(String.join(",", tuples.stream().map(YormTuple::dbFieldName).toList()))
            .append(") VALUES ");
        String operands = op.repeat(tuples.size());
        operands = operands.substring(0, operands.length() - 1);
        for (int k = 0; k < objList.size(); k++) {
            query.append("(").append(operands).append("),");
        }
        query.deleteCharAt(query.length() - 1);
        query.append(";");
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS)) {
            int paramIndex = 1;
            for (Record obj : objList) {
                paramIndex = populatePreparedStatement(tuples, paramIndex, preparedStatement, obj);
            }
            preparedStatement.executeUpdate();
        } catch (SQLException | IllegalAccessException | YormException | InvocationTargetException e) {
            throw new YormException(e.getMessage());
        }
    }

    public static int forceInsert(DataSource ds, Record obj, YormTable yormTable) throws YormException {
        String op = "?,";
        int id = 0;
        List<YormTuple> tuples = yormTable.getTuples();
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(yormTable.getDbTable())
            .append(" (")
            .append(String.join(",", tuples.stream().map(YormTuple::dbFieldName).toList()))
            .append(") VALUES (")
            .append(op.repeat(tuples.size()));
        query.deleteCharAt(query.length() - 1);
        query.append(")");
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS)) {
            int paramIndex = 1;
            populatePreparedStatement(tuples, paramIndex, preparedStatement, obj);
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getInt(1);
            }
        } catch (SQLException | IllegalAccessException | YormException | InvocationTargetException e) {
            throw new YormException(e.getMessage());
        }
        return id;
    }

    public static int insert(DataSource ds, Record obj, YormTable yormTable) throws YormException {
        String op = "?,";
        int id = 0;
        Predicate<YormTuple> predicateFilterOutPrimaryKeys = FilterPredicates.filterOutPrimaryKeys();
        List<YormTuple> tuples = yormTable.getTuples().stream().filter(predicateFilterOutPrimaryKeys).toList();
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(yormTable.getDbTable())
            .append(" (")
            .append(String.join(",", tuples.stream().map(YormTuple::dbFieldName).toList()))
            .append(") VALUES (")
            .append(op.repeat(tuples.size()));
        query.deleteCharAt(query.length() - 1);
        query.append(")");
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS)) {
            int paramIndex = 1;
            populatePreparedStatement(tuples, paramIndex, preparedStatement, obj);
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getInt(1);
            }
        } catch (SQLException | InvocationTargetException | IllegalAccessException | YormException e) {
            throw new YormException(e.getMessage());
        }
        return id;
    }

    public static void update(DataSource ds, Record obj, YormTable yormTable) throws YormException {
        String op = " = ?,";
        String op2 = "=? AND ";
        List<YormTuple> tuples = yormTable.getTuples();
        Predicate<YormTuple> predicateFilterKeepKeys = FilterPredicates.filterKeepKeys();
        List<YormTuple> keyTuples = tuples.stream().filter(predicateFilterKeepKeys).toList();
        StringBuilder query = new StringBuilder("UPDATE ");
        query.append(yormTable.getDbTable())
            .append(" SET ")
            .append(String.join(op, tuples.stream().map(YormTuple::dbFieldName).toList()))
            .append("=?");
        query.append(" WHERE ")
            .append(String.join(op2, keyTuples.stream().map(YormTuple::dbFieldName).toList()))
            .append("=?");
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            int paramIndex = 1;
            populatePreparedStatement(tuples, paramIndex, preparedStatement, obj);
            populatePreparedStatement(keyTuples, tuples.size() + 1, preparedStatement, obj);
            preparedStatement.executeUpdate();
        } catch (SQLException | IllegalAccessException | YormException | InvocationTargetException e) {
            throw new YormException(e.getMessage());
        }
    }

    private static int populatePreparedStatement(List<YormTuple> tuples, int paramIndex, PreparedStatement preparedStatement, Record obj)
        throws SQLException, IllegalAccessException, InvocationTargetException, YormException {
        for (YormTuple tuple : tuples) {
            Method method = tuple.method();
            switch (tuple.type()) {
                case TINYINT -> preparedStatement.setBoolean(paramIndex, (boolean) method.invoke(obj));
                case SMALLINT, MEDIUMINT, INT, INTEGER, BIT -> preparedStatement.setInt(paramIndex, (int) method.invoke(obj));
                case BIGINT -> preparedStatement.setLong(paramIndex, (long) method.invoke(obj));
                case VARCHAR, CHAR -> preparedStatement.setString(paramIndex, (String) method.invoke(obj));
                case DOUBLE -> preparedStatement.setDouble(paramIndex, (double) method.invoke(obj));
                case FLOAT -> preparedStatement.setFloat(paramIndex, (float) method.invoke(obj));
                case DECIMAL -> preparedStatement.setBigDecimal(paramIndex, (BigDecimal) method.invoke(obj));
                case DATE, DATETIME -> preparedStatement.setDate(paramIndex, Date.valueOf((LocalDate) method.invoke(obj)));
                case TIMESTAMP -> preparedStatement.setTimestamp(paramIndex, Timestamp.valueOf((LocalDateTime) method.invoke(obj)));
                default -> throw new YormException(String.format("Couldn't find type for %s", tuple.dbFieldName()));
            }
            paramIndex++;
        }
        return paramIndex;
    }

}
