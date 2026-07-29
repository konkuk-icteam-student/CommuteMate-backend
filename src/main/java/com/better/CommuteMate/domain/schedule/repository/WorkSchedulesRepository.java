package com.better.CommuteMate.domain.schedule.repository;

import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.global.code.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkSchedulesRepository extends JpaRepository<WorkSchedule, Long> {

    /**
     * 특정 날짜의 근무 일정 목록을 조회
     */
    List<WorkSchedule> findAllByDate(LocalDate date);

    List<WorkSchedule> findAllByUser_OrganizationIdAndDateAndStatusCode(
            Long organizationId,
            LocalDate date,
            CodeType statusCode
    );

    List<WorkSchedule> findAllByUser_UserIdInAndDateBetweenAndStatusCode(
            List<Long> userIds,
            LocalDate startDate,
            LocalDate endDate,
            CodeType statusCode
    );

    /**
     * 특정 사용자의 특정 기간 내 유효한 근무 일정 목록을 조회
     * 신청(WS01), 승인(WS02) 상태만 포함
     */
    List<WorkSchedule> findAllByUser_UserIdAndDateBetweenAndStatusCodeIn(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            List<CodeType> statusCodes
    );

    /**
     * 특정 사용자의 특정 기간 내 근무 일정 목록을 조회
     * 취소(WS04) 상태는 제외
     */
    List<WorkSchedule> findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            CodeType statusCode
    );

    /**
     * 특정 사용자의 특정 날짜, 시작 시간, 종료 시간이 일치하는 근무 일정을 조회
     * 일정 삭제 요청(deleteSlots) 처리 시 사용
     */
    Optional<WorkSchedule> findByUser_UserIdAndDateAndStartTimeAndEndTime(
            Long userId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    );

    /**
     * 특정 사용자의 특정 날짜, 시작 시간, 종료 시간이 일치하면서
     * 취소되지 않은 근무 일정이 존재하는지 확인
     * 일정 추가 요청(addSlots) 중복 검사 시 사용
     */
    boolean existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeNot(
            Long userId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            CodeType statusCode
    );

    boolean existsByUser_UserIdAndDateAndStartTimeAndEndTimeAndStatusCodeIn(
            Long userId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            List<CodeType> statusCodes
    );

    long countBySettingAndDateAndStartTimeAndEndTimeAndStatusCode(
            WorkScheduleSetting setting,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            CodeType statusCode
    );

    /**
     * 특정 사용자와 특정 날짜, 시작 시간을 기준으로 근무 일정을 조회
     */
    Optional<WorkSchedule> findByUserAndDateAndStartTime(
            User user,
            LocalDate date,
            LocalTime startTime
    );

    /**
     * 특정 상태 코드에 해당하는 근무 일정 목록을 조회
     */
    List<WorkSchedule> findAllByStatusCode(CodeType statusCode);

    /**
     * 특정 기간 내 유효한 근무 일정 목록을 조회
     * 신청(WS01), 승인(WS02) 상태만 포함
     */
    List<WorkSchedule> findAllByDateBetweenAndStatusCodeIn(
            LocalDate startDate,
            LocalDate endDate,
            List<CodeType> statusCodes
    );

    List<WorkSchedule> findAllBySettingAndStatusCodeIn(
            WorkScheduleSetting setting,
            List<CodeType> statusCodes
    );

    List<WorkSchedule> findAllBySettingAndDateBetweenAndStatusCodeIn(
            WorkScheduleSetting setting,
            LocalDate startDate,
            LocalDate endDate,
            List<CodeType> statusCodes
    );
}
