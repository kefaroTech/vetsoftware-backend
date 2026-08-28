package com.vetsoftware.app.companylimitoverride.domain;

/**
 * Por qué se negoció la excepción, en lista cerrada.
 *
 * <p>
 * Es la convención de la casa para los motivos: <strong>siempre dos
 * columnas</strong> —un código de lista cerrada para poder agrupar a quinientos
 * clientes y un texto libre para poder explicar—. Las dos obligatorias: una
 * excepción sin motivo escrito es una excepción que nadie puede defender seis
 * meses después.
 */
public enum OverrideReasonCode {
    RETENTION, MIGRATION, COMMERCIAL_AGREEMENT, SUPPORT_INCIDENT, OTHER
}
