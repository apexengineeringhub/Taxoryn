package com.taxoryn.module.organization.specification;

import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.user.entity.UserEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Type-safe dynamic specification builder for practice organization governance queries.
 * Avoids raw JPQL nullable parameter expressions that cause PostgreSQL parameter typing issues.
 */
public final class PracticeSpecification {

    private PracticeSpecification() {
    }

    public static Specification<OrganizationEntity> withFilters(
            String search,
            OrganizationEntity.OrganizationStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Status Filter (Only applied if non-null)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 2. Dynamic Search Filter (Only applied if non-empty string)
            if (StringUtils.hasText(search)) {
                String cleanSearch = search.trim();
                String term = "%" + cleanSearch.toLowerCase() + "%";

                List<Predicate> searchPredicates = new ArrayList<>();

                // Practice Organization Fields
                searchPredicates.add(cb.like(cb.lower(root.get("name")), term));
                searchPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("legalName"), "")), term));
                searchPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("tradeName"), "")), term));
                searchPredicates.add(cb.like(cb.lower(root.get("email")), term));
                searchPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), term));
                searchPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("city"), "")), term));
                searchPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("state"), "")), term));
                searchPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("pan"), "")), term));
                searchPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("gstin"), "")), term));

                // Subquery to match Practice Admins or Practice Users within the organization
                Subquery<Integer> userSubquery = query.subquery(Integer.class);
                Root<UserEntity> userRoot = userSubquery.from(UserEntity.class);
                userSubquery.select(cb.literal(1));

                Predicate orgMatch = cb.equal(userRoot.get("organizationId"), root.get("id"));
                Predicate userFirstMatch = cb.like(cb.lower(cb.coalesce(userRoot.get("firstName"), "")), term);
                Predicate userLastMatch = cb.like(cb.lower(cb.coalesce(userRoot.get("lastName"), "")), term);
                Predicate userEmailMatch = cb.like(cb.lower(userRoot.get("email")), term);
                Predicate userPhoneMatch = cb.like(cb.lower(cb.coalesce(userRoot.get("phone"), "")), term);

                userSubquery.where(cb.and(
                        orgMatch,
                        cb.or(userFirstMatch, userLastMatch, userEmailMatch, userPhoneMatch)
                ));

                searchPredicates.add(cb.exists(userSubquery));

                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
