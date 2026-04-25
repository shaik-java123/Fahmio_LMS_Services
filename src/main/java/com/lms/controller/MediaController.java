package com.lms.controller;

import com.lms.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    @Value("${app.upload.path:./uploads}")
    private String localUploadPath;

    private final MediaService mediaService;

    /** Direct upload — for thumbnails, avatars, PDFs */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            @RequestParam(value = "forceLocal", defaultValue = "false") boolean forceLocal) {
        try {
            log.info("Starting media upload for file: {}, folder: {}, forceLocal: {}", file.getOriginalFilename(), folder, forceLocal);
            String url = mediaService.uploadFile(file, folder, forceLocal);
            log.info("Upload successful! URL: {}", url);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("UPLOAD FAILED in controller: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal Error during upload"));
        }
    }

    /** Pre-signed URL — for large video uploads directly from the browser */
    @GetMapping("/presigned-url")
    public ResponseEntity<MediaService.PresignedUploadResponse> presignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam(value = "folder", defaultValue = "videos") String folder) {
        return ResponseEntity.ok(mediaService.generatePresignedUrl(folder, fileName, contentType));
    }

    /** Delete a file safely */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> delete(@RequestParam String key) {
        mediaService.deleteFile(key);
        return ResponseEntity.ok(Map.of("message", "File deleted"));
    }

    /** Serve Local File System Uploads */
    @GetMapping("/files/{folder}/{fileName}")
    public ResponseEntity<Resource> serveFile(@PathVariable String folder, @PathVariable String fileName) {
        try {
            // Match the path in MediaService
            Path filePath = Paths.get(localUploadPath).resolve(folder).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                 // Set content type automatically based on file extension
                 String contentType = java.nio.file.Files.probeContentType(filePath);
                 if (contentType == null) contentType = "application/octet-stream";

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
