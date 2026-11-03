-- Templates de checklist. owner_user_id NULL = template do sistema (só leitura, semeado pela V12);
-- senão é do usuário, que pode editá-lo e apagá-lo. icon é uma chave fixa (ex.: BEACH) que o
-- frontend troca por um ícone. due_offset_days é relativo à data de partida (ex.: -7 = uma semana
-- antes); ao aplicar o template vira a data limite do item.
CREATE TABLE checklist_template (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  owner_user_id BIGINT,
  name          VARCHAR(120) NOT NULL,
  description   VARCHAR(500),
  icon          VARCHAR(32)  NOT NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  version       BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_checklist_template_owner (owner_user_id),
  CONSTRAINT fk_checklist_template_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE template_item (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  template_id     BIGINT       NOT NULL,
  category        ENUM ('DOCUMENTS', 'BOOKINGS', 'PACKING', 'HOME', 'HEALTH', 'MONEY', 'OTHER') NOT NULL,
  title           VARCHAR(200) NOT NULL,
  due_offset_days INT,
  sort_order      INT          NOT NULL DEFAULT 0,
  created_at      DATETIME(6)  NOT NULL,
  updated_at      DATETIME(6)  NOT NULL,
  version         BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_template_item_order (template_id, sort_order),
  CONSTRAINT fk_template_item_template FOREIGN KEY (template_id) REFERENCES checklist_template (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
