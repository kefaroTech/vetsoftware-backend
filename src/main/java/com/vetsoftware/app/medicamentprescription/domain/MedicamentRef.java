package com.vetsoftware.app.medicamentprescription.domain;

/**
 * Companion VO del catálogo de medicamentos (feature `medicament`). La línea recetada
 * referencia el medicamento por id y guarda su nombre como snapshot histórico.
 */
public record MedicamentRef(Long id, String name) {
    public MedicamentRef {
        if (id == null) throw new IllegalArgumentException("medicament id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("medicament name is required");
        if (name.length() > 200) throw new IllegalArgumentException("medicament name must be 200 chars or less");
    }
}
