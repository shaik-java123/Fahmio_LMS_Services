package com.lms.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.PublicAccessType;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.UUID;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    @Value("${azure.storage.connection-string:}")
    private String connectionString;

    @Value("${azure.storage.container-name:lms-media}")
    private String containerName;

    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    @Value("${app.upload.path:./uploads}")
    private String localUploadPath;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    private BlobContainerClient containerClient;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private S3Client s3Client;

    @PostConstruct
    public void init() {
        log.info("INITIALIZING MEDIASERVICE: Checking storage providers...");
        if (connectionString != null && !connectionString.isEmpty()) {
            try {
                log.info("Azure connection string detected. Attempting to build BlobServiceClient...");
                BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                        .connectionString(connectionString)
                        .buildClient();
                this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
                
                if (!containerClient.exists()) {
                    containerClient.create();
                    log.info("AZURE ACTIVATED: Container '{}' created successfully.", containerName);
                }
                
                // Ensure the container allows public BLOB access (important for thumbnails)
                try {
                    containerClient.setAccessPolicy(PublicAccessType.BLOB, null);
                    log.info("AZURE SECURITY: Public Blob access enabled for container '{}'.", containerName);
                } catch(Exception e) {
                    log.error("AZURE SECURITY: Failed to set public access on existing container: {}", e.getMessage());
                }
                
            } catch(Exception e) {
                log.error("AZURE INITIALIZATION FAILED: Check your connection string. Error: {}", e.getMessage());
            }
        } else {
            log.warn("Azure connection string is missing. Defaulting to other providers.");
        }

        // Initialize local path
        try {
            Files.createDirectories(Paths.get(localUploadPath));
        } catch (IOException e) {
            log.error("Failed to create local upload directory: {}", e.getMessage());
        }
    }

    // Overload for byte[] content (useful for generated files like certificates)
    public String uploadFile(byte[] content, String originalFilename, String contentType, String folder) throws IOException {
        if (containerClient != null) {
            String fileName = buildKey(folder, originalFilename);
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            blobClient.upload(new java.io.ByteArrayInputStream(content), content.length, true);
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
            return blobClient.getBlobUrl();
        }
        
        // Fallback to local
        String fileName = buildKey(folder, originalFilename);
        Path path = Paths.get(localUploadPath, fileName).toAbsolutePath().normalize();
        Files.createDirectories(path.getParent());
        Files.write(path, content);
        return backendUrl + "/api/media/files/" + fileName;
    }

    public String uploadFile(MultipartFile file, String folder, boolean forceLocal) throws IOException {
        if (forceLocal) {
            return uploadFileLocal(file, folder);
        }

        if (containerClient != null) {
            String fileName = buildKey(folder, file.getOriginalFilename());
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            blobClient.upload(file.getInputStream(), file.getSize(), true);
            
            // Set content type so browsers display it as an image
            try {
                blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(file.getContentType()));
            } catch(Exception e) {
                log.warn("Failed to set content type for blob: {}", e.getMessage());
            }
            
            log.info("File uploaded to Azure Blob Storage: {}", fileName);
            return blobClient.getBlobUrl();
        } 
        
        if (s3Client != null && bucketName != null && !bucketName.isEmpty() && !bucketName.contains("your_")) {
            String fileName = buildKey(folder, file.getOriginalFilename());
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("File uploaded to AWS S3: {}", fileName);
            // Return public S3 URL
            return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
        }

        // Local storage as fallback
        return uploadFileLocal(file, folder);
    }

    private String uploadFileLocal(MultipartFile file, String folder) throws IOException {
        String fileName = buildKey(folder, file.getOriginalFilename());
        log.debug("Step 1: Generated key: {}", fileName);
        
        Path path = Paths.get(localUploadPath, fileName).toAbsolutePath().normalize();
        log.debug("Step 2: Resolved Absolute Path: {}", path);
        
        Files.createDirectories(path.getParent());
        log.debug("Step 3: Created parent directory: {}", path.getParent());
        
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        log.info("Step 4: Local file stored successfully at: {}", path);
        
        String url = backendUrl + "/api/media/files/" + fileName;
        log.debug("Step 5: Returning local URL: {}", url);
        return url;
    }

    public PresignedUploadResponse generatePresignedUrl(String folder, String fileName, String contentType) {
        if (containerClient == null) throw new IllegalStateException("Azure Blob Storage is not configured");

        String key = buildKey(folder, fileName);
        BlobClient blobClient = containerClient.getBlobClient(key);

        BlobSasPermission permission = new BlobSasPermission()
                .setReadPermission(true)
                .setWritePermission(true)
                .setCreatePermission(true);

        BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusHours(1), permission);
        
        String sasToken = blobClient.generateSas(sasSignatureValues);
        String uploadUrl = blobClient.getBlobUrl() + "?" + sasToken;
        String fileUrl = blobClient.getBlobUrl();

        return new PresignedUploadResponse(uploadUrl, fileUrl, key);
    }

    public void deleteFile(String key) {
        if (containerClient == null) return;

        try {
            BlobClient blobClient = containerClient.getBlobClient(key);
            blobClient.delete();
            log.info("Deleted Azure blob object: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete Azure blob {}: {}", key, e.getMessage());
        }
    }

    private String buildKey(String folder, String fileName) {
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID() + extension;
    }

    public record PresignedUploadResponse(String uploadUrl, String fileUrl, String s3Key) {}
}
