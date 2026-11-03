-- Templates do sistema (owner_user_id NULL). Cada bloco insere o template e os itens dele;
-- due_offset_days é relativo à partida (NULL = sem data limite).

INSERT INTO checklist_template (owner_user_id, name, description, icon, created_at, updated_at)
VALUES (NULL, 'Praia', 'Sol, mar e piscina: o que não pode faltar na mala e na reserva.', 'BEACH', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @t = LAST_INSERT_ID();
INSERT INTO template_item (template_id, category, title, due_offset_days, sort_order, created_at, updated_at)
VALUES
  (@t, 'PACKING', 'Protetor solar (rosto e corpo)', NULL, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Repelente', NULL, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Óculos de sol', NULL, 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Chapéu ou boné', NULL, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Roupas de banho', NULL, 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Canga ou toalha de praia', NULL, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Chinelo', NULL, 6, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Pós-sol ou hidratante', NULL, 7, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Bolsa estanque para o celular', NULL, 8, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Garrafa de água reutilizável', NULL, 9, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Reservar guarda-sol/espreguiçadeira, se a praia cobrar', -3, 10, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Verificar tábua de marés e previsão do tempo', -1, 11, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HEALTH', 'Remédio para enjoo e queimadura', -3, 12, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO checklist_template (owner_user_id, name, description, icon, created_at, updated_at)
VALUES (NULL, 'Neve', 'Frio intenso, roupas em camadas e esqui.', 'SNOW', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @t = LAST_INSERT_ID();
INSERT INTO template_item (template_id, category, title, due_offset_days, sort_order, created_at, updated_at)
VALUES
  (@t, 'PACKING', 'Segunda pele (camada térmica)', NULL, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Fleece ou blusa intermediária', NULL, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Jaqueta impermeável', NULL, 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Calça de neve', NULL, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Luvas impermeáveis', NULL, 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Gorro e balaclava', NULL, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Meias térmicas', NULL, 6, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Bota impermeável com solado antiderrapante', NULL, 7, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Óculos de sol ou de esqui', NULL, 8, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Protetor solar e labial', NULL, 9, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Alugar equipamento de esqui/snowboard', -14, 10, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Comprar passe de ski (lift pass)', -14, 11, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Agendar aula de esqui', -14, 12, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Verificar correntes/pneus de neve se for de carro', -7, 13, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HEALTH', 'Conferir se o seguro viagem cobre esportes de inverno', -14, 14, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO checklist_template (owner_user_id, name, description, icon, created_at, updated_at)
VALUES (NULL, 'Cidade', 'Passeios urbanos, museus e transporte público.', 'CITY', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @t = LAST_INSERT_ID();
INSERT INTO template_item (template_id, category, title, due_offset_days, sort_order, created_at, updated_at)
VALUES
  (@t, 'PACKING', 'Tênis confortável para caminhar', NULL, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Carregador portátil (power bank)', NULL, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Mochila pequena ou pochete', NULL, 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Guarda-chuva compacto', NULL, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Comprar ingressos de museus e atrações com hora marcada', -14, 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Reservar restaurantes concorridos', -7, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Ver passe de transporte público ou cartão de turista', -3, 6, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'OTHER', 'Baixar mapas offline da cidade', -1, 7, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'OTHER', 'Instalar apps de transporte e táxi locais', -1, 8, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'OTHER', 'Anotar endereço da hospedagem no idioma local', -1, 9, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO checklist_template (owner_user_id, name, description, icon, created_at, updated_at)
VALUES (NULL, 'Internacional', 'Documentos, dinheiro e conexão para sair do país.', 'GLOBE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @t = LAST_INSERT_ID();
INSERT INTO template_item (template_id, category, title, due_offset_days, sort_order, created_at, updated_at)
VALUES
  (@t, 'DOCUMENTS', 'Conferir validade do passaporte', -60, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'Verificar exigência de visto', -60, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HEALTH', 'Verificar vacinas exigidas (ex.: febre amarela)', -30, 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'Contratar seguro viagem', -14, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'Imprimir/salvar cópias de passaporte, reservas e seguro', -3, 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'MONEY', 'Comprar moeda/cartão internacional', -7, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'MONEY', 'Avisar banco', -3, 6, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'MONEY', 'Liberar o cartão para uso no exterior', -3, 7, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'OTHER', 'Ativar roaming ou comprar eSIM', -3, 8, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Adaptador de tomada', NULL, 9, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'Fazer check-in online do voo', -1, 10, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'Preencher formulários de entrada do país, se houver', -3, 11, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'OTHER', 'Anotar telefone do consulado brasileiro', -1, 12, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO checklist_template (owner_user_id, name, description, icon, created_at, updated_at)
VALUES (NULL, 'Com bebê', 'Fraldas, comida e conforto para os pequenos.', 'BABY', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @t = LAST_INSERT_ID();
INSERT INTO template_item (template_id, category, title, due_offset_days, sort_order, created_at, updated_at)
VALUES
  (@t, 'PACKING', 'Fraldas (uma por hora de viagem, mais reserva)', NULL, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Lenços umedecidos', NULL, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Trocador portátil', NULL, 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Mamadeiras, chupetas e esterilizador', NULL, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Leite/fórmula e papinhas', NULL, 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Roupas extras na bagagem de mão', NULL, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Brinquedos e livrinhos para o trajeto', NULL, 6, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Carrinho de bebê leve ou sling', NULL, 7, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Pedir berço na hospedagem', -14, 8, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Reservar bebê-conforto/cadeirinha no aluguel do carro', -14, 9, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Avisar a companhia aérea (assento, berço, carrinho)', -7, 10, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'Levar certidão de nascimento ou RG do bebê', -7, 11, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'Autorização de viagem para menor, se viajar sem um dos pais', -14, 12, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HEALTH', 'Consultar o pediatra e conferir vacinas', -21, 13, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HEALTH', 'Kit de remédios infantis e termômetro', -3, 14, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO checklist_template (owner_user_id, name, description, icon, created_at, updated_at)
VALUES (NULL, 'Com pet', 'Documentos, transporte e hospedagem para o seu bicho.', 'PET', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @t = LAST_INSERT_ID();
INSERT INTO template_item (template_id, category, title, due_offset_days, sort_order, created_at, updated_at)
VALUES
  (@t, 'HEALTH', 'Consultar o veterinário e atualizar vacinas', -30, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'Atestado de saúde do veterinário', -10, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'Carteira de vacinação (antirrábica em dia)', -10, 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'DOCUMENTS', 'CVI/certificado internacional, se sair do país', -30, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Confirmar que a hospedagem aceita pets', -14, 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'BOOKINGS', 'Reservar o transporte do pet na companhia aérea', -21, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Caixa de transporte adequada', NULL, 6, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Ração para todos os dias, mais reserva', NULL, 7, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Potes de água e comida', NULL, 8, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Coleira com plaquinha de identificação', NULL, 9, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Guia, saquinhos e tapete higiênico', NULL, 10, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'PACKING', 'Brinquedo ou manta com o cheiro de casa', NULL, 11, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HEALTH', 'Anotar um veterinário 24 h no destino', -3, 12, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO checklist_template (owner_user_id, name, description, icon, created_at, updated_at)
VALUES (NULL, 'Antes de sair de casa', 'O que fazer em casa nos dias e horas antes de partir.', 'HOME', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
SET @t = LAST_INSERT_ID();
INSERT INTO template_item (template_id, category, title, due_offset_days, sort_order, created_at, updated_at)
VALUES
  (@t, 'HOME', 'Desligar o gás', 0, 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HOME', 'Desligar aparelhos da tomada', 0, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HOME', 'Fechar janelas e trancar portas', 0, 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HOME', 'Tirar o lixo', 0, 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HOME', 'Esvaziar a geladeira de perecíveis', -1, 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HOME', 'Regar plantas/pedir a alguém', -1, 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HOME', 'Deixar a chave com alguém de confiança', -2, 6, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HOME', 'Programar luzes ou pedir a um vizinho para acendê-las', -1, 7, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HOME', 'Suspender entregas e correspondência', -3, 8, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'HOME', 'Combinar quem cuida dos animais', -7, 9, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'MONEY', 'Avisar o banco/liberar o cartão para uso internacional', -3, 10, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'MONEY', 'Pagar contas que vencem durante a viagem', -3, 11, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'OTHER', 'Ativar roaming/comprar eSIM', -3, 12, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'OTHER', 'Ativar a resposta automática do e-mail', -1, 13, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
  (@t, 'OTHER', 'Fazer backup do celular', -2, 14, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
