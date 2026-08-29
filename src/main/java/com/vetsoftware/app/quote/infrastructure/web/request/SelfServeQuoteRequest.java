package com.vetsoftware.app.quote.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Una clinica pide su propia oferta.
 *
 * <p>
 * <b>No lleva companyId</b>, y no puede llevarlo: la empresa la deriva el
 * controller del principal autenticado. Aceptarla en el cuerpo permitiria
 * contratar a nombre de otra clinica, y es lo que la regla
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} rompe el build por hacer.
 *
 * <p>
 * <b>Tampoco lleva ningun termino economico.</b> Compare con
 * {@link CreateQuoteRequest}: alli viajan {@code priceListId},
 * {@code validUntil}, {@code trialDays} y el descuento de cada linea. Aqui no
 * hay ninguno, y no por omision sino por diseno — el precio lo pone el
 * servidor, y la unica forma de garantizarlo es que el cliente no tenga donde
 * ponerlo. Un {@code @PreAuthorize} no puede mirar dentro de un cuerpo.
 *
 * <p>
 * El {@code @Valid} de la lista no es decorativo: sin el, las restricciones de
 * {@link SelfServeQuoteLineRequest} estan escritas y no se evaluan nunca
 * ({@code CUERPO_CON_RESTRICCIONES_SE_VALIDA}).
 *
 * @param billingCycle
 *            {@code MONTHLY} o {@code ANNUAL}. Elegir ciclo es elegir que
 *            columna del catalogo se lee, no cuanto cuesta: cada ciclo lleva su
 *            propio importe en la tarifa. El patron lo rechaza en el borde para
 *            que un valor invalido salga como error de campo y no como un 500
 *            desde el {@code valueOf} del enumerado.
 *            <p>
 *            <strong>El {@code allowableValues} publica esa restriccion en el
 *            contrato, que es donde faltaba.</strong> Springdoc no deriva nada
 *            de un {@code @Pattern}: el esquema anunciaba {@code string} a
 *            secas, asi que los dos fronts ataban su union de dos valores a
 *            mano y ninguna de sus pruebas de contrato podia notar que
 *            apareciera un tercer ciclo. Ahora la lista viaja en el contrato y
 *            la union se genera de ahi.
 *            <p>
 *            <strong>Y sigue siendo {@code String}, no el enumerado.</strong>
 *            Tipar el campo como {@code BillingCycle} obligaria a
 *            {@code infrastructure/web/request} a importar un tipo de dominio,
 *            y sobre todo mete en juego la fusion de esquemas de springdoc, que
 *            agrupa por nombre simple: hay <b>dos</b> enumerados
 *            {@code BillingCycle} en el proyecto —{@code quote.domain} y
 *            {@code pricelist.domain}— y hoy no hay ni un solo esquema con
 *            nombre de enumerado en el contrato. Un {@code String} con la lista
 *            de valores dice exactamente lo mismo sin tocar nada de eso.
 * @param clientRequestId
 *            llave de idempotencia que genera el cliente. Es lo que hace que un
 *            doble clic en «Confirmar» no cree dos ofertas.
 */
public record SelfServeQuoteRequest(@NotBlank @Size(min = 1, max = 64) String clientRequestId,
        @NotBlank @Pattern(regexp = "MONTHLY|ANNUAL") @Schema(allowableValues = {
                "MONTHLY", "ANNUAL"}) String billingCycle,
        @NotEmpty @Valid List<SelfServeQuoteLineRequest> lines) {
}
