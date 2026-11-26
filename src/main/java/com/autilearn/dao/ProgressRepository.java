package com.autilearn.dao;

import com.autilearn.model.ChildProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProgressRepository extends JpaRepository<ChildProgress, Integer> {
    List<ChildProgress> findByChildId(Integer childId);
    int countByChildIdAndCompleted(Integer childId, Integer completed);

    @Query("SELECT COALESCE(SUM(p.score), 0) FROM ChildProgress p WHERE p.childId = :childId")
    Integer sumScoreByChildId(@Param("childId") Integer childId);

    List<ChildProgress> findByChildIdAndModuleId(Integer childId, Integer moduleId);

    @Query("SELECT p FROM ChildProgress p WHERE p.childId IN :childIds ORDER BY p.lastAccessed DESC")
    List<ChildProgress> findByChildIdInOrderByLastAccessedDesc(@Param("childIds") List<Integer> childIds);
}
