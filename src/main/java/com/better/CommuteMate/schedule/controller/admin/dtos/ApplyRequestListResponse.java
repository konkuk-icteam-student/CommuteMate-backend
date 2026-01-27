package com.better.CommuteMate.schedule.controller.admin.dtos;

import com.better.CommuteMate.global.controller.dtos.ResponseDetail;
import lombok.Getter;
import java.util.List;

@Getter
public class ApplyRequestListResponse extends ResponseDetail {
    private final List<ApplyRequestResponse> requests;

    public ApplyRequestListResponse(List<ApplyRequestResponse> requests) {
        this.requests = requests;
    }

    public static ApplyRequestListResponse of(List<ApplyRequestResponse> requests) {
        return new ApplyRequestListResponse(requests);
    }
}
