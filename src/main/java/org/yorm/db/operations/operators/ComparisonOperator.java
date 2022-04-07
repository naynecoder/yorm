package org.yorm.db.operations.operators;

public enum ComparisonOperator {
    EQUALS("="),
    NOT_EQUALS("="),
    LIKE("LIKE"),
    GREATER_THAN(">="),
    LESS_THAN("<=");

    private String operator;

    ComparisonOperator(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return this.operator;
    }


}
