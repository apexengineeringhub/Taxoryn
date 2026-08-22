package com.taxoryn.module.marketplace.mapper;

import com.taxoryn.module.marketplace.dto.*;
import com.taxoryn.module.marketplace.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MarketplaceMapper {

    @Mapping(target = "specializations", source = "specializations", qualifiedByName = "stringToList")
    @Mapping(target = "publicSlug", source = "slug")
    @Mapping(target = "description", source = "bio")
    @Mapping(target = "website", source = "websiteUrl")
    @Mapping(target = "services", ignore = true)
    @Mapping(target = "recentReviews", ignore = true)
    PublicMarketplaceProfileDto toProfileDto(MarketplaceProfileEntity entity);

    List<PublicMarketplaceProfileDto> toProfileDtoList(List<MarketplaceProfileEntity> entities);

    MarketplaceServiceDto toServiceDto(MarketplaceServiceEntity entity);

    List<MarketplaceServiceDto> toServiceDtoList(List<MarketplaceServiceEntity> entities);

    @Mapping(target = "serviceTitle", ignore = true)
    @Mapping(target = "convertedClientName", ignore = true)
    @Mapping(target = "assignedEmployeeName", ignore = true)
    MarketplaceLeadDto toLeadDto(MarketplaceLeadEntity entity);

    List<MarketplaceLeadDto> toLeadDtoList(List<MarketplaceLeadEntity> entities);

    @Mapping(target = "practiceDisplayName", ignore = true)
    @Mapping(target = "assignedEmployeeName", ignore = true)
    MarketplaceConsultationDto toConsultationDto(MarketplaceConsultationEntity entity);

    List<MarketplaceConsultationDto> toConsultationDtoList(List<MarketplaceConsultationEntity> entities);

    MarketplaceReviewDto toReviewDto(MarketplaceReviewEntity entity);

    List<MarketplaceReviewDto> toReviewDtoList(List<MarketplaceReviewEntity> entities);

    @Mapping(target = "organizationName", ignore = true)
    MarketplaceVerificationDto toVerificationDto(MarketplaceVerificationEntity entity);

    List<MarketplaceVerificationDto> toVerificationDtoList(List<MarketplaceVerificationEntity> entities);

    @Mapping(target = "practiceDisplayName", ignore = true)
    @Mapping(target = "clientName", ignore = true)
    @Mapping(target = "clientEmail", ignore = true)
    @Mapping(target = "clientPhone", ignore = true)
    @Mapping(target = "serviceTitle", ignore = true)
    MarketplaceProposalDto toProposalDto(MarketplaceProposalEntity entity);

    List<MarketplaceProposalDto> toProposalDtoList(List<MarketplaceProposalEntity> entities);

    @Mapping(target = "practiceDisplayName", ignore = true)
    @Mapping(target = "proposalTitle", ignore = true)
    @Mapping(target = "assignedEmployeeName", ignore = true)
    @Mapping(target = "documents", ignore = true)
    MarketplaceOnboardingDto toOnboardingDto(MarketplaceOnboardingEntity entity);

    List<MarketplaceOnboardingDto> toOnboardingDtoList(List<MarketplaceOnboardingEntity> entities);

    OnboardingDocumentDto toOnboardingDocumentDto(MarketplaceOnboardingDocumentEntity entity);

    List<OnboardingDocumentDto> toOnboardingDocumentDtoList(List<MarketplaceOnboardingDocumentEntity> entities);

    @Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Named("listToString")
    default String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(", ", list);
    }
}
