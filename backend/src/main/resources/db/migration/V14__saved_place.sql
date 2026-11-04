-- Lugares favoritos de uma parada (restaurantes, atrações, outros), salvos a partir da busca no
-- Places (Fase 17). Os termos do Google só permitem guardar o place_id: nome (no momento em que foi
-- salvo, para listagem e uso offline), faixa de preço, notas e "visitado" são dados do usuário; nota
-- e posição são buscadas sob demanda, com cache curto. Um lugar aparece uma vez por parada.
-- Excluir a parada exclui os favoritos dela.
CREATE TABLE saved_place (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  stop_id           BIGINT       NOT NULL,
  place_id          VARCHAR(255) NOT NULL,
  name              VARCHAR(160) NOT NULL,
  kind              ENUM ('RESTAURANT', 'ATTRACTION', 'OTHER') NOT NULL,
  price_tier        TINYINT,
  price_tier_source ENUM ('PLACES', 'HEURISTIC', 'AI', 'USER'),
  notes             TEXT,
  visited           BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at        DATETIME(6)  NOT NULL,
  updated_at        DATETIME(6)  NOT NULL,
  version           BIGINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_saved_place_stop_place (stop_id, place_id),
  CONSTRAINT fk_saved_place_stop FOREIGN KEY (stop_id) REFERENCES stop (id) ON DELETE CASCADE,
  CONSTRAINT ck_saved_place_price_tier CHECK (price_tier BETWEEN 1 AND 4)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
