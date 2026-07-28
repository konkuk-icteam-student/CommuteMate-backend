package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.BulkApproveWorkChangeRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.BulkApproveWorkChangeResponse;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessWorkChangeRequest;
import com.better.CommuteMate.schedule.controller.admin.dtos.ProcessWorkChangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminWorkChangeRequestBulkService {

    private static final ProcessWorkChangeRequest APPROVE_COMMAND =
            new ProcessWorkChangeRequest("CS02", null);

    private final AdminWorkChangeRequestProcessService processService;

    public BulkApproveWorkChangeResponse approve(
            BulkApproveWorkChangeRequest command,
            Long adminId,
            Long organizationId
    ) {
        validate(command);

        List<BulkApproveWorkChangeResponse.Result> results = new ArrayList<>();
        int successCount = 0;

        for (Long requestId : command.requestIds()) {
            try {
                ProcessWorkChangeResponse response = processService.process(
                        requestId, APPROVE_COMMAND, adminId, organizationId
                );
                results.add(new BulkApproveWorkChangeResponse.Result(
                        requestId, "SUCCESS", response.processedAt
                ));
                successCount++;
            } catch (CustomException exception) {
                results.add(new BulkApproveWorkChangeResponse.Result(
                        requestId, toResultCode(exception), null
                ));
            }
        }

        int totalCount = results.size();
        return new BulkApproveWorkChangeResponse(
                new BulkApproveWorkChangeResponse.Summary(
                        totalCount,
                        successCount,
                        totalCount - successCount
                ),
                results
        );
    }

    private void validate(BulkApproveWorkChangeRequest command) {
        if (command == null
                || command.requestIds() == null
                || command.requestIds().isEmpty()
                || command.requestIds().stream().anyMatch(id -> id == null || id < 1)) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_IDS);
        }
    }

    private String toResultCode(CustomException exception) {
        if (exception.getErrorCode() == ScheduleErrorCode.CHANGE_REQUEST_NOT_FOUND) {
            return "NOT_FOUND";
        }
        if (exception.getErrorCode() == ScheduleErrorCode.CHANGE_REQUEST_ALREADY_PROCESSED) {
            return "ALREADY_PROCESSED";
        }
        if (exception.getErrorCode() == ScheduleErrorCode.CHANGE_REQUEST_CAPACITY_EXCEEDED) {
            return "CAPACITY_EXCEEDED";
        }
        throw exception;
    }
}
