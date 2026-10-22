# LetterPDF

Aplicativo Android nativo para escanear documentos ou reunir imagens em PDF A4.

## Funções

- Scanner de documentos do Google ML Kit, com detecção de bordas, recorte e correção de perspectiva.
- Importa JPG, PNG, HEIC e HEIF.
- Revisa páginas, altera ordem, remove e adiciona páginas; limite de 20.
- Exporta PDF A4; nome opcional; abre e compartilha o arquivo.
- Tema claro, escuro ou do sistema.
- Sem login, anúncios, servidor ou envio das páginas pelo LetterPDF.

## Requisitos

- Android 8.0 ou superior.
- JDK 17.
- Android SDK com `compileSdk 35`.

No primeiro uso, o Google Play Services pode preparar o módulo do scanner. Depois disso, o fluxo de páginas é local.

## Build

```powershell
$jdkHome='C:\caminho\para\jdk-17'
$env:JAVA_HOME=$jdkHome
$env:Path="$jdkHome\bin;$env:Path"
.\gradlew.bat :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Testes

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline --no-daemon --console=plain
```

O protótipo do scanner foi validado em Xiaomi 23049PCD8G (`marble`). Detalhes: [PROTOTIPO_CAMERA.md](PROTOTIPO_CAMERA.md). Requisitos e limites: [Escopo.md](Escopo.md).

## Arquitetura

- Kotlin, Jetpack Compose e Material 3.
- Google ML Kit Document Scanner.
- `PdfDocument` para gerar PDF A4.
- Storage Access Framework para destino do arquivo.

## Limitações conhecidas

- A interface de câmera, flash e orientação é do scanner oficial do Google.
- Não há ajuste interno manual dos quatro cantos nem filtros; esta remoção evita recortes inconsistentes.
- O seletor Android controla conflitos de nome no destino escolhido.
