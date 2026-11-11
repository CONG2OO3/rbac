package com.company.rbac.system.controller;

import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.api.Result;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.system.entity.SysMenu;
import com.company.rbac.system.service.SystemCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {
    private final SystemCrudService service;

    @GetMapping("/list")
    public Result<PageResult<SysMenu>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(service.menus(pageNum, pageSize, keyword)));
    }

    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() { return Result.success(service.menuTree()); }

    @PostMapping
    @OperationLog(module = "菜单管理", operation = "新增菜单")
    public Result<Void> add(@RequestBody SysMenu menu) { service.save(menu); return Result.success(); }

    @PutMapping
    @OperationLog(module = "菜单管理", operation = "修改菜单")
    public Result<Void> edit(@RequestBody SysMenu menu) { service.update(menu); return Result.success(); }

    @DeleteMapping("/{ids}")
    @OperationLog(module = "菜单管理", operation = "删除菜单")
    public Result<Void> remove(@PathVariable String ids) {
        service.removeMenus(Arrays.stream(ids.split(",")).map(Long::valueOf).toList());
        return Result.success();
    }
}
