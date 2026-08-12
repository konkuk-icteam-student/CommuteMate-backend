package com.better.CommuteMate.user.application;

import com.better.CommuteMate.domain.user.entity.User;
import com.better.CommuteMate.domain.user.repository.UserRepository;
import com.better.CommuteMate.global.code.CodeType;
import com.better.CommuteMate.global.exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserSearchServiceTest {

    @Mock UserRepository userRepository;

    AdminUserSearchService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserSearchService(userRepository);
    }

    @Test
    @DisplayName("관리자 학생 검색 - 같은 조직의 학생을 이름 부분 일치로 조회한다")
    void searchesStudentsInAdminOrganization() {
        User first = User.builder().userId(2L).name("박보검").build();
        User second = User.builder().userId(3L).name("박영희").build();
        when(userRepository
                .findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCaseOrderByNameAscUserIdAsc(
                        10L, CodeType.RL01, "박"
                ))
                .thenReturn(List.of(first, second));

        var response = service.search(10L, "  박  ");

        assertThat(response.users).hasSize(2);
        assertThat(response.users.get(0).userId()).isEqualTo("2");
        assertThat(response.users.get(0).userName()).isEqualTo("박보검");
        verify(userRepository)
                .findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCaseOrderByNameAscUserIdAsc(
                        10L, CodeType.RL01, "박"
                );
    }

    @Test
    @DisplayName("관리자 학생 검색 - 결과가 없으면 빈 배열을 반환한다")
    void returnsEmptyUsersWhenNothingMatches() {
        when(userRepository
                .findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCaseOrderByNameAscUserIdAsc(
                        10L, CodeType.RL01, "없는이름"
                ))
                .thenReturn(List.of());

        var response = service.search(10L, "없는이름");

        assertThat(response.users).isEmpty();
    }

    @Test
    @DisplayName("관리자 학생 검색 - 검색어가 누락되거나 공백이면 실패한다")
    void rejectsBlankKeyword() {
        assertThatThrownBy(() -> service.search(10L, null))
                .isInstanceOf(CustomException.class)
                .hasMessage("검색어를 입력해주세요.");
        assertThatThrownBy(() -> service.search(10L, "   "))
                .isInstanceOf(CustomException.class)
                .hasMessage("검색어를 입력해주세요.");

        verify(userRepository, never())
                .findAllByOrganizationIdAndRoleCodeAndNameContainingIgnoreCaseOrderByNameAscUserIdAsc(
                        10L, CodeType.RL01, ""
                );
    }
}
