package com.better.CommuteMate.manager.application.dto.response;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.manager.entity.Manager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "담당자 상세 정보 조회 응답 DTO")
public class GetManagerListResponse{

    @Schema(description = "카테고리 ID", example = "1")
    private final Long categoryId;

    @Schema(description = "카테고리 이름", example = "인사관리")
    private final String categoryName;

    @Schema(description = "카테고리 즐겨찾기 여부", example = "true")
    private final boolean categoryFavorite;

    @Schema(description = "담당자 ID", example = "1")
    private final Long managerId;

    @Schema(description = "담당자 이름", example = "홍길동")
    private final String managerName;

    @Schema(description = "소속 ID", example = "1")
    private final Long teamId;

    @Schema(description = "소속 이름", example = "정보운영팀")
    private final String teamName;

    @Schema(description = "전화번호", example = "01012345678")
    private final String phonenum;

    public GetManagerListResponse(Category category, Manager manager) {
        this.categoryId = category.getId();
        this.categoryName = category.getName();
        this.categoryFavorite = category.isFavorite();
        this.managerId = manager.getId();
        this.managerName = manager.getName();
        this.teamId = manager.getTeam().getId();
        this.teamName = manager.getTeam().getName();
        this.phonenum = manager.getPhonenum();
    }
}