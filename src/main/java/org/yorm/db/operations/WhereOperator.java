package org.yorm.db.operations;

public enum WhereOperator {
    OR("OR"),
    AND("AND"),
    EQUALS("="),
    LIKE("LIKE");

    private String operator;

    WhereOperator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return this.operator;
    }


}
