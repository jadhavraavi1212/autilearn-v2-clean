package com.autilearn.dao;

import com.autilearn.model.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Integer> {
    Optional<Parent> findByEmail(String email);
    Optional<Parent> findByEmailAndIsActive(String email, Integer isActive);
    boolean existsByEmail(String email);
}
