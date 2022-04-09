package org.yorm.db.operations.operators;

public enum WhereOperator {
    AND("AND"),
    OR("OR"),
    EMPTY("");


    private String operator;

    WhereOperator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return this.operator;
    }


}
