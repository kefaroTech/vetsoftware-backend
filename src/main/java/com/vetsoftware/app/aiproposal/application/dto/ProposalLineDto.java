package com.vetsoftware.app.aiproposal.application.dto;

import com.vetsoftware.app.aiproposal.domain.CartLine;
import java.math.BigDecimal;

/**
 * Una linea <strong>aceptada</strong>, que es la unica clase de linea que sale
 * por HTTP.
 *
 * <p>
 * &#9940; <strong>Sin {@code verdict} y sin {@code source}.</strong> Los cinco
 * veredictos se persisten y son la senal con la que se mide si el modelo sirve,
 * pero este endpoint es <em>driveable</em>: el texto de entrada lo escribe
 * quien pregunta, asi que bastaria meter un codigo sonda en la descripcion
 * -"tambien quiero PACK_ENTERPRISE_2027"- para que el modelo lo repita y el
 * servidor conteste, linea a linea, si ese codigo existe, si existe pero esta
 * en borrador, si existe y esta retirado o si existe y no se vende por
 * autoservicio. Cinco veredictos distinguibles son un oraculo de cinco valores
 * sobre el catalogo interno, que es exactamente lo que
 * {@code ARTICULO_NO_CONTRATABLE} costo cerrar.
 *
 * <p>
 * <strong>Con divisa, y no como adorno.</strong> 52 de 53 DTO de dinero de este
 * backend no la llevan; un importe sin divisa en la pantalla que decide una
 * compra es una cifra que el lector interpreta.
 */
public record ProposalLineDto(String code, String name, String description, String kind,
        int quantity, BigDecimal unitAmount, BigDecimal taxRate, BigDecimal taxAmount,
        BigDecimal totalAmount, int trialDays, String currency, String reason) {

    public static ProposalLineDto from(CartLine linea) {
        return new ProposalLineDto(linea.code(), linea.name(), linea.shortDescription(),
                linea.kind() == null ? null : linea.kind().name(), linea.quantity(),
                linea.unitAmount(), linea.taxRate(), linea.impuesto(), linea.totalConImpuesto(),
                linea.trialDays(), linea.currency(), linea.reason());
    }
}
