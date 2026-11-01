-- Viagens e membros (autorização por viagem). Datas manuais por enquanto; a partir da Fase 8
-- passam a ser derivadas dos trechos de transporte quando houver. IN_PROGRESS e DONE são calculados
-- na leitura (datas + relógio); o usuário só grava PLANNING ou CONFIRMED.
CREATE TABLE trip (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  name        VARCHAR(120)  NOT NULL,
  start_date  DATE,
  end_date    DATE,
  status      ENUM('PLANNING', 'CONFIRMED', 'IN_PROGRESS', 'DONE') NOT NULL DEFAULT 'PLANNING',
  cover_color VARCHAR(16),
  owner_id    BIGINT        NOT NULL,
  created_at  DATETIME(6)   NOT NULL,
  updated_at  DATETIME(6)   NOT NULL,
  version     BIGINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_trip_owner FOREIGN KEY (owner_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Membros da viagem. Convites pendentes (Fase 11) têm só invite_email; ao aceitar, user_id é
-- preenchido. MySQL permite vários NULL num UNIQUE, então as duas chaves convivem.
CREATE TABLE trip_member (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  trip_id      BIGINT        NOT NULL,
  user_id      BIGINT,
  invite_email VARCHAR(320),
  role         ENUM('OWNER', 'EDITOR', 'VIEWER') NOT NULL,
  accepted_at  DATETIME(6),
  created_at   DATETIME(6)   NOT NULL,
  updated_at   DATETIME(6)   NOT NULL,
  version      BIGINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_trip_member_trip FOREIGN KEY (trip_id) REFERENCES trip (id) ON DELETE CASCADE,
  CONSTRAINT fk_trip_member_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
  CONSTRAINT uk_trip_member_user UNIQUE (trip_id, user_id),
  CONSTRAINT uk_trip_member_invite UNIQUE (trip_id, invite_email),
  INDEX ix_trip_member_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
