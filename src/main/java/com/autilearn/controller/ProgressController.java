package com.autilearn.controller;

import com.autilearn.dao.ProgressRepository;
import com.autilearn.model.ChildProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/progress")
@CrossOrigin(origins = "*")
public class ProgressController {

    @Autowired private ProgressRepository progressRepo;

    // ── SAVE PROGRESS ─────────────────────────────────────────
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveProgress(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Integer childId   = toInt(body.get("childId"));
            Integer moduleId  = toInt(body.get("moduleId"));
            Integer lessonId  = body.containsKey("lessonId") ? toInt(body.get("lessonId")) : null;
            Integer score     = toInt(body.getOrDefault("score", 0));
            Integer completed = toInt(body.getOrDefault("completed", 0));

            if (childId == null || moduleId == null) {
                res.put("success", false); res.put("message", "childId and moduleId are required.");
                return ResponseEntity.badRequest().body(res);
            }

            List<ChildProgress> existing = progressRepo.findByChildIdAndModuleId(childId, moduleId);
            ChildProgress progress;
            if (!existing.isEmpty()) {
                progress = existing.get(0);
                if (score > progress.getScore()) progress.setScore(score);
                progress.setAttempts(progress.getAttempts() + 1);
                if (completed == 1) progress.setCompleted(1);
            } else {
                progress = new ChildProgress();
                progress.setChildId(childId);
                progress.setModuleId(moduleId);
                progress.setLessonId(lessonId);
                progress.setScore(score);
                progress.setCompleted(completed);
                progress.setAttempts(1);
            }
            progressRepo.save(progress);

            res.put("success", true);
            res.put("message", "Progress saved!");
            res.put("score",   progress.getScore());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false); res.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ── GET PROGRESS ──────────────────────────────────────────
    @GetMapping("/{childId}")
    public ResponseEntity<Map<String, Object>> getProgress(@PathVariable Integer childId) {
        Map<String, Object> res = new HashMap<>();
        List<ChildProgress> list = progressRepo.findByChildId(childId);
        int totalScore = progressRepo.sumScoreByChildId(childId);

        List<Map<String, Object>> data = new ArrayList<>();
        for (ChildProgress p : list) {
            Map<String, Object> e = new HashMap<>();
            e.put("moduleId",  p.getModuleId());
            e.put("score",     p.getScore());
            e.put("completed", p.getCompleted());
            e.put("attempts",  p.getAttempts());
            data.add(e);
        }
        res.put("success",    true);
        res.put("progress",   data);
        res.put("totalScore", totalScore);
        return ResponseEntity.ok(res);
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
