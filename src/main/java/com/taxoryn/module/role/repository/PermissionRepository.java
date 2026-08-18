package com.taxoryn.module.role.repository;

import com.taxoryn.module.role.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    Optional<PermissionEntity> findByCode(String code);

    List<PermissionEntity> findByCodeIn(Set<String> codes);

    List<PermissionEntity> findByModule(String module);
}
