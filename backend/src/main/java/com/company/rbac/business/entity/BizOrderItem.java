package com.company.rbac.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("biz_order_item")
public class BizOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private String productCode;
    private String productName;
    private String unit;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineAmount;
}
