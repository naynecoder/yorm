package org.yorm.db;

import org.yorm.db.operations.operators.ComparisonOperator;
import org.yorm.db.operations.operators.WhereOperator;
import org.yorm.util.DbType;

public record FilteringFieldValue(String fieldName, DbType dbType, Object value, ComparisonOperator comparisonOperator, WhereOperator whereOperator) {

}
