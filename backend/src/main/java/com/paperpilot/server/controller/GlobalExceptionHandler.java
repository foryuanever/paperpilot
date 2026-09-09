package com.paperpilot.server.controller;

import com.paperpilot.server.entity.SystemLogEntity;
import com.paperpilot.server.repository.SystemLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final SystemLogRepository systemLogRepository;

    public GlobalExceptionHandler(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    private void saveSystemLog(String message, String level) {
        try {
            if (message != null && message.length() > 500) {
                message = message.substring(0, 500) + "...";
            }
            SystemLogEntity logEntity = new SystemLogEntity();
            logEntity.setMessage(message != null ? message : "未知错误");
            logEntity.setLevel(level);
            systemLogRepository.save(logEntity);
        } catch (Exception e) {
            log.error("Failed to write to SystemLogEntity", e);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Validation error: ", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        body.put("message", "Validation failed: " + errors);
        body.put("errors", errors);
        
        saveSystemLog("参数校验失败: " + body.get("message"), "warn");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception: ", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "服务器暂时无法处理该请求");
        
        saveSystemLog("全局未捕获异常: " + body.get("message"), "error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
        body.put("error", "Payload Too Large");
        body.put("message", "文件超过 120 MB 大小限制");
        saveSystemLog("文件上传超过 120 MB", "warn");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> handleMissingRequestPart(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "请求缺少必要参数，请检查后重试");
        saveSystemLog("请求参数缺失: " + ex.getMessage(), "warn");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "请求参数有误");
        
        saveSystemLog("非法参数异常: " + body.get("message"), "warn");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("error", ex.getStatusCode().toString());
        body.put("message", ex.getReason() != null ? ex.getReason() : "请求处理失败");
        
        saveSystemLog("请求状态异常 (" + status + "): " + body.get("message"), "error");
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}
