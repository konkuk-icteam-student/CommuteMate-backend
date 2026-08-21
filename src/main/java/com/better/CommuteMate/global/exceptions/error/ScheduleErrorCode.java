package com.better.CommuteMate.global.exceptions.error;

import org.springframework.http.HttpStatus;

public enum ScheduleErrorCode implements CustomErrorCode {
    ADMIN_WORK_SCHEDULE_TARGET_USER_NOT_FOUND("사용자를 찾을 수 없습니다.", "[Error] : admin work schedule target user not found", HttpStatus.NOT_FOUND),
    INVALID_ADMIN_WORK_ASSIGNMENT_TIME("근로 시간은 30분 단위로만 지정할 수 있습니다.", "[Error] : invalid admin work assignment time", HttpStatus.BAD_REQUEST),
    ADMIN_WORK_ASSIGNMENT_USER_NOT_FOUND("사용자를 찾을 수 없습니다.", "[Error] : admin work assignment user not found", HttpStatus.BAD_REQUEST),
    ADMIN_WORK_ASSIGNMENT_DUPLICATED("이미 해당 시간에 배치된 사용자입니다.", "[Error] : duplicated admin work assignment", HttpStatus.CONFLICT),
    ADMIN_WORK_ASSIGNMENT_WORKPLACE_NOT_FOUND("조직의 근무지를 찾을 수 없습니다.", "[Error] : admin work assignment workplace not found", HttpStatus.NOT_FOUND),
    ADMIN_WORK_SCHEDULE_NOT_FOUND("근로 시간표를 찾을 수 없습니다.", "[Error] : admin work schedule not found", HttpStatus.NOT_FOUND),
    ADMIN_WORK_SCHEDULE_HAS_ATTENDANCE("출퇴근 기록이 있어 삭제할 수 없습니다.", "[Error] : admin work schedule has attendance", HttpStatus.CONFLICT),
    ADMIN_WORK_SCHEDULE_USER_NOT_FOUND("사용자를 찾을 수 없습니다.", "[Error] : admin work schedule user not found", HttpStatus.BAD_REQUEST),
    ADMIN_WORK_SCHEDULE_INVALID_RANGE("조회 기간이 올바르지 않습니다.", "[Error] : invalid admin work schedule range", HttpStatus.BAD_REQUEST),
    INVALID_CHANGE_REQUEST_IDS("요청 ID 목록이 올바르지 않습니다.", "[Error] : invalid change request ids", HttpStatus.BAD_REQUEST),
    INVALID_CHANGE_REQUEST_PROCESS_STATUS("올바르지 않은 처리 상태입니다.", "[Error] : invalid change request process status", HttpStatus.BAD_REQUEST),
    CHANGE_REQUEST_REJECT_REASON_REQUIRED("거절 사유를 입력해야 합니다.", "[Error] : change request reject reason required", HttpStatus.BAD_REQUEST),
    CHANGE_REQUEST_ALREADY_PROCESSED("이미 처리된 요청입니다.", "[Error] : change request already processed", HttpStatus.BAD_REQUEST),
    CHANGE_REQUEST_NOT_FOUND("근로시간 수정 요청을 찾을 수 없습니다.", "[Error] : change request not found", HttpStatus.NOT_FOUND),
    CHANGE_REQUEST_CAPACITY_EXCEEDED("해당 시간대의 최대 근무 인원을 초과했습니다.", "[Error] : change request capacity exceeded", HttpStatus.CONFLICT),
    INVALID_CHANGE_REQUEST_STATUS("올바르지 않은 변경 요청 상태입니다.", "[Error] : invalid change request status", HttpStatus.BAD_REQUEST),
    INVALID_CHANGE_REQUEST_HISTORY_STATUS("올바르지 않은 신청기록 상태입니다.", "[Error] : invalid change request history status", HttpStatus.BAD_REQUEST),
    INVALID_CHANGE_REQUEST_PAGE("페이지 요청 값이 올바르지 않습니다.", "[Error] : invalid change request page", HttpStatus.BAD_REQUEST),
    INVALID_CHANGE_REQUEST_YEAR_MONTH("조회 연도 또는 월 값이 올바르지 않습니다.", "[Error] : invalid change request year or month", HttpStatus.BAD_REQUEST),
    ADMIN_SCHEDULE_QUERY_INVALID("조회 연도 또는 월 값이 올바르지 않습니다.", "[Error] : invalid admin schedule query range", HttpStatus.BAD_REQUEST),
    ADMIN_SCHEDULE_SETTING_NOT_FOUND("해당 월의 스케줄 설정을 찾을 수 없습니다.", "[Error] : admin schedule setting not found", HttpStatus.NOT_FOUND),
    INVALID_SCHEDULE_SETTING_REQUEST("요청 값이 올바르지 않습니다.", "[Error] : invalid schedule setting request", HttpStatus.BAD_REQUEST),
    INVALID_SETTING_APPLY_DATE("근로신청 시작일은 마감일보다 이전이어야 합니다.", "[Error] : apply start date is after apply end date", HttpStatus.BAD_REQUEST),
    INVALID_SETTING_MIN_MAX("최소 근무시간은 최대 근무시간보다 작아야 합니다.", "[Error] : minimum work minutes exceed maximum work minutes", HttpStatus.BAD_REQUEST),
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
    CROSS_WEEK_RANGE_NOT_ALLOWED("조회 기간은 같은 주 이내여야 합니다.", "[Error] : 다른 주에 걸친 조회 범위", HttpStatus.BAD_REQUEST),
    APPLY_PERIOD_NOT_ACTIVE("근로 신청 기간이 아닙니다.", "[Error] : 근로 신청 기간 외 신청 시도", HttpStatus.BAD_REQUEST),
    INVALID_SLOT_UNIT("근무 시간은 최소 근무 단위 기준으로 신청해야 합니다.", "[Error] : 최소 근무 단위 미준수", HttpStatus.BAD_REQUEST),
    INVALID_SLOT_BOUNDARY("근무 시간은 30분 단위로 신청해야 합니다.", "[Error] : 30분 경계 정렬 위반", HttpStatus.BAD_REQUEST),
    INVALID_SLOT_DURATION("근무 시간은 최소 근무 시간 이상으로 신청해야 합니다.", "[Error] : 최소 근무 시간 미충족", HttpStatus.BAD_REQUEST),
    UNAVAILABLE_TIME_CONFLICT("근무 불가 시간대에 신청할 수 없습니다.", "[Error] : 근무 불가 시간대 신청 시도", HttpStatus.UNPROCESSABLE_ENTITY),
    EDIT_NOT_ALLOWED_DURING_APPLY_PERIOD("신청 기간 중에는 수정 요청을 할 수 없습니다.", "[Error] : 신청 기간 중 수정 요청 시도", HttpStatus.BAD_REQUEST);


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
