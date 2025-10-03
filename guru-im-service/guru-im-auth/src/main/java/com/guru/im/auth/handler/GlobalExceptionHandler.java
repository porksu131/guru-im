package com.guru.im.auth.handler;

import com.guru.im.common.exception.ServiceException;
import com.guru.im.common.model.ResponseResult;
import com.guru.im.common.utils.ConvertUtils;
import com.guru.im.common.utils.EscapeUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Optional;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 获取当前请求的 HttpServletRequest
     */
    private Optional<HttpServletRequest> getCurrentRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest);
    }

    /**
     * 获取请求URI，如果无法获取则返回"unknown"
     */
    private String getRequestURI() {
        return getCurrentRequest()
                .map(HttpServletRequest::getRequestURI)
                .orElse("unknown");
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseResult<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String requestURI = getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return ResponseResult.fail("请求方法不支持");
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseResult<Void> handleServiceException(ServiceException e) {
        log.error("业务异常: {}", e.getMessage());
        Integer code = e.getCode();
        return code != null ? ResponseResult.fail(code, e.getMessage()) : ResponseResult.fail(e.getMessage());
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseResult<Void> handleMissingPathVariableException(MissingPathVariableException e) {
        String requestURI = getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return ResponseResult.fail(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseResult<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String requestURI = getRequestURI();
        String value = ConvertUtils.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value)) {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);
        return ResponseResult.fail(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType().getName(), value));
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseResult<Void> handleRuntimeException(RuntimeException e) {
        String requestURI = getRequestURI();
        log.error("请求地址'{}'，发生未知运行时异常", requestURI, e);
        return ResponseResult.fail("系统运行时异常，请联系管理员");
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception e) {
        String requestURI = getRequestURI();
        log.error("请求地址'{}'，发生系统异常", requestURI, e);
        return ResponseResult.fail("系统异常，请联系管理员");
    }
}