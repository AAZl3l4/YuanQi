package com.YuanQi.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;


@RestController
@RequestMapping("/file")
public class FileController {
    private static final String UPLOAD_DIR = "src/main/resources/files/";

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
            File dest = new File(paths+'/' + randomFilename);
            file.transferTo(dest);
            return "http://localhost:8080/file/" + randomFilename;
        } catch (IOException e) {
            e.printStackTrace();
            return "文件保存失败: " + e.getMessage();
        }
    }

    // 预览文件
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> preview(@PathVariable String filename) {
        File file = new File(UPLOAD_DIR + filename);
        if (!file.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Resource resource;
        try {
            resource = new UrlResource(file.toURI());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // 下载文件
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        File file = new File(UPLOAD_DIR + filename);
        if (!file.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Resource resource;
        try {
            resource = new UrlResource(file.toURI());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

}

