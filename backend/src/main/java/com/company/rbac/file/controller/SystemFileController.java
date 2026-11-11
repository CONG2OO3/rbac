package com.company.rbac.file.controller;

import com.company.rbac.common.api.PageResult;
import com.company.rbac.common.api.Result;
import com.company.rbac.file.entity.SysFile;
import com.company.rbac.file.service.FileService;
import com.company.rbac.log.annotation.OperationLog;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/system/file")
@RequiredArgsConstructor
public class SystemFileController {

    private final FileService fileService;

    @GetMapping("/list")
    public Result<PageResult<SysFile>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "12") long pageSize,
            @RequestParam String fileType,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(
                fileService.list(pageNum, pageSize, fileType, keyword)));
    }

    @PostMapping("/upload")
    @OperationLog(module = "文件管理", operation = "上传文件")
    public Result<SysFile> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam String fileType) {
        return Result.success(fileService.upload(file, fileType));
    }

    @DeleteMapping("/{ids}")
    @OperationLog(module = "文件管理", operation = "删除文件")
    public Result<Void> remove(@PathVariable String ids) {
        fileService.remove(ids);
        return Result.success();
    }

    @GetMapping("/{id}/content")
    public void content(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean download,
            HttpServletResponse response) throws IOException {
        SysFile file = fileService.get(id);
        response.setContentType(file.getContentType());
        response.setContentLengthLong(file.getFileSize());
        String encodedName = URLEncoder.encode(
                        file.getOriginalName(),
                        StandardCharsets.UTF_8)
                .replace("+", "%20");
        String disposition = download ? "attachment" : "inline";
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                disposition + "; filename*=UTF-8''" + encodedName);
        fileService.download(id, response.getOutputStream());
    }
}
