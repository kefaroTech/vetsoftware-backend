package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.SelfServeQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.PlatformQuoteIssuerPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.domain.PriceListRef;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final PlatformQuoteIssuerPort issuer;
    private final PriceListQueryPort priceListQueryPort;
    private final Clock clock;

    public SelfServeQuoteService(PlatformQuoteIssuerPort issuer,
            PriceListQueryPort priceListQueryPort, Clock clock) {
        this.issuer = issuer;
        this.priceListQueryPort = priceListQueryPort;
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

        return issuer.issue(new CreateQuoteCommand(command.clientRequestId(), command.companyId(),
                null, null, null, null, tarifa.id(), command.billingCycle(),
                today.plusDays(VIGENCIA_DIAS), SIN_PRUEBA_EN_CABECERA, toLineCommands(command),
                List.of()));
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
     * El puente entre el command estrecho y el ancho. Aqui es donde el descuento se
     * escribe en cero: es el <em>unico</em> sitio del camino de autoservicio donde
     * ese campo llega a existir, y llega ya sin valor posible desde arriba.
     */
    private static List<QuoteLineCommand> toLineCommands(SelfServeQuoteCommand command) {
        return command.lines() == null
                ? List.of()
                : command.lines().stream().map(linea -> new QuoteLineCommand(linea.catalogItemId(),
                        linea.quantity(), BigDecimal.ZERO)).toList();
    }
}
