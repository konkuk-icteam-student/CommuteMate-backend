package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestItemRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkChangeRequestHistoryServiceTest {

    @Mock WorkChangeRequestRepository requestRepository;
    @Mock WorkChangeRequestItemRepository itemRepository;

    private WorkChangeRequestHistoryService service;

    @BeforeEach
    void setUp() {
        service = new WorkChangeRequestHistoryService(requestRepository, itemRepository);
    }

    @Test
    void searchesMonthWithoutBindingNullableStatusForAllFilter() {
        when(requestRepository.findUserRequestsByMonth(
                any(), any(), any(), any(Pageable.class)
        )).thenReturn(Page.empty());

        service.getHistory(1L, 2026, 8, "ALL", 0, 10);

        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 31);
        verify(requestRepository).findUserRequestsByMonth(
                eq(1L), eq(startDate), eq(endDate), any(Pageable.class)
        );
        verify(requestRepository, never()).findUserRequestsByMonthAndStatus(
                any(), any(), any(), any(), any(Pageable.class)
        );
        verify(requestRepository).countUserRequestsByMonth(1L, startDate, endDate);
        verify(requestRepository).countUserRequestsByMonthAndStatus(
                1L, startDate, endDate, CodeType.CS01
        );
        verify(requestRepository).countUserRequestsByMonthAndStatus(
                1L, startDate, endDate, CodeType.CS02
        );
        verify(requestRepository).countUserRequestsByMonthAndStatus(
                1L, startDate, endDate, CodeType.CS03
        );
    }
}
