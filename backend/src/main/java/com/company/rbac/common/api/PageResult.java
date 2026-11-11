package com.company.rbac.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

public record PageResult<T>(List<T> records, long total, long pageNum, long pageSize) {
    public static <T> PageResult<T> of(IPage<T> page) { return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()); }
}
