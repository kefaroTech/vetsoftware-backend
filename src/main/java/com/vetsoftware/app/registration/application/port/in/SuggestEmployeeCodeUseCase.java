package com.vetsoftware.app.registration.application.port.in;

/** Propone un código de acceso disponible a partir del nombre de la empresa y del empleado (Opción A). */
public interface SuggestEmployeeCodeUseCase {
    String suggest(String companyName, String employeeName);
}
