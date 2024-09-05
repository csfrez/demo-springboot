package com.csfrez.demospringboot.advice;

import cn.hutool.core.util.StrUtil;
import com.csfrez.demospringboot.base.ResponseResult;
import com.csfrez.demospringboot.base.ResultCode;
import com.csfrez.demospringboot.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseResult<String> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.error("[GlobalExceptionHandler][handleNoHandlerFoundException] 请求路径不存在", ex);
        return ResponseResult.failed(ResultCode.NOT_FOUND.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("[GlobalExceptionHandler][handleMethodArgumentNotValidException] 参数校验异常", ex);
        return wrapperBindingResult(ex.getBindingResult());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseResult<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.error("[GlobalExceptionHandler][handleMissingServletRequestParameterException] 参数校验异常", ex);
        String errorMessage = String.format("必填请求参数%s不存在", ex.getParameterName());
        return ResponseResult.failed(errorMessage);
    }

    @ExceptionHandler(BindException.class)
    public ResponseResult<String> handleBindException(BindException ex) {
        log.error("[GlobalExceptionHandler][handleBindException] 参数校验异常", ex);
        return wrapperBindingResult(ex.getBindingResult());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseResult<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("[GlobalExceptionHandler][handleIllegalArgumentException] 参数校验异常", ex);
        return ResponseResult.failed(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseResult<Void> handleRuntimeException(RuntimeException ex) {
        // 记录异常日志
        log.error("[GlobalExceptionHandler][handleRuntimeException] RuntimeException", ex);

        if(ex instanceof CustomException){
            return ResponseResult.failed(((CustomException) ex).getCode(), ex.getMessage());
        }

        // 返回自定义的错误响应
        return ResponseResult.failed(ResultCode.INTERNAL_SERVER_ERROR.getCode(), ex.getMessage());
    }

    // 处理其他类型的异常
    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleGenericException(Exception ex) {
        // 记录异常日志
        log.error("[GlobalExceptionHandler][handleGenericException] Exception", ex);
        if(ex instanceof CustomException){
            return ResponseResult.failed(((CustomException) ex).getCode(), ex.getMessage());
        }
        // 返回自定义的错误响应
        return ResponseResult.failed(ResultCode.INTERNAL_SERVER_ERROR.getCode(), ResultCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    private ResponseResult<String> wrapperBindingResult(BindingResult bindingResult) {
        List<String> errors = bindingResult.getAllErrors().stream().map(error -> {
            if (error instanceof FieldError) {
                return ((FieldError) error).getField() + ":" + error.getDefaultMessage();
            } else {
                return error.getDefaultMessage();
            }
        }).sorted().collect(Collectors.toList());
        String errorMsg = StrUtil.join("; ", errors);
        return ResponseResult.failed(errorMsg);
    }

    private ResponseResult<String> wrapperBindingResult(BindingResult bindingResult, HttpServletResponse httpServletResponse) {
        /*StringBuilder errorMsg = new StringBuilder();
        for (ObjectError error : bindingResult.getAllErrors()) {
            if (error instanceof FieldError) {
                errorMsg.append(((FieldError) error).getField()).append(":");
            }
            errorMsg.append(error.getDefaultMessage() == null ? "" : error.getDefaultMessage());
        }*/
        httpServletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        //return ResponseEntity.internalServerError().body(errorMsg.toString());
        return this.wrapperBindingResult(bindingResult);
    }



}
