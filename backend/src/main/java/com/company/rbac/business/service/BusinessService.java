package com.company.rbac.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rbac.business.entity.BizOrder;
import com.company.rbac.business.entity.BizOrderItem;
import com.company.rbac.business.entity.BizProduct;
import com.company.rbac.business.mapper.BizOrderItemMapper;
import com.company.rbac.business.mapper.BizOrderMapper;
import com.company.rbac.business.mapper.BizProductMapper;
import com.company.rbac.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("999999999999.99");
    private static final DateTimeFormatter ORDER_NUMBER_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Map<String, List<String>> NEXT_STATUS = Map.of(
            "NEW", List.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", List.of("SHIPPED", "CANCELLED"),
            "SHIPPED", List.of("COMPLETED"),
            "COMPLETED", List.of(),
            "CANCELLED", List.of());

    private final BizProductMapper productMapper;
    private final BizOrderMapper orderMapper;
    private final BizOrderItemMapper itemMapper;

    public IPage<BizProduct> products(long pageNum, long pageSize, String keyword, String status) {
        checkPage(pageNum, pageSize);
        String search = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<BizProduct> query = new LambdaQueryWrapper<BizProduct>()
                .eq(status != null && !status.isBlank(), BizProduct::getStatus, status)
                .orderByDesc(BizProduct::getId);
        if (!search.isEmpty()) {
            query.and(item -> item.like(BizProduct::getProductName, search)
                    .or().like(BizProduct::getProductCode, search));
        }
        return productMapper.selectPage(new Page<>(pageNum, pageSize), query);
    }

    public List<BizProduct> productOptions() {
        return productMapper.selectList(new LambdaQueryWrapper<BizProduct>()
                .eq(BizProduct::getStatus, "0")
                .orderByAsc(BizProduct::getProductName));
    }

    @Transactional
    public Long saveProduct(Long id, ProductInput input) {
        validateProduct(input);
        BizProduct product = id == null ? new BizProduct() : productMapper.selectForUpdate(id);
        if (product == null) {
            throw new BusinessException("产品不存在或已删除");
        }
        Long duplicate = productMapper.selectList(new LambdaQueryWrapper<BizProduct>()
                        .eq(BizProduct::getProductCode, input.productCode().trim()))
                .stream().map(BizProduct::getId).findFirst().orElse(null);
        if (duplicate != null && !duplicate.equals(id)) {
            throw new BusinessException("产品编码已存在");
        }
        product.setProductCode(input.productCode().trim());
        product.setProductName(input.productName().trim());
        product.setCategory(defaultText(input.category(), ""));
        product.setUnit(defaultText(input.unit(), "件"));
        product.setPrice(input.price());
        product.setStock(input.stock());
        product.setStatus(input.status());
        product.setDescription(defaultText(input.description(), ""));
        if (id == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        return product.getId();
    }

    @Transactional
    public void removeProduct(Long id) {
        if (productMapper.selectForUpdate(id) == null) {
            throw new BusinessException("产品不存在或已删除");
        }
        Long references = itemMapper.selectCount(new LambdaQueryWrapper<BizOrderItem>()
                .eq(BizOrderItem::getProductId, id));
        if (references > 0) {
            throw new BusinessException("产品已被订单引用，请停用产品而非删除");
        }
        productMapper.physicalDelete(id);
    }

    public IPage<BizOrder> orders(long pageNum, long pageSize, String keyword, String status) {
        checkPage(pageNum, pageSize);
        String search = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<BizOrder> query = new LambdaQueryWrapper<BizOrder>()
                .eq(status != null && !status.isBlank(), BizOrder::getStatus, status)
                .orderByDesc(BizOrder::getId);
        if (!search.isEmpty()) {
            query.and(item -> item.like(BizOrder::getOrderNo, search)
                    .or().like(BizOrder::getCustomerName, search));
        }
        return orderMapper.selectPage(new Page<>(pageNum, pageSize), query);
    }

    public OrderDetail order(Long id) {
        BizOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在或已删除");
        }
        return new OrderDetail(order, orderItems(id));
    }

    @Transactional
    public Long saveOrder(Long id, OrderInput input) {
        validateOrder(input);
        BizOrder order = id == null ? new BizOrder() : orderMapper.selectForUpdate(id);
        if (order == null) {
            throw new BusinessException("订单不存在或已删除");
        }
        if (id != null && !"NEW".equals(order.getStatus())) {
            throw new BusinessException("只有待处理订单可以修改");
        }
        List<BizOrderItem> previous = id == null ? List.of() : orderItems(id);
        Set<Long> productIds = new HashSet<>();
        previous.forEach(item -> productIds.add(item.getProductId()));
        input.items().forEach(item -> productIds.add(item.productId()));
        Map<Long, BizProduct> products = lockProducts(productIds);

        for (BizOrderItem item : previous) {
            productMapper.releaseStock(item.getProductId(), item.getQuantity());
        }
        List<BizOrderItem> lines = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO;
        int quantity = 0;
        for (OrderLineInput requested : input.items()) {
            BizProduct product = products.get(requested.productId());
            if (product == null || !"0".equals(product.getStatus())) {
                throw new BusinessException("所选产品不存在或已停用");
            }
            if (productMapper.reserveStock(product.getId(), requested.quantity()) != 1) {
                throw new BusinessException("产品“" + product.getProductName() + "”库存不足");
            }
            BizOrderItem line = new BizOrderItem();
            line.setProductId(product.getId());
            line.setProductCode(product.getProductCode());
            line.setProductName(product.getProductName());
            line.setUnit(product.getUnit());
            line.setUnitPrice(product.getPrice());
            line.setQuantity(requested.quantity());
            line.setLineAmount(product.getPrice().multiply(BigDecimal.valueOf(requested.quantity())));
            if (line.getLineAmount().compareTo(MAX_ORDER_AMOUNT) > 0) {
                throw new BusinessException("订单产品金额超出允许范围");
            }
            lines.add(line);
            amount = amount.add(line.getLineAmount());
            if (amount.compareTo(MAX_ORDER_AMOUNT) > 0) {
                throw new BusinessException("订单总金额超出允许范围");
            }
            quantity = Math.addExact(quantity, requested.quantity());
        }

        order.setCustomerName(input.customerName().trim());
        order.setCustomerPhone(defaultText(input.customerPhone(), ""));
        order.setRemark(defaultText(input.remark(), ""));
        order.setTotalQuantity(quantity);
        order.setTotalAmount(amount);
        if (id == null) {
            order.setOrderNo("ORD" + LocalDateTime.now().format(ORDER_NUMBER_TIME)
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            order.setStatus("NEW");
            orderMapper.insert(order);
        } else {
            orderMapper.updateById(order);
            itemMapper.delete(new LambdaQueryWrapper<BizOrderItem>()
                    .eq(BizOrderItem::getOrderId, id));
        }
        for (BizOrderItem line : lines) {
            line.setOrderId(order.getId());
            itemMapper.insert(line);
        }
        return order.getId();
    }

    @Transactional
    public void changeOrderStatus(Long id, String target) {
        BizOrder order = orderMapper.selectForUpdate(id);
        if (order == null) {
            throw new BusinessException("订单不存在或已删除");
        }
        if (!NEXT_STATUS.getOrDefault(order.getStatus(), List.of()).contains(target)) {
            throw new BusinessException("当前订单状态不允许该操作");
        }
        if ("CANCELLED".equals(target)) {
            List<BizOrderItem> lines = orderItems(id);
            lockProducts(lines.stream().map(BizOrderItem::getProductId).collect(
                    java.util.stream.Collectors.toSet()));
            lines.forEach(item -> productMapper.releaseStock(
                    item.getProductId(), item.getQuantity()));
        }
        order.setStatus(target);
        orderMapper.updateById(order);
    }

    @Transactional
    public void removeOrder(Long id) {
        BizOrder order = orderMapper.selectForUpdate(id);
        if (order == null) {
            throw new BusinessException("订单不存在或已删除");
        }
        if (!"NEW".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("只有待处理或已取消订单可以删除");
        }
        List<BizOrderItem> lines = orderItems(id);
        if ("NEW".equals(order.getStatus())) {
            lockProducts(lines.stream().map(BizOrderItem::getProductId).collect(
                    java.util.stream.Collectors.toSet()));
            lines.forEach(item -> productMapper.releaseStock(
                    item.getProductId(), item.getQuantity()));
        }
        itemMapper.delete(new LambdaQueryWrapper<BizOrderItem>()
                .eq(BizOrderItem::getOrderId, id));
        orderMapper.deleteById(id);
    }

    private Map<Long, BizProduct> lockProducts(Set<Long> ids) {
        Map<Long, BizProduct> products = new HashMap<>();
        ids.stream().sorted(Comparator.naturalOrder()).forEach(id -> {
            BizProduct product = productMapper.selectForUpdate(id);
            if (product != null) {
                products.put(id, product);
            }
        });
        return products;
    }

    private List<BizOrderItem> orderItems(Long id) {
        return itemMapper.selectList(new LambdaQueryWrapper<BizOrderItem>()
                .eq(BizOrderItem::getOrderId, id)
                .orderByAsc(BizOrderItem::getId));
    }

    private void checkPage(long pageNum, long pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException("分页参数不正确");
        }
    }

    private void validateProduct(ProductInput input) {
        if (input == null || input.productCode() == null
                || !input.productCode().trim().matches("[A-Za-z0-9_-]{2,64}")) {
            throw new BusinessException("产品编码需为 2-64 位字母、数字、下划线或横线");
        }
        if (input.productName() == null || input.productName().isBlank()
                || input.productName().trim().length() > 120) {
            throw new BusinessException("产品名称不能为空且不能超过 120 字");
        }
        if (input.price() == null || input.price().signum() < 0
                || input.price().scale() > 2
                || input.price().compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new BusinessException("产品价格需为非负数，最多保留两位小数");
        }
        if (input.stock() == null || input.stock() < 0 || input.stock() > 100000000) {
            throw new BusinessException("产品库存需在 0-100000000 之间");
        }
        if (input.status() == null || !List.of("0", "1").contains(input.status())) {
            throw new BusinessException("产品状态不正确");
        }
        if (defaultText(input.category(), "").length() > 64
                || defaultText(input.unit(), "件").length() > 20
                || defaultText(input.description(), "").length() > 500) {
            throw new BusinessException("产品信息超出长度限制");
        }
    }

    private void validateOrder(OrderInput input) {
        if (input == null || input.customerName() == null
                || input.customerName().isBlank()
                || input.customerName().trim().length() > 80) {
            throw new BusinessException("客户名称不能为空且不能超过 80 字");
        }
        if (defaultText(input.customerPhone(), "").length() > 30
                || defaultText(input.remark(), "").length() > 500) {
            throw new BusinessException("客户电话或备注超出长度限制");
        }
        if (input.items() == null || input.items().isEmpty() || input.items().size() > 50) {
            throw new BusinessException("订单需包含 1-50 个产品");
        }
        Set<Long> uniqueProducts = new HashSet<>();
        for (OrderLineInput item : input.items()) {
            if (item == null || item.productId() == null || item.quantity() == null
                    || item.quantity() < 1 || item.quantity() > 10000) {
                throw new BusinessException("请选择产品，数量需为 1-10000 的整数");
            }
            if (!uniqueProducts.add(item.productId())) {
                throw new BusinessException("同一产品只能添加一次，请修改数量");
            }
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record ProductInput(
            String productCode,
            String productName,
            String category,
            String unit,
            BigDecimal price,
            Integer stock,
            String status,
            String description) {
    }

    public record OrderLineInput(Long productId, Integer quantity) {
    }

    public record OrderInput(
            String customerName,
            String customerPhone,
            String remark,
            List<OrderLineInput> items) {
    }

    public record OrderDetail(BizOrder order, List<BizOrderItem> items) {
    }
}
