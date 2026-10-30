# Objetivo

Um sistema para planejar as viagens que fazemos. O sistema deve ter checklists e propor tarefas que possamos ter esquecido.

## Requisitos

- Sistema acessível pelo browser.
- Responsivo para o celular.
- Quero usar o login do google.
- Quero criar viagens:
  - Data de ínicio e fim
  - Cidade(s)
  - Período por cidade
  - Transporte para ir e voltar.
- Quero poder partilhar viagens.
- Quero um dashboard para controlar as viagens.
- Quero que tenha uma lista de hoteis/resorts da cidade da minha viagem, organizados por categorias, avaliações, preço médio, etc.
- O sistema vai identificando o que falta e vai sugerindo:
  - Transporte de/para o aeroporto (uber, transfer do booking)
  - Transporte no local de destino
  - Restaurantes
  - Opções do resort
  - Locais turísticos
- O sistema ajuda a fazer as malas
  - Sugere quantidade de roupas
- O sistema lembra se para alguns local de destino precisamos de alguma documento antecipado (tipo o e-ticket in punta cana)

## Tecnologia

- Deploy no app engine do Google pois já tenho projetos lá.
- Já tenho uma instancia do MySQL lá que pode ser usada.
- Backend em java.

## Open questions

- Como ter as informações dos hoteis.
- Como ter as informações dos restaurantes.
- Como ter as informações dos locais turísticos.
- Ter IA para processamento.
- Como ter as informações sobre os requisitos de documentos dos destinos.