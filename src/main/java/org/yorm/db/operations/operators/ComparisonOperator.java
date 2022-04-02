package org.yorm.db.operations.operators;

public enum ComparisonOperator {
    EQUALS("="),
    NOT_EQUALS("="),
    LIKE("LIKE");

    private String operator;

    ComparisonOperator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return this.operator;
    }


}
