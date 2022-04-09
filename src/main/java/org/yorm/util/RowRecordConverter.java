package org.yorm.util;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.yorm.exception.YormException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RowRecordConverter {

    public void recordToRow(int paramIndex, PreparedStatement preparedStatement, String dbColumnName, Object value, DbType type) throws SQLException, YormException {
        switch (type) {
            //MySql does not have a truly boolean type, bool/boolean are a synonym of tinyint(1)
            //Postgresql maps booleans to bits
            case TINYINT, BIT -> preparedStatement.setBoolean(paramIndex, (boolean) value);
            case SMALLINT, INTEGER -> {
                if (value instanceof Integer) {
                    preparedStatement.setInt(paramIndex, (int) value);
                } else if (value instanceof Long) {
                    preparedStatement.setLong(paramIndex, (long) value);
                } else {
                    throw new YormException("Incompatible value:" + value + " of class:" + value.getClass().getName() + " for column type:" + type);
                }
            }
            case BIGINT -> preparedStatement.setLong(paramIndex, (long) value);
            case VARCHAR, CHAR -> preparedStatement.setString(paramIndex, (String) value);
            case DOUBLE -> preparedStatement.setDouble(paramIndex, (double) value);
            case FLOAT, REAL -> preparedStatement.setFloat(paramIndex, (float) value);
            case DECIMAL -> preparedStatement.setBigDecimal(paramIndex, (BigDecimal) value);
            case DATE -> preparedStatement.setDate(paramIndex, Date.valueOf((LocalDate) value));
            case TIMESTAMP -> preparedStatement.setTimestamp(paramIndex, Timestamp.valueOf((LocalDateTime) value));
            default -> throw new YormException("Couldn't find type for " + dbColumnName);
        }
    }

    public int rowToRecord(ResultSet rs, Object[] values, int params, String dbColumnName, DbType type) throws SQLException, YormException {
        switch (type) {
            case TINYINT, BIT -> {
                //MySql does not have a truly boolean type, bool/boolean are a synonym of tinyint(1)
                boolean tiny = rs.getBoolean(dbColumnName);
                values[params++] = tiny;
            }
            case SMALLINT, INTEGER -> {
                int ii = rs.getInt(dbColumnName);
                values[params++] = ii;
            }
            case BIGINT -> {
                long ll = rs.getLong(dbColumnName);
                values[params++] = ll;
            }
            case VARCHAR, CHAR -> {
                String str = rs.getString(dbColumnName);
                values[params++] = str;
            }
            case DOUBLE -> {
                double dd = rs.getDouble(dbColumnName);
                values[params++] = dd;
            }
            case FLOAT, REAL -> {
                float ff = rs.getFloat(dbColumnName);
                values[params++] = ff;
            }
            case DECIMAL -> {
                BigDecimal bb = rs.getBigDecimal(dbColumnName);
                values[params++] = bb;
            }
            case DATE -> {
                Date date = rs.getDate(dbColumnName);
                values[params++] = date.toLocalDate();
            }
            case TIMESTAMP -> {
                Timestamp ts = rs.getTimestamp(dbColumnName);
                values[params++] = ts.toLocalDateTime();
            }
            default -> throw new YormException("Couldn't find type for " + dbColumnName);
        }
        return params;
    }
}
