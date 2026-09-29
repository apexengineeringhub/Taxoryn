package com.taxoryn.module.user.specification;

import com.taxoryn.module.role.entity.RoleEntity;
import com.taxoryn.module.user.entity.UserEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Type-safe dynamic specification builder for practice-scoped user queries.
 * Enforces tenant isolation on organizationId and prevents parameter typing issues.
 */
public final class PracticeUserSpecification {

    private PracticeUserSpecification() {
    }

    public static Specification<UserEntity> withFilters(
            UUID organizationId,
            String search,
            String role,
            UserEntity.UserStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Organization Scope
            predicates.add(cb.equal(root.get("organizationId"), organizationId));

            // 2. Status Filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Role Filter (Uses subquery to avoid entity multiplication in paged queries)
            if (StringUtils.hasText(role) && !"ALL".equalsIgnoreCase(role)) {
                String cleanRole = role.trim().toUpperCase();
                Subquery<Integer> roleSubquery = query.subquery(Integer.class);
                Root<UserEntity> subUserRoot = roleSubquery.from(UserEntity.class);
                Join<UserEntity, RoleEntity> roleJoin = subUserRoot.join("roles");
                roleSubquery.select(cb.literal(1));
                roleSubquery.where(
                        cb.equal(subUserRoot.get("id"), root.get("id")),
                        cb.equal(cb.upper(roleJoin.get("code")), cleanRole)
                );
                predicates.add(cb.exists(roleSubquery));
            }

            // 4. Search Filter
            if (StringUtils.hasText(search)) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate firstMatch = cb.like(cb.lower(cb.coalesce(root.get("firstName"), "")), term);
                Predicate lastMatch = cb.like(cb.lower(cb.coalesce(root.get("lastName"), "")), term);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), term);
                Predicate phoneMatch = cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), term);

                predicates.add(cb.or(firstMatch, lastMatch, emailMatch, phoneMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
