package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum ScheduleErrorCode implements CustomErrorCode {
    SCHEDULE_PARTIAL_FAILURE("신청하신 일정 중 실패한 일정이 존재합니다.", "[Error] : 신청하신 일정 중 실패한 일정이 존재합니다.", HttpStatus.MULTI_STATUS),
    SCHEDULE_FAILURE("신청하신 일정이 모두 실패하였습니다.", "[Error] : 신청하신 일정이 모두 실패하였습니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_APPLY_TERM("신청 기간이 유효하지 않습니다. 시작 시간이 종료 시간보다 이전이어야 합니다.", "[Error] : 신청 기간 유효성 검증 실패", HttpStatus.BAD_REQUEST),
    MONTHLY_SCHEDULE_CONFIG_NOT_FOUND("해당 연월의 스케줄 설정을 찾을 수 없습니다.", "[Error] : 월별 스케줄 설정 미존재", HttpStatus.NOT_FOUND),
    WORK_DURATION_MISMATCH("삭제하는 일정의 총 근무 시간과 추가하는 일정의 총 근무 시간이 일치하지 않습니다.", "[Error] : 근무 시간 불일치", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_IDS_COUNT("변경 요청 ID 개수는 반드시 짝수여야 합니다.", "[Error] : 변경 요청 ID 개수가 홀수", HttpStatus.BAD_REQUEST),
    TOTAL_WORK_TIME_EXCEEDED("월 최대 근무 시간을 초과하였습니다.", "[Error] : 월 근무 시간 초과", HttpStatus.UNPROCESSABLE_ENTITY),
    WEEKLY_WORK_TIME_EXCEEDED("주 최대 근무 시간(13시간)을 초과하였습니다.", "[Error] : 주 근무 시간 초과", HttpStatus.BAD_REQUEST),
    MIN_WORK_TIME_NOT_MET("1회 최소 근무 시간(2시간)을 충족하지 못했습니다.", "[Error] : 최소 근무 시간 미충족", HttpStatus.BAD_REQUEST),
    PAST_MONTH_MODIFICATION_NOT_ALLOWED("지난 달의 근무 일정은 수정할 수 없습니다.", "[Error] : 지난 달 근무 수정 시도", HttpStatus.BAD_REQUEST),
    SCHEDULE_NOT_FOUND("해당 근무 일정을 찾을 수 없습니다.", "[Error] : 근무 일정 미존재", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_ACCESS("해당 근무 일정에 대한 권한이 없습니다.", "[Error] : 근무 일정 접근 권한 없음", HttpStatus.FORBIDDEN),
    INVALID_YEAR_MONTH("유효하지 않은 연도 또는 월 값입니다.", "[Error] : 유효하지 않은 연도/월", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE("시작 날짜는 종료 날짜보다 늦을 수 없습니다.", "[Error] : 시작 날짜 > 종료 날짜", HttpStatus.BAD_REQUEST),
    CROSS_MONTH_RANGE_NOT_ALLOWED("조회 기간은 같은 달 이내여야 합니다.", "[Error] : 다른 달에 걸친 조회 범위", HttpStatus.BAD_REQUEST),
    EDIT_REQUEST_EMPTY("수정 요청 항목이 없습니다.", "[Error] : 수정 요청 항목 없음", HttpStatus.BAD_REQUEST),
    EDIT_REQUEST_REASON_REQUIRED("수정 요청 사유를 입력해야 합니다.", "[Error] : 수정 요청 사유 미입력", HttpStatus.BAD_REQUEST),
    DELETE_SCHEDULE_NOT_FOUND("삭제 요청한 스케줄을 찾을 수 없습니다.", "[Error] : 삭제 요청 스케줄 미존재", HttpStatus.NOT_FOUND),
    CROSS_WEEK_RANGE_NOT_ALLOWED("조회 기간은 같은 주 이내여야 합니다.", "[Error] : 다른 주에 걸친 조회 범위", HttpStatus.BAD_REQUEST);


    private final String message;
    private final String logMessage;
    private final HttpStatus status;

    ScheduleErrorCode(String message, String logMessage, HttpStatus status) {
        this.message = message;
        this.logMessage = logMessage;
        this.status = status;
    }

    @Override
    public String getLogMessage() {
        return this.logMessage;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public HttpStatus getStatus() {
        return this.status;
    }
}
