package com.better.CommuteMate.admin.application;

import com.better.CommuteMate.admin.controller.dto.AdminWorkerPageResponse;
import com.better.CommuteMate.admin.controller.dto.AdminWorkerDetailResponse;
import com.better.CommuteMate.admin.controller.dto.UpdateAdminWorkerRequest;
import com.better.CommuteMate.admin.controller.dto.UpdateAdminWorkerResponse;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkScheduleSettingRepository;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.entity.UserProfile;
import com.better.CommuteMate.domain.user.repository.UserProfileRepository;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.entity.WorkAttendance;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.domain.workchangerequest.entity.WorkChangeRequest;
import com.better.CommuteMate.domain.workchangerequest.repository.WorkChangeRequestRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.AdminWorkerErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminWorkerService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final WorkSchedulesRepository scheduleRepository;
    private final WorkAttendanceRepository attendanceRepository;
    private final WorkScheduleSettingRepository settingRepository;
    private final WorkChangeRequestRepository changeRequestRepository;

    @Transactional
    public UpdateAdminWorkerResponse updateWorker(Long organizationId, Long userId,
                                                   UpdateAdminWorkerRequest request) {
        if (request == null || request.isEmpty()) {
            throw CustomException.of(AdminWorkerErrorCode.INVALID_WORKER_UPDATE);
        }
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> CustomException.of(AdminWorkerErrorCode.WORKER_NOT_FOUND));
        if (!Objects.equals(user.getOrganizationId(), organizationId) || user.getRoleCode() != CodeType.RL01) {
            throw CustomException.of(AdminWorkerErrorCode.WORKER_UPDATE_ACCESS_DENIED);
        }

        String name = trimmed(request.name());
        String studentId = trimmed(request.studentId());
        String department = trimmed(request.department());
        String phoneNumber = trimmed(request.phoneNumber());
        if ((request.name() != null && name.isEmpty())
                || (request.studentId() != null && studentId.isEmpty())
                || (request.department() != null && department.isEmpty())
                || (request.phoneNumber() != null && phoneNumber.isEmpty())) {
            throw CustomException.of(AdminWorkerErrorCode.INVALID_WORKER_UPDATE);
        }
        if (name != null) user.setName(name);
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> UserProfile.builder().user(user).build());
        profile.update(studentId, department, request.grade(), phoneNumber);
        userProfileRepository.save(profile);
        return new UpdateAdminWorkerResponse(user, profile);
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    public AdminWorkerDetailResponse getWorker(Long organizationId, Long userId, String dateValue) {
        LocalDate date = parseDate(dateValue);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> CustomException.of(AdminWorkerErrorCode.WORKER_NOT_FOUND));
        if (!Objects.equals(user.getOrganizationId(), organizationId) || user.getRoleCode() != CodeType.RL01) {
            throw CustomException.of(AdminWorkerErrorCode.WORKER_ACCESS_DENIED);
        }
        WorkScheduleSetting setting = settingRepository.findByOrganizationIdAndYearAndMonth(
                organizationId, date.getYear(), date.getMonthValue()
        ).orElseThrow(() -> CustomException.of(AdminWorkerErrorCode.WORK_SCHEDULE_SETTING_NOT_FOUND));
        UserProfile profile = userProfileRepository.findAllByUserIdIn(List.of(userId)).stream()
                .findFirst().orElse(null);

        YearMonth month = YearMonth.from(date);
        LocalDate monthStart = month.atDay(1), monthEnd = month.atEndOfMonth();
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate scheduleStart = monthStart.isBefore(weekStart) ? monthStart : weekStart;
        LocalDate scheduleEnd = monthEnd.isAfter(weekEnd) ? monthEnd : weekEnd;
        List<WorkSchedule> schedules = scheduleRepository.findAllByUser_UserIdInAndDateBetweenAndStatusCode(
                List.of(userId), scheduleStart, scheduleEnd, CodeType.WS02
        );
        List<WorkAttendance> attendances = schedules.isEmpty()
                ? List.of() : attendanceRepository.findAllByScheduleIn(schedules);
        Map<Long, List<WorkAttendance>> attendancesBySchedule = attendances.stream()
                .collect(Collectors.groupingBy(a -> a.getSchedule().getScheduleId()));
        List<WorkSchedule> monthlySchedules = schedules.stream()
                .filter(s -> !s.getDate().isBefore(monthStart) && !s.getDate().isAfter(monthEnd)).toList();
        LocalDateTime referenceTime = referenceTime(date);

        return new AdminWorkerDetailResponse(
                date, user.getUserId(), user.getName(),
                profile == null ? null : profile.getStudentId(),
                profile == null ? null : profile.getDepartment(),
                profile == null ? null : profile.getGrade(),
                profile == null ? null : profile.getPhoneNumber(),
                user.getEmail(), user.getCreatedAt() == null ? null : user.getCreatedAt().toLocalDate(),
                workedMinutes(schedules, attendancesBySchedule,
                        s -> !s.getDate().isBefore(weekStart) && !s.getDate().isAfter(weekEnd), referenceTime),
                setting.getWeeklyMaxMinutes(),
                workedMinutes(monthlySchedules, attendancesBySchedule, s -> true, referenceTime),
                setting.getMonthlyMaxMinutes(),
                changeRequestRepository.countByUser_UserId(userId),
                changeRequestRepository.countByUser_UserIdAndStatusCode(userId, CodeType.CS02)
        );
    }

    public AdminWorkerPageResponse getWorkers(Long organizationId, String dateValue, String keyword,
                                               Integer pageValue, Integer sizeValue) {
        LocalDate date = parseDate(dateValue);
        int page = pageValue == null ? 0 : pageValue;
        int size = sizeValue == null ? 10 : sizeValue;
        if (page < 0 || size < 1) throw CustomException.of(AdminWorkerErrorCode.INVALID_REQUEST);

        WorkScheduleSetting setting = settingRepository.findByOrganizationIdAndYearAndMonth(
                organizationId, date.getYear(), date.getMonthValue()
        ).orElseThrow(() -> CustomException.of(AdminWorkerErrorCode.WORK_SCHEDULE_SETTING_NOT_FOUND));

        Page<User> users = userRepository.findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCase(
                organizationId, CodeType.RL01, keyword == null ? "" : keyword.trim(), PageRequest.of(page, size));
        List<Long> userIds = users.getContent().stream().map(User::getUserId).toList();
        Map<Long, UserProfile> profiles = userIds.isEmpty() ? Map.of() : userProfileRepository.findAllByUserIdIn(userIds)
                .stream().collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        YearMonth month = YearMonth.from(date);
        LocalDate monthStart = month.atDay(1), monthEnd = month.atEndOfMonth();
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate scheduleStart = monthStart.isBefore(weekStart) ? monthStart : weekStart;
        LocalDate scheduleEnd = monthEnd.isAfter(weekEnd) ? monthEnd : weekEnd;
        List<WorkSchedule> schedules = userIds.isEmpty() ? List.of() :
                scheduleRepository.findAllByUser_UserIdInAndDateBetweenAndStatusCode(userIds, scheduleStart, scheduleEnd, CodeType.WS02);
        List<WorkAttendance> attendances = schedules.isEmpty() ? List.of() : attendanceRepository.findAllByScheduleIn(schedules);
        Map<Long, List<WorkSchedule>> schedulesByUser = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getUser().getUserId()));
        Map<Long, List<WorkAttendance>> attendancesBySchedule = attendances.stream()
                .collect(Collectors.groupingBy(a -> a.getSchedule().getScheduleId()));
        List<WorkChangeRequest> requests = userIds.isEmpty() ? List.of() :
                changeRequestRepository.findAllByUsersAndItemDateBetween(userIds, monthStart, monthEnd);
        Map<Long, List<WorkChangeRequest>> requestsByUser = requests.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getUserId()));
        LocalDateTime referenceTime = referenceTime(date);

        List<AdminWorkerPageResponse.Worker> workers = users.getContent().stream().map(user -> {
            UserProfile profile = profiles.get(user.getUserId());
            List<WorkSchedule> userSchedules = schedulesByUser.getOrDefault(user.getUserId(), List.of());
            List<WorkChangeRequest> userRequests = requestsByUser.getOrDefault(user.getUserId(), List.of());
            List<WorkSchedule> monthlySchedules = userSchedules.stream()
                    .filter(s -> !s.getDate().isBefore(monthStart) && !s.getDate().isAfter(monthEnd)).toList();
            long lateCount = monthlySchedules.stream().filter(s -> attendanceIssue(s, attendancesBySchedule, referenceTime) == CodeType.AT02).count();
            long issueCount = monthlySchedules.stream().filter(s -> {
                CodeType issue = attendanceIssue(s, attendancesBySchedule, referenceTime);
                return issue == CodeType.AT02 || issue == CodeType.AT03;
            }).count();
            return new AdminWorkerPageResponse.Worker(
                    user.getUserId(), user.getName(), profile == null ? null : profile.getStudentId(),
                    profile == null ? null : profile.getDepartment(), profile == null ? null : profile.getGrade(),
                    profile == null ? null : profile.getPhoneNumber(),
                    workedMinutes(userSchedules, attendancesBySchedule, s -> !s.getDate().isBefore(weekStart) && !s.getDate().isAfter(weekEnd), referenceTime),
                    setting.getWeeklyMaxMinutes(), workedMinutes(monthlySchedules, attendancesBySchedule, s -> true, referenceTime),
                    setting.getMonthlyMaxMinutes(), userRequests.size(),
                    userRequests.stream().filter(r -> r.getStatusCode() == CodeType.CS02).count(), issueCount, lateCount);
        }).toList();

        return new AdminWorkerPageResponse(date, workers, page, size, users.getTotalElements(),
                users.getTotalPages(), users.isFirst(), users.isLast());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) throw CustomException.of(AdminWorkerErrorCode.INVALID_REQUEST);
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { throw CustomException.of(AdminWorkerErrorCode.INVALID_REQUEST); }
    }

    private LocalDateTime referenceTime(LocalDate date) {
        if (date.isBefore(LocalDate.now())) return date.plusDays(1).atStartOfDay();
        if (date.isAfter(LocalDate.now())) return date.atStartOfDay();
        return LocalDateTime.now();
    }

    private CodeType attendanceIssue(WorkSchedule schedule, Map<Long, List<WorkAttendance>> bySchedule,
                                     LocalDateTime referenceTime) {
        Optional<LocalDateTime> checkIn = bySchedule.getOrDefault(schedule.getScheduleId(), List.of()).stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01).map(WorkAttendance::getCheckTime).min(Comparator.naturalOrder());
        LocalDateTime start = LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
        if (checkIn.isPresent()) return checkIn.get().isAfter(start.plusMinutes(10)) ? CodeType.AT02 : CodeType.AT01;
        return referenceTime.isAfter(LocalDateTime.of(schedule.getDate(), schedule.getEndTime())) ? CodeType.AT03 : null;
    }

    private int workedMinutes(List<WorkSchedule> schedules, Map<Long, List<WorkAttendance>> bySchedule,
                              Predicate<WorkSchedule> include, LocalDateTime referenceTime) {
        long total = 0;
        for (WorkSchedule schedule : schedules.stream().filter(include).toList()) {
            List<WorkAttendance> records = bySchedule.getOrDefault(schedule.getScheduleId(), List.of());
            Optional<LocalDateTime> checkIn = records.stream().filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                    .map(WorkAttendance::getCheckTime).min(Comparator.naturalOrder());
            if (checkIn.isEmpty()) continue;
            LocalDateTime scheduledStart = LocalDateTime.of(schedule.getDate(), schedule.getStartTime());
            LocalDateTime scheduledEnd = LocalDateTime.of(schedule.getDate(), schedule.getEndTime());
            LocalDateTime start = checkIn.get().isBefore(scheduledStart) ? scheduledStart : checkIn.get();
            LocalDateTime end = records.stream().filter(a -> a.getCheckTypeCode() == CodeType.CT02)
                    .map(WorkAttendance::getCheckTime).min(Comparator.naturalOrder()).orElse(referenceTime);
            if (end.isAfter(scheduledEnd)) end = scheduledEnd;
            if (end.isAfter(start)) total += Duration.between(start, end).toMinutes();
        }
        return Math.toIntExact(total);
    }
}
