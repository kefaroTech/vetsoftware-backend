package com.vetsoftware.app.laboratorytestfile.application.port.out;

public interface FileStoragePort {

  /** Sube el contenido bajo la clave indicada y devuelve los datos del objeto almacenado. */
  StoredFile store(String key, byte[] content, String contentType);

  /** Recupera el contenido del objeto por su clave. */
  byte[] retrieve(String key);

  /** Elimina el objeto por su clave. */
  void delete(String key);

  record StoredFile(String bucket, String key, String eTag) {}
}
