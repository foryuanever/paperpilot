-- MySQL schema for Paperslover backend
-- Run this script to create required tables

CREATE DATABASE IF NOT EXISTS paperslover CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE paperslover;

-- Members table
CREATE TABLE IF NOT EXISTS members (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  role ENUM('导师','学生','特权用户','管理员') NOT NULL,
  token_used BIGINT DEFAULT 0,
  token_limit BIGINT DEFAULT 1000000,
  status ENUM('online','offline') DEFAULT 'offline',
  register_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  active_time INT DEFAULT 0
);

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
  id VARCHAR(36) PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  deadline DATETIME,
  status ENUM('进行中','已完成','已截止') DEFAULT '进行中',
  creator_id VARCHAR(36),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (creator_id) REFERENCES members(id) ON DELETE SET NULL
);

-- Task attachments
CREATE TABLE IF NOT EXISTS task_attachments (
  id VARCHAR(36) PRIMARY KEY,
  task_id VARCHAR(36),
  name VARCHAR(255),
  size VARCHAR(50),
  mime_type VARCHAR(100),
  file_path VARCHAR(500),
  uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- Announcements table
CREATE TABLE IF NOT EXISTS announcements (
  id VARCHAR(36) PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  content TEXT,
  image_path VARCHAR(500),
  link VARCHAR(500),
  creator_id VARCHAR(36),
  publish_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (creator_id) REFERENCES members(id) ON DELETE SET NULL
);

-- Announcement attachments
CREATE TABLE IF NOT EXISTS announcement_attachments (
  id VARCHAR(36) PRIMARY KEY,
  announcement_id VARCHAR(36),
  name VARCHAR(255),
  size VARCHAR(50),
  mime_type VARCHAR(100),
  file_path VARCHAR(500),
  uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE
);

-- Resources table (file library)
CREATE TABLE IF NOT EXISTS resources (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  size VARCHAR(50),
  mime_type VARCHAR(100),
  uploader_id VARCHAR(36),
  upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  file_path VARCHAR(500),
  FOREIGN KEY (uploader_id) REFERENCES members(id) ON DELETE SET NULL
);

-- Checkins table (optional)
CREATE TABLE IF NOT EXISTS checkins (
  member_id VARCHAR(36),
  checkin_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  status ENUM('已打卡','未打卡') DEFAULT '已打卡',
  PRIMARY KEY (member_id, checkin_time),
  FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);

-- Indexes for faster queries
CREATE INDEX idx_tasks_deadline ON tasks(deadline);
CREATE INDEX idx_task_attachments_task ON task_attachments(task_id);
CREATE INDEX idx_ann_attachments_ann ON announcement_attachments(announcement_id);
CREATE INDEX idx_resources_uploader ON resources(uploader_id);

-- End of schema
