package org.yorm.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.yorm.YormTuple;
import org.yorm.exception.YormException;
/*
Inspired on the work of Benji Weber
https://github.com/benjiman/benjiql
With Java Records 17 this nasty trick of matching values has to be applied,
as Proxy instances neither Bytebuddy can be used, since records are final
 */

public class ReflectionUtil {

    private Constructor<Record> constructor;
    private List<YormTuple> yormTupleList;
    private Record sampleObject;
    private List<MapTuple> mapTuplelist = new ArrayList<>();

    public ReflectionUtil(Constructor<Record> constructor, List<YormTuple> yormTupleList) throws YormException {
        this.constructor = constructor;
        this.yormTupleList = yormTupleList;
        try {
            buildSample();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new YormException("Error building a sample", e);
        }
    }

    private void buildSample() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        this.mapTuplelist = buildSampleObject();
        this.sampleObject = this.constructor.newInstance(this.mapTuplelist.stream().map(MapTuple::object).toList().toArray(new Object[0]));
    }

    public <T extends Record, U> String getFunctionName(Function<T, U> getter) throws YormException {
        Object obj = getter.apply((T) sampleObject);
        YormTuple yormTuple = mapTuplelist.stream().filter(mp -> mp.object().equals(obj)).map(MapTuple::tuple).findFirst()
            .orElseThrow(() -> new YormException("Couldn't find a database field mapped to that property"));
        return yormTuple.dbFieldName();
    }

    private List<MapTuple> buildSampleObject() {
        List<MapTuple> list = new ArrayList<>();
        int sampleNumber = 1;
        LocalDate sampleDate = LocalDate.now();
        boolean sampleBoolean = false;
        LocalDateTime sampleTime = LocalDateTime.of(sampleDate, LocalTime.MIDNIGHT);
        LocalTime secondSampleTime = LocalTime.MIDNIGHT;
        for (YormTuple yormTuple : yormTupleList) {
            switch (yormTuple.type()) {
                case TINYINT, BIT, BOOLEAN -> {
                    list.add(new MapTuple(yormTuple, sampleBoolean));
                    sampleBoolean = !sampleBoolean;
                }
                case VARCHAR, CHAR, TINYTEXT -> list.add(new MapTuple(yormTuple, String.valueOf(sampleNumber++)));
                case TEXT, MEDIUMTEXT, LONGTEXT -> list.add(new MapTuple(yormTuple, String.valueOf(sampleNumber++)));
                case SMALLINT, INTEGER -> list.add(new MapTuple(yormTuple, (sampleNumber++)));
                case BIGINT -> list.add(new MapTuple(yormTuple, (Long.valueOf(sampleNumber++))));
                case REAL, FLOAT -> list.add(new MapTuple(yormTuple, (Float.valueOf(sampleNumber++))));
                case DOUBLE -> list.add(new MapTuple(yormTuple, (Double.valueOf(sampleNumber++))));
                case DECIMAL -> list.add(new MapTuple(yormTuple, (BigDecimal.valueOf(sampleNumber++))));
                case DATE -> {
                    list.add(new MapTuple(yormTuple, (sampleDate)));
                    sampleDate = sampleDate.plusDays(1);
                }
                case TIME -> {
                    list.add(new MapTuple(yormTuple, secondSampleTime));
                    secondSampleTime = secondSampleTime.minusSeconds(30);
                }
                case TIMESTAMP -> {
                    list.add(new MapTuple(yormTuple, (sampleTime)));
                    sampleDate = sampleDate.plusDays(1);
                    sampleTime = LocalDateTime.of(sampleDate, LocalTime.MIDNIGHT);
                }
                default -> list.add(new MapTuple(yormTuple, (sampleNumber++)));
            }
        }
        return list;
    }

    private record MapTuple(YormTuple tuple, Object object) {

    }

}
