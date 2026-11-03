-- Itens de checklist da viagem. Origem: MANUAL (digitado), RULE (motor de lacunas, Fase 14), AI ou
-- TEMPLATE (Fase 13). Itens de regra são identificados por (rule_code, rule_key), para a regra não
-- duplicar o item ao rodar de novo; nos demais as duas colunas ficam nulas (o UNIQUE aceita vários
-- NULL). Responsável e "concluído por" são usuários com conta; se a conta sumir, viram NULL.
CREATE TABLE checklist_item (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  trip_id          BIGINT       NOT NULL,
  category         ENUM ('DOCUMENTS', 'BOOKINGS', 'PACKING', 'HOME', 'HEALTH', 'MONEY', 'OTHER') NOT NULL,
  title            VARCHAR(200) NOT NULL,
  notes            TEXT,
  due_date         DATE,
  status           ENUM ('OPEN', 'DONE') NOT NULL DEFAULT 'OPEN',
  done_at          DATETIME(6),
  done_by          BIGINT,
  assignee_user_id BIGINT,
  source           ENUM ('MANUAL', 'RULE', 'AI', 'TEMPLATE') NOT NULL DEFAULT 'MANUAL',
  rule_code        VARCHAR(64),
  rule_key         VARCHAR(160),
  sort_order       INT          NOT NULL DEFAULT 0,
  created_at       DATETIME(6)  NOT NULL,
  updated_at       DATETIME(6)  NOT NULL,
  version          BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_checklist_item_rule UNIQUE (trip_id, rule_code, rule_key),
  KEY ix_checklist_item_trip_order (trip_id, sort_order),
  CONSTRAINT fk_checklist_item_trip FOREIGN KEY (trip_id) REFERENCES trip (id) ON DELETE CASCADE,
  CONSTRAINT fk_checklist_item_done_by FOREIGN KEY (done_by) REFERENCES app_user (id) ON DELETE SET NULL,
  CONSTRAINT fk_checklist_item_assignee FOREIGN KEY (assignee_user_id) REFERENCES app_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
