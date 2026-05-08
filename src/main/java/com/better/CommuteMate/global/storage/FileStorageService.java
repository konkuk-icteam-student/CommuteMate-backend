package com.better.CommuteMate.global.storage;

import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.StorageErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
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

    @Value("${storage.url}")
    private String baseUrl;

    private static final List<String> ALLOWED_IMAGE_EXTENSION = List.of(".jpg", ".jpeg", ".png");
    private static final List<String> ALLOWED_FILE_EXTENSION = List.of(".pdf", ".doc", ".docx", ".xls", ".xlsx");

    public FileUploadResult uploadImage(MultipartFile file) {
        return upload(file, "faq/images", ALLOWED_IMAGE_EXTENSION);
    }

    public FileUploadResult uploadFile(MultipartFile file) {
        return upload(file, "faq/files", ALLOWED_FILE_EXTENSION);
    }

    private FileUploadResult upload(MultipartFile file, String subDir,  List<String> allowedExtension) {

        validateFile(file, allowedExtension);

        try {
            String originalName = file.getOriginalFilename();
            String extension = originalName.substring(originalName.lastIndexOf("."));
            String fileName = UUID.randomUUID() + extension;

            Path path = Paths.get(baseDir, subDir, fileName);

            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            String url = baseUrl + "/" + subDir + "/" + fileName;

            return new FileUploadResult(url, path.toString());

        } catch (IOException e) {
            throw new CustomException(StorageErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void validateFile(MultipartFile file, List<String> allowedExt) {

        if (file == null || file.isEmpty()) {
            throw new CustomException(StorageErrorCode.FILE_EMPTY);
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || !originalName.contains(".")) {
            throw new CustomException(StorageErrorCode.FILE_INVALID_NAME);
        }

        String extension = originalName.substring(originalName.lastIndexOf("."));

        if (!allowedExt.contains(extension)) {
            throw new CustomException(StorageErrorCode.FILE_UNSUPPORTED_TYPE);
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
