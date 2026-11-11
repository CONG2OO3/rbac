package com.company.rbac.system.controller;

import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.api.Result;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.system.entity.SysRole;
import com.company.rbac.system.service.SystemCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {
    private final SystemCrudService service;

    @GetMapping("/list")
    public Result<PageResult<SysRole>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(service.roles(pageNum, pageSize, keyword)));
    }

    @GetMapping("/{id}/menus")
    public Result<List<Long>> menus(@PathVariable Long id) { return Result.success(service.roleMenuIds(id)); }

    @PutMapping("/{id}/menus")
    @OperationLog(module = "角色管理", operation = "分配角色菜单")
    public Result<Void> menus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        service.setRoleMenus(id, menuIds);
        return Result.success();
    }

    @PostMapping
    @OperationLog(module = "角色管理", operation = "新增角色")
    public Result<Void> add(@RequestBody SysRole role) { service.save(role); return Result.success(); }

    @PutMapping
    @OperationLog(module = "角色管理", operation = "修改角色")
    public Result<Void> edit(@RequestBody SysRole role) { service.update(role); return Result.success(); }

    @DeleteMapping("/{ids}")
    @OperationLog(module = "角色管理", operation = "删除角色")
    public Result<Void> remove(@PathVariable String ids) {
        service.removeRoles(Arrays.stream(ids.split(",")).map(Long::valueOf).toList());
        return Result.success();
    }
}
