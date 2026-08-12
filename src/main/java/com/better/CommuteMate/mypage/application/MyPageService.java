package com.better.CommuteMate.mypage.application;

import com.better.CommuteMate.domain.faq.entity.Faq;
import com.better.CommuteMate.domain.faq.entity.FaqStatus;
import com.better.CommuteMate.domain.faq.repository.FaqRepository;
import com.better.CommuteMate.domain.organization.entity.Organization;
import com.better.CommuteMate.domain.organization.repository.OrganizationRepository;
import com.better.CommuteMate.domain.schedule.entity.WorkSchedule;
import com.better.CommuteMate.domain.schedule.entity.WorkScheduleSetting;
import com.better.CommuteMate.domain.schedule.repository.WorkSchedulesRepository;
import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.entity.UserProfile;
import com.better.CommuteMate.domain.user.repository.UserProfileRepository;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.domain.workattendance.repository.WorkAttendanceRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.GlobalErrorCode;
import com.better.CommuteMate.global.exceptions.error.OrganizationErrorCode;
import com.better.CommuteMate.global.util.WorkWeekUtils;
import com.better.CommuteMate.mypage.dto.GetMyFaqListResponse;
import com.better.CommuteMate.mypage.dto.GetMyFaqListWrapper;
import com.better.CommuteMate.mypage.dto.GetMyPageResponse;
import com.better.CommuteMate.mypage.dto.MyPageInfoResponse;
import com.better.CommuteMate.schedule.application.WorkScheduleSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {
    private final UserRepository userRepository;
    private final FaqRepository faqRepository;
    private final OrganizationRepository organizationRepository;
    private final UserProfileRepository userProfileRepository;
    private final WorkSchedulesRepository workSchedulesRepository;
    private final WorkAttendanceRepository workAttendanceRepository;
    private final WorkScheduleSettingService workScheduleSettingService;

    public MyPageInfoResponse getMyPageInfo(Long userId, Long organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> CustomException.of(GlobalErrorCode.USER_NOT_FOUND));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> CustomException.of(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        UserProfile profile = userProfileRepository.findById(userId).orElse(null);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        LocalDate weekStart = WorkWeekUtils.weekStart(today);
        LocalDate weekEnd = weekStart.plusDays(6);

        YearMonth yearMonth = YearMonth.from(today);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        List<WorkSchedule> weekSlots = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(userId, weekStart, weekEnd, CodeType.WS04);
        List<WorkSchedule> monthSlots = workSchedulesRepository
                .findAllByUser_UserIdAndDateBetweenAndStatusCodeNot(userId, monthStart, monthEnd, CodeType.WS04);

        Set<Long> weekCheckedInIds = workAttendanceRepository.findAllByScheduleIn(weekSlots).stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                .map(a -> a.getSchedule().getScheduleId())
                .collect(Collectors.toSet());
        Set<Long> monthCheckedInIds = workAttendanceRepository.findAllByScheduleIn(monthSlots).stream()
                .filter(a -> a.getCheckTypeCode() == CodeType.CT01)
                .map(a -> a.getSchedule().getScheduleId())
                .collect(Collectors.toSet());

        long weekWorkedMinutes = weekSlots.stream()
                .filter(s -> weekCheckedInIds.contains(s.getScheduleId())
                        && LocalDateTime.of(s.getDate(), s.getEndTime()).isBefore(now))
                .mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                .sum();
        long monthWorkedMinutes = monthSlots.stream()
                .filter(s -> monthCheckedInIds.contains(s.getScheduleId())
                        && LocalDateTime.of(s.getDate(), s.getEndTime()).isBefore(now))
                .mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                .sum();

        Optional<WorkScheduleSetting> settingOpt =
                workScheduleSettingService.getSetting(organizationId, today.getYear(), today.getMonthValue());
        int weekLimitHours = settingOpt
                .map(s -> s.getWeeklyMaxMinutes() != null ? s.getWeeklyMaxMinutes() / 60 : 0)
                .orElse(0);
        int monthLimitHours = settingOpt
                .map(s -> s.getMonthlyRequiredMinutes() != null ? s.getMonthlyRequiredMinutes() / 60 : 0)
                .orElse(0);

        return MyPageInfoResponse.builder()
                .userName(user.getName())
                .roleName(user.getRoleCode().getCodeValue())
                .organizationName(organization.getName())
                .department(profile != null ? profile.getDepartment() : null)
                .studentId(profile != null ? profile.getStudentId() : null)
                .week(MyPageInfoResponse.PeriodHours.builder()
                        .workedHours((int) (weekWorkedMinutes / 60))
                        .limitHours(weekLimitHours)
                        .build())
                .month(MyPageInfoResponse.PeriodHours.builder()
                        .workedHours((int) (monthWorkedMinutes / 60))
                        .limitHours(monthLimitHours)
                        .build())
                .build();
    }

    public GetMyPageResponse getMyPage(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        CustomException.of(GlobalErrorCode.USER_NOT_FOUND)
                );

        Organization organization = organizationRepository
                .findById(user.getOrganizationId())
                .orElseThrow(() ->
                        CustomException.of(OrganizationErrorCode.ORGANIZATION_NOT_FOUND)
                );

        long publishedCount = faqRepository
                .countByWriter_UserIdAndStatusAndDeletedFlagFalse(
                        userId,
                        FaqStatus.PUBLISHED
                );

        long draftCount = faqRepository
                .countByWriter_UserIdAndStatusAndDeletedFlagFalse(
                        userId,
                        FaqStatus.DRAFT
                );

        return new GetMyPageResponse(
                user.getName(),
                user.getEmail(),
                organization.getId(),
                organization.getName(),
                publishedCount,
                draftCount
        );
    }

    @Transactional(readOnly = true)
    public GetMyFaqListWrapper getMyPublishedFaqs(
            Long userId,
            int page
    ) {
        Pageable pageable = PageRequest.of(
                page,
                10,
                Sort.by(Sort.Direction.DESC, "updatedDate")
        );

        Page<Faq> faqPage =
                faqRepository.findByWriter_UserIdAndStatusAndDeletedFlagFalse(
                        userId,
                        FaqStatus.PUBLISHED,
                        pageable
                );

        List<GetMyFaqListResponse> faqs = faqPage.getContent()
                .stream()
                .map(GetMyFaqListResponse::new)
                .toList();

        return new GetMyFaqListWrapper(
                faqs,
                faqPage.getNumber(),
                faqPage.getTotalPages(),
                faqPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public GetMyFaqListWrapper getMyDraftFaqs(
            Long userId,
            int page
    ) {
        Pageable pageable = PageRequest.of(
                page,
                10,
                Sort.by(Sort.Direction.DESC, "updatedDate")
        );

        Page<Faq> faqPage =
                faqRepository.findByWriter_UserIdAndStatusAndDeletedFlagFalse(
                        userId,
                        FaqStatus.DRAFT,
                        pageable
                );

        List<GetMyFaqListResponse> faqs = faqPage.getContent()
                .stream()
                .map(GetMyFaqListResponse::new)
                .toList();

        return new GetMyFaqListWrapper(
                faqs,
                faqPage.getNumber(),
                faqPage.getTotalPages(),
                faqPage.getTotalElements()
        );
    }
}
