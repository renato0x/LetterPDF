
# LetterPDF

> Escopo do MVP — Android, documentos em foto para PDF.

## Produto

**LetterPDF**: câmera ou galeria → PDF A4 legível, salvo e compartilhável.

Usuário principal: mãe do Renato; uso ocasional; português-BR; pouca familiaridade técnica.

## Objetivo

Criar PDFs de uma ou mais páginas, sem conta, internet, servidor ou configuração técnica.

## Princípios

- Uma ação principal por tela.
- Botões grandes; ícone e texto.
- Linguagem simples.
- Processamento local.
- Usuário pode cancelar, remover página ou voltar antes de salvar.

## MVP

### Início

- Ações: **Escanear documento**; **Escolher imagens**.
- O scanner oficial solicita a câmera ao escanear; a galeria continua disponível se a câmera for negada.

### Câmera e corte

- Usa o Google ML Kit Document Scanner, componente consolidado do Google Play Services.
- O scanner oferece seus modos manual e automático, detecta bordas, recorta e corrige perspectiva.
- Na primeira abertura, o Google Play Services pode preparar o módulo do scanner; depois, o processamento das páginas é local.
- A tela nativa do scanner controla enquadramento, flash e orientação visual. LetterPDF não duplica nem promete alertas próprios de foco, luz ou sombra.
- Captura não é bloqueada se a detecção não estiver perfeita; o usuário confirma no scanner.
- Sem editor interno de quatro cantos: remover este ajuste foi decisão de produto para evitar resultado inconsistente.

### Galeria

- Selecionar uma ou mais imagens JPG, PNG ou HEIC.
- Não importar PDFs existentes.
- Câmara e galeria podem compor o mesmo documento.

### Revisão da página

- A captura pelo scanner usa o recorte e a perspectiva automáticos do scanner.
- Imagens da galeria entram inteiras, sem editor manual.
- Revisar por miniatura grande antes de salvar.
- Não há corte manual, perspectiva manual ou filtros no MVP.

### Páginas

- Miniaturas de todas as páginas.
- Adicionar páginas por câmera ou galeria.
- Reordenar por **Subir** e **Descer**; leitor de tela oferece as mesmas ações.
- Remover com confirmação.
- Limite: 20 páginas por PDF; avisar antes de exceder.

### PDF, arquivo e compartilhamento

- Um PDF A4; orientação definida por página.
- Máxima resolução prática; sem compressão agressiva. Arquivo pode ser grande.
- Campo de nome opcional.
- Nome vazio: `Documento-AAAA-MM-DD.pdf`.
- Salvar pelo seletor nativo Android.
- Se nome já existir: **Substituir**, **Mudar nome**, **Cancelar**.
- Tela final: **Abrir PDF**, **Compartilhar**, **Novo documento**.

## Fluxo

1. Abrir LetterPDF.
2. Escanear documento ou escolher imagens.
3. Capturar no scanner ou selecionar páginas.
4. O scanner corrige a perspectiva automaticamente; imagens da galeria permanecem inteiras.
5. Conferir miniaturas.
6. Adicionar, revisar, reordenar ou remover páginas.
7. Finalizar; preencher nome opcional.
8. Salvar PDF.
9. Abrir, compartilhar ou criar novo documento.

## Requisitos

- Android 8.0+.
- Sem login, anúncios, nuvem, servidor ou internet obrigatória.
- Sem histórico interno: PDFs ficam somente na pasta escolhida.
- Rascunho não salvo é descartado ao fechar ou cancelar.
- Texto legível, alto contraste, áreas de toque amplas e rótulos para leitor de tela.
- Não gerar nem compartilhar PDF incompleto se a exportação falhar.

## Fora do MVP

- OCR e pesquisa textual.
- Assinatura digital, formulários e edição de PDF existente.
- Sincronização, backup próprio e conta.
- QR code, código de barras ou cartões.
- Ajustes avançados de imagem.
- iOS, web, desktop, anúncios, assinatura ou monetização.

## Tecnologia

- Kotlin.
- Jetpack Compose e Material 3.
- Google ML Kit Document Scanner.
- `PdfDocument`.
- Storage Access Framework e `FileProvider`.

## Aceite

- Câmera, galeria, revisão, reordenação e remoção funcionam.
- O scanner do ML Kit detecta, recorta e corrige perspectiva automaticamente.
- PDF A4 com até 20 páginas abre corretamente; ordem preservada.
- Salvar, abrir e compartilhar funcionam.
- Sem conta ou envio de imagens para servidor. O primeiro uso do scanner pode precisar baixar/preparar módulo do Google Play Services.
- Fluxo testado no Xiaomi da mãe do Renato e em outro Android.
