package com.vetsoftware.app.animal.domain;

/**
 * Companion VO del animal usado por WeightRecord (miembro del mismo agregado). Guarda solo los
 * campos que un registro de peso necesita del animal, sin arrastrar el agregado completo.
 */
public record AnimalRef(Long id, String name, String code) {
  public AnimalRef {
    if (id == null) throw new IllegalArgumentException("animal id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("animal name is required");
  }
}
