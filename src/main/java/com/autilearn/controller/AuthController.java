package com.autilearn.controller;

import com.autilearn.dao.ChildRepository;
import com.autilearn.dao.ParentRepository;
import com.autilearn.model.Child;
import com.autilearn.model.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private ParentRepository parentRepo;
    @Autowired private ChildRepository  childRepo;
    @Autowired private BCryptPasswordEncoder encoder;

    // ── PARENT LOGIN ──────────────────────────────────────────
    @PostMapping("/parent/login")
    public ResponseEntity<Map<String,Object>> parentLogin(@RequestBody Map<String,String> body) {
        Map<String,Object> res = new HashMap<>();
        String email = body.get("email"), password = body.get("password");
        if (email == null || password == null) {
            res.put("success",false); res.put("message","Email and password are required.");
            return ResponseEntity.badRequest().body(res);
        }
        Optional<Parent> opt = parentRepo.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) {
            res.put("success",false); res.put("message","No account found with this email.");
            return ResponseEntity.status(401).body(res);
        }
        Parent p = opt.get();
        if (p.getIsActive() == null || p.getIsActive() != 1) {
            res.put("success",false); res.put("message","Account is inactive. Contact support.");
            return ResponseEntity.status(403).body(res);
        }
        if (!encoder.matches(password, p.getPasswordHash())) {
            res.put("success",false); res.put("message","Incorrect password. Please try again.");
            return ResponseEntity.status(401).body(res);
        }
        res.put("success",true); res.put("parentId",p.getParentId());
        res.put("fullName",p.getFullName()); res.put("email",p.getEmail());
        res.put("message","Login successful!");
        return ResponseEntity.ok(res);
    }

    // ── CHILD LOGIN ───────────────────────────────────────────
    @PostMapping("/child/login")
    public ResponseEntity<Map<String,Object>> childLogin(@RequestBody Map<String,String> body) {
        Map<String,Object> res = new HashMap<>();
        String username = body.get("username"), password = body.get("password");
        if (username == null || password == null) {
            res.put("success",false); res.put("message","Username and password are required.");
            return ResponseEntity.badRequest().body(res);
        }
        Optional<Child> opt = childRepo.findByUsername(username.trim());
        if (opt.isEmpty()) {
            res.put("success",false); res.put("message","Username not found. Please check and try again.");
            return ResponseEntity.status(401).body(res);
        }
        Child c = opt.get();
        if (!encoder.matches(password, c.getPasswordHash())) {
            res.put("success",false); res.put("message","Wrong password. Try again!");
            return ResponseEntity.status(401).body(res);
        }
        res.put("success",true); res.put("childId",c.getChildId());
        res.put("fullName",c.getFullName()); res.put("displayName",c.getFullName());
        res.put("username",c.getUsername()); res.put("avatar",c.getAvatar());
        res.put("age",c.getAge()); res.put("points",0);
        res.put("message","Welcome back!");
        return ResponseEntity.ok(res);
    }

    // ── REGISTER (Parent + Child together, or child-only if password blank) ──
    @PostMapping("/register")
    public ResponseEntity<Map<String,Object>> register(@RequestBody Map<String,Object> body) {
        Map<String,Object> res = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String,Object> parentData = (Map<String,Object>) body.get("parent");
            @SuppressWarnings("unchecked")
            Map<String,Object> childData  = (Map<String,Object>) body.get("child");

            if (parentData == null || childData == null) {
                res.put("success",false); res.put("message","Parent and child data are required.");
                return ResponseEntity.badRequest().body(res);
            }

            String email    = ((String) parentData.get("email")).trim().toLowerCase();
            String password = (String) parentData.getOrDefault("password","");
            String username = ((String) childData.get("username")).trim();

            if (childRepo.existsByUsername(username)) {
                res.put("success",false); res.put("message","Username already taken. Choose another.");
                return ResponseEntity.badRequest().body(res);
            }

            Parent savedParent;
            if (password.isEmpty()) {
                // Child-only: find existing parent
                Optional<Parent> pOpt = parentRepo.findByEmail(email);
                if (pOpt.isEmpty()) {
                    res.put("success",false);
                    res.put("message","No parent account found with email: "+email+". Register the parent first.");
                    return ResponseEntity.badRequest().body(res);
                }
                savedParent = pOpt.get();
            } else {
                if (password.length() < 6) {
                    res.put("success",false); res.put("message","Password must be at least 6 characters.");
                    return ResponseEntity.badRequest().body(res);
                }
                if (parentRepo.existsByEmail(email)) {
                    res.put("success",false); res.put("message","An account with this email already exists.");
                    return ResponseEntity.badRequest().body(res);
                }
                Parent parent = new Parent();
                parent.setFullName((String) parentData.getOrDefault("fullName", email.split("@")[0]));
                parent.setEmail(email);
                parent.setPhone((String) parentData.getOrDefault("phone",""));
                parent.setPasswordHash(encoder.encode(password));
                parent.setIsActive(1);
                savedParent = parentRepo.save(parent);
            }

            Child child = new Child();
            child.setParent(savedParent);
            child.setFullName((String) childData.getOrDefault("fullName", username));
            child.setUsername(username);
            child.setPasswordHash(encoder.encode((String) childData.get("password")));
            child.setAge(childData.get("age") instanceof Integer
                ? (Integer) childData.get("age")
                : Integer.parseInt(childData.getOrDefault("age","7").toString()));
            child.setAvatar((String) childData.getOrDefault("avatar","🦁"));
            child.setIsActive(1);
            childRepo.save(child);

            res.put("success",true); res.put("parentId",savedParent.getParentId());
            res.put("message","Account created successfully! Please login.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace(); res.put("success",false);
            res.put("message","Registration failed: "+e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ── PARENT-ONLY REGISTER  POST /api/auth/parent/register ──
    @PostMapping("/parent/register")
    public ResponseEntity<Map<String,Object>> parentRegister(@RequestBody Map<String,String> body) {
        Map<String,Object> res = new HashMap<>();
        try {
            String email = body.get("email").trim().toLowerCase();
            String password = body.getOrDefault("password","");
            String fullName = body.getOrDefault("fullName", email.split("@")[0]);
            if (password.length() < 6) {
                res.put("success",false); res.put("message","Password must be at least 6 characters.");
                return ResponseEntity.badRequest().body(res);
            }
            if (parentRepo.existsByEmail(email)) {
                res.put("success",false); res.put("message","Email already registered.");
                return ResponseEntity.badRequest().body(res);
            }
            Parent parent = new Parent();
            parent.setFullName(fullName); parent.setEmail(email);
            parent.setPhone(body.getOrDefault("phone",""));
            parent.setPasswordHash(encoder.encode(password)); parent.setIsActive(1);
            Parent saved = parentRepo.save(parent);
            res.put("success",true); res.put("parentId",saved.getParentId());
            res.put("message","Parent account created! You can now add a child.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace(); res.put("success",false);
            res.put("message","Error: "+e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ── CHILD-ONLY REGISTER  POST /api/auth/child/register ──
    @PostMapping("/child/register")
    public ResponseEntity<Map<String,Object>> childRegister(@RequestBody Map<String,Object> body) {
        Map<String,Object> res = new HashMap<>();
        try {
            String parentEmail = ((String) body.get("parentEmail")).trim().toLowerCase();
            String displayName = (String) body.getOrDefault("displayName","");
            String username    = ((String) body.get("username")).trim();
            String password    = (String) body.get("password");
            String avatarId    = (String) body.getOrDefault("avatarId","1");
            int age = body.get("age") instanceof Integer
                      ? (Integer) body.get("age")
                      : Integer.parseInt(body.getOrDefault("age","7").toString());

            String[] AVATARS = {"🦁","🐯","🐸","🐼","🦊","🐨"};
            int avIdx = 0;
            try { avIdx = Integer.parseInt(avatarId) - 1; } catch (Exception ignored) {}
            String avatar = (avIdx >= 0 && avIdx < AVATARS.length) ? AVATARS[avIdx] : "🦁";

            Optional<Parent> pOpt = parentRepo.findByEmail(parentEmail);
            if (pOpt.isEmpty()) {
                res.put("success",false);
                res.put("message","No parent account found with email: "+parentEmail);
                return ResponseEntity.badRequest().body(res);
            }
            if (childRepo.existsByUsername(username)) {
                res.put("success",false); res.put("message","Username already taken. Choose another.");
                return ResponseEntity.badRequest().body(res);
            }
            Child child = new Child();
            child.setParent(pOpt.get());
            child.setFullName(displayName.isEmpty() ? username : displayName);
            child.setUsername(username);
            child.setPasswordHash(encoder.encode(password));
            child.setAge(age); child.setAvatar(avatar); child.setIsActive(1);
            Child saved = childRepo.save(child);
            res.put("success",true); res.put("childId",saved.getChildId());
            res.put("message","Child account created! Ready to learn!");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace(); res.put("success",false);
            res.put("message","Error: "+e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }
}
