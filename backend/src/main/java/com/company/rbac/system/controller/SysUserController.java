package com.company.rbac.system.controller;

import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.api.Result;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.system.entity.SysUser;
import com.company.rbac.system.service.SystemCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {
    private final SystemCrudService service;

    @GetMapping("/list")
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(service.users(pageNum, pageSize, keyword)));
    }

    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Long id) { return Result.success(service.user(id)); }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> roles(@PathVariable Long id) { return Result.success(service.userRoleIds(id)); }

    @PutMapping("/{id}/roles")
    @OperationLog(module = "用户管理", operation = "分配用户角色")
    public Result<Void> roles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        service.setUserRoles(id, roleIds);
        return Result.success();
    }

    @PostMapping
    @OperationLog(module = "用户管理", operation = "新增用户")
    public Result<Void> add(@RequestBody SysUser user) { service.save(user); return Result.success(); }

    @PutMapping
    @OperationLog(module = "用户管理", operation = "修改用户")
    public Result<Void> edit(@RequestBody SysUser user) { service.update(user); return Result.success(); }

    @DeleteMapping("/{ids}")
    @OperationLog(module = "用户管理", operation = "删除用户")
    public Result<Void> remove(@PathVariable String ids) {
        service.removeUsers(Arrays.stream(ids.split(",")).map(Long::valueOf).toList());
        return Result.success();
    }
}
