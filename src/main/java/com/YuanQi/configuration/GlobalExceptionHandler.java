package com.YuanQi.configuration;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.YuanQi.utils.BusinessException;
import com.YuanQi.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 参数校验异常（@Valid）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return Result.badRequest(message);
    }

    /**
     * 参数校验异常（@RequestParam）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return Result.badRequest(message);
    }

    /**
     * Sa-Token未登录异常（401）
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLoginException(NotLoginException ex) {
        log.warn("未登录: {}", ex.getMessage());
        return Result.error(401, "请先登录");
    }

    /**
     * Sa-Token无权限异常（403）
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotPermissionException(NotPermissionException ex) {
        log.warn("无权限: {}", ex.getMessage());
        return Result.error(403, "无权限访问");
    }

    /**
     * Sa-Token无角色异常（403）
     */
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotRoleException(NotRoleException ex) {
        log.warn("无角色: {}", ex.getMessage());
        return Result.error(403, "无权限访问");
    }

    /**
     * 请求方法不支持异常（405）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("请求方法不支持: {}", ex.getMessage());
        return Result.error(405, "请求方法不支持: " + ex.getMethod());
    }

    /**
     * API限流异常处理（429）
     */
    @ExceptionHandler(WebClientResponseException.TooManyRequests.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleTooManyRequests(WebClientResponseException.TooManyRequests ex) {
        log.warn("API请求过于频繁: {}", ex.getMessage());
        return Result.error(429, "AI服务请求过于频繁，请稍后再试");
    }

    /**
     * WebClient响应异常处理
     */
    @ExceptionHandler(WebClientResponseException.class)
    public Result<Void> handleWebClientResponseException(WebClientResponseException ex) {
        log.error("AI服务调用异常: status={}, message={}", ex.getStatusCode(), ex.getMessage());
        if (ex.getStatusCode().value() == 429) {
            return Result.error(429, "AI服务请求过于频繁，请稍后再试");
        }
        return Result.error("AI服务调用失败: " + ex.getStatusText());
    }

    /**
     * 运行时异常处理
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        log.error("运行时异常: ", ex);
        return Result.error(ex.getMessage());
    }

    /**
     * 其他异常处理
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常: ", ex);
        return Result.error("系统繁忙，请稍后重试");
    }
}
