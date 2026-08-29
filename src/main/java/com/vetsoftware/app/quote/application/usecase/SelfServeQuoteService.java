package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.SelfServeQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.PlatformQuoteIssuerPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.PublishedCatalogItemQueryPort;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.PriceListRef;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve en servidor <strong>todo</strong> lo que tiene precio, y delega el
 * resto en el embudo que ya existe.
 *
 * <p>
 * Este archivo es el gate de verdad. El {@code @PreAuthorize} del puerto dice
 * <em>quien</em> puede pedir una oferta; lo que impide que esa oferta sea
 * regalada lo decide esta clase, porque es la unica que escribe los terminos.
 * Punto por punto, y cada uno con el abuso concreto que cierra:
 *
 * <ul>
 * <li><b>La tarifa.</b> No llega del cuerpo: se elige la lista
 * {@code PUBLISHED} <em>vigente hoy</em>. Si viniera del cliente, bastaria
 * apuntar a una lista del ano pasado que nadie archivo para contratarse al
 * precio viejo — que es el abuso de D-73 visto desde el otro lado.</li>
 * <li><b>El descuento.</b> Siempre cero, y no por convenio:
 * {@link SelfServeQuoteLineCommand} no tiene donde llevarlo. Un descuento es
 * una negociacion, y nadie negocia consigo mismo.</li>
 * <li><b>Que articulos son contratables.</b> Ver
 * {@link #traducirCodigos(SelfServeQuoteCommand, PriceListRef, BillingCycle)}:
 * el cliente nombra rotulos y el servidor decide si existen. Un rotulo que la
 * portada no publica no llega ni a mirarse contra el catalogo.</li>
 * <li><b>Los importes, el IVA y los tramos.</b> Los congela
 * {@code CreateQuoteService} contra el catalogo, igual que en el camino de
 * plataforma. No se tocan aqui, ni podrian: el cliente no manda ningun
 * importe.</li>
 * <li><b>La vigencia.</b> {@link #VIGENCIA_DIAS} desde hoy. Del cuerpo saldria
 * una oferta perpetua.</li>
 * <li><b>Los dias de prueba.</b> Cero en la cabecera, y es deliberado: ver mas
 * abajo.</li>
 * <li><b>La empresa.</b> La pone el controller desde el principal y el puerto
 * la revalida. No viaja en el cuerpo
 * ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}).</li>
 * </ul>
 *
 * <p>
 * <strong>Por que {@code trialDays = 0} y no un numero.</strong> La prueba de
 * este producto <em>vence por linea, no por contrato</em>: cada
 * {@code ModuleGrantLine} lleva su propio {@code trialEndDate} y
 * {@code catalog_items.default_trial_days} es por articulo, asi que Caja puede
 * terminar el dia 14 y Agenda el 30 dentro del mismo contrato. Rellenar el
 * entero de la cabecera con «el maximo» regalaria de mas y con «el minimo» de
 * menos, y las dos versiones ponen por escrito una promesa plana que el modelo
 * no tiene — que es exactamente el error que hace que un cliente descubra el
 * dia 14 que le estan cobrando algo que creia gratis. Cero significa «esta
 * cabecera no promete prueba»; la prueba real la concede el camino de contrato,
 * articulo a articulo, y {@code GET /plans} la publica por linea.
 *
 * <p>
 * <strong>Lo que este caso de uso NO hace.</strong> Deja una oferta emitida y
 * lista para aceptar. Aceptarla es {@code AcceptQuoteUseCase}, que ya admite al
 * tenant. Pero <strong>aceptar no crea el contrato</strong>: nadie reacciona
 * hoy a {@code QuoteStatus.ACCEPTED}, asi que los modulos no se encienden
 * solos. Ese eslabon —cotizacion aceptada a suscripcion mas concesiones— no
 * existe y no se inventa aqui.
 */
@Observed(name = "quote.selfserve")
@Service
public class SelfServeQuoteService implements SelfServeQuoteUseCase {

    /**
     * Lo que dura una oferta de autoservicio. Suficiente para pensarlo un fin de
     * semana y corto para que el precio que se acepta siga siendo el vigente.
     */
    private static final int VIGENCIA_DIAS = 15;

    /** Ver el javadoc de la clase: la prueba vence por linea, no por contrato. */
    private static final int SIN_PRUEBA_EN_CABECERA = 0;

    /**
     * <b>Un solo texto para todos los rechazos de un rotulo, y es una decision de
     * seguridad, no pereza.</b> «No existe», «esta en borrador», «se retiro de la
     * venta», «es un cargo unico que la portada no anuncia» y «no esta tarifado en
     * este ciclo» tienen que salir <em>indistinguibles</em>: en cuanto el mensaje
     * separa el codigo desconocido del codigo interno, este endpoint pasa a
     * responder la pregunta «¿existe el articulo X?» a cualquiera con
     * {@code quote.request}, que es exactamente lo que {@code GET /catalog-items}
     * evita cerrandose a {@code SYSTEM}.
     *
     * <p>
     * <b>Tampoco lleva eco del codigo recibido</b>, para que el texto sea el mismo
     * byte a byte en todos los casos y la propiedad se pueda comprobar en un test
     * sin trucos de normalizacion. Quien llama sabe que codigos mando: todos
     * salieron de {@code GET /plans}, y que uno falle significa «el catalogo
     * cambio, vuelve a leer los planes».
     */
    private static final String ARTICULO_NO_CONTRATABLE = "Unknown or unavailable catalog item code";

