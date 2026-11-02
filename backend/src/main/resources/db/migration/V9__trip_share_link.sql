-- Links de partilha de uma viagem: quem abre o link (logado) entra como membro com o papel do link.
-- Token = 32 bytes aleatórios em base64url sem padding (43 caracteres). Revogar só preenche
-- revoked_at (o link para de funcionar, mas fica o registro).
CREATE TABLE trip_share_link (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  trip_id    BIGINT      NOT NULL,
  token      CHAR(43)    NOT NULL,
  role       ENUM ('EDITOR', 'VIEWER') NOT NULL,
  created_by BIGINT      NOT NULL,
  revoked_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version    BIGINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_trip_share_link_token UNIQUE (token),
  CONSTRAINT fk_trip_share_link_trip FOREIGN KEY (trip_id) REFERENCES trip (id) ON DELETE CASCADE,
  CONSTRAINT fk_trip_share_link_user FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE CASCADE,
  INDEX ix_trip_share_link_trip (trip_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Convites pendentes são procurados pelo e-mail no login.
CREATE INDEX ix_trip_member_invite_email ON trip_member (invite_email);
