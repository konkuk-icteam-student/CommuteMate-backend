package com.better.CommuteMate.team.application;

import com.better.CommuteMate.category.application.dto.response.GetCategoryListResponse;
import com.better.CommuteMate.category.application.dto.response.GetCategoryListWrapper;
import com.better.CommuteMate.domain.category.entity.Category;
import com.better.CommuteMate.domain.manager.repository.ManagerRepository;
import com.better.CommuteMate.domain.team.entity.Team;
import com.better.CommuteMate.domain.team.repository.TeamRepository;
import com.better.CommuteMate.global.exceptions.CategoryException;
import com.better.CommuteMate.global.exceptions.TeamException;
import com.better.CommuteMate.global.exceptions.error.CategoryErrorCode;
import com.better.CommuteMate.global.exceptions.error.TeamErrorCode;
import com.better.CommuteMate.team.application.dto.request.PostTeamRequest;
import com.better.CommuteMate.team.application.dto.response.GetTeamListResponse;
import com.better.CommuteMate.team.application.dto.response.GetTeamListWrapper;
import com.better.CommuteMate.team.application.dto.response.PostTeamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final ManagerRepository managerRepository;

    public PostTeamResponse registerTeam(PostTeamRequest request) {

        if (teamRepository.existsByName(request.teamName())) {
            throw new TeamException(TeamErrorCode.TEAM_ALREADY_EXISTS);
        }

        Team team = new Team(request.teamName());
        Team saved = teamRepository.save(team);

        return new PostTeamResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public GetTeamListWrapper getTeamList() {
        List<Team> teams = teamRepository.findAll();

        List<GetTeamListResponse> result = teams.stream()
                .map(team -> new GetTeamListResponse(team.getId(), team.getName()))
                .toList();

        return new GetTeamListWrapper(result);
    }

    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamException(TeamErrorCode.TEAM_NOT_FOUND));

        teamRepository.delete(team);
    }
}
