package com.YuanQi.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


@RestController
@RequestMapping("/file")
public class FileController {
    @Value("${file.upload-path}")
    private String UPLOAD_DIR;

    @PostMapping("/upload")
    // 保存文件到服务器
    public String saveFile(@RequestPart("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "文件名获取失败";
        }
        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
        }
        String randomFilename = UUID.randomUUID().toString() + fileExtension;

        try {
            File directory = new File(UPLOAD_DIR);
            String paths = directory.getCanonicalPath();
            File dest = new File(paths + '/' + randomFilename);
            file.transferTo(dest);
            return "http://47.105.51.84/api/file/" + randomFilename;
        } catch (IOException e) {
            e.printStackTrace();
            return "文件保存失败: " + e.getMessage();
        }
    }

    /**
     * 预览文件
     */
    @GetMapping("/{filename}")
    public void preview(@PathVariable String filename, HttpServletResponse response) throws IOException {
        File file = new File(UPLOAD_DIR, filename);
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("File not found");
            return;
        }

        response.setContentType(getContentType(filename));
        response.setHeader("Content-Disposition", "inline; filename=\"" + encodeFilename(filename) + "\"");
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{filename}")
    public void download(@PathVariable String filename, HttpServletResponse response) throws IOException {
        File file = new File(UPLOAD_DIR, filename);
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("File not found");
            return;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodeFilename(filename) + "\"");
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 获取文件Content-Type
     */
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            default -> "application/octet-stream";
        };
    }

    /**
     * 编码文件名（支持中文）
     */
    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

