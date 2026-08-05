package com.better.CommuteMate.global.code;

import lombok.Getter;

/**
 * 시스템 전체에서 사용하는 코드 타입 Enum
 * 각 코드는 full_code(4자리), major_code(2자리), sub_code(2자리)로 구성됨
 */
@Getter
public enum CodeType {
    // WS: 근무신청 상태 (Work Schedule Status)
    WS01("WS", "01", "REQUESTED", "신청"),
    WS02("WS", "02", "APPROVED", "승인"),
    WS03("WS", "03", "REJECTED", "반려"),
    WS04("WS", "04", "CANCELLED", "취소"),

    // WK: 실제 근무 상태 (Work State)
    WK01("WK", "01", "SCHEDULED", "근무 예정"),
    WK02("WK", "02", "WORKING", "근무 중"),
    WK03("WK", "03", "COMPLETED", "근무 완료"),
    WK04("WK", "04", "NO_SHOW", "미출근"),

    // AT: 근태 상태 (Attendance State)
    AT01("AT", "01", "NORMAL", "정상"),
    AT02("AT", "02", "LATE", "지각"),
    AT03("AT", "03", "ABSENT", "결근"),

    // CR: 요청 유형 (Change Request Type)
    CR01("CR", "01", "EDIT", "수정 요청"),
    CR02("CR", "02", "DELETE", "삭제 요청"),

    // CS: 요청 상태 (Change Request Status)
    CS01("CS", "01", "PENDING", "대기"),
    CS02("CS", "02", "APPROVED", "승인"),
    CS03("CS", "03", "REJECTED", "거절"),

    // CT: 출근 인증 타입 (Check Type)
    CT01("CT", "01", "CHECK_IN", "출근 체크"),
    CT02("CT", "02", "CHECK_OUT", "퇴근 체크"),

    // TT: 업무 유형 (Task Type)
    TT01("TT", "01", "REGULAR", "정기 업무"),
    TT02("TT", "02", "IRREGULAR", "비정기 업무"),

    // RL: 사용자 역할 (Role)
    RL01("RL", "01", "STUDENT", "학생"),
    RL02("RL", "02", "ADMIN", "관리자"),

    // NT: 알림 유형 (Notification Type)
    NT01("NT", "01", "WORK_CHANGE_APPROVED", "근무 변경 요청 승인"),
    NT02("NT", "02", "WORK_CHANGE_REJECTED", "근무 변경 요청 거절"),
    NT03("NT", "03", "WORK_SCHEDULE_OPEN", "근무 신청 시작");

    private final String majorCode;
    private final String subCode;
    private final String codeName;
    private final String codeValue;

    CodeType(String majorCode, String subCode, String codeName, String codeValue) {
        this.majorCode = majorCode;
        this.subCode = subCode;
        this.codeName = codeName;
        this.codeValue = codeValue;
    }

    /**
     * full_code 반환 (예: "WS01")
     */
    public String getFullCode() {
        return majorCode + subCode;
    }

    /**
     * full_code로부터 CodeType을 찾아 반환
     * @param fullCode 4자리 코드 (예: "WS01")
     * @return 해당하는 CodeType
     * @throws IllegalArgumentException 해당하는 코드가 없을 경우
     */
    public static CodeType fromFullCode(String fullCode) {
        if (fullCode == null || fullCode.length() != 4) {
            throw new IllegalArgumentException("Invalid full code format: " + fullCode);
        }
        for (CodeType type : values()) {
            if (type.getFullCode().equals(fullCode)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + fullCode);
    }

    /**
     * major_code로 해당하는 모든 CodeType 배열 반환
     * @param majorCode 2자리 대분류 코드 (예: "WS")
     * @return 해당 대분류의 CodeType 배열
     */
    public static CodeType[] getByMajorCode(String majorCode) {
        return java.util.Arrays.stream(values())
                .filter(type -> type.getMajorCode().equals(majorCode))
                .toArray(CodeType[]::new);
    }
}
