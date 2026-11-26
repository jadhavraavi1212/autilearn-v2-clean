-- ============================================================
-- AUTILEARN DATABASE SCHEMA
-- Run this ENTIRE file in MySQL Workbench or MySQL CLI before starting the app
-- ============================================================

CREATE DATABASE IF NOT EXISTS autilearn_db;
USE autilearn_db;

-- ============================================================
-- TABLE 1: parents
-- ============================================================
CREATE TABLE IF NOT EXISTS parents (
    parent_id       INT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone           VARCHAR(15),
    password_hash   VARCHAR(255) NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active       TINYINT(1) DEFAULT 1
);

-- ============================================================
-- TABLE 2: children
-- ============================================================
CREATE TABLE IF NOT EXISTS children (
    child_id        INT AUTO_INCREMENT PRIMARY KEY,
    parent_id       INT NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    age             INT,
    avatar          VARCHAR(50) DEFAULT '👦',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_active       TINYINT(1) DEFAULT 1,
    FOREIGN KEY (parent_id) REFERENCES parents(parent_id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE 3: modules
-- ============================================================
CREATE TABLE IF NOT EXISTS modules (
    module_id       INT AUTO_INCREMENT PRIMARY KEY,
    module_name     VARCHAR(100) NOT NULL,
    description     TEXT,
    difficulty      ENUM('easy','medium','hard') DEFAULT 'easy',
    icon            VARCHAR(100),
    is_active       TINYINT(1) DEFAULT 1
);

-- ============================================================
-- TABLE 4: lessons
-- ============================================================
CREATE TABLE IF NOT EXISTS lessons (
    lesson_id       INT AUTO_INCREMENT PRIMARY KEY,
    module_id       INT NOT NULL,
    lesson_title    VARCHAR(150) NOT NULL,
    content_type    ENUM('visual','audio','game','quiz') DEFAULT 'visual',
    content_data    TEXT,
    lesson_order    INT DEFAULT 1,
    FOREIGN KEY (module_id) REFERENCES modules(module_id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE 5: child_progress
-- ============================================================
CREATE TABLE IF NOT EXISTS child_progress (
    progress_id     INT AUTO_INCREMENT PRIMARY KEY,
    child_id        INT NOT NULL,
    lesson_id       INT,
    module_id       INT NOT NULL,
    score           INT DEFAULT 0,
    completed       TINYINT(1) DEFAULT 0,
    attempts        INT DEFAULT 0,
    last_accessed   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (child_id) REFERENCES children(child_id) ON DELETE CASCADE,
    FOREIGN KEY (module_id) REFERENCES modules(module_id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE 6: subscriptions
-- ============================================================
CREATE TABLE IF NOT EXISTS subscriptions (
    subscription_id     INT AUTO_INCREMENT PRIMARY KEY,
    parent_id           INT NOT NULL,
    plan_name           VARCHAR(50) DEFAULT 'free',
    amount_paid         DECIMAL(10,2) DEFAULT 0.00,
    currency            VARCHAR(10) DEFAULT 'INR',
    payment_status      VARCHAR(20) DEFAULT 'pending',
    razorpay_order_id   VARCHAR(200),
    razorpay_payment_id VARCHAR(200),
    start_date          DATE,
    end_date            DATE,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES parents(parent_id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE 7: badges
-- ============================================================
CREATE TABLE IF NOT EXISTS badges (
    badge_id        INT AUTO_INCREMENT PRIMARY KEY,
    badge_name      VARCHAR(100) NOT NULL,
    badge_image     VARCHAR(100),
    description     VARCHAR(255)
);

-- ============================================================
-- TABLE 8: child_badges
-- ============================================================
CREATE TABLE IF NOT EXISTS child_badges (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    child_id        INT NOT NULL,
    badge_id        INT NOT NULL,
    earned_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (child_id) REFERENCES children(child_id) ON DELETE CASCADE,
    FOREIGN KEY (badge_id) REFERENCES badges(badge_id) ON DELETE CASCADE
);

-- ============================================================
-- SAMPLE DATA
-- ============================================================

INSERT IGNORE INTO modules (module_id, module_name, description, difficulty, icon) VALUES
(1, 'Alphabets', 'Learn A to Z with pictures and sounds', 'easy', '🔤'),
(2, 'Numbers',   'Count from 1 to 10 with fun visuals',   'easy', '🔢'),
(3, 'Colors',    'Learn colors through interactive games', 'easy', '🎨'),
(4, 'Shapes',    'Identify circles, squares, triangles',   'easy', '🔷'),
(5, 'Animals',   'Learn about animals with sounds',        'medium','🐶'),
(6, 'Emotions',  'Understand and express feelings',        'medium','😊');

INSERT IGNORE INTO lessons (module_id, lesson_title, content_type, lesson_order) VALUES
(1, 'Letter A - Apple',  'visual', 1),
(1, 'Letter B - Ball',   'visual', 2),
(1, 'Letter C - Cat',    'visual', 3),
(1, 'Alphabet Quiz',     'quiz',   4),
(2, 'Count 1 to 5',      'visual', 1),
(2, 'Count 6 to 10',     'visual', 2),
(2, 'Number Matching',   'game',   3),
(2, 'Numbers Quiz',      'quiz',   4),
(3, 'Primary Colors',    'visual', 1),
(3, 'Secondary Colors',  'visual', 2),
(3, 'Color Matching',    'game',   3);

INSERT IGNORE INTO badges (badge_name, badge_image, description) VALUES
('Star Learner',   '⭐', 'Completed first lesson'),
('ABC Champion',   '🔤', 'Mastered all alphabet lessons'),
('Number Wizard',  '🔢', 'Mastered all number lessons'),
('Color Expert',   '🎨', 'Mastered all color lessons'),
('Super Achiever', '🏆', 'Completed 3 or more modules');
