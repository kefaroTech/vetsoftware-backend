package com.vetsoftware.app.aiproposal.application.command;

/**
 * Una casilla marcada, tal como la devuelve {@code LegalConsentCheckbox} del
 * front publico: el par {@code (code, documentVersion)}.
 *
 * <p>
 * &#9940; <strong>El par, y no solo el codigo.</strong> La fila que se guarda
 * como aceptada tiene que ser <em>la misma</em> que se le mostro al prospecto;
 * con solo el codigo se resolveria "la vigente ahora", que puede ser otra si
 * alguien publico una version entre que se pinto la pantalla y se envio el
 * formulario. Esa ventana es pequena y la prueba de cumplimiento que produce es
 * falsa, que es peor que no tenerla.
 */
public record LegalAcceptanceCommand(String code, int documentVersion) {

    public LegalAcceptanceCommand {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("legal document code is required");
        if (documentVersion < 1)
            throw new IllegalArgumentException("documentVersion must be at least 1: " + code);
    }
}
