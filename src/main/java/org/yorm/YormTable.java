package org.yorm;

import java.lang.reflect.Constructor;
import java.util.List;

public record YormTable(String dbTable, List<YormTuple> tuples, Constructor<Record> constructor) {

}
