-- Viajantes (S1): quem vai na viagem, com ou sem conta. Pertencem a um usuário (owner_user_id) e
-- são reaproveitados entre viagens. O viajante "eu" de cada usuário tem linked_user_id = o próprio
-- usuário. Não guardamos o número do passaporte, só a validade.
CREATE TABLE traveler (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  owner_user_id   BIGINT        NOT NULL,
  linked_user_id  BIGINT,
  name            VARCHAR(120)  NOT NULL,
  age_group       ENUM('BABY', 'CHILD', 'TEEN', 'ADULT') NOT NULL,
  birth_year      SMALLINT,
  nationalities   VARCHAR(64)   NOT NULL DEFAULT 'BR',
  passport_expiry DATE,
  created_at      DATETIME(6)   NOT NULL,
  updated_at      DATETIME(6)   NOT NULL,
  version         BIGINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_traveler_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_traveler_linked FOREIGN KEY (linked_user_id) REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT uk_traveler_owner_linked UNIQUE (owner_user_id, linked_user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE trip_traveler (
  trip_id     BIGINT NOT NULL,
  traveler_id BIGINT NOT NULL,
  PRIMARY KEY (trip_id, traveler_id),
  CONSTRAINT fk_trip_traveler_trip FOREIGN KEY (trip_id) REFERENCES trip (id) ON DELETE CASCADE,
  CONSTRAINT fk_trip_traveler_traveler FOREIGN KEY (traveler_id) REFERENCES traveler (id) ON DELETE CASCADE,
  INDEX ix_trip_traveler_traveler (traveler_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Viajante "eu" dos usuários que já existem (novos usuários ganham o seu no primeiro login).
INSERT INTO traveler (owner_user_id, linked_user_id, name, age_group, nationalities,
                      created_at, updated_at, version)
SELECT id, id, LEFT(COALESCE(NULLIF(TRIM(name), ''), SUBSTRING_INDEX(email, '@', 1)), 120),
       'ADULT', nationality, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0
FROM app_user;

-- ...e ele vai nas viagens de que o usuário já é dono.
INSERT INTO trip_traveler (trip_id, traveler_id)
SELECT t.id, tr.id
FROM trip t
JOIN traveler tr ON tr.owner_user_id = t.owner_id AND tr.linked_user_id = t.owner_id;
