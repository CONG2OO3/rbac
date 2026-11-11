package com.company.rbac.file.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.rbac.common.exception.BusinessException;
import com.company.rbac.file.entity.SysFile;
import com.company.rbac.file.mapper.SysFileMapper;
import com.company.rbac.file.storage.StorageStrategy;
import com.company.rbac.file.storage.StorageStrategyFactory;
import com.company.rbac.system.entity.SysUser;
import com.company.rbac.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Duration URL_DURATION = Duration.ofHours(1);
    private static final long STANDARD_FILE_SIZE_LIMIT = 50L * 1024 * 1024;
    private static final long VIDEO_FILE_SIZE_LIMIT = 500L * 1024 * 1024;
    private static final List<String> FILE_TYPES = List.of("image", "attachment", "video");

    private final SysFileMapper fileMapper;
    private final SysUserMapper userMapper;
    private final StorageStrategyFactory strategyFactory;

    public IPage<SysFile> list(
            long pageNum,
            long pageSize,
            String fileType,
            String keyword) {
        validateFileType(fileType);
        LambdaQueryWrapper<SysFile> query = new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getFileType, fileType)
                .like(keyword != null && !keyword.isBlank(), SysFile::getOriginalName, keyword)
                .orderByDesc(SysFile::getId);
        IPage<SysFile> page = fileMapper.selectPage(new Page<>(pageNum, pageSize), query);
        page.getRecords().forEach(file -> {
            try {
                file.setAccessUrl(strategyFactory.get(file.getStorageType())
                        .temporaryUrl(file.getObjectKey(), URL_DURATION));
            } catch (BusinessException exception) {
                file.setAccessUrl("");
            }
        });
        return page;
    }

    @Transactional
    public SysFile upload(MultipartFile multipartFile, String fileType) {
        validateFileType(fileType);
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException("请选择需要上传的文件");
        }
        long sizeLimit = "video".equals(fileType)
                ? VIDEO_FILE_SIZE_LIMIT
                : STANDARD_FILE_SIZE_LIMIT;
        if (multipartFile.getSize() > sizeLimit) {
            throw new BusinessException("video".equals(fileType)
                    ? "单个视频不能超过 500MB"
                    : "单个文件不能超过 50MB");
        }
        String contentType = multipartFile.getContentType() == null
                ? "application/octet-stream"
                : multipartFile.getContentType();
        if ("image".equals(fileType) && !contentType.startsWith("image/")) {
            throw new BusinessException("图片管理仅支持上传图片文件");
        }
        if ("video".equals(fileType) && !contentType.startsWith("video/")) {
            throw new BusinessException("视频管理仅支持上传视频文件");
        }

        String originalName = safeName(multipartFile.getOriginalFilename());
        String suffix = suffix(originalName);
        String objectKey = buildObjectKey(fileType, suffix);
        StorageStrategy strategy = strategyFactory.current();
        if ("starimg".equals(strategy.type()) && !"image".equals(fileType)) {
            throw new BusinessException("星云图仅支持图片上传");
        }
        try {
            objectKey = strategy.upload(
                    objectKey,
                    multipartFile.getInputStream(),
                    multipartFile.getSize(),
                    contentType);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("文件上传失败：" + exception.getMessage());
        }

        SysFile file = new SysFile();
        file.setFileName(objectKey.substring(objectKey.lastIndexOf('/') + 1));
        file.setOriginalName(originalName);
        file.setFileSuffix(suffix);
        file.setObjectKey(objectKey);
        file.setFileSize(multipartFile.getSize());
        file.setContentType(contentType);
        file.setFileType(fileType);
        file.setStorageType(strategy.type());
        file.setUploader(currentUserName());
        file.setCreateTime(LocalDateTime.now());
        try {
            fileMapper.insert(file);
        } catch (Exception exception) {
            try {
                strategy.delete(objectKey);
            } catch (Exception cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
        file.setAccessUrl(strategy.temporaryUrl(objectKey, URL_DURATION));
        return file;
    }

    @Transactional
    public void remove(String ids) {
        Arrays.stream(ids.split(","))
                .map(Long::valueOf)
                .forEach(id -> {
                    SysFile file = get(id);
                    strategyFactory.get(file.getStorageType()).delete(file.getObjectKey());
                    fileMapper.deleteById(id);
                });
    }

    public SysFile get(Long id) {
        SysFile file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException("文件不存在或已被删除");
        }
        return file;
    }

    public void download(Long id, OutputStream outputStream) {
        SysFile file = get(id);
        strategyFactory.get(file.getStorageType())
                .download(file.getObjectKey(), outputStream);
    }

    private void validateFileType(String fileType) {
        if (!FILE_TYPES.contains(fileType)) {
            throw new BusinessException("不支持的文件类型");
        }
    }

    private String buildObjectKey(String fileType, String suffix) {
        String folder = switch (fileType) {
            case "image" -> "images";
            case "video" -> "videos";
            default -> "attachments";
        };
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return folder + "/" + date + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    private String safeName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "未命名文件";
        }
        return originalName.replace("\\", "/")
                .substring(originalName.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private String suffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        String suffix = fileName.substring(index).toLowerCase(Locale.ROOT);
        return suffix.length() <= 20 ? suffix : "";
    }

    private String currentUserName() {
        if (!StpUtil.isLogin()) {
            return "匿名用户";
        }
        SysUser user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        return user == null ? String.valueOf(StpUtil.getLoginId()) : user.getNickName();
    }
}
