package com.example.demo_jdbc_samples.service;

import com.example.demo_jdbc_samples.exception.CustomException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * One local transaction with retry; the JDBC counterpart of runInTransaction in the
 * ScalarDB version's services.
 *
 * The work runs on the connection bound to the thread by DataSourceTransactionManager,
 * so the repositories (JdbcTemplate) take part in it without a transaction parameter.
 * The isolation level is READ COMMITTED (spring.datasource.hikari.transaction-isolation).
 * Each failed attempt is rolled back and logged at ERROR with its stack trace, as in
 * the ScalarDB version.
 */
@Slf4j
@Component
public class TransactionRunner {
    // Commit outcome unknown (as UnknownTransactionStatusException in the ScalarDB version)
    public static final int UNKNOWN_TRANSACTION_STATUS = 9200;

    private final PlatformTransactionManager transactionManager;
    private final RetryTemplate retryTemplate;

    public TransactionRunner(PlatformTransactionManager transactionManager,
            @Qualifier("jdbcRetryTemplate") RetryTemplate retryTemplate) {
        this.transactionManager = transactionManager;
        this.retryTemplate = retryTemplate;
    }

    public <T> T run(Supplier<T> work) throws CustomException {
        return run(work, null);
    }

    /** As run(work), counting every attempt (retries included) in attempts if not null. */
    public <T> T run(Supplier<T> work, AtomicLong attempts) throws CustomException {
        try {
            return retryTemplate.execute(() -> {
                if (attempts != null) {
                    attempts.incrementAndGet();
                }
                return runOnce(work);
            });
        } catch (RetryException re) {
            Throwable cause = (re.getCause() != null) ? re.getCause() : re;
            throw new CustomException(cause.getMessage(), cause, determineErrorCode(cause));
        }
    }

    /** One transaction without retry. Failures are thrown as they are. */
    public <T> T runOnce(Supplier<T> work) {
        TransactionStatus status = transactionManager.getTransaction(TransactionDefinition.withDefaults());
        T result;
        try {
            result = work.get();
        } catch (RuntimeException | Error e) {
            log.error(e.getMessage(), e);
            try {
                transactionManager.rollback(status);
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
            throw e;
        }
        try {
            transactionManager.commit(status);
        } catch (TransactionException e) {
            // E.g. the connection was lost during the commit: it may have been committed.
            // Not retried, as 9200 in the ScalarDB version.
            log.error(e.getMessage(), e);
            log.warn("Commit failed - the transaction may have been committed. Verify it before retrying.");
            throw new CustomException("Commit outcome unknown: " + e.getMessage(), e, UNKNOWN_TRANSACTION_STATUS);
        }
        return result;
    }

    // Same codes as the ScalarDB version (see CustomException)
    private static int determineErrorCode(Throwable e) {
        for (Throwable c = e; c != null; c = c.getCause()) {
            if (c instanceof CustomException && ((CustomException) c).getErrorCode() != null) {
                return ((CustomException) c).getErrorCode();
            }
        }
        for (Throwable c = e; c != null; c = c.getCause()) {
            // Includes a duplicate key on insert (ScalarDB: a conflict, 9300 after the retries)
            if (c instanceof DataAccessException || c instanceof TransactionException) return 9300;
        }
        if (e instanceof RuntimeException) return 9400;
        return 9500;
    }
}