    private final PlatformQuoteIssuerPort issuer;
    private final PriceListQueryPort priceListQueryPort;
    private final PublishedCatalogItemQueryPort publishedCatalogItemQueryPort;
    private final Clock clock;

    public SelfServeQuoteService(PlatformQuoteIssuerPort issuer,
            PriceListQueryPort priceListQueryPort,
            PublishedCatalogItemQueryPort publishedCatalogItemQueryPort, Clock clock) {
        this.issuer = issuer;
        this.priceListQueryPort = priceListQueryPort;
        this.publishedCatalogItemQueryPort = publishedCatalogItemQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public QuoteDto execute(SelfServeQuoteCommand command) {
        // El dia sale del reloj inyectado, que es el unico que lleva la zona del
        // negocio (D-81): un LocalDate.now() pelado contesta manana entre las 19:00 y
        // la medianoche y rechazaria una contratacion legitima el ultimo dia de la
        // tarifa.
        LocalDate today = LocalDateTime.now(clock).toLocalDate();
        PriceListRef tarifa = tarifaVigente(today).orElseThrow(() -> new IllegalStateException(
                "No published price list is effective on " + today));
        BillingCycle ciclo = parseBillingCycle(command.billingCycle());

        return issuer.issue(new CreateQuoteCommand(command.clientRequestId(), command.companyId(),
                null, null, null, null, tarifa.id(), command.billingCycle(),
                today.plusDays(VIGENCIA_DIAS), SIN_PRUEBA_EN_CABECERA,
                traducirCodigos(command, tarifa, ciclo), List.of()));
    }

    /**
     * De las publicadas, la vigente hoy; y si hay varias solapadas —el esquema no
     * lo impide— gana la de {@code validFrom} mas reciente, con el id como
     * desempate. Determinista, en vez de «la primera que devuelva la consulta».
     */
    private Optional<PriceListRef> tarifaVigente(LocalDate today) {
        return priceListQueryPort.findAllPublished().stream()
                .filter(lista -> lista.isEffectiveOn(today))
                .max(Comparator.comparing(PriceListRef::validFrom).thenComparing(PriceListRef::id));
    }

    /**
     * El puente entre el command estrecho y el ancho, y <b>el sitio donde el id del
     * catalogo llega a existir en este camino</b>.
     *
     * <p>
     * Dos cosas pasan aqui, y las dos son de seguridad:
     *
     * <ul>
     * <li><b>El descuento se escribe en cero.</b> Es el <em>unico</em> punto del
     * camino de autoservicio donde ese campo llega a existir, y llega ya sin valor
     * posible desde arriba: {@link SelfServeQuoteLineCommand} no lo declara.</li>
     * <li><b>El rotulo se traduce a id contra el conjunto publicado.</b> No contra
     * {@code catalog_items} entero: el puerto resuelve exactamente lo mismo que
     * devuelve {@code GET /plans} para esta tarifa y este ciclo. Un rotulo de fuera
     * de ese conjunto —inexistente, en borrador, retirado, o un {@code ONE_TIME}
     * que la portada no anuncia— se rechaza igual y con el mismo texto,
     * {@link #ARTICULO_NO_CONTRATABLE}. Sin esa igualdad, el traductor seria el
     * oraculo que enumera el catalogo interno.</li>
     * </ul>
     *
     * <p>
     * El {@code code} identifica sin ambiguedad: {@code uq_catalog_items_code}
     * (changeset 229) es {@code UNIQUE} global sobre la columna, asi que no hay
     * desempate que decidir ni criterio que documentar. Si algun dia esa constraint
     * se relajara a «unico por tipo» o «unico por lista», este metodo empezaria a
     * elegir en silencio y habria que volver aqui.
     */
    private List<QuoteLineCommand> traducirCodigos(SelfServeQuoteCommand command,
            PriceListRef tarifa, BillingCycle ciclo) {
        if (command.lines() == null) {
            return List.of();
        }
        List<QuoteLineCommand> lineas = new ArrayList<>();
        for (SelfServeQuoteLineCommand linea : command.lines()) {
            Long catalogItemId = publishedCatalogItemQueryPort
                    .findPublishedIdByCode(linea.code(), tarifa.id(), ciclo)
                    .orElseThrow(() -> new IllegalArgumentException(ARTICULO_NO_CONTRATABLE));
            lineas.add(new QuoteLineCommand(catalogItemId, linea.quantity(), BigDecimal.ZERO));
        }
        return List.copyOf(lineas);
    }

    /**
     * El ciclo llega como texto porque {@link SelfServeQuoteCommand} lo comparte
     * con el camino de plataforma, pero aqui hace falta el enumerado: es una de las
     * tres coordenadas con las que se busca el precio de entrada del articulo. El
     * {@code @Pattern} del request ya lo acota en el borde REST; esta comprobacion
     * cubre la llamada directa al puerto, que {@code SYSTEM} tambien puede hacer.
     */
    private static BillingCycle parseBillingCycle(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("billingCycle is required");
        }
        try {
            return BillingCycle.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown billingCycle: " + raw);
        }
    }
}
