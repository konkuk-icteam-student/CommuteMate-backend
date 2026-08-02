package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequestItem;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestItemRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.ScheduleErrorCode;
import com.better.CommuteMate.schedule.controller.workchangerequest.dtos.WorkChangeRequestHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkChangeRequestHistoryService {

    private final WorkChangeRequestRepository requestRepository;
    private final WorkChangeRequestItemRepository itemRepository;

    public WorkChangeRequestHistoryResponse getHistory(
            Long userId,
            Integer year,
            Integer month,
            String statusCodeValue,
            Integer pageValue,
            Integer sizeValue
    ) {
        // TODO: year/month 중 하나만 오는 경우의 처리 규칙이 스펙에 명시되지 않았습니다.
        //       현재는 둘 다 있을 때만 연월 필터를 적용하고, 하나만 있으면 무시합니다.
        YearMonth targetMonth = resolveYearMonth(year, month);

        int page = pageValue == null ? 0 : pageValue;
        int size = sizeValue == null ? 10 : sizeValue;
        validatePage(page, size);

        StatusFilter statusFilter = parseStatus(statusCodeValue);

        LocalDate startDate = targetMonth != null ? targetMonth.atDay(1) : null;
        LocalDate endDate = targetMonth != null ? targetMonth.atEndOfMonth() : null;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WorkChangeRequest> requestPage = requestRepository.findUserRequests(
                userId, startDate, endDate, statusFilter.code, pageable
        );

        List<Long> requestIds = requestPage.getContent().stream()
                .map(WorkChangeRequest::getRequestId)
                .toList();
        Map<Long, List<WorkChangeRequestItem>> itemsByRequest = requestIds.isEmpty()
                ? Map.of()
                : itemRepository.findAllByRequest_RequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getRequestId()));

        List<WorkChangeRequestHistoryResponse.HistoryItem> histories = requestPage.getContent().stream()
                .map(request -> toHistoryItem(request, itemsByRequest, targetMonth))
                .toList();

        WorkChangeRequestHistoryResponse.Summary summary = new WorkChangeRequestHistoryResponse.Summary(
                countUser(userId, startDate, endDate, null),
                countUser(userId, startDate, endDate, CodeType.CS02),
                countUser(userId, startDate, endDate, CodeType.CS01),
                countUser(userId, startDate, endDate, CodeType.CS03)
        );

        return new WorkChangeRequestHistoryResponse(
                year,
                month,
                statusFilter.responseValue,
                summary,
                histories,
                page,
                size,
                requestPage.getTotalElements(),
                requestPage.getTotalPages()
        );
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            return null;
        }
        if (year < 1900 || year > 9999) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_YEAR_MONTH);
        }
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException e) {
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_YEAR_MONTH);
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1) {
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
            throw CustomException.of(ScheduleErrorCode.INVALID_CHANGE_REQUEST_HISTORY_STATUS);
        }
    }

    private long countUser(Long userId, LocalDate startDate, LocalDate endDate, CodeType statusCode) {
        return requestRepository.countUserRequests(userId, startDate, endDate, statusCode);
    }

    private WorkChangeRequestHistoryResponse.HistoryItem toHistoryItem(
            WorkChangeRequest request,
            Map<Long, List<WorkChangeRequestItem>> itemsByRequest,
            YearMonth targetMonth
    ) {
        List<WorkChangeRequestItem> items = new ArrayList<>(
                itemsByRequest.getOrDefault(request.getRequestId(), List.of())
        );
        if (targetMonth != null) {
            items = items.stream()
                    .filter(item -> YearMonth.from(item.getDate()).equals(targetMonth))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        items.sort(Comparator.comparing(WorkChangeRequestItem::getDate)
                .thenComparing(WorkChangeRequestItem::getStartTime));

        List<WorkChangeRequestHistoryResponse.SlotItem> deleteSlots = items.stream()
                .filter(item -> item.getChangeTypeCode() == CodeType.CR02)
                .map(this::toSlotItem)
                .toList();
        List<WorkChangeRequestHistoryResponse.SlotItem> addSlots = items.stream()
                .filter(item -> item.getChangeTypeCode() == CodeType.CR01)
                .map(this::toSlotItem)
                .toList();

        return new WorkChangeRequestHistoryResponse.HistoryItem(
                request.getRequestId(),
                request.getStatusCode().name(),
                request.getStatusCode().getCodeValue(),
                request.getCreatedAt(),
                request.getProcessedAt(),
                request.getReason(),
                request.getRejectReason(),
                deleteSlots,
                addSlots
        );
    }

    private WorkChangeRequestHistoryResponse.SlotItem toSlotItem(WorkChangeRequestItem item) {
        return new WorkChangeRequestHistoryResponse.SlotItem(
                LocalDateTime.of(item.getDate(), item.getStartTime()),
                LocalDateTime.of(item.getDate(), item.getEndTime()),
                item.getChangeTypeCode().name()
        );
    }

    private record StatusFilter(CodeType code, String responseValue) {}
}
