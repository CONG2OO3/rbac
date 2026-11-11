package com.company.rbac.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.rbac.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_order")
public class BizOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String customerName;
    private String customerPhone;
    private String status;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private String remark;
}
