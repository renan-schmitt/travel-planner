-- Requisitos de documentos por país × nacionalidade (Fase 19). Só os PUBLISHED valem para as
-- viagens; o seed entra como DRAFT, para o admin confirmar cada um na fonte oficial e publicar.
CREATE TABLE destination_requirement (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  country_code  CHAR(2)      NOT NULL,
  nationality   CHAR(2)      NOT NULL DEFAULT 'BR',
  type          ENUM ('VISA', 'ETA', 'ENTRY_FORM', 'VACCINE', 'INSURANCE', 'PASSPORT_VALIDITY',
                      'ID_CARD', 'OTHER') NOT NULL,
  title         VARCHAR(200) NOT NULL,
  description   TEXT,
  lead_days     INT,
  official_url  VARCHAR(500) NOT NULL,
  status        ENUM ('DRAFT', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT',
  verified_at   DATE,
  verified_by   BIGINT,
  source        ENUM ('MANUAL', 'AI') NOT NULL DEFAULT 'MANUAL',
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  version       BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_destination_requirement_country (country_code, nationality, status),
  CONSTRAINT fk_destination_requirement_country FOREIGN KEY (country_code) REFERENCES country (code),
  CONSTRAINT fk_destination_requirement_verified_by FOREIGN KEY (verified_by) REFERENCES app_user (id)
    ON DELETE SET NULL,
  CONSTRAINT ck_destination_requirement_lead_days CHECK (lead_days IS NULL OR lead_days >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
