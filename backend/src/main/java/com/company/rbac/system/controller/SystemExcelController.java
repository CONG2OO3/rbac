package com.company.rbac.system.controller;

import com.alibaba.excel.EasyExcel;
import com.company.rbac.common.api.Result;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.log.annotation.OperationLog;
import com.company.rbac.system.service.SystemCrudService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/excel")
@RequiredArgsConstructor
public class SystemExcelController {

    private static final Map<String, List<String>> FIELDS = new LinkedHashMap<>();

    static {
        FIELDS.put("users", List.of("userName", "nickName", "phone", "email", "sex", "deptId", "status", "remark"));
        FIELDS.put("roles", List.of("roleName", "roleKey", "roleSort", "dataScope", "status", "remark"));
        FIELDS.put("menus", List.of("menuName", "parentId", "orderNum", "path", "component", "menuType", "visible", "status", "perms", "icon", "remark"));
        FIELDS.put("departments", List.of("deptName", "parentId", "orderNum", "leader", "phone", "email", "status"));
        FIELDS.put("positions", List.of("postName", "postCode", "postSort", "status", "remark"));
    }

    private final SystemCrudService service;

    @PostMapping("/{resource}/export")
    @OperationLog(module = "数据导出", operation = "导出系统数据")
    public void export(@PathVariable String resource, HttpServletResponse response) throws IOException {
        List<String> fields = fields(resource);
        String fileName = URLEncoder.encode(resource + "-export", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");

        List<List<String>> head = fields.stream().map(field -> List.of(field)).toList();
        List<List<Object>> data = service.all(resource).stream()
                .map(item -> fields.stream()
                        .map(field -> new BeanWrapperImpl(item).getPropertyValue(field))
                        .toList())
                .toList();
        EasyExcel.write(response.getOutputStream())
                .head(head)
                .autoCloseStream(false)
                .sheet(resource)
                .doWrite(data);
    }

    @PostMapping("/{resource}/import")
    @OperationLog(module = "数据导入", operation = "导入系统数据")
    public Result<Map<String, Object>> importFile(
            @PathVariable String resource,
            @RequestPart("file") MultipartFile file) throws IOException {
        List<String> fields = fields(resource);
        if (file.isEmpty()) {
            throw new BusinessException("请选择要导入的 Excel 文件");
        }
        List<Map<Integer, String>> source = EasyExcel.read(file.getInputStream())
                .headRowNumber(1)
                .sheet()
                .doReadSync();
        List<Map<String, String>> rows = new ArrayList<>();
        for (Map<Integer, String> sourceRow : source) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int index = 0; index < fields.size(); index++) {
                String value = sourceRow.get(index);
                if (value != null && !value.isBlank()) {
                    row.put(fields.get(index), value.trim());
                }
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        service.importRows(resource, rows);
        return Result.success(Map.of("count", rows.size(), "message", "导入成功"));
    }

    private List<String> fields(String resource) {
        List<String> fields = FIELDS.get(resource);
        if (fields == null) {
            throw new BusinessException("不支持的数据类型");
        }
        return fields;
    }
}
