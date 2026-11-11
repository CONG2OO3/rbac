package com.company.rbac.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.rbac.log.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
