package com.better.CommuteMate.global.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileStorageService {
    @Value("${storage.path}")
    private String baseDir;

    public FileUploadResult uploadImage(MultipartFile file) {
        return upload(file, "faq/images");
    }

    public FileUploadResult uploadFile(MultipartFile file) {
        return upload(file, "faq/files");
    }

    private FileUploadResult upload(MultipartFile file, String subDir) {
        try {
            String originalName = file.getOriginalFilename();
            String extension = originalName.substring(originalName.lastIndexOf("."));
            String fileName = UUID.randomUUID() + extension;

            Path path = Paths.get(baseDir, subDir, fileName);

            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            String url = "https://kusd.konkuk.ac.kr/" + subDir + "/" + fileName;

            return new FileUploadResult(url, path.toString());

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패");
        }
    }

    public void deleteFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", path);
        }
    }
}
