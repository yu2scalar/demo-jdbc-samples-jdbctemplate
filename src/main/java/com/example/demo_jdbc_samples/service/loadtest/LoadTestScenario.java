package com.example.demo_jdbc_samples.service.loadtest;

import com.example.demo_jdbc_samples.dto.LoadTestVerificationDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * One API under load test. LoadTestRunner does the threads, ramp-up and metrics;
 * a scenario only prepares the data, makes one call and checks the result.
 * Add a Spring bean implementing this to make a new API available at
 * /load-test/{name}/init and /load-test/{name}/run.
 *
 * @param <R> what execute() keeps for each successful call, for verify()
 */
public interface LoadTestScenario<R> {

    /** Path name, e.g. "place-order" */
    String name();

    /** Prepares the data. Returns what was created, as JSON. */
    Map<String, Object> init(LoadTestParams params) throws CustomException;

    /** Checks the parameters and that init was called, then takes the snapshot for verify(). */
    void beforeRun(LoadTestParams params) throws CustomException;

    /**
     * One call. Returns the record for verify() (null = nothing to keep); throws on failure.
     * Throws NoMoreWorkException when a count-based run has nothing left; that call is not counted.
     */
    R execute(Random random, LoadTestParams params) throws Exception;

    /** True when a count-based run has finished; threads then stop calling execute(). */
    default boolean isDone() {
        return false;
    }

    /** Thrown by execute() when there is nothing left to do */
    class NoMoreWorkException extends RuntimeException {
        public NoMoreWorkException() {
            super(null, null, false, false);
        }
    }

    /** Transaction attempts so far including retries, or -1 if not counted. */
    long attemptCount();

    /**
     * Checks the database against every successful call of the run (ramp-up included).
     * failuresByCode is also for the whole run.
     */
    LoadTestVerificationDto verify(List<R> successes, Map<Integer, Long> failuresByCode) throws CustomException;
}
