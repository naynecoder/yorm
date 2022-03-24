package org.yorm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class YormTable {

    private String dbTable;
    private List<YormTuple> list = new ArrayList<>();
    private Constructor<Record> constructor;

    public YormTable(String dbTable) {
        this.dbTable = dbTable;
    }

    public void add(YormTuple tuple) {
        list.add(tuple);
    }

    public List<YormTuple> getTuples() {
        return list.stream().toList();
    }

    public String getDbTable() {
        return dbTable;
    }

    public Constructor<Record> getConstructor() {
        return constructor;
    }

    public void setConstructor(Constructor<?> constructor) {
        this.constructor = (Constructor<Record>) constructor;
    }
}
