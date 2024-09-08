package org.yorm.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yorm.exception.YormException;

public class RowRecordConverter {

    private static final Logger logger = LoggerFactory.getLogger(RowRecordConverter.class);

    private static final Map<Class<?>, Converter<?>> PASS_THROUGH_CONVERTERS = Map.ofEntries(
        Map.entry(boolean.class, input -> input),
        Map.entry(byte.class, input -> input),
        Map.entry(char.class, input -> input),
        Map.entry(short.class, input -> input),
        Map.entry(int.class, input -> input),
        Map.entry(long.class, input -> input),
        Map.entry(float.class, input -> input),
        Map.entry(double.class, input -> input),
        Map.entry(Boolean.class, input -> input),
        Map.entry(Byte.class, input -> (byte) input),
        Map.entry(Character.class, input -> (char) input),
        Map.entry(Short.class, input -> (short) input),
        Map.entry(Integer.class, input -> (int) input),
        Map.entry(Long.class, input -> (long) input),
        Map.entry(Float.class, input -> (float) input),
        Map.entry(Double.class, input -> (double) input),
        Map.entry(String.class, input -> (String) input)
    );

    private static final Map<Class<?>, Converter<?>> ENUM_SERIALIZERS = Map.ofEntries(
        Map.entry(String.class, input -> ((Enum<?>) input).toString()),
        Map.entry(char.class, input -> (char) ((Enum<?>) input).ordinal()),
        Map.entry(int.class, input -> ((Enum<?>) input).ordinal()),
        Map.entry(long.class, input -> (long) ((Enum<?>) input).ordinal()),
        Map.entry(Character.class, input -> (char) ((Enum<?>) input).ordinal()),
        Map.entry(Integer.class, input -> ((Enum<?>) input).ordinal()),
        Map.entry(Long.class, input -> (long) ((Enum<?>) input).ordinal())
    );

    private static final Converter<String> TO_STRING_CONVERTER = input -> input.toString();


