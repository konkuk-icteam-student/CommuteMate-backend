package com.better.CommuteMate.faq.application.dto.response;

import com.better.CommuteMate.domain.faq.embedded.FaqHistoryManager;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "FAQ 임시저장 상세 조회 응답 DTO")
@Getter
public class GetDraftFaqDetailResponse extends ResponseDetail {

    @Schema(description = "faq id", example = "1")
    private final Long faqId;

    @Schema(description = "faq 제목", example = "학정시 로그인 오류")
    private final String title;

    @Schema(description = "카테고리 이름 목록", example = "[\"로그인\", \"학정시\"]")
    private final List<String> categoryNames;

    @Schema(description = "민원인 이름", example = "홍길동")
    private final String complainantName;

    @Schema(description = "작성자 이름", example = "양지윤")
    private final String writerName;

    @Schema(description = "본문 내용 (HTML)", example = "<p>로그인 시 OTP 오류가 발생합니다.<img src=\"...\"></p>")
    private final String content;

    @Schema(description = "답변 내용 (HTML)", example = "<p>비밀번호 재설정 후 다시 로그인해주세요.</p>")
    private final String answer;

    @Schema(description = "비고", example = "반복 문의 발생")
    private final String etc;

    @Schema(description = "첨부 파일 목록")
    private final List<FileInfo> files;

    @Schema(description = "현재 담당자 목록")
    private final List<FaqHistoryManager> currentManagers;

    @Schema(description = "관련 FAQ 목록")
    private final List<RelatedFaqInfo> relatedFaqs;

    public GetDraftFaqDetailResponse(Faq faq, List<Faq> relatedFaqs) {
        super();

        this.faqId = faq.getId();
        this.title = faq.getTitle();

        this.categoryNames = faq.getFaqCategories()
                .stream()
                .map(fc -> fc.getCategory().getName())
                .toList();

        this.complainantName = faq.getComplainantName();
        this.writerName = faq.getWriter().getName();
        this.content = faq.getContent();
        this.answer = faq.getAnswer();
        this.etc = faq.getEtc();

        this.files = faq.getFiles()
                .stream()
                .map(f -> new FileInfo(f.getUrl(), f.getOriginalName()))
                .toList();

        this.currentManagers = faq.getFaqCategories()
                .stream()
                .flatMap(fc -> fc.getCategory().getManagers().stream())
                .map(mc -> new FaqHistoryManager(
                        mc.getManager().getName(),
                        mc.getManager().getOrganization().getName(),
                        mc.getCategory().getName()
                ))
                .toList();

        this.relatedFaqs = relatedFaqs.stream()
                .map(f -> new RelatedFaqInfo(
                        f.getId(),
                        f.getTitle(),
                        f.getUpdatedDate().toLocalDate()
                ))
                .toList();
    }
}