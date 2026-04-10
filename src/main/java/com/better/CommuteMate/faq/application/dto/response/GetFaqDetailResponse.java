package com.better.CommuteMate.faq.application.dto.response;

import com.better.CommuteMate.domain.faq.embedded.ManagerSnapshot;
import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqHistory;
import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "FAQ 상세 조회 응답 DTO")
@Getter
public class GetFaqDetailResponse extends ResponseDetail {

    @Schema(description = "faq id", example = "1")
    private final Long faqId;

    @Schema(description = "faq 제목", example = "학적시 로그인 오류")
    private final String title;

    @Schema(description = "카테고리 이름 목록", example = "[\"로그인\", \"계정\"]")
    private final List<String> categoryNames;

    @Schema(description = "faq 삭제 여부", example = "true")
    private final Boolean deletedFlag;

    @Schema(description = "민원인 이름", example = "홍길동")
    private final String complainantName;

    @Schema(description = "작성자 이름", example = "양지윤")
    private final String writerName;

    @Schema(description = "답변 내용", example = "비밀번호 재설정 후 다시 로그인해주세요.")
    private final String answer;

    @Schema(description = "비고", example = "반복 문의 발생")
    private final String etc;

    @Schema(description = "과거 담당자 목록")
    private final List<ManagerSnapshot> pastManagers;

    @Schema(description = "현재 담당자 목록")
    private final List<ManagerSnapshot> currentManagers;

    @Schema(description = "수정 이력 날짜 목록", example = "[\"2024-03-01\", \"2024-03-05\"]")
    private final List<LocalDate> editedDates;

    @Schema(description = "삭제 일자", example = "2024-03-10")
    private final LocalDate deletedAt;

    public GetFaqDetailResponse(Faq faq, FaqHistory history, List<LocalDate> editedDates) {
        super();
        this.faqId = faq.getId();
        this.title = history.getTitle();

        this.categoryNames = history.getCategoryNames();

        this.deletedFlag = faq.getDeletedFlag();
        this.complainantName = history.getComplainantName();
        this.writerName = history.getWriterName();
        this.answer = history.getAnswer();
        this.etc = history.getEtc();

        this.pastManagers = history.getManagers()
                .stream()
                .map(snapshot -> new ManagerSnapshot(
                        snapshot.getManagerName(),
                        snapshot.getTeamName(),
                        snapshot.getCategoryName()
                ))
                .toList();

        this.currentManagers = faq.getFaqCategories()
                .stream()
                .flatMap(fc -> fc.getCategory().getManagers().stream())
                .map(mc -> new ManagerSnapshot(
                        mc.getManager().getName(),
                        mc.getManager().getTeam().getName(),
                        mc.getCategory().getName()
                ))
                .toList();

        this.editedDates = editedDates;
        this.deletedAt = faq.getDeletedAt();
    }
}