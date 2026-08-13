package com.vetsoftware.app.auth.application.dto;

/**
 * Quien esta autenticado: un empleado de una empresa o un usuario de la
 * plataforma.
 *
 * <p>
 * TR-01: era un {@code String} con los literales {@code "EMPLOYEE"} y
 * {@code "SYSTEM_USER"} repetidos por el codigo, asi que el contrato lo
 * publicaba como texto libre. Cada frontend adivinaba: el operativo declaraba
 * una union cerrada y el admin un {@code string}, y ninguno de los dos podia
 * tener razon porque el servidor no lo decia. Siendo un enum, springdoc lo
 * emite como conjunto cerrado y los dos lo reciben igual.
 */
public enum AuthSubjectType {
    EMPLOYEE, SYSTEM_USER
}
