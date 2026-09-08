package com.vetsoftware.app.companytrialwindow.domain;

/**
 * No hay ventana. Se distingue de «hay una y está cerrada»: la primera es un
 * alta que no pasó por ninguno de los dos caminos de entrada (registro público
 * o cotización aceptada), la segunda es el estado normal de un cliente que ya
 * pagó.
 */
public class CompanyTrialWindowNotFoundException extends RuntimeException {

    public CompanyTrialWindowNotFoundException(Long companyId) {
        super("Company " + companyId + " has no trial window");
    }
}
