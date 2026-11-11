package com.company.rbac.common.web;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
@Component public class AuditMetaObjectHandler implements MetaObjectHandler {
    public void insertFill(MetaObject m){ strictInsertFill(m,"createTime",LocalDateTime.class,LocalDateTime.now()); strictInsertFill(m,"updateTime",LocalDateTime.class,LocalDateTime.now()); strictInsertFill(m,"deleted",Integer.class,0); }
    public void updateFill(MetaObject m){ strictUpdateFill(m,"updateTime",LocalDateTime.class,LocalDateTime.now()); }
}
