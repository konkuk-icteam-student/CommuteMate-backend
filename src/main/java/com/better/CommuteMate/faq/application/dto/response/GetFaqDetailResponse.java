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

    private final Long faqId;
    private final String title;

    private final List<String> categoryNames;

    private final Boolean deletedFlag;
    private final String complainantName;
    private final String writerName;
    private final String answer;
    private final String etc;

    private final List<ManagerSnapshot> pastManagers;
    private final List<ManagerSnapshot> currentManagers;

    private final List<LocalDate> editedDates;
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