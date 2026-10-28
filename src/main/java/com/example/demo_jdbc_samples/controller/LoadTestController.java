package com.example.demo_jdbc_samples.controller;

import com.example.demo_jdbc_samples.dto.ApiResponse;
import com.example.demo_jdbc_samples.dto.LoadTestInitRequestDto;
import com.example.demo_jdbc_samples.dto.LoadTestRunRequestDto;
import com.example.demo_jdbc_samples.dto.LoadTestRunResponseDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.service.loadtest.LoadTestRunner;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Load test of the APIs: one scenario per API (see LoadTestScenario)
@RequestMapping(value = "/load-test")
@RestController
public class LoadTestController {
    @Autowired
    private LoadTestRunner loadTestRunner;

    @GetMapping("/scenarios")
    public ResponseEntity<ApiResponse<List<String>>> scenarios() {
        return ResponseEntity.ok(ApiResponse.success(loadTestRunner.scenarioNames()));
    }

    // {"params":{...}} - the parameters depend on the scenario
    @PostMapping("/{scenario}/init")
    public ResponseEntity<ApiResponse<Map<String, Object>>> init(
            @PathVariable("scenario") String scenario,
            @RequestBody(required = false) LoadTestInitRequestDto request) {
        Map<String, Object> params = request != null ? request.getParams() : null;
        return ResponseEntity.ok(ApiResponse.success(loadTestRunner.init(scenario, params)));
    }

    // {"number_of_threads":8,"ramp_up_period_seconds":5,"duration_seconds":30,"params":{...}}
    @PostMapping("/{scenario}/run")
    public ResponseEntity<ApiResponse<LoadTestRunResponseDto>> run(
            @PathVariable("scenario") String scenario,
            @RequestBody LoadTestRunRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(loadTestRunner.run(scenario, request)));
    }

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleScalarDbException(CustomException ex) {
        if (ex.getErrorCode() != null
                && (ex.getErrorCode() == LoadTestRunner.NOT_INITIALIZED
                    || ex.getErrorCode() == LoadTestRunner.ALREADY_RUNNING)) {
            return new ResponseEntity<>(ApiResponse.error(ex.getErrorCode(), ex.getMessage()), HttpStatus.CONFLICT);
        }
        return OrderController.errorResponse(ex);
    }
}
