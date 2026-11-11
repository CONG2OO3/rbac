package com.company.rbac.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.company.rbac.common.api.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class) public Result<Void> business(BusinessException e) { return Result.fail(e.getCode(), e.getMessage()); }
    @ExceptionHandler(NotLoginException.class) public Result<Void> notLogin(NotLoginException e) { return Result.fail(401, "登录状态已失效"); }
    @ExceptionHandler(NotPermissionException.class) public Result<Void> forbidden(NotPermissionException e) { return Result.fail(403, "无权执行此操作"); }
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public Result<Void> validation(Exception e) { return Result.fail(400, "请求参数校验失败"); }
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> duplicateKey(DuplicateKeyException e) {
        return Result.fail(400, "数据已存在，请检查账号或编码是否重复");
    }
    @ExceptionHandler(Exception.class) public Result<Void> other(Exception e) { return Result.fail(500, e.getMessage() == null ? "系统内部错误" : e.getMessage()); }
}
