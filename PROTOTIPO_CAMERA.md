# Protótipo de câmera

- Abordagem aprovada: Google ML Kit Document Scanner (`play-services-mlkit-document-scanner:16.0.0`), modo completo, até 20 páginas, retorno JPEG para o LetterPDF.
- Motivo: CameraX + OpenCV local não reconheceu com precisão a mesma capa que o modo Documentos da Xiaomi reconheceu. A tentativa por Hough teve recorte inválido e consumo de ~6 s por quadro; foi removida.
- Dispositivo: Xiaomi 23049PCD8G (`marble`), Android via `adb`.
- Resultado: ML Kit abriu no Xiaomi, mostrou contorno adaptativo, captura manual e automática. Uma captura retornou ao LetterPDF como Página 1 já recortada.
- Privacidade: imagens permanecem no aparelho; o scanner é executado pelo Google Play Services e só entrega ao LetterPDF as imagens digitalizadas.
- Limitação aceita: na primeira execução, Google Play Services baixa módulos, lógica e interface. Depois, o processamento do scanner é local. Isso substitui a exigência anterior de internet nunca ser necessária.
- Revisão posterior: o editor próprio foi removido. O scanner entrega a página já recortada e corrigida; imagens da galeria entram inteiras para manter o fluxo previsível.
