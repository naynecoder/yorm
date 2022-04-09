package org.yorm.db.operations;

import org.yorm.YormTable;
import org.yorm.YormTuple;
import org.yorm.db.FilteringFieldValue;
import org.yorm.exception.YormException;
import org.yorm.util.RowRecordConverter;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QueryFind {

    private static final RowRecordConverter rowRecordConverter = new RowRecordConverter();

    private QueryFind() {
    }

    public static <T extends Record> List<T> findAll(DataSource ds, YormTable yormTable) throws YormException {
        List<YormTuple> tuples = yormTable.tuples();
        List<T> resultList = new ArrayList<>();
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(yormTable.selectAllFromTable())) {
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

    public static <T extends Record> T findById(DataSource ds, YormTable yormTable, long id) throws YormException {
        List<YormTuple> tuples = yormTable.tuples();
        String query = yormTable.selectAllFromTable() + " WHERE ID = ?";
        Object result = null;
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, id);
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

    public static <T extends Record> List<T> findByForeignId(DataSource ds, YormTable yormTable, String fieldName, long id) throws YormException {
        List<T> resultList = new ArrayList<>();
        List<YormTuple> tuples = yormTable.tuples();
        String query = yormTable.selectAllFromTable() + " WHERE " + fieldName + " = ?";
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, id);
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

    public static <T extends Record> List<T> findFiltering(DataSource ds, YormTable yormTable, List<FilteringFieldValue> filteringList) throws YormException {
        List<T> resultList = new ArrayList<>();
        List<YormTuple> tuples = yormTable.tuples();
        StringBuilder query = new StringBuilder(yormTable.selectAllFromTable());
        if (!filteringList.isEmpty()) {
            query.append(" WHERE ")
                .append(String.join(" ",
                    filteringList.stream().map(fv -> fv.whereOperator().getOperator() + " " + fv.fieldName() + " " + fv.comparisonOperator().getOperator() + " ? ").toList()));
        }
        String completeQuery = cleanSql(query.toString());
        try (Connection connection = ds.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(completeQuery)) {
            int paramIndex = 1;
            for (FilteringFieldValue fieldValue : filteringList) {
                rowRecordConverter.recordToRow(paramIndex, preparedStatement, fieldValue.fieldName(), fieldValue.value(), fieldValue.dbType());
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

    private static String cleanSql(String query) {
        query = query.replace("  ", " ");
        query = query.replace("WHERE OR", "WHERE");
        query = query.replace("WHERE AND", "WHERE");
        return query;
    }

    private static void loopResults(List<YormTuple> tuples, ResultSet rs, Object[] values, int params) throws SQLException, YormException {
        for (YormTuple tuple : tuples) {
            params = rowRecordConverter.rowToRecord(rs, values, params, tuple.dbFieldName(), tuple.type());
        }
    }


}
