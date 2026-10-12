# Sandevistan (Cyberpunk: Edgerunners) — Minecraft Java 1.21.1 / NeoForge 21.1.172

Este mod adiciona **somente** o Sandevistan. / This mod adds **only** the Sandevistan.

## Como compilar (GitHub Actions)
1. Crie um repositório e envie estes arquivos (a pasta `.github/workflows` inclusa já faz o build).
2. Aba **Actions** → rode o workflow → baixe o artefato `sandevistan-mod` (o `.jar` fica dentro).
3. Coloque o `.jar` na pasta `mods` do Minecraft 1.21.1 com NeoForge.

## Como obter
**Crafting** (mesa de trabalho, coluna do meio): **Neuralink** em cima, **Espinha** no meio, **Acelerador** embaixo.
Receitas das peças (ajustáveis em `data/sandevistan/recipe`):
- **Chip de Circuito**: Esmeralda e Redstone em cima; Barra de ferro e Pepita de ouro embaixo (2x2).
- **Neuralink**: Chip, Redstone, Chip em uma linha.
- **Espinha**: 3 Chips em coluna.
- **Acelerador**: Chip no centro, Diamante à esquerda e à direita, Barra de ouro em cima e embaixo.
Tudo também está na aba criativa "Sandevistan".

## Implante
- Clique com o botão direito segurando o item. Aparece um **aviso**: o implante causa **3 corações de dano instantâneo**.
- Clique de novo em até 10 segundos para confirmar. Precisa de mais de 3,5 corações de vida (criativo ignora).
- Depois de implantado, a coluna verde fica **nas suas costas permanentemente** (visível para todos os jogadores).
- Só sai se você remover o implante na aba **J** (botão "Remover implante", com o Sandevistan desligado): o item volta para o inventário. Morrer não remove o implante.

## Uso
| Tecla (configurável em Controles) | Ação |
|---|---|
| **V** | Ligar/desligar o Sandevistan (espera de 2 s entre trocas) |
| **J** | Abrir a aba do Sandevistan |

Enquanto está ligado:
- O jogo inteiro roda a **2 ticks por segundo** (igual a `/tick rate 2`).
- Você ganha o **efeito de Velocidade** (nível 55, aparece nos efeitos), pulo alto (~3 blocos), gravidade reduzida, queda rápida, Força IV, Resistência IV, Pressa III, Visão Noturna, bater/quebrar rápido como se o tempo fosse normal e **nenhum dano de queda**.
- A tela fica **levemente esverdeada** e silhuetas (afterimages) do jogador ficam paradas por onde ele passou, **cada uma dura 3 segundos** e muda suavemente de cor entre verde, azul e roxo.
- Ao desligar, o jogador **para no lugar** na hora (sem sair disparado).
- Sons: "Sandevistan Usa" ao ligar, "Sandevistan Para De Usar" ao desligar (os sons acompanham o jogador).

## Mente
- A barra **Mente** (HUD e aba J) é o tempo de uso: cheia dura **60 segundos reais** de uso (a contagem é em tempo real, mesmo com o jogo a 2 ticks).
- Desligado, ela recupera do zero ao cheio em **200 segundos** (1 de Mente a cada 2 segundos). Precisa de pelo menos 10 de Mente para ligar.
- Se a Mente chegar a zero o Sandevistan desliga sozinho e o jogador sofre **sobrecarga** (náusea, fraqueza e lentidão).
- A posição do HUD da Mente muda na aba J (4 cantos; fica salva em `config/sandevistan-client.properties`).

## Ajustes de balanceamento
Tudo fica no topo de `SandevistanLogic.java` (velocidade, pulo, gravidade, força, pressa) e em `SandevistanData.java`
(duração da Mente, regeneração, tick rate). Textos em `assets/sandevistan/lang`.

## Idiomas
Português (pt_br e pt_pt) para quem joga em português; inglês para todos os outros idiomas.

## Multiplayer
O tick rate é do servidor inteiro: enquanto qualquer jogador está com o Sandevistan ligado, todos ficam a 2 ticks.
