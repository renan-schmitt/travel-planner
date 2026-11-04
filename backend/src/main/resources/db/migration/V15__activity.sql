-- Atividades planejadas num dia da viagem (Fase 18): texto livre ("passeio de catamarã 9h") ou
-- ligada a um lugar favorito. Horário opcional (relógio local do dia). Sem horário, a ordem é a de
-- sort_order (arrastar e soltar). Excluir a viagem exclui as atividades; excluir o favorito só
-- desfaz o vínculo (a atividade fica com o título).
CREATE TABLE activity (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  trip_id        BIGINT       NOT NULL,
  day            DATE         NOT NULL,
  start_time     TIME,
  end_time       TIME,
  title          VARCHAR(200) NOT NULL,
  notes          TEXT,
  saved_place_id BIGINT,
  sort_order     INT          NOT NULL DEFAULT 0,
  created_at     DATETIME(6)  NOT NULL,
  updated_at     DATETIME(6)  NOT NULL,
  version        BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_activity_trip_day (trip_id, day, sort_order),
  CONSTRAINT fk_activity_trip FOREIGN KEY (trip_id) REFERENCES trip (id) ON DELETE CASCADE,
  CONSTRAINT fk_activity_saved_place FOREIGN KEY (saved_place_id) REFERENCES saved_place (id)
    ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
