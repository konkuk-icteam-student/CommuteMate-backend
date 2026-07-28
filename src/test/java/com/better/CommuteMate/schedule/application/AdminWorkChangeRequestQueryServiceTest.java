package com.better.CommuteMate.schedule.application;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequestItem;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestItemRepository;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWorkChangeRequestQueryServiceTest {

    @Mock WorkChangeRequestRepository requestRepository;
    @Mock WorkChangeRequestItemRepository itemRepository;

    AdminWorkChangeRequestQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminWorkChangeRequestQueryService(requestRepository, itemRepository);
    }

    @Test
    void returnsFilteredRequestsWithSeparatedChangeItemsAndSummary() {
        User user = User.builder().userId(2L).organizationId(10L).name("김길동").build();
        WorkChangeRequest request = WorkChangeRequest.builder()
                .requestId(1L)
                .user(user)
                .statusCode(CodeType.CS01)
                .reason("근무시간 변경 요청")
                .createdAt(LocalDateTime.of(2026, 6, 13, 10, 20))
                .build();
        WorkChangeRequestItem deleteItem = item(request, CodeType.CR02, 15, 9, 11);
        WorkChangeRequestItem addItem = item(request, CodeType.CR01, 17, 13, 15);

        when(requestRepository.findAdminRequests(
                eq(10L), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30)),
                eq(CodeType.CS01), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(request)));
        when(itemRepository.findAllByRequest_RequestIdIn(List.of(1L)))
                .thenReturn(List.of(addItem, deleteItem));
        when(requestRepository.countAdminRequests(
                eq(10L), any(LocalDate.class), any(LocalDate.class), nullable(CodeType.class)
        )).thenAnswer(invocation -> {
            CodeType status = invocation.getArgument(3);
            return status == null ? 4L : status == CodeType.CS01 ? 2L : 1L;
        });

        var response = service.getRequests(10L, 2026, 6, "CS01", null, null);

        assertThat(response.statusCode).isEqualTo("CS01");
        assertThat(response.summary.totalCount()).isEqualTo(4);
        assertThat(response.summary.pendingCount()).isEqualTo(2);
        assertThat(response.requests).hasSize(1);
        assertThat(response.requests.get(0).userName()).isEqualTo("김길동");
        assertThat(response.requests.get(0).deleteSchedules())
                .singleElement().extracting(schedule -> schedule.changeTypeCode())
                .isEqualTo("CR02");
        assertThat(response.requests.get(0).addSchedules())
                .singleElement().extracting(schedule -> schedule.changeTypeCode())
                .isEqualTo("CR01");
        assertThat(response.page).isZero();
        assertThat(response.size).isEqualTo(10);
    }

    @Test
    void rejectsInvalidStatus() {
        assertThatThrownBy(() -> service.getRequests(10L, 2026, 6, "CS99", 0, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage("올바르지 않은 변경 요청 상태입니다.");
    }

    @Test
    void rejectsInvalidPage() {
        assertThatThrownBy(() -> service.getRequests(10L, 2026, 6, "ALL", -1, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage("페이지 요청 값이 올바르지 않습니다.");
    }

    @Test
    void rejectsInvalidYearOrMonth() {
        assertThatThrownBy(() -> service.getRequests(10L, 2026, 13, "ALL", 0, 10))
                .isInstanceOf(CustomException.class)
                .hasMessage("조회 연도 또는 월 값이 올바르지 않습니다.");
    }

    private WorkChangeRequestItem item(
            WorkChangeRequest request,
            CodeType changeType,
            int day,
            int startHour,
            int endHour
    ) {
        return WorkChangeRequestItem.builder()
                .request(request)
                .changeTypeCode(changeType)
                .date(LocalDate.of(2026, 6, day))
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(endHour, 0))
                .build();
    }
}
