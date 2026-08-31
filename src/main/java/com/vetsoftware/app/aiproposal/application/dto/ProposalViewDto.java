package com.vetsoftware.app.aiproposal.application.dto;

import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.PackComparisonResult;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * La propuesta tal como la ve el prospecto, y <strong>lo unico</strong> que
 * sale por los cuatro endpoints.
 *
 * @param discardedLines
 *            cuantas lineas no se pudieron cotizar, y nada mas: ni codigos, ni
 *            veredictos, ni desglose. Va porque la pantalla necesita poder
 *            decir "no todo lo que propusimos se puede contratar", no para
 *            depurar. &#9940; <strong>No distingue causas a proposito</strong>:
 *            un contador que valiera 1 solo para el codigo inventado seria el
 *            mismo oraculo con menos ruido
 * @param recommendations
 *            el bloque "tambien podria interesarte". <strong>No suma al
 *            total</strong> y llega sin marcar: fundirlo en el carrito es
 *            exactamente el patron oscuro que S1.5 prohibe -el prompt sesga
 *            hacia recomendar de mas, y la interfaz las presentaria ya
 *            elegidas-
 * @param packOffer
 *            la comparacion de paquete, con su ahorro <em>y</em> su coste en
 *            dias de prueba. Se muestra, nunca se sustituye
 * @param refinementsLeft
 *            cuantos ajustes le quedan. Al cuarto se devuelve 200 con la
 *            propuesta intacta y {@code recalculated = false}, nunca un 400
 * @param version
 *            el bloqueo optimista que serializa a dos pestanas
 */
public record ProposalViewDto(String publicToken, ProposalPresentation presentation,
        LocalDateTime expiresAt, Long version, List<ProposalLineDto> lines,
        List<ProposalLineDto> recommendations, int discardedLines, String currency,
        BigDecimal subtotal, BigDecimal taxes, BigDecimal total, BigDecimal firstPeriodTotal,
        PackComparisonResult packOffer, int refinementsLeft, boolean recalculated) {

    public ProposalViewDto {
        lines = lines == null ? List.of() : List.copyOf(lines);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        if (discardedLines < 0)
            throw new IllegalArgumentException("discardedLines cannot be negative");
        if (refinementsLeft < 0)
            throw new IllegalArgumentException("refinementsLeft cannot be negative");
    }

    /** La vista de un carrito ya construido y cotizado. */
    public static ProposalViewDto de(String publicToken, ProposalPresentation presentation,
            LocalDateTime expiresAt, Long version, CartResult carrito,
            Optional<PackComparisonResult> oferta, int refinementsLeft, boolean recalculated) {
        return new ProposalViewDto(publicToken, presentation, expiresAt, version,
                carrito.aceptadas().stream().map(ProposalLineDto::from).toList(),
                carrito.recomendaciones().stream().map(ProposalLineDto::from).toList(),
                carrito.descartadas(), carrito.currency(), carrito.subtotal(), carrito.impuestos(),
                carrito.total(), carrito.totalPrimerPeriodo(), oferta.orElse(null), refinementsLeft,
                recalculated);
    }

    /**
     * La respuesta cuando no hay tarifa publicada y por tanto no hay nada que
     * cotizar. Es un estado normal del catalogo -{@code 310} siembra la lista en
     * {@code DRAFT} y {@code 311} solo la publica si existe una cuenta de sistema
     * real-, asi que se responde 200 con la propuesta vacia y sin token: no se
     * persistio nada que releer.
     *
     * <p>
     * &#9940; <strong>{@link ProposalPresentation#NO_CATALOG} y NO
     * {@code DETERMINISTIC}, que es lo que devolvia hasta hoy.</strong> Por este
     * camino no corrio ni el determinista ni el modelo —{@code
     * GenerateProposalService.generate} vuelve en sus dos returns tempranos, antes
     * del generador—, asi que anunciar la pantalla determinista era decir «hubo
     * degradacion del modelo y estas son sus lineas» cuando no hay ni motor que
     * degradar ni una sola linea. Un mismo valor con dos lecturas incompatibles
     * hace exactamente lo que hizo: mandar el diagnostico a buscar el modelo caido
     * en vez de la tarifa sin publicar.
     */
    public static ProposalViewDto sinCatalogo() {
        return new ProposalViewDto(null, ProposalPresentation.NO_CATALOG, null, null, List.of(),
                List.of(), 0, null, null, null, null, null, null, 0, false);
    }
}
