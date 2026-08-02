package com.vetsoftware.app.infrastructure.pdf;

/** Motor de infraestructura que transforma HTML/XHTML ya procesado en un documento PDF. */
public interface HtmlToPdfEngine {
  byte[] render(String html);
}
