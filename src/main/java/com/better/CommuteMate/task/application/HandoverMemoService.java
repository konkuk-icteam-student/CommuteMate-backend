package com.better.CommuteMate.task.application;

import com.better.CommuteMate.domain.handovermemo.entity.HandoverMemo;
import com.better.CommuteMate.domain.handovermemo.repository.HandoverMemoRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.HandoverMemoErrorCode;
import com.better.CommuteMate.task.controller.dtos.CreateHandoverMemoRequest;
import com.better.CommuteMate.task.controller.dtos.CreateHandoverMemoResponse;
import com.better.CommuteMate.task.controller.dtos.HandoverMemosResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HandoverMemoService {

    private final HandoverMemoRepository handoverMemoRepository;

    @Transactional
    public CreateHandoverMemoResponse createMemo(Long organizationId, User createdBy, CreateHandoverMemoRequest request) {
        HandoverMemo memo = HandoverMemo.builder()
                .organizationId(organizationId)
                .createdBy(createdBy)
                .content(request.content())
                .build();
        return CreateHandoverMemoResponse.from(handoverMemoRepository.save(memo));
    }

    public HandoverMemosResponse getMemos(Long organizationId, String dateValue) {
        LocalDate date = parseDate(dateValue);
        List<HandoverMemo> memos = handoverMemoRepository.findDailyMemos(
                organizationId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );
        return new HandoverMemosResponse(
                date,
                memos.stream().map(HandoverMemosResponse::toItem).toList()
        );
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw CustomException.of(HandoverMemoErrorCode.INVALID_DATE_FORMAT);
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw CustomException.of(HandoverMemoErrorCode.INVALID_DATE_FORMAT);
        }
    }
}
