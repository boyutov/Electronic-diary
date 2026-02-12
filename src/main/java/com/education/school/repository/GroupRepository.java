package com.education.school.repository;

import com.education.school.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<GroupEntity, Integer> {
    Optional<GroupEntity> findByCuratorId(Long curatorId);
}
