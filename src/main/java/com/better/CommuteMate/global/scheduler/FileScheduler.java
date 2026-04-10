package com.better.CommuteMate.global.scheduler;

import com.better.CommuteMate.domain.faq.entity.FaqImage;
import com.better.CommuteMate.domain.faq.entity.FaqFile;
import com.better.CommuteMate.domain.faq.repository.FaqImageRepository;
import com.better.CommuteMate.domain.faq.repository.FaqFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileScheduler {

    private final FaqImageRepository faqImageRepository;
    private final FaqFileRepository faqFileRepository;

    // 24시간 지난 orphan 파일 삭제
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanFiles() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        List<FaqImage> images = faqImageRepository.findOrphanImages(cutoffTime);

        for (FaqImage img : images) {
            deleteFile(img.getStoragePath());
            faqImageRepository.delete(img);
        }

        List<FaqFile> files = faqFileRepository.findOrphanFiles(cutoffTime);

        for (FaqFile file : files) {
            deleteFile(file.getStoragePath());
            faqFileRepository.delete(file);
        }

        log.info("orphan 파일 정리 완료");
    }

    private void deleteFile(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.error("orphan 파일 삭제 실패: {}", path);
        }
    }
}