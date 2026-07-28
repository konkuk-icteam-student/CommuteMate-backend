package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequestItem;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestItemRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.admin.dtos.WorkChangeRequestListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminWorkChangeRequestQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WorkChangeRequestRepository requestRepository;
    private final WorkChangeRequestItemRepository itemRepository;

    public WorkChangeRequestListResponse getRequests(
            Long organizationId,
            Integer year,
            Integer month,
            String statusCodeValue,
            Integer pageValue,
            Integer sizeValue
    ) {
        YearMonth targetMonth = validateYearMonth(year, month);
        int page = pageValue == null ? 0 : pageValue;
        int size = sizeValue == null ? 10 : sizeValue;
        validatePage(page, size);

        StatusFilter statusFilter = parseStatus(statusCodeValue);
        PageRequest pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<WorkChangeRequest> requestPage = requestRepository.findAdminRequests(
                organizationId,
                targetMonth.atDay(1),
                targetMonth.atEndOfMonth(),
                statusFilter.code,
                pageable
        );

        List<Long> requestIds = requestPage.getContent().stream()
                .map(WorkChangeRequest::getRequestId)
                .toList();
        Map<Long, List<WorkChangeRequestItem>> itemsByRequest = requestIds.isEmpty()
                ? Map.of()
                : itemRepository.findAllByRequest_RequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getRequestId()));

        List<WorkChangeRequestListResponse.RequestItem> requests =
                requestPage.getContent().stream()
                        .map(request -> toResponse(request, itemsByRequest, targetMonth))
                        .toList();

        WorkChangeRequestListResponse.Summary summary =
                new WorkChangeRequestListResponse.Summary(
                        count(organizationId, targetMonth, null),
                        count(organizationId, targetMonth, CodeType.CS01),
                        count(organizationId, targetMonth, CodeType.CS02),
                        count(organizationId, targetMonth, CodeType.CS03)
                );

        return new WorkChangeRequestListResponse(
                year,
                month,
                statusFilter.responseValue,
                summary,
                requests,
                page,
                size,
                requestPage.getTotalElements(),
                requestPage.getTotalPages()
        );
    }

    private YearMonth validateYearMonth(Integer year, Integer month) {
        if (year == null || month == null || year < 1900 || year > 9999) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_YEAR_MONTH);
        }
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException e) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_YEAR_MONTH);
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_PAGE);
        }
    }

    private StatusFilter parseStatus(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("ALL")) {
            return new StatusFilter(null, "ALL");
        }
        try {
            CodeType code = CodeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (code != CodeType.CS01 && code != CodeType.CS02 && code != CodeType.CS03) {
                throw new IllegalArgumentException();
            }
            return new StatusFilter(code, code.name());
        } catch (IllegalArgumentException e) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_STATUS);
        }
    }

    private long count(Long organizationId, YearMonth month, CodeType statusCode) {
        return requestRepository.countAdminRequests(
                organizationId, month.atDay(1), month.atEndOfMonth(), statusCode
        );
    }

    private WorkChangeRequestListResponse.RequestItem toResponse(
            WorkChangeRequest request,
            Map<Long, List<WorkChangeRequestItem>> itemsByRequest,
            YearMonth targetMonth
    ) {
        List<WorkChangeRequestItem> items = itemsByRequest
                .getOrDefault(request.getRequestId(), List.of())
                .stream()
                .filter(item -> YearMonth.from(item.getDate()).equals(targetMonth))
                .collect(Collectors.toCollection(ArrayList::new));
        items.sort(Comparator.comparing(WorkChangeRequestItem::getDate)
                .thenComparing(WorkChangeRequestItem::getStartTime));

        List<WorkChangeRequestListResponse.ScheduleItem> deleteSchedules =
                items.stream()
                        .filter(item -> item.getChangeTypeCode() == CodeType.CR02)
                        .map(this::toScheduleItem)
                        .toList();
        List<WorkChangeRequestListResponse.ScheduleItem> addSchedules =
                items.stream()
                        .filter(item -> item.getChangeTypeCode() == CodeType.CR01)
                        .map(this::toScheduleItem)
                        .toList();

        return new WorkChangeRequestListResponse.RequestItem(
                request.getRequestId(),
                request.getUser().getUserId(),
                request.getUser().getName(),
                request.getStatusCode().name(),
                request.getCreatedAt(),
                request.getProcessedAt(),
                request.getReason(),
                request.getRejectReason(),
                deleteSchedules,
                addSchedules
        );
    }

    private WorkChangeRequestListResponse.ScheduleItem toScheduleItem(
            WorkChangeRequestItem item
    ) {
        return new WorkChangeRequestListResponse.ScheduleItem(
                item.getDate(),
                item.getStartTime(),
                item.getEndTime(),
                item.getChangeTypeCode().name()
        );
    }

    private record StatusFilter(CodeType code, String responseValue) {
    }
}
