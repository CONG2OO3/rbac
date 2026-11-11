package com.company.rbac.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String originalName;
    private String fileSuffix;
    private String objectKey;
    private Long fileSize;
    private String contentType;
    private String fileType;
    private String storageType;
    private String uploader;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String accessUrl;
}
