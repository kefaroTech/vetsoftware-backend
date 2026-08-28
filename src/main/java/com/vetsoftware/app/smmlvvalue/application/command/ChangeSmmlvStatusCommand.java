package com.vetsoftware.app.smmlvvalue.application.command;

import com.vetsoftware.app.smmlvvalue.domain.SmmlvStatus;
import java.time.LocalDate;

/**
 * Anota el desenlace judicial o normativo sobre el ano que ya existe.
 *
 * <p>
 * Va <strong>por ano y no por id</strong>: quien registra un auto del Consejo
 * de Estado sabe a que ano afecta, no el identificador de la fila; y pedir el
 * id obligaria a una lectura previa cuyo unico proposito seria traducir el ano.
 */
public record ChangeSmmlvStatusCommand(int fiscalYear, SmmlvStatus status, String statusReference,
        LocalDate statusChangedOn) {
}
