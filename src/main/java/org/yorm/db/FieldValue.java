package org.yorm.db;

import org.yorm.db.operations.WhereOperator;
import org.yorm.util.DbType;

public record FieldValue(String fieldName, DbType dbType, Object value, WhereOperator whereOperator) {

}
