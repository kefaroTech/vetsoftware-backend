package com.vetsoftware.app.companytrialwindow.domain;

/**
 * No hay ventana. Se distingue de «hay una y está cerrada»: la primera es un
 * alta que no pasó por el único camino de entrada (D-55, todo cliente nace con
 * su ventana), la segunda es el estado normal de un cliente que ya pagó.
 */
public class CompanyTrialWindowNotFoundException extends RuntimeException {

    public CompanyTrialWindowNotFoundException(Long companyId) {
        super("Company " + companyId + " has no trial window");
    }
}
