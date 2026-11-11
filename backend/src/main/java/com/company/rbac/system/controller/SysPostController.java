package com.company.rbac.system.controller;

import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.api.Result;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.system.entity.SysPost;
import com.company.rbac.system.service.SystemCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;

@RestController
@RequestMapping("/system/post")
@RequiredArgsConstructor
public class SysPostController {
    private final SystemCrudService service;

    @GetMapping("/list")
    public Result<PageResult<SysPost>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(service.posts(pageNum, pageSize, keyword)));
    }

    @PostMapping
    @OperationLog(module = "岗位管理", operation = "新增岗位")
    public Result<Void> add(@RequestBody SysPost post) {
        service.save(post);
        return Result.success();
    }

    @PutMapping
    @OperationLog(module = "岗位管理", operation = "修改岗位")
    public Result<Void> edit(@RequestBody SysPost post) {
        service.update(post);
        return Result.success();
    }

    @DeleteMapping("/{ids}")
    @OperationLog(module = "岗位管理", operation = "删除岗位")
    public Result<Void> remove(@PathVariable String ids) {
        service.removePosts(Arrays.stream(ids.split(",")).map(Long::valueOf).toList());
        return Result.success();
    }
}
