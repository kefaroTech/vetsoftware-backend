package com.vetsoftware.app.employee.domain;

/**
 * Companion VO de una sede ({@code branch}) referenciada por un empleado. La feature {@code
 * employee} no importa el dominio de {@code branch}: solo guarda los campos que necesita mostrar
 * (id + nombre) en su listado/detalle.
 */
public record BranchRef(Long id, String name) {
  public BranchRef {
    if (id == null) throw new IllegalArgumentException("branch id is required");
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("branch name is required");
  }
}
