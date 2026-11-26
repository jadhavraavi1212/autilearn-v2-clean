package com.autilearn.dao;

import com.autilearn.model.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChildRepository extends JpaRepository<Child, Integer> {
    Optional<Child> findByUsername(String username);
    List<Child> findByParent_ParentId(Integer parentId);
    int countByParent_ParentId(Integer parentId);
    boolean existsByUsername(String username);

    @Query("SELECT c FROM Child c WHERE c.parent.parentId = :parentId AND c.isActive = 1")
    List<Child> findActiveChildrenByParentId(@Param("parentId") Integer parentId);
}
