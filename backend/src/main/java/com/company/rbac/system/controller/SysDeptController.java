package com.company.rbac.system.controller;

import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.api.Result;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.system.entity.SysDept;
import com.company.rbac.system.service.SystemCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;

@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class SysDeptController {
    private final SystemCrudService service;

    @GetMapping("/list")
    public Result<PageResult<SysDept>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(service.depts(pageNum, pageSize, keyword)));
    }

    @PostMapping
    @OperationLog(module = "部门管理", operation = "新增部门")
    public Result<Void> add(@RequestBody SysDept dept) { service.save(dept); return Result.success(); }

    @PutMapping
    @OperationLog(module = "部门管理", operation = "修改部门")
    public Result<Void> edit(@RequestBody SysDept dept) { service.update(dept); return Result.success(); }

    @DeleteMapping("/{ids}")
    @OperationLog(module = "部门管理", operation = "删除部门")
    public Result<Void> remove(@PathVariable String ids) {
        service.removeDepts(Arrays.stream(ids.split(",")).map(Long::valueOf).toList());
        return Result.success();
    }
}
