package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.command.SelfServeQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Autocontratacion: la clinica pide su propia oferta y la recibe ya emitida,
 * lista para aceptar.
 *
 * <p>
 * <strong>Por que un puerto nuevo y no abrir
 * {@link CreateQuoteUseCase}.</strong> El javadoc de aquel ya lo dejo escrito y
 * sigue siendo cierto: {@code QuoteLineRequest.discountPercent} viaja en su
 * cuerpo, {@code validUntil} y {@code trialDays} tambien, y {@code freezeLines}
 * pasa el descuento al dominio sin mediar tarifa. Un tenant con ese puerto
 * abierto se cotizaria al 100 % de descuento, con vigencia perpetua y la prueba
 * que quisiera. Y no se arregla con un gate: {@code @PreAuthorize} evalua quien
 * llama, no que trae el cuerpo. Tampoco se arregla validando en el servicio —el
 * mismo {@code record} lo comparten los dos caminos, y el de plataforma
 * <em>necesita</em> esos campos—.
 *
 * <p>
 * La linea que separa los dos puertos es exactamente esa: aqui el cliente elige
 * <strong>que compra y cuanto</strong>, y todo lo que tiene precio lo resuelve
 * el servidor. {@link SelfServeQuoteCommand} no declara ni un solo campo
 * economico, asi que no hay nada que validar ni que olvidarse de validar.
 *
 * <p>
 * <strong>Reutiliza el embudo, no lo duplica.</strong> El servicio compone el
 * {@code CreateQuoteCommand} con los terminos ya resueltos y delega en el
 * camino de plataforma bajo {@code SystemAuthRunner} —el mismo patron con el
 * que {@code registration} acuña el primer contrato de una empresa—. Asi el
 * troceo acumulativo por tramos (D-66), la lista vigente por fecha (D-73), la
 * idempotencia y el rastro de aceptacion ({@code acceptedAt},
 * {@code acceptedByEmail}, {@code acceptedIp}) siguen escritos una sola vez. El
 * rastro de aceptacion es un requisito legal, y reimplementarlo es reconstruir
 * la unica parte delicada.
 *
 * <p>
 * <strong>La devuelve {@code SENT}, no {@code DRAFT}.</strong>
 * {@link SendQuoteUseCase} sigue cerrado a SYSTEM con su motivo intacto —«el
 * cliente no se envia una oferta a si mismo»— y no se toca: quien emite aqui es
 * la plataforma, en respuesta a una peticion. Sin ese paso el tenant se
 * quedaria con un borrador que {@code Quote.accept} rechaza, porque solo se
 * acepta lo que esta {@code SENT}, y el flujo moriria en el ultimo clic.
 *
 * <p>
 * El gate copia la forma de {@link AcceptQuoteUseCase} y de
 * {@code CancelSubscriptionUseCase}: rama de plataforma mas rama de tenant con
 * la empresa revalidada. {@code quote.request} es un permiso nuevo y hay que
 * sembrarlo; sin esa fila, ningun empleado alcanza este puerto.
 *
 * <p>
 * <strong>El articulo se pide por {@code code}, y sin eso este puerto era
 * inalcanzable.</strong> Dos decisiones correctas por separado se anulaban:
 * {@code PublicPlanResponse} no publica ningun id —«un id es una llave de
 * escritura y un {@code code} es un rotulo»— y el unico traductor
 * {@code code -> id}, {@code GET /catalog-items}, esta cerrado a
 * {@code hasRole('SYSTEM')}. Con el id en la linea, la autocontratacion tenia
 * ruta, permiso sembrado y <em>ninguna</em> cadena por la que un empleado del
 * tenant obtuviera los numeros que exigia.
 *
 * <p>
 * La salida no fue publicar los ids en {@code GET /plans} —eso deshace una
 * decision de seguridad tomada a proposito y expone llaves de escritura en la
 * superficie anonima—, sino traducir del lado de quien ya esta autenticado:
 * {@code SelfServeQuoteService} resuelve el rotulo contra
 * {@code PublishedCatalogItemQueryPort}, que solo conoce el mismo conjunto que
 * publica la portada. Y responde lo mismo para un codigo inexistente que para
 * uno interno, o el traductor seria la puerta de atras del listado que
 * {@code SYSTEM} guarda por delante.
 */
public interface SelfServeQuoteUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('quote.request') "
            + "and @authz.isMyCompany(#command.companyId))")
    QuoteDto execute(SelfServeQuoteCommand command);
}
