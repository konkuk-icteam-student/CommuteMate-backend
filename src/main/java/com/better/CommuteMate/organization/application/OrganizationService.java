package com.better.CommuteMate.organization.application;

import com.better.CommuteMate.domain.manager.repository.ManagerRepository;
import com.better.CommuteMate.domain.organization.entity.Organization;
import com.better.CommuteMate.domain.organization.repository.OrganizationRepository;
import com.better.CommuteMate.global.exceptions.CustomException;
import com.better.CommuteMate.global.exceptions.error.OrganizationErrorCode;
import com.better.CommuteMate.organization.application.dto.request.PostOrganizationRequest;
import com.better.CommuteMate.organization.application.dto.response.GetOrganizationListResponse;
import com.better.CommuteMate.organization.application.dto.response.GetOrganizationListWrapper;
import com.better.CommuteMate.organization.application.dto.response.PostOrganizationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ManagerRepository managerRepository;

    public PostOrganizationResponse registerOrganization(PostOrganizationRequest request) {

        if (organizationRepository.existsByName(request.organizationName())) {
            throw new CustomException(OrganizationErrorCode.ORGANIZATION_ALREADY_EXISTS);
        }

        Organization organization = new Organization(request.organizationName());
        Organization saved = organizationRepository.save(organization);

        return new PostOrganizationResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public GetOrganizationListWrapper getOrganizationList() {
        List<Organization> organizations = organizationRepository.findAll();

        List<GetOrganizationListResponse> result = organizations.stream()
                .map(organization -> new GetOrganizationListResponse(organization.getId(), organization.getName()))
                .toList();

        return new GetOrganizationListWrapper(result);
    }

    public void deleteOrganization(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new CustomException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        if (managerRepository.existsByOrganizationId(organizationId)) {
            throw new CustomException(OrganizationErrorCode.ORGANIZATION_DELETE_NOT_ALLOWED);
        }

        organizationRepository.delete(organization);
    }
}
