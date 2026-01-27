package com.better.CommuteMate.team.application;

import com.better.CommuteMate.domain.team.entity.Team;
import com.better.CommuteMate.domain.team.repository.TeamRepository;
import com.better.CommuteMate.global.exceptions.TeamException;
import com.better.CommuteMate.global.exceptions.error.TeamErrorCode;
import com.better.CommuteMate.team.application.dto.request.PostTeamRequest;
import com.better.CommuteMate.team.application.dto.response.PostTeamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;

    public PostTeamResponse registerTeam(PostTeamRequest request) {

        if (teamRepository.existsByName(request.teamName())) {
            throw new TeamException(TeamErrorCode.TEAM_ALREADY_EXISTS);
        }

        Team team = new Team(request.teamName());
        Team saved = teamRepository.save(team);

        return new PostTeamResponse(saved.getId());
    }
}
