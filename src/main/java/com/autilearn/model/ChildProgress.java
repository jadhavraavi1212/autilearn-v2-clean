package com.autilearn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "child_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Integer progressId;

    @Column(name = "child_id", nullable = false)
    private Integer childId;

    @Column(name = "lesson_id")
    private Integer lessonId;

    @Column(name = "module_id", nullable = false)
    private Integer moduleId;

    @Column(name = "score")
    private Integer score = 0;

    @Column(name = "completed")
    private Integer completed = 0;

    @Column(name = "attempts")
    private Integer attempts = 0;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.lastAccessed = LocalDateTime.now();
        if (this.score == null) this.score = 0;
        if (this.completed == null) this.completed = 0;
        if (this.attempts == null) this.attempts = 0;
    }
}
