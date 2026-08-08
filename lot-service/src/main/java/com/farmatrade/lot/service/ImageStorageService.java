package com.farmatrade.lot.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {

    private final Path uploadDirectory =
            Paths.get("uploads/lots");

    public ImageStorageService() throws IOException {

        Files.createDirectories(uploadDirectory);
    }

    // SAVE IMAGE
    public String saveImage(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName =
                file.getOriginalFilename();

        String extension = "";

        if (originalFileName != null &&
                originalFileName.contains(".")) {

            extension = originalFileName.substring(
                    originalFileName.lastIndexOf("."));
        }

        String fileName =
                UUID.randomUUID() + extension;

        Path filePath =
                uploadDirectory.resolve(fileName);

        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        return "/uploads/lots/" + fileName;
    }
    
    // DELETE OLD IMAGE
    
    public void deleteImage(String imageUrl) throws IOException {

        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String fileName = Paths.get(imageUrl).getFileName().toString();

        Path filePath = uploadDirectory.resolve(fileName);

        if (Files.exists(filePath)) {

            Files.delete(filePath);

            System.out.println(
                    "Old image deleted successfully: "
                    + fileName
            );
        }
    }
}