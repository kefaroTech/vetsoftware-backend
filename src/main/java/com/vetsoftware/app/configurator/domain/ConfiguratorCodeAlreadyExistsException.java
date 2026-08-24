package com.vetsoftware.app.configurator.domain;

/**
 * El código de una pregunta es único global y el de una opción único dentro de
 * su pregunta ({@code uq_configurator_questions_code},
 * {@code uq_configurator_options_code}). Se comprueba antes de insertar para
 * que el choque sea un 409 con mensaje y no un 500 traduciendo una violación de
 * índice.
 */
public class ConfiguratorCodeAlreadyExistsException extends RuntimeException {
    public ConfiguratorCodeAlreadyExistsException(String what, String code) {
        super(what + " code already exists: " + code);
    }
}
