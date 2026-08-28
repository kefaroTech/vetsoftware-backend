package com.vetsoftware.app.companytrialgrant.domain;

/**
 * Nivel de acceso del permiso sucesor. Companion de esta feature.
 *
 * <p>
 * El máximo estado de restricción del producto es solo lectura: una prueba
 * vencida consulta e imprime lo que ya cargó. No existe —ni debe añadirse— un
 * nivel que corte el acceso entero.
 */
public enum TrialAccessLevel {
    FULL, READ_ONLY
}
