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
import java.util.Set;

/**
 * Type-safe dynamic specification builder for platform-wide user governance queries.
 * Replaces in-memory stream filtering with database-side filtering.
 */
public final class PlatformUserSpecification {

    private PlatformUserSpecification() {
    }

    public static Specification<UserEntity> withFilters(
            String role,
            String status,
            String search
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Status Filter
            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                try {
                    UserEntity.UserStatus userStatus = UserEntity.UserStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), userStatus));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // 2. Role Filter (Individual code or role group)
            if (StringUtils.hasText(role) && !"ALL".equalsIgnoreCase(role)) {
                String cleanRole = role.trim().toUpperCase();
                Set<String> matchingRoleCodes = resolveRoleCodes(cleanRole);

                Subquery<Integer> roleSubquery = query.subquery(Integer.class);
                Root<UserEntity> subUserRoot = roleSubquery.from(UserEntity.class);
                Join<UserEntity, RoleEntity> roleJoin = subUserRoot.join("roles");
                roleSubquery.select(cb.literal(1));

                Predicate matchUser = cb.equal(subUserRoot.get("id"), root.get("id"));
                Predicate rolePredicate;
                if (matchingRoleCodes.size() == 1) {
                    rolePredicate = cb.equal(cb.upper(roleJoin.get("code")), matchingRoleCodes.iterator().next());
                } else {
                    rolePredicate = cb.upper(roleJoin.get("code")).in(matchingRoleCodes);
                }

                roleSubquery.where(cb.and(matchUser, rolePredicate));
                predicates.add(cb.exists(roleSubquery));
            }

            // 3. Search Filter
            if (StringUtils.hasText(search)) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate firstMatch = cb.like(cb.lower(cb.coalesce(root.get("firstName"), "")), term);
                Predicate lastMatch = cb.like(cb.lower(cb.coalesce(root.get("lastName"), "")), term);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), term);
                Predicate phoneMatch = cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), term);

                predicates.add(cb.or(firstMatch, lastMatch, emailMatch, phoneMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Set<String> resolveRoleCodes(String role) {
        return switch (role) {
            case "SUPERADMIN" -> Set.of("SUPER_ADMIN", "TAXORYN_SUPERADMIN");
            case "OPERATIONS" -> Set.of("TAXORYN_OPERATIONS_ADMIN");
            case "SUPPORT" -> Set.of("TAXORYN_SUPPORT_ADMIN");
            case "MARKETPLACE" -> Set.of("TAXORYN_MARKETPLACE_ADMIN");
            case "FINANCE" -> Set.of("TAXORYN_FINANCE_ADMIN");
            case "CONTENT" -> Set.of("TAXORYN_CONTENT_ADMIN");
            case "SECURITY" -> Set.of("TAXORYN_SECURITY_ADMIN");
            case "ENGINEERING" -> Set.of("TAXORYN_ENGINEERING_ADMIN");
            case "PRACTITIONERS" -> Set.of("ORG_ADMIN", "PRACTICE_ADMIN", "PRACTICE_OWNER", "PRACTITIONER");
            case "STAFF" -> Set.of("STAFF", "ARTICLE_ASSISTANT", "PRACTICE_EMPLOYEE");
            case "CUSTOMERS" -> Set.of("CLIENT_USER", "CLIENT_ADMIN", "MARKETPLACE_CUSTOMER");
            default -> Set.of(role);
        };
    }
}
