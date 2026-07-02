CREATE DATABASE IF NOT EXISTS paper
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE paper;

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  email VARCHAR(128) NOT NULL UNIQUE,
  invite_code VARCHAR(64),
  password_hash VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invite_code (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  active TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS model_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider_name VARCHAR(64) NOT NULL,
  base_url VARCHAR(255) NOT NULL,
  api_key VARCHAR(255) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  scene VARCHAR(32) NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_model_config_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS paper_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workspace_id VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  source VARCHAR(64) NOT NULL,
  title VARCHAR(512) NOT NULL,
  authors VARCHAR(255),
  paper_url VARCHAR(512),
  abstract_text TEXT,
  progress VARCHAR(16),
  importance VARCHAR(8),
  note TEXT,
  journal_tags VARCHAR(512),
  publish_year VARCHAR(16),
  read_at DATETIME,
  uploaded_at DATE,
  folder VARCHAR(64),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_paper_record_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS search_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  engine_id VARCHAR(64),
  engine_name VARCHAR(128),
  url VARCHAR(512),
  query VARCHAR(512),
  journal VARCHAR(255),
  author VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_search_session_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

INSERT INTO app_user (username, email, invite_code, password_hash)
SELECT 'Local User', 'local@paperpilot.app', 'LOCAL-SEED', 'local-only'
WHERE NOT EXISTS (
  SELECT 1 FROM app_user WHERE email = 'local@paperpilot.app'
);

INSERT INTO invite_code (code, active)
SELECT 'PAPERPILOT2026', 1
WHERE NOT EXISTS (SELECT 1 FROM invite_code WHERE code = 'PAPERPILOT2026');

INSERT INTO invite_code (code, active)
SELECT 'RESEARCH-LAB', 1
WHERE NOT EXISTS (SELECT 1 FROM invite_code WHERE code = 'RESEARCH-LAB');

INSERT INTO invite_code (code, active)
SELECT 'INVITE-ONLY', 1
WHERE NOT EXISTS (SELECT 1 FROM invite_code WHERE code = 'INVITE-ONLY');
