package com.better.CommuteMate.faq.application.dto.response;

import com.better.CommuteMate.domain.faq.embedded.FaqHistoryManager;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqHistory;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
class FileInfo {

    @Schema(description = "첨부 파일 Url", example = "https://kusd.konkuk.ac.kr/uploads/prod/faq/files/3f1c2a9b-1234-4abc-9def-123456789abc.pdf")
    private final String url;

    @Schema(description = "원본 파일 이름", example = "강의자료.pdf")
    private final String originalName;

    FileInfo(String url, String originalName) {
        this.url = url;
        this.originalName = originalName;
    }
}

@Schema(description = "FAQ 상세 조회 응답 DTO")
@Getter
public class GetFaqDetailResponse extends ResponseDetail {

    @Schema(description = "faq id", example = "1")
    private final Long faqId;

    @Schema(description = "faq 제목", example = "학정시 로그인 오류")
    private final String title;

    @Schema(description = "카테고리 이름 목록", example = "[\"로그인\", \"계정\"]")
    private final List<String> categoryNames;

    @Schema(description = "faq 삭제 여부", example = "true")
    private final Boolean deletedFlag;

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

    @Schema(description = "과거 시점 기준 담당자 목록")
    private final List<FaqHistoryManager> pastManagers;

    @Schema(description = "현재 담당자 목록")
    private final List<FaqHistoryManager> currentManagers;

    @Schema(description = "수정 이력 날짜 목록", example = "[\"2024-11-01\", \"2024-11-10\"]")
    private final List<LocalDate> editedDates;

    @Schema(description = "삭제 일자", nullable = true, example = "2026-01-01")
    private final LocalDate deletedAt;

    public GetFaqDetailResponse(Faq faq, FaqHistory history, List<LocalDate> editedDates) {
        super();
        this.faqId = faq.getId();
        this.title = history.getTitle();
        this.categoryNames = history.getCategoryNames();
        this.deletedFlag = faq.getDeletedFlag();
        this.complainantName = history.getComplainantName();
        this.writerName = history.getWriterName();
        this.content = history.getContent();
        this.answer = history.getAnswer();
        this.etc = history.getEtc();
        this.files = history.getFiles().stream()
                .map(f -> new FileInfo(f.getUrl(), f.getOriginalName()))
                .toList();

        this.pastManagers = history.getManagers()
                .stream()
                .map(snapshot -> new FaqHistoryManager(
                        snapshot.getManagerName(),
                        snapshot.getTeamName(),
                        snapshot.getCategoryName()
                ))
                .toList();

        this.currentManagers = faq.getFaqCategories()
                .stream()
                .flatMap(fc -> fc.getCategory().getManagers().stream())
                .map(mc -> new FaqHistoryManager(
                        mc.getManager().getName(),
                        mc.getManager().getTeam().getName(),
                        mc.getCategory().getName()
                ))
                .toList();

        this.editedDates = editedDates;
        this.deletedAt = faq.getDeletedAt();
    }
}
