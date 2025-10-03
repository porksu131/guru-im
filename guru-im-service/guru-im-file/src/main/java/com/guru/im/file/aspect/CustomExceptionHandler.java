package com.guru.im.file.aspect;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.file.exception.StorageException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

//@ControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(StorageException.class)
    public ResponseResult<String> handleStorageException(StorageException e) {
        return ResponseResult.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseResult<String> handleException(Exception e) {
        return ResponseResult.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error: " + e.getMessage());
    }
}