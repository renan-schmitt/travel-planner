-- Usuários (login Google). Nacionalidade e cidade de origem são preenchidas no perfil e usadas
-- pelas regras de transfer e de viagem internacional.
CREATE TABLE app_user (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  google_sub         VARCHAR(255)  NOT NULL,
  email              VARCHAR(320)  NOT NULL,
  name               VARCHAR(255),
  picture_url        VARCHAR(1024),
  locale             VARCHAR(16)   NOT NULL DEFAULT 'pt-BR',
  nationality        CHAR(2)       NOT NULL DEFAULT 'BR',
  home_city_name     VARCHAR(255),
  home_city_place_id VARCHAR(255),
  home_country_code  CHAR(2),
  home_timezone      VARCHAR(64),
  created_at         DATETIME(6)   NOT NULL,
  updated_at         DATETIME(6)   NOT NULL,
  version            BIGINT        NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_app_user_google_sub UNIQUE (google_sub),
  INDEX ix_app_user_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
