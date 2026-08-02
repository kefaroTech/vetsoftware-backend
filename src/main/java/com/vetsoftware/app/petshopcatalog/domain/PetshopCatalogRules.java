package com.vetsoftware.app.petshopcatalog.domain;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PetshopCatalogRules {
  private PetshopCatalogRules() {}

  public static String text(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
    }
    return normalized;
  }

  public static BigDecimal price(BigDecimal value) {
    if (value == null || value.signum() < 0) {
      throw new IllegalArgumentException("salePrice must be zero or positive");
    }
    return value;
  }

  public static int positive(Integer value, String field) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(field + " must be positive");
    }
    return value;
  }

  public static int nonNegative(Integer value, String field) {
    if (value == null || value < 0) {
      throw new IllegalArgumentException(field + " must be zero or positive");
    }
    return value;
  }

  public static List<String> barcodes(List<String> values) {
    if (values == null) return List.of();
    Set<String> normalized = new LinkedHashSet<>();
    for (String value : values) {
      String barcode = text(value, "barcode", 64);
      if (barcode
          .chars()
          .anyMatch(
              character ->
                  Character.isISOControl(character) || Character.isWhitespace(character))) {
        throw new IllegalArgumentException("barcode contains whitespace or control characters");
      }
      if (!normalized.add(barcode)) {
        throw new IllegalArgumentException("barcode is repeated: " + barcode);
      }
    }
    return List.copyOf(normalized);
  }

  public static void defaultFactor(boolean defaultPresentation, int conversionFactor) {
    if (defaultPresentation && conversionFactor != 1) {
      throw new IllegalArgumentException("The default presentation must have conversionFactor 1");
    }
  }

  public static void expectedVersion(Long expected, Long actual) {
    if (expected == null) {
      throw new IllegalArgumentException("expectedVersion is required");
    }
    if (!expected.equals(actual)) {
      throw new PetshopCatalogConflictException(
          "CONCURRENT_MODIFICATION",
          "El registro fue modificado por otra operación. Recarga e intenta de nuevo.");
    }
  }
}
