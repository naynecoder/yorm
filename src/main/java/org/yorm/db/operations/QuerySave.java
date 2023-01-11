package org.yorm.db.operations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.function.Predicate;
import javax.sql.DataSource;
import org.yorm.YormTable;
import org.yorm.YormTuple;
import org.yorm.exception.YormException;
import org.yorm.util.DbType;
import org.yorm.util.RowRecordConverter;

public class QuerySave {

    private static final RowRecordConverter rowRecordConverter = new RowRecordConverter();

    private QuerySave() {
    }

    public static <T extends Record> void bulkInsert(DataSource ds, List<T> objList, YormTable yormTable) throws YormException {
        String op = "?,";
        List<YormTuple> tuples = yormTable.tuples();
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(yormTable.dbTable())
            .append(" (")
            .append(yormTable.concatenatedFieldNames())
            .append(") VALUES ");
        String operands = op.repeat(tuples.size());
        operands = operands.substring(0, operands.length() - 1);
        for (int k = 0; k < objList.size(); k++) {
            query.append("(").append(operands).append("),");
        }
        query.deleteCharAt(query.length() - 1);
        query.append(";");
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString(),
                yormTable.hasPrimaryKey() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            int paramIndex = 1;
            for (Record obj : objList) {
                paramIndex = populatePreparedStatement(tuples, paramIndex, preparedStatement, obj);
            }
            preparedStatement.executeUpdate();
        } catch (SQLException | IllegalAccessException | InvocationTargetException e) {
            throw new YormException("Error while bulk inserting records:" + objList + " into table:" + yormTable.dbTable(), e);
        }
    }

    public static long forceInsert(DataSource ds, Record obj, YormTable yormTable) throws YormException {
        String op = "?,";
        long id = 0;
        List<YormTuple> tuples = yormTable.tuples();
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(yormTable.dbTable())
            .append(" (")
            .append(yormTable.concatenatedFieldNames())
            .append(") VALUES (")
            .append(op.repeat(tuples.size()));
        query.deleteCharAt(query.length() - 1);
        query.append(")");
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString(),
                yormTable.hasPrimaryKey() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            int paramIndex = 1;
            populatePreparedStatement(tuples, paramIndex, preparedStatement, obj);
            preparedStatement.executeUpdate();
            if (yormTable.hasPrimaryKey()) {
                ResultSet rs = preparedStatement.getGeneratedKeys();
                if (rs.next()) {
                    id = rs.getInt(1);
                }
            }
        } catch (SQLException | IllegalAccessException | InvocationTargetException e) {
            throw new YormException("Error while force inserting record:" + obj + " into table:" + yormTable.dbTable(), e);
        }
        return id;
    }

    public static long insert(DataSource ds, Record obj, YormTable yormTable) throws YormException {
        String op = "?,";
        long id = 0;
        Predicate<YormTuple> predicateFilterOutPrimaryKeys = FilterPredicates.filterOutPrimaryKeys();
        List<YormTuple> tuples = yormTable.tuples().stream().filter(predicateFilterOutPrimaryKeys).toList();
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(yormTable.dbTable())
            .append(" (")
            .append(String.join(",", tuples.stream().map(YormTuple::dbFieldName).toList()))
            .append(") VALUES (")
            .append(op.repeat(tuples.size()));
        query.deleteCharAt(query.length() - 1);
        query.append(")");
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query.toString(),
                yormTable.hasPrimaryKey() ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            int paramIndex = 1;
            populatePreparedStatement(tuples, paramIndex, preparedStatement, obj);
            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                id = rs.getInt(1);
            }
        } catch (SQLException | InvocationTargetException | IllegalAccessException e) {
            throw new YormException("Error while inserting record:" + obj + " into table:" + yormTable.dbTable(), e);
        }
        return id;
    }

    public static void update(DataSource ds, Record obj, YormTable yormTable) throws YormException {
        String op = " = ?,";
        String op2 = "=? AND ";
        List<YormTuple> tuples = yormTable.tuples();
        Predicate<YormTuple> predicateFilterKeepKeys = FilterPredicates.filterKeepKeys();
        List<YormTuple> keyTuples = tuples.stream().filter(predicateFilterKeepKeys).toList();
        StringBuilder query = new StringBuilder("UPDATE ");
        query.append(yormTable.dbTable())
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
        } catch (SQLException | IllegalAccessException | InvocationTargetException e) {
            throw new YormException("Error while updating record:" + obj + " in table:" + yormTable.dbTable(), e);
        }
    }

    private static int populatePreparedStatement(List<YormTuple> tuples, int paramIndex, PreparedStatement preparedStatement, Record obj)
        throws SQLException, IllegalAccessException, InvocationTargetException, YormException {
        for (YormTuple tuple : tuples) {
            Method method = tuple.method();
            final Object value = method.invoke(obj);
            final DbType type = tuple.type();
            rowRecordConverter.recordToRow(paramIndex, preparedStatement, tuple.dbFieldName(), value, type);
            paramIndex++;
        }
        return paramIndex;
    }


}
