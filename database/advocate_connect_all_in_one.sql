-- ============================================================================
-- ADVOCATE CONNECT: COMPLETE SINGLE-FILE DATABASE INSTALLER
-- Target: MySQL 8.0+
--
-- IMPORT THIS FILE ONLY for a fresh/demo installation.
-- It creates the database, all tables, indexes, foreign keys, and demo records.
--
-- IMPORTANT: This installer DROPS the four application tables before creating
-- them. Back up any existing advocate_portal database before importing.
--
-- Demo accounts:
--   Admin:    admin@advocateconnect.local / Admin@123
--   Client:   client@advocateconnect.local / Demo@123
--   Advocates: aarav@, priya@, rohan@, sneha@, karan@, nisha@
--              (append @advocateconnect.local; password Demo@123)
--
-- Application schema is aligned with Portal.java, Auth.java, AIHelper.java,
-- advocate/client/admin JSPs, and advocate profile photo upload.
-- ============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Advocate Connect database (MySQL 8+)
-- NOTE: passwords are stored as plain, readable text in users.password (academic/demo project).
CREATE DATABASE IF NOT EXISTS advocate_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE advocate_portal;

SET FOREIGN_KEY_CHECKS=0;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS advocate_profiles;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS=1;

CREATE TABLE users(
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    phone VARCHAR(25),
    password VARCHAR(100) NOT NULL,
    role ENUM('ADMIN','CLIENT','ADVOCATE') NOT NULL DEFAULT 'CLIENT',
    status ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE advocate_profiles(
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    specialisation VARCHAR(255) DEFAULT 'General Practice',
    experience_years INT DEFAULT 0,
    city VARCHAR(80),
    languages VARCHAR(120),
    fee INT DEFAULT 0,
    bio TEXT,
    profile_photo VARCHAR(255) NULL,
    -- KYC details submitted at registration and reviewed by an admin
    bar_council_number VARCHAR(40) NOT NULL DEFAULT '',
    bar_council_state VARCHAR(120) NOT NULL DEFAULT '',
    enrolment_year SMALLINT NULL,
    -- clients only see advocates whose verification_status is APPROVED
    verification_status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    verification_note VARCHAR(300),
    verified_at TIMESTAMP NULL,
    verified_by INT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(verified_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_verification(verification_status),
    INDEX idx_bar(bar_council_state, bar_council_number)
) ENGINE=InnoDB;

CREATE TABLE appointments(
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    advocate_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    topic VARCHAR(160) NOT NULL,
    details TEXT,
    status ENUM('PENDING','APPROVED','REJECTED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    advocate_note VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(client_id) REFERENCES users(id),
    FOREIGN KEY(advocate_id) REFERENCES users(id),
    INDEX idx_client(client_id),
    INDEX idx_advocate(advocate_id),
    INDEX idx_date(appointment_date)
) ENGINE=InnoDB;

CREATE TABLE reviews(
    id INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id INT NOT NULL UNIQUE,
    client_id INT NOT NULL,
    advocate_id INT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    FOREIGN KEY(client_id) REFERENCES users(id),
    FOREIGN KEY(advocate_id) REFERENCES users(id),
    INDEX idx_review_advocate(advocate_id)
) ENGINE=InnoDB;

-- Create accounts through the Register page, or run demo_data.sql for sample accounts.
-- To promote your first registered account to administrator, run:
-- UPDATE users SET role='ADMIN' WHERE email='your-admin-email@example.com';


-- ============================================================================
-- DEMO DATA (included in this one-file installer)
-- ============================================================================
-- Optional demo data for Advocate Connect. Run AFTER advocate_portal.sql.
-- Admin: admin@advocateconnect.local / Admin@123 | Advocates and client: password Demo@123
-- Passwords are stored as plain readable text in users.password.
USE advocate_portal;

INSERT INTO users (name, email, phone, password, role)
VALUES (
    'Portal Admin',
    'admin@advocateconnect.local',
    '9000000000',
    'Admin@123',
    'ADMIN'
);

INSERT INTO users (name, email, phone, password, role)
VALUES (
    'Aarav Mehta',
    'aarav@advocateconnect.local',
    '9000000001',
    'Demo@123',
    'ADVOCATE'
);

INSERT INTO advocate_profiles (user_id, specialisation, experience_years, city, languages, fee, bio)
VALUES (
    (SELECT id FROM users WHERE email='aarav@advocateconnect.local'),
    'Property Law, Civil Law',
    12,
    'Mumbai',
    'English, Hindi, Marathi',
    1500,
    'Property title disputes, tenant and landlord matters, partition and land records.'
);

INSERT INTO users (name, email, phone, password, role)
VALUES (
    'Priya Nair',
    'priya@advocateconnect.local',
    '9000000002',
    'Demo@123',
    'ADVOCATE'
);

INSERT INTO advocate_profiles (user_id, specialisation, experience_years, city, languages, fee, bio)
VALUES (
    (SELECT id FROM users WHERE email='priya@advocateconnect.local'),
    'Family Law',
    9,
    'Bengaluru',
    'English, Malayalam, Kannada',
    1200,
    'Divorce, child custody, maintenance and domestic-violence matters. Mediation friendly.'
);

INSERT INTO users (name, email, phone, password, role)
VALUES (
    'Rohan Kapoor',
    'rohan@advocateconnect.local',
    '9000000003',
    'Demo@123',
    'ADVOCATE'
);

INSERT INTO advocate_profiles (user_id, specialisation, experience_years, city, languages, fee, bio)
VALUES (
    (SELECT id FROM users WHERE email='rohan@advocateconnect.local'),
    'Criminal Law',
    15,
    'Delhi',
    'English, Hindi, Punjabi',
    2000,
    'Bail, anticipatory bail, FIR quashing and trial defence.'
);

INSERT INTO users (name, email, phone, password, role)
VALUES (
    'Sneha Iyer',
    'sneha@advocateconnect.local',
    '9000000004',
    'Demo@123',
    'ADVOCATE'
);

INSERT INTO advocate_profiles (user_id, specialisation, experience_years, city, languages, fee, bio)
VALUES (
    (SELECT id FROM users WHERE email='sneha@advocateconnect.local'),
    'Banking & Cheque Bounce, Civil Law',
    7,
    'Chennai',
    'English, Tamil',
    1000,
    'Cheque bounce (Section 138), loan recovery and money suits.'
);

INSERT INTO users (name, email, phone, password, role)
VALUES (
    'Karan Malhotra',
    'karan@advocateconnect.local',
    '9000000005',
    'Demo@123',
    'ADVOCATE'
);

INSERT INTO advocate_profiles (user_id, specialisation, experience_years, city, languages, fee, bio)
VALUES (
    (SELECT id FROM users WHERE email='karan@advocateconnect.local'),
    'Labour & Employment Law, Corporate & Business Law',
    10,
    'Pune',
    'English, Hindi',
    1300,
    'Wrongful termination, unpaid wages, startup and contract advice.'
);

INSERT INTO users (name, email, phone, password, role)
VALUES (
    'Nisha Verma',
    'nisha@advocateconnect.local',
    '9000000006',
    'Demo@123',
    'ADVOCATE'
);

INSERT INTO advocate_profiles (user_id, specialisation, experience_years, city, languages, fee, bio)
VALUES (
    (SELECT id FROM users WHERE email='nisha@advocateconnect.local'),
    'Cyber Law, Consumer Protection',
    6,
    'Hyderabad',
    'English, Hindi, Telugu',
    900,
    'Online fraud, data breaches, e-commerce and consumer complaints.'
);

INSERT INTO users (name, email, phone, password, role)
VALUES (
    'Demo Client',
    'client@advocateconnect.local',
    '9000000007',
    'Demo@123',
    'CLIENT'
);

-- sample appointments for the demo client
INSERT INTO appointments (client_id, advocate_id, appointment_date, appointment_time, topic, details, status, advocate_note)
VALUES (
    (SELECT id FROM users WHERE email='client@advocateconnect.local'),
    (SELECT id FROM users WHERE email='aarav@advocateconnect.local'),
    DATE_SUB(CURDATE(),INTERVAL 10 DAY),
    '11:00:00',
    'Property dispute',
    'Boundary issue with neighbour over my plot.',
    'COMPLETED',
    'Please bring the sale deed and survey map.'
);

INSERT INTO reviews (appointment_id, client_id, advocate_id, rating, comment)
VALUES (
    LAST_INSERT_ID(),
    (SELECT id FROM users WHERE email='client@advocateconnect.local'),
    (SELECT id FROM users WHERE email='aarav@advocateconnect.local'),
    5,
    'Clear advice and very patient with all my questions.'
);

INSERT INTO appointments (client_id, advocate_id, appointment_date, appointment_time, topic, details, status, advocate_note)
VALUES (
    (SELECT id FROM users WHERE email='client@advocateconnect.local'),
    (SELECT id FROM users WHERE email='priya@advocateconnect.local'),
    DATE_ADD(CURDATE(),INTERVAL 3 DAY),
    '15:30:00',
    'Divorce consultation',
    'Want to understand mutual consent process and custody.',
    'APPROVED',
    'Looking forward to meeting you. Bring ID proof.'
);

INSERT INTO appointments (client_id, advocate_id, appointment_date, appointment_time, topic, details, status)
VALUES (
    (SELECT id FROM users WHERE email='client@advocateconnect.local'),
    (SELECT id FROM users WHERE email='sneha@advocateconnect.local'),
    DATE_ADD(CURDATE(),INTERVAL 5 DAY),
    '10:00:00',
    'Cheque bounce',
    'Received a dishonoured cheque and need to send a legal notice.',
    'PENDING'
);

-- Bar Council KYC details. All demo advocates are pre-approved so they appear to clients.
-- To test the admin approval flow, register a new advocate at /register.jsp and approve them as admin.
SET SQL_SAFE_UPDATES = 0;

UPDATE advocate_profiles p
JOIN users u ON u.id = p.user_id
SET p.verification_status = 'APPROVED',
    p.verified_at = NOW(),
    p.verified_by = (SELECT id FROM (SELECT id FROM users WHERE email='admin@advocateconnect.local') a),
    p.verification_note = 'Verified against Bar Council records (demo data).',
    p.bar_council_state = CASE u.email
        WHEN 'aarav@advocateconnect.local' THEN 'Maharashtra & Goa'
        WHEN 'priya@advocateconnect.local' THEN 'Karnataka'
        WHEN 'rohan@advocateconnect.local' THEN 'Delhi'
        WHEN 'sneha@advocateconnect.local' THEN 'Tamil Nadu & Puducherry'
        WHEN 'karan@advocateconnect.local' THEN 'Punjab & Haryana'
        WHEN 'nisha@advocateconnect.local' THEN 'Karnataka'
    END,
    p.bar_council_number = CASE u.email
        WHEN 'aarav@advocateconnect.local' THEN 'MAH/1234/2014'
        WHEN 'priya@advocateconnect.local' THEN 'KAR/2345/2017'
        WHEN 'rohan@advocateconnect.local' THEN 'D/3456/2012'
        WHEN 'sneha@advocateconnect.local' THEN 'TN/4567/2016'
        WHEN 'karan@advocateconnect.local' THEN 'P/5678/2015'
        WHEN 'nisha@advocateconnect.local' THEN 'KAR/6789/2019'
    END,
    p.enrolment_year = CASE u.email
        WHEN 'aarav@advocateconnect.local' THEN 2014
        WHEN 'priya@advocateconnect.local' THEN 2017
        WHEN 'rohan@advocateconnect.local' THEN 2012
        WHEN 'sneha@advocateconnect.local' THEN 2016
        WHEN 'karan@advocateconnect.local' THEN 2015
        WHEN 'nisha@advocateconnect.local' THEN 2019
    END
WHERE u.role = 'ADVOCATE';

SET SQL_SAFE_UPDATES = 1;