    public static <OutputType> Converter<OutputType> converterFor(Class<?> inputTypeClass, Class<OutputType> outputTypeClass) {
        if (logger.isDebugEnabled()) {
            logger.debug("Building a deserializer for {} to {}.", inputTypeClass.getName(), outputTypeClass.getName());
        }
        if (outputTypeClass.isAssignableFrom(inputTypeClass)) {
            if (outputTypeClass.isPrimitive()) {
                //noinspection unchecked
                return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(inputTypeClass);
            } else {
                return outputTypeClass::cast;
            }
        }
        if (outputTypeClass.isEnum() && inputTypeClass == String.class) {
            return enumFromString(outputTypeClass);
        }
        if (outputTypeClass.isEnum() && inputTypeClass == char.class) {
            return enumFromOrdinal(outputTypeClass);
        }
        if (inputTypeClass.isEnum()) {
            //noinspection unchecked
            return (Converter<OutputType>) ENUM_SERIALIZERS.get(outputTypeClass);
        }
        if (outputTypeClass == String.class) {
            //noinspection unchecked
            return (Converter<OutputType>) TO_STRING_CONVERTER;
        }
        // YUCK!
        if (inputTypeClass.equals(Boolean.class) && outputTypeClass.equals(boolean.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(Boolean.class);
        }
        if (inputTypeClass.equals(boolean.class) && outputTypeClass.equals(Boolean.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(boolean.class);
        }
        if (inputTypeClass.equals(Byte.class) && outputTypeClass.equals(byte.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(Byte.class);
        }
        if (inputTypeClass.equals(byte.class) && outputTypeClass.equals(Byte.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(byte.class);
        }
        if (inputTypeClass.equals(Character.class) && outputTypeClass.equals(char.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(Character.class);
        }
        if (inputTypeClass.equals(char.class) && outputTypeClass.equals(Character.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(char.class);
        }
        if (inputTypeClass == String.class && outputTypeClass == char.class) {
            return (Converter<OutputType>) TO_STRING_CONVERTER;
        }
        if (inputTypeClass.equals(Short.class) && outputTypeClass.equals(short.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(Short.class);
        }
        if (inputTypeClass.equals(short.class) && outputTypeClass.equals(Short.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(short.class);
        }
        if (inputTypeClass.equals(Integer.class) && outputTypeClass.equals(int.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(Integer.class);
        }
        if (inputTypeClass.equals(int.class) && outputTypeClass.equals(Integer.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(int.class);
        }
        if (inputTypeClass.equals(Long.class) && outputTypeClass.equals(long.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(Long.class);
        }
        if (inputTypeClass.equals(long.class) && outputTypeClass.equals(Long.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(long.class);
        }
        if (inputTypeClass.equals(Float.class) && outputTypeClass.equals(float.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(Float.class);
        }
        if (inputTypeClass.equals(float.class) && outputTypeClass.equals(Float.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(float.class);
        }
        if (inputTypeClass.equals(Double.class) && outputTypeClass.equals(double.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(Double.class);
        }
        if (inputTypeClass.equals(double.class) && outputTypeClass.equals(Double.class)) {
            //noinspection unchecked
            return (Converter<OutputType>) PASS_THROUGH_CONVERTERS.get(double.class);
        }
        throw new RuntimeException(String.format(
            "No deserializer found for %s to %s. You should file a bug to let us know what we need to add!",
            inputTypeClass.getName(), outputTypeClass.getName()
        ));
    }

    // TODO: Cache for each enum type? I could imagine times when we'd be calling this a lot for the same thing.
    private static <OutputType> Converter<OutputType> enumFromOrdinal(Class<OutputType> outputTypeClass) {
        try {
            MethodHandle methodHandle = MethodHandles
                .lookup()
                .findStatic(
                    outputTypeClass,
                    "values",
                    MethodType.methodType(
                        // Like typing MyEnum[].class.
                        java.lang.reflect.Array.newInstance(outputTypeClass, 0).getClass()
                    )
                );
            return input -> {
                try {
                    // noinspection unchecked
                    return (OutputType) ((OutputType[]) methodHandle.invokeExact())[(int) input];
                } catch (IllegalArgumentException e) {
                    // Rethrow real errors...
                    throw e;
                } catch (Throwable e) {
                    // This should never happen, but let's give helpful output just in case weird stuff goes down!
                    throw new IllegalStateException(
                        String.format(
                            "Unexpected exception invoking %s.value()[%d].",
                            outputTypeClass.getName(),
                            (int) input
                        ),
                        e
                    );
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // Again... This should never happen, but let's give helpful output just in case weird stuff goes down!
            throw new IllegalStateException(
                String.format("Unexpected exception finding %s.value()[] method.", outputTypeClass.getName()),
                e
            );
        }
    }

    // TODO: Cache for each enum type? I could imagine times when we'd be calling this a lot for the same thing.
    private static <OutputType> Converter<OutputType> enumFromString(Class<OutputType> outputTypeClass) {
        try {
            MethodHandle methodHandle = MethodHandles
                .lookup()
                .findStatic(
                    outputTypeClass,
                    "valueOf",
                    MethodType.methodType(outputTypeClass, String.class)
                );
            return input -> {
                try {
                    // noinspection unchecked
                    return (OutputType) methodHandle.invoke((String) input);
                } catch (IllegalArgumentException e) {
                    // Rethrow real errors...
                    throw e;
                } catch (Throwable e) {
                    // This should never happen, but let's give helpful output just in case weird stuff goes down!
                    throw new IllegalStateException(
                        String.format(
                            "Unexpected exception invoking %s.valueOf(\"%s\").",
                            outputTypeClass.getName(),
                            (String) input
                        ),
                        e
                    );
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // Again... This should never happen, but let's give helpful output just in case weird stuff goes down!
            throw new IllegalStateException(
                String.format("Unexpected exception finding %s.valueOf() method.", outputTypeClass.getName()),
                e
            );
        }
    }

    public void recordToRow(
        int paramIndex,
        PreparedStatement preparedStatement,
        String dbColumnName,
        Object value,
        DbType type,
        Converter<?> converter
    ) throws SQLException, YormException {
        switch (type) {
            //MySql does not have a truly boolean type, bool/boolean are a synonym of tinyint(1)
            //Postgresql maps booleans to bits
            case TINYINT, BIT, BOOLEAN -> preparedStatement.setBoolean(paramIndex, (boolean) value);
            case SMALLINT, INTEGER -> {
                if (value instanceof Integer) {
                    preparedStatement.setInt(paramIndex, (int) value);
                } else if (value instanceof Long) {
                    preparedStatement.setLong(paramIndex, (long) value);
                } else {
                    throw new YormException("Incompatible value:" + value + " of class:" + value.getClass().getName() + " for column type:" + type);
                }
            }
            case BIGINT -> preparedStatement.setLong(paramIndex, (long) converter.convert(value));
            case VARCHAR, TEXT, CHAR -> preparedStatement.setString(paramIndex, (String) converter.convert(value));
            case DOUBLE -> preparedStatement.setDouble(paramIndex, (double) converter.convert(value));
            case FLOAT, REAL -> preparedStatement.setFloat(paramIndex, (float) converter.convert(value));
            case DECIMAL -> preparedStatement.setBigDecimal(paramIndex, (BigDecimal) converter.convert(value));
            case DATE -> preparedStatement.setDate(paramIndex, Date.valueOf((LocalDate) converter.convert(value)));
            case TIME -> preparedStatement.setTime(paramIndex, Time.valueOf((LocalTime) converter.convert(value)));
            case TIMESTAMP -> preparedStatement.setTimestamp(paramIndex, Timestamp.valueOf((LocalDateTime) converter.convert(value)));
            default -> throw new YormException("Couldn't find type for " + dbColumnName);
        }
    }

    public int rowToRecord(
        ResultSet rs,
        Object[] values,
        int params,
        String dbColumnName,
        DbType type,
        Converter<?> converter
    ) throws SQLException, YormException {
        switch (type) {
            case TINYINT, BIT, BOOLEAN -> {
                //MySql does not have a truly boolean type, bool/boolean are a synonym of tinyint(1)
                boolean tiny = rs.getBoolean(dbColumnName);
                values[params++] = converter.convert(tiny);
            }
            case SMALLINT, INTEGER -> {
                int ii = rs.getInt(dbColumnName);
                values[params++] = converter.convert(ii);
            }
            case BIGINT -> {
                long ll = rs.getLong(dbColumnName);
                values[params++] = converter.convert(ll);
            }
            case VARCHAR, CHAR, TEXT -> {
                String str = rs.getString(dbColumnName);
                values[params++] = converter.convert(str);
            }
            case DOUBLE -> {
                double dd = rs.getDouble(dbColumnName);
                values[params++] = converter.convert(dd);
            }
            case FLOAT, REAL -> {
                float ff = rs.getFloat(dbColumnName);
                values[params++] = converter.convert(ff);
            }
            case DECIMAL -> {
                BigDecimal bb = rs.getBigDecimal(dbColumnName);
                values[params++] = converter.convert(bb);
            }
            case DATE -> {
                Date date = rs.getDate(dbColumnName);
                values[params++] = converter.convert(date.toLocalDate());
            }
            case TIME -> {
                Time time = rs.getTime(dbColumnName);
                values[params++] = converter.convert(time.toLocalTime());
            }
            case TIMESTAMP -> {
                Timestamp ts = rs.getTimestamp(dbColumnName);
                values[params++] = converter.convert(ts.toLocalDateTime());
            }
            default -> throw new YormException("Couldn't find type for " + dbColumnName);
        }
        return params;
    }

    public interface Converter<OutputType> {

        OutputType convert(Object input);
    }
}
