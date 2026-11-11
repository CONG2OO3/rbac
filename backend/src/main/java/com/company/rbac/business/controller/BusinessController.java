package com.company.rbac.business.controller;

import com.company.rbac.business.entity.BizOrder;
import com.company.rbac.business.entity.BizProduct;
import com.company.rbac.business.service.BusinessService;
import com.company.rbac.business.service.BusinessService.OrderDetail;
import com.company.rbac.business.service.BusinessService.OrderInput;
import com.company.rbac.business.service.BusinessService.ProductInput;
import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.api.Result;
import com.company.rbac.log.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService service;

    @GetMapping("/products")
    public Result<PageResult<BizProduct>> products(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(
                service.products(pageNum, pageSize, keyword, status)));
    }

    @GetMapping("/products/options")
    public Result<List<BizProduct>> productOptions() {
        return Result.success(service.productOptions());
    }

    @PostMapping("/products")
    @OperationLog(module = "产品管理", operation = "新增产品")
    public Result<Map<String, Long>> addProduct(@RequestBody ProductInput input) {
        return Result.success(Map.of("id", service.saveProduct(null, input)));
    }

    @PutMapping("/products/{id}")
    @OperationLog(module = "产品管理", operation = "修改产品")
    public Result<Map<String, Long>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductInput input) {
        return Result.success(Map.of("id", service.saveProduct(id, input)));
    }

    @DeleteMapping("/products/{id}")
    @OperationLog(module = "产品管理", operation = "删除产品")
    public Result<Void> removeProduct(@PathVariable Long id) {
        service.removeProduct(id);
        return Result.success();
    }

    @GetMapping("/orders")
    public Result<PageResult<BizOrder>> orders(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(
                service.orders(pageNum, pageSize, keyword, status)));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderDetail> order(@PathVariable Long id) {
        return Result.success(service.order(id));
    }

    @PostMapping("/orders")
    @OperationLog(module = "订单管理", operation = "新增订单")
    public Result<Map<String, Long>> addOrder(@RequestBody OrderInput input) {
        return Result.success(Map.of("id", service.saveOrder(null, input)));
    }

    @PutMapping("/orders/{id}")
    @OperationLog(module = "订单管理", operation = "修改订单")
    public Result<Map<String, Long>> updateOrder(
            @PathVariable Long id,
            @RequestBody OrderInput input) {
        return Result.success(Map.of("id", service.saveOrder(id, input)));
    }

    @PutMapping("/orders/{id}/status")
    @OperationLog(module = "订单管理", operation = "更新订单状态")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        service.changeOrderStatus(id, request.get("status"));
        return Result.success();
    }

    @DeleteMapping("/orders/{id}")
    @OperationLog(module = "订单管理", operation = "删除订单")
    public Result<Void> removeOrder(@PathVariable Long id) {
        service.removeOrder(id);
        return Result.success();
    }
}
