package com.vetsoftware.app.infrastructure.pdf;

public class PdfRenderException extends RuntimeException {
  public PdfRenderException(String message) {
    super(message);
  }

  public PdfRenderException(String message, Throwable cause) {
    super(message, cause);
  }
}
