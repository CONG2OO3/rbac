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
@TableName("biz_product")
public class BizProduct extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String productCode;
    private String productName;
    private String category;
    private String unit;
    private BigDecimal price;
    private Integer stock;
    private String status;
    private String description;
}
