package com.better.CommuteMate.manager.application;

import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.category.entity.ManagerCategory;
import com.better.CommuteMate.domain.category.repository.CategoryRepository;
import com.better.CommuteMate.domain.category.repository.ManagerCategoryRepository;
import com.better.CommuteMate.domain.manager.entity.Manager;
import com.better.CommuteMate.domain.manager.repository.ManagerRepository;
import com.better.CommuteMate.domain.team.entity.Team;
import com.better.CommuteMate.domain.team.repository.TeamRepository;
import com.better.CommuteMate.global.exceptions.CategoryException;
import com.better.CommuteMate.global.exceptions.ManagerException;
import com.better.CommuteMate.global.exceptions.TeamException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import com.better.CommuteMate.global.exceptions.error.ManagerErrorCode;
import com.better.CommuteMate.global.exceptions.error.TeamErrorCode;
import com.better.CommuteMate.manager.application.dto.request.PostManagerRequest;
import com.better.CommuteMate.manager.application.dto.response.GetManagerListResponse;
import com.better.CommuteMate.manager.application.dto.response.GetManagerListWrapper;
import com.better.CommuteMate.manager.application.dto.response.PostManagerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final CategoryRepository categoryRepository;
    private final ManagerCategoryRepository managerCategoryRepository;
    private final TeamRepository teamRepository;

    public PostManagerResponse registerManager(PostManagerRequest request) {

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));

        Manager manager = managerRepository.findByNameAndTeamAndPhonenum(request.name(), team, request.phonenum())
                .orElseGet(() -> managerRepository.save(new Manager(request.name(), team, request.phonenum())));

        if (managerCategoryRepository.existsByManagerIdAndCategoryId(manager.getId(), request.categoryId())) {
            throw new ManagerException(ManagerErrorCode.MANAGER_CATEGORY_ALREADY_EXISTS);
        }

        ManagerCategory managerCategory = ManagerCategory.assign(manager, category);

        managerCategoryRepository.save(managerCategory);

        return new PostManagerResponse(manager.getId(), category.getId());
    }

    @Transactional(readOnly = true)
    public GetManagerListWrapper getManagerList(Long categoryId, Long teamId, boolean favoriteOnly) {
        Team team = null;

        if (teamId != null) {
            team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));
        }

        List<ManagerCategory> managerCategories = managerCategoryRepository.getManagers(categoryId, team, favoriteOnly);

        List<GetManagerListResponse> result = managerCategories.stream()
                .map(mc -> new GetManagerListResponse(mc.getCategory(), mc.getManager()))
                .toList();

        return new GetManagerListWrapper(result);
    }
}
