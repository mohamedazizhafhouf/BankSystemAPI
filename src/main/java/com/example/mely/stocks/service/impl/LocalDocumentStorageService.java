package com.example.mely.stocks.service.impl;

import com.example.mely.config.StockStorageProperties;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class LocalDocumentStorageService {

    private final Path basePath;

    public LocalDocumentStorageService(StockStorageProperties properties) {
        this.basePath = Path.of(properties.getBasePath()).toAbsolutePath().normalize();
    }

    public StoredDocument store(UUID requestId, MultipartFile file) {
        try {
            Files.createDirectories(basePath.resolve(requestId.toString()));
            String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename());
            String storedFileName = UUID.randomUUID() + "-" + originalName.replace(" ", "_");
            Path targetPath = basePath.resolve(requestId.toString()).resolve(storedFileName).normalize();

            String checksum = calculateChecksum(file.getInputStream());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return new StoredDocument(storedFileName, targetPath.toString(), checksum, file.getSize(), file.getContentType());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store uploaded document", ex);
        }
    }

    private String calculateChecksum(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    @Getter
    public static class StoredDocument {
        private final String storedFileName;
        private final String storagePath;
        private final String checksum;
        private final long size;
        private final String contentType;

        public StoredDocument(String storedFileName, String storagePath, String checksum, long size, String contentType) {
            this.storedFileName = storedFileName;
            this.storagePath = storagePath;
            this.checksum = checksum;
            this.size = size;
            this.contentType = contentType == null ? "application/pdf" : contentType;
        }
    }
}
