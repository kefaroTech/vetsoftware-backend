package com.vetsoftware.app.aiproposal.application.dto;

import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import java.util.List;

/**
 * Lo que hace falta para pedirle una propuesta al modelo.
 *
 * @param customerTexts
 *            los textos del prospecto <strong>en orden de turno y
 *            acumulativos</strong>: el inicial primero y despues cada
 *            refinamiento. Mandar solo el ultimo es la regresion silenciosa de
 *            S7.2.1 —el modelo devolveria {@code GROOMING} y nada mas, y la
 *            regla de fusion borraria las siete lineas del primer turno una a
 *            una— y el copy de la pantalla promete justo lo contrario ("no hace
 *            falta que repitas lo de antes"). Son {@link ProspectText} y no
 *            {@code String} para que ninguna senal de telemetria los imprima
 *            por descuido
 * @param currentCartCodes
 *            los codigos vigentes del carrito, para que el modelo no vuelva a
 *            proponer lo que el cliente quito. Sin ellos la edicion manual no
 *            es soberana: la regla de fusion filtraria igual, pero el modelo
 *            gastaria una linea en cada vuelta
 * @param catalog
 *            la foto del catalogo, que decide que codigos existen
 */
public record ProposalGenerationRequest(List<ProspectText> customerTexts,
        List<String> currentCartCodes, SellableCatalog catalog) {

    public ProposalGenerationRequest {
        if (customerTexts == null || customerTexts.isEmpty())
            throw new IllegalArgumentException("at least one customer text is required");
        if (catalog == null)
            throw new IllegalArgumentException("catalog is required");
        customerTexts = List.copyOf(customerTexts);
        currentCartCodes = currentCartCodes == null ? List.of() : List.copyOf(currentCartCodes);
    }

    /** Los caracteres que se le mandan al modelo. Metrica, no contenido. */
    public int totalChars() {
        return customerTexts.stream().mapToInt(ProspectText::length).sum();
    }
}
