-- Account table (login credentials + status)
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(255) UNIQUE,
    phone       VARCHAR(20)  UNIQUE,
    password    VARCHAR(255) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);

-- User profile table (nickname, avatar, etc.)
CREATE TABLE IF NOT EXISTS user_profiles (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    nickname    VARCHAR(100) NOT NULL,
    avatar      VARCHAR(500),
    bio         TEXT,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);

-- Token balance table (for AI chat consumption)
CREATE TABLE IF NOT EXISTS user_token_balances (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance      INTEGER      NOT NULL DEFAULT 0,
    total_used   BIGINT       NOT NULL DEFAULT 0,
    total_granted BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_token_balances_user_id ON user_token_balances(user_id);

CREATE TABLE IF NOT EXISTS notes (
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    content     TEXT,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE notes ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE notes ADD COLUMN IF NOT EXISTS format VARCHAR(20) NOT NULL DEFAULT 'md';
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id);

CREATE TABLE IF NOT EXISTS files (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id),
    name        VARCHAR(255) NOT NULL,
    is_folder   BOOLEAN      NOT NULL DEFAULT FALSE,
    parent_id   BIGINT       REFERENCES files(id) ON DELETE CASCADE,
    format      VARCHAR(20),
    size        BIGINT,
    url         VARCHAR(500),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_files_user_id ON files(user_id);
CREATE INDEX IF NOT EXISTS idx_files_parent_id ON files(parent_id);

CREATE TABLE IF NOT EXISTS conversations (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id),
    title       VARCHAR(255) NOT NULL DEFAULT 'New Chat',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations(user_id);

CREATE TABLE IF NOT EXISTS messages (
    id              BIGSERIAL    PRIMARY KEY,
    conversation_id BIGINT       NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20)  NOT NULL,
    content         TEXT         NOT NULL,
    tokens          INTEGER      DEFAULT 0,
    compressed      BOOLEAN      DEFAULT FALSE,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);

ALTER TABLE messages ADD COLUMN IF NOT EXISTS branch      INTEGER  NOT NULL DEFAULT 0;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS version_of  BIGINT   REFERENCES messages(id);
ALTER TABLE messages ADD COLUMN IF NOT EXISTS deleted     BOOLEAN  NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_messages_branch ON messages(branch);

CREATE TABLE IF NOT EXISTS test_user (
    id     BIGSERIAL    PRIMARY KEY,
    name   VARCHAR(100) NOT NULL,
    email  VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_test_user_email ON test_user(email);

INSERT INTO test_user (name, email) VALUES ('Test User', 'test@auraspark.com')
ON CONFLICT (email) DO NOTHING;
