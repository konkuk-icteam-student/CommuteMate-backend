package com.better.CommuteMate.category.application.dto.response;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Getter;

import java.util.List;

@Getter
public class GetCategoryListWrapper extends ResponseDetail {
    private final List<GetCategoryListResponse> categories;

    public GetCategoryListWrapper(List<GetCategoryListResponse> categories) {
        this.categories = categories;
    }

}
