package com.vetsoftware.app.aiproposal.infrastructure.web.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * La oferta de paquete, con <strong>las dos dimensiones</strong>.
 *
 * <p>
 * &#9940; <strong>Se compara, nunca se sustituye.</strong> Los tres paquetes
 * son {@code NEVER_FREE} y 11 de los 13 modulos dan 14 o 30 dias de prueba:
 * cambiar en silencio ahorra 35.000 al mes y le quita al cliente ~164.500 del
 * primer mes, mientras toda la landing promete "prueba gratis, sin tarjeta".
 * Por eso {@code trialDaysLost} y {@code modulesLosingTrial} viajan al lado del
 * ahorro y no como letra pequena: sin ellos la tarjeta seria un patron oscuro
 * con apariencia de favor.
 */
public record AssistantPackOfferResponse(String packCode, String packName, BigDecimal packAmount,
        BigDecimal standaloneTotal, BigDecimal monthlySaving, String currency, int trialDaysLost,
        List<String> modulesLosingTrial) {
}
