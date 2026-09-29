package com.taxoryn.module.organization.service;

import com.taxoryn.module.organization.dto.PlanRecommendationDto;
import com.taxoryn.module.organization.entity.PracticeProfileEntity;
import com.taxoryn.module.organization.entity.PracticeType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlanRecommendationServiceImpl implements PlanRecommendationService {

    @Override
    public PlanRecommendationDto recommendPlan(PracticeProfileEntity profile) {
        if (profile == null) {
            return defaultStarterRecommendation("Default recommendation for new organization");
        }

        PracticeType type = profile.getPracticeType() != null ? profile.getPracticeType() : PracticeType.UNKNOWN;
        int clientCount = profile.getApproximateClientCount() != null ? profile.getApproximateClientCount() : 0;
        int practitionerCount = profile.getPractitionerCount() != null ? profile.getPractitionerCount() : 1;
        int employeeCount = profile.getEmployeeCount() != null ? profile.getEmployeeCount() : 1;
        int locationCount = profile.getLocationCount() != null ? profile.getLocationCount() : 1;

        List<String> matchingCriteria = new ArrayList<>();

        // 1. Enterprise Rule Evaluation
        if (type == PracticeType.ENTERPRISE || locationCount > 3 || practitionerCount > 10 || employeeCount > 20 || clientCount > 250) {
            if (type == PracticeType.ENTERPRISE) matchingCriteria.add("Practice Type: Enterprise Organization");
            if (locationCount > 3) matchingCriteria.add("Multiple Locations: " + locationCount + " branches");
            if (practitionerCount > 10) matchingCriteria.add("Practitioners: " + practitionerCount + " partners/directors");
            if (employeeCount > 20) matchingCriteria.add("Staff: " + employeeCount + " team members");
            if (clientCount > 250) matchingCriteria.add("Client Base: " + clientCount + "+ active clients");

            return PlanRecommendationDto.builder()
                    .recommendedPlan("ENTERPRISE")
                    .planName("Enterprise / Network")
                    .justification("Tailored for large multi-branch tax and accounting firms managing high volume clients with dedicated infrastructure.")
                    .matchingCriteria(matchingCriteria)
                    .keyFeatures(List.of("Up to 50 Locations", "250 Users", "2500 Clients", "Full Notice Resolution & Hearing Operations", "Priority Infrastructure"))
                    .multiLocationEnabled(true)
                    .maxLocations(50)
                    .maxUsers(250)
                    .maxClients(2500)
                    .build();
        }

        // 2. Business Rule Evaluation
        if (type == PracticeType.FIRM && (practitionerCount > 3 || clientCount > 50 || locationCount > 1 || employeeCount > 5)) {
            if (practitionerCount > 3) matchingCriteria.add("Practitioners: " + practitionerCount + " practitioners");
            if (clientCount > 50) matchingCriteria.add("Client Base: " + clientCount + "+ active clients");
            if (locationCount > 1) matchingCriteria.add("Locations: " + locationCount + " offices");
            if (employeeCount > 5) matchingCriteria.add("Staff: " + employeeCount + " staff members");

            return PlanRecommendationDto.builder()
                    .recommendedPlan("BUSINESS")
                    .planName("Business Firm")
                    .justification("Designed for growing multi-partner CA firms with multi-office operations and collaborative workflow demands.")
                    .matchingCriteria(matchingCriteria)
                    .keyFeatures(List.of("Up to 10 Locations", "50 Users", "500 Clients", "Multi-branch Workflow Scoping", "Advanced Compliance Automation"))
                    .multiLocationEnabled(true)
                    .maxLocations(10)
                    .maxUsers(50)
                    .maxClients(500)
                    .build();
        }

        // 3. Professional Rule Evaluation
        if (type == PracticeType.FIRM || clientCount > 15 || practitionerCount > 1 || employeeCount > 2) {
            if (type == PracticeType.FIRM) matchingCriteria.add("Practice Type: Established Firm");
            if (clientCount > 15) matchingCriteria.add("Client Base: " + clientCount + " clients");
            if (practitionerCount > 1) matchingCriteria.add("Practitioners: " + practitionerCount + " practitioners");
            if (employeeCount > 2) matchingCriteria.add("Staff: " + employeeCount + " assistants");

            return PlanRecommendationDto.builder()
                    .recommendedPlan("PROFESSIONAL")
                    .planName("Professional Practice")
                    .justification("Optimal for established professional practices and boutique firms seeking comprehensive multi-tax management.")
                    .matchingCriteria(matchingCriteria)
                    .keyFeatures(List.of("Up to 3 Locations", "15 Users", "100 Clients", "GST, ITR, TDS & Tax Notice Workspaces", "Multi-Service Invoicing"))
                    .multiLocationEnabled(true)
                    .maxLocations(3)
                    .maxUsers(15)
                    .maxClients(100)
                    .build();
        }

        // 4. Starter Fallback
        matchingCriteria.add("Solo / Small Practitioner Practice");
        matchingCriteria.add("Single Location Management");
        return defaultStarterRecommendation("Essential compliance and client management for solo practitioners and nascent practices.");
    }

    private PlanRecommendationDto defaultStarterRecommendation(String justification) {
        return PlanRecommendationDto.builder()
                .recommendedPlan("STARTER")
                .planName("Starter Practice")
                .justification(justification)
                .matchingCriteria(List.of("Solo practitioner or starting practice", "Single location", "Core compliance suite"))
                .keyFeatures(List.of("1 Location", "1 Staff User", "25 Clients", "Core Tax Modules (GST, ITR, TDS, Notices)", "Client Vault"))
                .multiLocationEnabled(false)
                .maxLocations(1)
                .maxUsers(1)
                .maxClients(25)
                .build();
    }
}
