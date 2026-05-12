package com.better.CommuteMate.faq.application;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqFile;
import com.better.CommuteMate.domain.faq.entity.FaqHistory;
import com.better.CommuteMate.domain.faq.entity.FaqImage;
import com.better.CommuteMate.domain.faq.repository.FaqFileRepository;
import com.better.CommuteMate.domain.faq.repository.FaqHistoryRepository;
import com.better.CommuteMate.domain.faq.repository.FaqImageRepository;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.faq.application.dto.request.FaqSearchScope;
import com.better.CommuteMate.faq.application.dto.request.PostFaqRequest;
import com.better.CommuteMate.faq.application.dto.request.PutFaqUpdateRequest;
import com.better.CommuteMate.faq.application.dto.response.GetFaqDetailResponse;
import com.better.CommuteMate.faq.application.dto.response.GetFaqListResponse;
import com.better.CommuteMate.faq.application.dto.response.GetFaqListWrapper;
import com.better.CommuteMate.faq.application.dto.response.PostFaqFileResponse;
import com.better.CommuteMate.faq.application.dto.response.PostFaqImageResponse;
import com.better.CommuteMate.faq.application.dto.response.PostFaqResponse;
import com.better.CommuteMate.faq.application.dto.response.PutFaqUpdateResponse;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import com.better.CommuteMate.global.exceptions.error.FaqErrorCode;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.storage.FileStorageService;
import com.better.CommuteMate.global.storage.FileUploadResult;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FaqService {

    private final FaqRepository faqRepository;
    private final FaqHistoryRepository faqHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final FaqImageRepository faqImageRepository;
    private final FaqFileRepository faqFileRepository;
    private final FileStorageService fileStorageService;

    public PostFaqResponse createFaq(Long userId, PostFaqRequest request) {

        User writer = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        if (request.categoryIds().size() > 3) {
            throw CustomException.of(FaqErrorCode.CATEGORY_LIMIT_EXCEEDED);
        }

        List<Category> categories = categoryRepository.findAllById(request.categoryIds());

        if (categories.size() != request.categoryIds().size()) {
            throw CustomException.of(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }

        Faq faq = Faq.create(
                request.title(),
                request.complainantName(),
                request.content(),
                request.answer(),
                request.etc(),
                categories,
                writer
        );

        faqRepository.save(faq);

        List<String> imageUrls = extractImageUrls(request.content(), request.answer());
        if (!imageUrls.isEmpty()) {
            List<FaqImage> images = faqImageRepository.findByUrlIn(imageUrls);
            images.forEach(faq::addImage);
        }

        if (request.fileUrls() != null && !request.fileUrls().isEmpty()) {
            List<FaqFile> files = faqFileRepository.findByUrlIn(request.fileUrls());
            files.forEach(faq::addFile);
        }

        FaqHistory faqhistory = FaqHistory.create(faq);

        faqHistoryRepository.save(faqhistory);

        return new PostFaqResponse(faq.getId());
    }

    public PutFaqUpdateResponse updateFaq(Long userId, Long faqId, PutFaqUpdateRequest request) {
        User modifier = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> CustomException.of(FaqErrorCode.FAQ_NOT_FOUND));

        if (Boolean.TRUE.equals(faq.getDeletedFlag())) {
            throw CustomException.of(FaqErrorCode.FAQ_ALREADY_DELETED);
        }

        List<Category> categories = new ArrayList<>();

        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {

            if (request.categoryIds().size() > 3) {
                throw CustomException.of(FaqErrorCode.CATEGORY_LIMIT_EXCEEDED);
            }

            categories = categoryRepository.findAllById(request.categoryIds());

            if (categories.size() != request.categoryIds().size()) {
                throw CustomException.of(CategoryErrorCode.CATEGORY_NOT_FOUND);
            }
        }

        faq.update(
                request.title(),
                request.complainantName(),
                request.content(),
                request.answer(),
                request.etc(),
                categories,
                modifier
        );

        faq.getImages().forEach(FaqImage::detachFaq);
        faq.getFiles().forEach(FaqFile::detachFaq);

        List<String> imageUrls = extractImageUrls(request.content(), request.answer());
        if (!imageUrls.isEmpty()) {
            List<FaqImage> images = faqImageRepository.findByUrlIn(imageUrls);
            images.forEach(faq::addImage);
        }

        if (request.fileUrls() != null && !request.fileUrls().isEmpty()) {
            List<FaqFile> files = faqFileRepository.findByUrlIn(request.fileUrls());
            files.forEach(faq::addFile);
        }

        faqRepository.save(faq);

        faqHistoryRepository.deleteByFaqIdAndEditedAt(faqId, LocalDate.now());

        FaqHistory faqhistory = FaqHistory.create(faq);
        faqHistoryRepository.save(faqhistory);

        return new PutFaqUpdateResponse(faqId);
    }

    @Transactional(readOnly = true)
    public GetFaqListWrapper getFaqList(Long teamId, Long categoryId, String keyword, FaqSearchScope searchScope, LocalDate startDate, LocalDate endDate, int page) {
        Pageable pageable = PageRequest.of(page, 10);

        Page<Faq> faqPage = faqRepository.searchFaqs(teamId, categoryId, keyword, searchScope, startDate, endDate, pageable);

        List<GetFaqListResponse> faqs = faqPage.getContent().stream()
                .map(GetFaqListResponse::new)
                .toList();

        return new GetFaqListWrapper(faqs, faqPage.getNumber(), faqPage.getTotalPages());

    }

    @Transactional(readOnly = true)
    public GetFaqDetailResponse getFaqDetailByDate(Long faqId, LocalDate date) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> CustomException.of(FaqErrorCode.FAQ_NOT_FOUND));

        if (faq.getDeletedAt() != null && faq.getDeletedAt().equals(date)) {
            throw CustomException.of(FaqErrorCode.INVALID_FAQ_HISTORY_DATE);
        }

        FaqHistory history = faqHistoryRepository.findByFaqIdAndEditedAt(faqId, date)
                .orElseThrow(() -> CustomException.of(FaqErrorCode.INVALID_FAQ_HISTORY_DATE));

        List<LocalDate> editedDates = faqHistoryRepository.findAllEditedDatesByFaqId(faqId);

        return new GetFaqDetailResponse(faq, history, editedDates);
    }

    public void deleteFaq(Long faqId) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> CustomException.of(FaqErrorCode.FAQ_NOT_FOUND));

        if (Boolean.TRUE.equals(faq.getDeletedFlag())) {
            throw CustomException.of(FaqErrorCode.FAQ_ALREADY_DELETED);
        }

        faq.getImages().forEach(FaqImage::detachFaq);
        faq.getFiles().forEach(FaqFile::detachFaq);

        faq.delete();
    }

    public PostFaqImageResponse uploadFaqImage(MultipartFile imageFile) {

        FileUploadResult fileUploadResult = fileStorageService.uploadImage(imageFile);

        FaqImage image = FaqImage.create(fileUploadResult.url(), fileUploadResult.storagePath());
        faqImageRepository.save(image);

        return new PostFaqImageResponse(fileUploadResult.url());
    }

    public PostFaqFileResponse uploadFaqFile(MultipartFile file) {

        FileUploadResult fileUploadResult = fileStorageService.uploadFile(file);

        FaqFile faqFile = FaqFile.create(
                fileUploadResult.url(),
                fileUploadResult.storagePath(),
                file.getOriginalFilename()
        );

        faqFileRepository.save(faqFile);

        return new PostFaqFileResponse(
                fileUploadResult.url(),
                file.getOriginalFilename()
        );
    }

    private List<String> extractImageUrls(String content, String answer) {

        Set<String> urls = new LinkedHashSet<>();

        extractImageUrlsFromHtml(content, urls);
        extractImageUrlsFromHtml(answer, urls);

        return new ArrayList<>(urls);
    }

    private void extractImageUrlsFromHtml(String html, Set<String> urls) {

        if (html == null || html.isBlank()) {
            return;
        }

        Document document = Jsoup.parse(html);

        Elements images = document.select("img[src]");

        for (Element image : images) {
            urls.add(image.attr("src"));
        }
    }
}
