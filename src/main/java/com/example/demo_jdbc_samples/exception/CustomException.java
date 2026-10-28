package com.example.demo_jdbc_samples.exception;

/**
 * The application's error contract, with the same codes as the ScalarDB version:
 * 9100 condition not met (update / delete of a missing row), 9200 commit outcome unknown,
 * 9300 database / transaction failure, 9400 bad request, 9500 other, 9700 not found,
 * 9800-9802 business rejections.
 */
public class CustomException extends RuntimeException {

    private Integer errorCode;

    // Kept for the same response shape as the ScalarDB version; JDBC has no transaction ID
    private String transactionId;

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public CustomException(Exception ex, Integer errorCode) {
        super(ex.getMessage(), ex);
        this.setErrorCode(errorCode);
    }

    public CustomException(String message, Integer errorCode) {
        super(message);
        this.setErrorCode(errorCode);
    }

    public CustomException(String message, Throwable cause, Integer errorCode) {
        super(message, cause);
        this.setErrorCode(errorCode);
    }
}
