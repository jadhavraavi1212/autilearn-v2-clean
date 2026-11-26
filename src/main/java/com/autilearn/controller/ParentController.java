package com.autilearn.controller;

import com.autilearn.dao.ChildRepository;
import com.autilearn.dao.ParentRepository;
import com.autilearn.dao.ProgressRepository;
import com.autilearn.model.Child;
import com.autilearn.model.ChildProgress;
import com.autilearn.model.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/parent")
@CrossOrigin(origins = "*")
public class ParentController {

    @Autowired private ParentRepository parentRepo;
    @Autowired private ChildRepository childRepo;
    @Autowired private ProgressRepository progressRepo;
    @Autowired private BCryptPasswordEncoder encoder;

    private static final Map<Integer, String> MODULE_NAMES = Map.of(
        1, "Alphabets", 2, "Numbers", 3, "Colors",
        4, "Shapes",    5, "Animals", 6, "Emotions"
    );
    private static final int TOTAL_LESSONS_PER_MODULE = 4;

    // ── DASHBOARD ─────────────────────────────────────────────
    @GetMapping("/dashboard/{parentId}")
    public ResponseEntity<Map<String, Object>> getDashboard(@PathVariable Integer parentId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Optional<Parent> opt = parentRepo.findById(parentId);
            if (opt.isEmpty()) {
                res.put("success", false); res.put("message", "Parent not found.");
                return ResponseEntity.status(404).body(res);
            }

            List<Child> children = childRepo.findActiveChildrenByParentId(parentId);
            int totalLessons = 0, totalScore = 0, totalBadges = 0;
            List<Map<String, Object>> childrenData = new ArrayList<>();

            for (Child child : children) {
                int id = child.getChildId();
                int completedLessons = progressRepo.countByChildIdAndCompleted(id, 1);
                int score = progressRepo.sumScoreByChildId(id);
                int pct = (int) Math.min(
                    ((double) completedLessons / (MODULE_NAMES.size() * TOTAL_LESSONS_PER_MODULE)) * 100, 100
                );
                totalLessons += completedLessons;
                totalScore   += score;

                Set<Integer> completedMods = new HashSet<>();
                for (ChildProgress p : progressRepo.findByChildId(id)) {
                    if (p.getCompleted() != null && p.getCompleted() == 1)
                        completedMods.add(p.getModuleId());
                }
                totalBadges += completedMods.size();

                Map<String, Object> m = new HashMap<>();
                m.put("childId",         id);
                m.put("fullName",        child.getFullName());
                m.put("age",             child.getAge());
                m.put("avatar",          child.getAvatar());
                m.put("lessonsCompleted",completedLessons);
                m.put("totalScore",      score);
                m.put("progressPercent", pct);
                childrenData.add(m);
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalChildren", children.size());
            stats.put("totalLessons",  totalLessons);
            stats.put("totalScore",    totalScore);
            stats.put("totalBadges",   totalBadges);

            res.put("success",        true);
            res.put("parentName",     opt.get().getFullName());
            res.put("stats",          stats);
            res.put("children",       childrenData);
            res.put("recentActivity", buildRecentActivity(parentId));
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false); res.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    private List<Map<String, Object>> buildRecentActivity(Integer parentId) {
        List<Map<String, Object>> activity = new ArrayList<>();
        List<Child> children = childRepo.findActiveChildrenByParentId(parentId);
        if (children.isEmpty()) return activity;

        Map<Integer, Child> childMap = new HashMap<>();
        List<Integer> ids = new ArrayList<>();
        for (Child c : children) { childMap.put(c.getChildId(), c); ids.add(c.getChildId()); }

        List<ChildProgress> all = progressRepo.findByChildIdInOrderByLastAccessedDesc(ids);
        int limit = Math.min(all.size(), 10);
        for (int i = 0; i < limit; i++) {
            ChildProgress p = all.get(i);
            Child c = childMap.get(p.getChildId());
            if (c == null) continue;
            Map<String, Object> e = new HashMap<>();
            e.put("childName",   c.getFullName());
            e.put("avatar",      c.getAvatar());
            e.put("moduleName",  MODULE_NAMES.getOrDefault(p.getModuleId(), "Module " + p.getModuleId()));
            e.put("score",       p.getScore());
            e.put("lastAccessed",p.getLastAccessed());
            activity.add(e);
        }
        return activity;
    }

    // ── ADD CHILD ─────────────────────────────────────────────
    @PostMapping("/add-child")
    public ResponseEntity<Map<String, Object>> addChild(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Integer parentId = toInt(body.get("parentId"));
            String fullName  = (String) body.get("fullName");
            String username  = ((String) body.get("username")).trim();
            String password  = (String) body.get("password");
            Integer age      = toInt(body.getOrDefault("age", 7));
            String avatar    = (String) body.getOrDefault("avatar", "👦");

            if (parentId == null || fullName == null || username == null || password == null) {
                res.put("success", false); res.put("message", "Missing required fields.");
                return ResponseEntity.badRequest().body(res);
            }
            if (childRepo.existsByUsername(username)) {
                res.put("success", false); res.put("message", "Username already taken. Choose another.");
                return ResponseEntity.badRequest().body(res);
            }

            Optional<Parent> opt = parentRepo.findById(parentId);
            if (opt.isEmpty()) {
                res.put("success", false); res.put("message", "Parent not found.");
                return ResponseEntity.status(404).body(res);
            }

            Child child = new Child();
            child.setParent(opt.get());
            child.setFullName(fullName);
            child.setUsername(username);
            child.setPasswordHash(encoder.encode(password));
            child.setAge(age);
            child.setAvatar(avatar);
            child.setIsActive(1);
            Child saved = childRepo.save(child);

            res.put("success", true);
            res.put("childId", saved.getChildId());
            res.put("message", "Child account created successfully!");
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            e.printStackTrace();
            res.put("success", false); res.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ── LIST CHILDREN ─────────────────────────────────────────
    @GetMapping("/children/{parentId}")
    public ResponseEntity<Map<String, Object>> getChildren(@PathVariable Integer parentId) {
        Map<String, Object> res = new HashMap<>();
        List<Child> children = childRepo.findActiveChildrenByParentId(parentId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Child c : children) {
            Map<String, Object> m = new HashMap<>();
            m.put("childId",  c.getChildId());
            m.put("fullName", c.getFullName());
            m.put("username", c.getUsername());
            m.put("age",      c.getAge());
            m.put("avatar",   c.getAvatar());
            int completed = progressRepo.countByChildIdAndCompleted(c.getChildId(), 1);
            int score     = progressRepo.sumScoreByChildId(c.getChildId());
            m.put("lessonsCompleted", completed);
            m.put("totalScore",       score);
            list.add(m);
        }
        res.put("success",  true);
        res.put("children", list);
        return ResponseEntity.ok(res);
    }

    // ── CHILD PROGRESS ────────────────────────────────────────
    @GetMapping("/progress/{childId}")
    public ResponseEntity<Map<String, Object>> getChildProgress(@PathVariable Integer childId) {
        Map<String, Object> res = new HashMap<>();
        Optional<Child> opt = childRepo.findById(childId);
        if (opt.isEmpty()) {
            res.put("success", false); res.put("message", "Child not found.");
            return ResponseEntity.status(404).body(res);
        }
        Child child = opt.get();
        List<ChildProgress> list = progressRepo.findByChildId(childId);
        List<Map<String, Object>> data = new ArrayList<>();
        for (ChildProgress p : list) {
            Map<String, Object> e = new HashMap<>();
            e.put("moduleId",    p.getModuleId());
            e.put("moduleName",  MODULE_NAMES.getOrDefault(p.getModuleId(), "Module"));
            e.put("score",       p.getScore());
            e.put("completed",   p.getCompleted());
            e.put("attempts",    p.getAttempts());
            e.put("lastAccessed",p.getLastAccessed());
            data.add(e);
        }
        res.put("success",          true);
        res.put("childName",        child.getFullName());
        res.put("avatar",           child.getAvatar());
        res.put("age",              child.getAge());
        res.put("totalScore",       progressRepo.sumScoreByChildId(childId));
        res.put("completedModules", progressRepo.countByChildIdAndCompleted(childId, 1));
        res.put("progress",         data);
        return ResponseEntity.ok(res);
    }

    // ── DELETE CHILD ──────────────────────────────────────────
    @DeleteMapping("/delete-child/{childId}")
    public ResponseEntity<Map<String, Object>> deleteChild(@PathVariable Integer childId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Optional<Child> opt = childRepo.findById(childId);
            if (opt.isEmpty()) {
                res.put("success", false); res.put("message", "Child not found.");
                return ResponseEntity.status(404).body(res);
            }
            Child child = opt.get();
            child.setIsActive(0);
            childRepo.save(child);
            res.put("success", true); res.put("message", "Child removed successfully.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false); res.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
