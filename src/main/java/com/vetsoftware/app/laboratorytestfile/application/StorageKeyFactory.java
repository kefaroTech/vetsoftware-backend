package com.vetsoftware.app.laboratorytestfile.application;

import com.vetsoftware.app.laboratorytestfile.domain.LaboratoryTestStoragePathRef;
import java.text.Normalizer;
import java.util.UUID;

/**
 * Construye la clave (ruta dentro del bucket) de un LaboratoryTestFile siguiendo la regla:
 * {companyId}/{ownerId}/{animalSlug}-{animalId}/{uuid}-{originalFileName}
 */
public final class StorageKeyFactory {

  private StorageKeyFactory() {}

  public static String build(LaboratoryTestStoragePathRef path, String originalFileName) {
    return path.companyId()
        + "/"
        + path.ownerId()
        + "/"
        + slug(path.animalName())
        + "-"
        + path.animalId()
        + "/"
        + UUID.randomUUID()
        + "-"
        + sanitizeFileName(originalFileName);
  }

  private static String slug(String input) {
    String normalized =
        Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "") // quita diacríticos
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", "-") // todo lo no alfanumérico -> '-'
            .replaceAll("-{2,}", "-") // colapsa '-' repetidos
            .replaceAll("^-|-$", ""); // recorta extremos
    return normalized.isBlank() ? "animal" : normalized;
  }

  private static String sanitizeFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) return "archivo";
    return fileName.replaceAll("[\\\\/]+", "_").trim(); // evita separadores de ruta
  }
}
