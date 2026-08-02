package com.vetsoftware.app.systemconfiguration.domain;

import java.time.LocalDateTime;

/**
 * Configuración general del sistema (GLOBAL, no scoped a empresa). Almacén clave-valor: cada fila
 * es una propiedad ({@code propertyName}) con su {@code value} en texto. Permite controlar
 * distintos ajustes del sistema (p.ej. el valor del UVT vigente, umbrales, flags) sin nuevas
 * tablas.
 *
 * <p>El {@code value} se guarda como String genérico; cada consumidor lo interpreta según la
 * propiedad (p.ej. el UVT se parsea a número). {@code propertyName} es la clave única de la fila.
 */
public class SystemConfiguration {
  private Long id;
  private final String propertyName;
  private String value;
  private final LocalDateTime createdDate;
  private boolean enabled;

  public SystemConfiguration(
      Long id, String propertyName, String value, LocalDateTime createdDate, boolean enabled) {
    validate(propertyName, value);
    this.id = id;
    this.propertyName = propertyName;
    this.value = value;
    this.createdDate = createdDate;
    this.enabled = enabled;
  }

  public static SystemConfiguration create(String propertyName, String value) {
    return new SystemConfiguration(null, propertyName, value, LocalDateTime.now(), true);
  }

  public void update(String value) {
    validateValue(value);
    this.value = value;
  }

  private static void validate(String propertyName, String value) {
    if (propertyName == null || propertyName.isBlank())
      throw new IllegalArgumentException("propertyName is required");
    validateValue(value);
  }

  private static void validateValue(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("value is required");
  }

  public Long getId() {
    return id;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String getValue() {
    return value;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }
}
