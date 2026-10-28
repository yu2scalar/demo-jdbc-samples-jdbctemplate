package com.example.demo_jdbc_samples.service.loadtest;

import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.service.OrderRules;
import java.util.Collections;
import java.util.Map;

// Scenario parameters from the "params" object of a load-test request
public class LoadTestParams {
    private final Map<String, Object> values;

    public LoadTestParams(Map<String, Object> values) {
        this.values = values != null ? values : Collections.emptyMap();
    }

    public long getLong(String name, long defaultValue) {
        Object v = values.get(name);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            throw new CustomException(name + " must be a number: " + v, OrderRules.INVALID_REQUEST);
        }
    }

    public int getInt(String name, int defaultValue) {
        long v = getLong(name, defaultValue);
        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
            throw new CustomException(name + " is out of the int range: " + v, OrderRules.INVALID_REQUEST);
        }
        return (int) v;
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        Object v = values.get(name);
        if (v == null) {
            return defaultValue;
        }
        return v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(v.toString());
    }

    public Map<String, Object> asMap() {
        return values;
    }
}
