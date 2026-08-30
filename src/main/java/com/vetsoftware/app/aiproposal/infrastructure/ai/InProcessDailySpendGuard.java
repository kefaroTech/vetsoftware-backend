package com.vetsoftware.app.aiproposal.infrastructure.ai;

import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El tope de gasto, contado en memoria del proceso.
 *
 * <p>
 * <strong>Los numeros salen del presupuesto dividido entre 30, no al
 * reves</strong> (plan S6.3): USD 0,33/dia en dev y USD 1,33/dia en prod. Si se
 * sube uno hay que subir el otro en el mismo PR, o el control que corta y el
 * que avisa dejan de hablar del mismo sistema. El valor por defecto es el de
 * dev, que es el lado seguro: un despliegue sin configurar gasta poco, no
 * mucho.
 *
 * <p>
 * ⚠️ <strong>LIMITE CONOCIDO Y DECLARADO: el contador es por proceso.</strong>
 * Con N tareas de ECS el techo efectivo es N veces el configurado, porque cada
 * una lleva su propio acumulador. El plan especifica Valkey con
 * {@code INCRBYFLOAT}, que es lo unico que da un contador global, y esta clase
 * es la implementacion que se puede escribir y probar hoy — no la definitiva.
 * <strong>Se declara aqui y no se calla</strong> porque un techo que se cree
 * global y no lo es es peor que ninguno: da la confianza sin dar la proteccion.
 * Aun asi acota: hoy no hay ningun techo, y con este el peor caso pasa de "sin
 * limite" a "N x el limite", que con la topologia actual es una cota conocida.
 *
 * <p>
 * <strong>Fail-closed de verdad.</strong> Cualquier camino que no pueda afirmar
 * que queda cupo devuelve vacio, incluida una estimacion nula o negativa: un
 * coste que no se sabe calcular no se puede acotar.
 *
 * <p>
 * <strong>Y es el unico sitio desde el que se puede medir el gasto.</strong> El
 * contador se incrementa en {@link #reconcile}, que es el punto por el que
 * pasan <em>todos</em> los cargos —tambien el del intento que fallo despues de
 * pagar, donde no hay {@code ModelUsage} que leer—. Emitirlo desde el caso de
 * uso dejaria fuera precisamente ese, y la metrica diria menos que la factura
 * de AWS, que es la unica forma de equivocarse que aqui importa.
 *
 * <p>
 * <strong>Un contador acumulativo y un medidor del dia, no uno solo.</strong>
 * El contador responde «cuanto llevamos gastado» y sobrevive a un envio OTLP
 * perdido, porque la agregacion de Micrometer es acumulativa y el scrape
 * siguiente trae el total. El medidor responde «cuanto queda del cupo de hoy» y
 * se reinicia solo al rotar el dia, que es justo lo que el contador no puede
 * hacer. Son dos preguntas distintas y ninguna de las dos sustituye a la otra.
 */
@Component
public class InProcessDailySpendGuard implements SpendGuardPort {

    private static final Logger log = LoggerFactory.getLogger(InProcessDailySpendGuard.class);

    private final Clock clock;

    private final BigDecimal dailyCapUsd;

    private final Counter gastoAcumulado;

    /** Protegido por {@code this}: el dia y el gastado se mueven juntos. */
    private LocalDate diaEnCurso;

    private BigDecimal gastadoHoy = BigDecimal.ZERO;

    /**
     * El dia en el que ya se aviso de que el cupo se agoto.
     *
     * <p>
     * &#9940; <strong>Sin esta guarda el aviso es una tormenta.</strong> Con el
     * tope de dev -USD 0,33- bastan unas veinte llamadas para agotarlo, y a partir
     * de ahi <em>cada</em> peticion de un endpoint publico escribiria un
     * {@code WARN} identico hasta medianoche. Un canal que grita todos los dias es
     * un canal que se deja de mirar, y entonces el aviso que si importa se pierde
     * con el resto. Quien despierta a alguien es la alerta sobre el contador de
     * gasto, no este evento.
     */
    private LocalDate diaDelAviso;

    public InProcessDailySpendGuard(Clock clock, MeterRegistry registry,
            @Value("${vetsoftware.ai.proposal.daily-spend-cap-usd:0.33}") BigDecimal dailyCapUsd) {
        this.clock = clock;
        this.dailyCapUsd = dailyCapUsd == null || dailyCapUsd.signum() < 0
                ? BigDecimal.ZERO
                : dailyCapUsd;
        this.diaEnCurso = LocalDate.now(clock);
        // Se registra de una vez y no por MeterProvider: no lleva etiquetas, asi que
        // no hay nada que diferir, y pre-registrarlo a cero es lo que hace que una
        // alerta increase(...) > 0 funcione desde el primer scrape en vez de depender
        // de que la serie nazca justo durante el incidente.
        this.gastoAcumulado = Counter.builder(BusinessMetricNames.AI_PROPOSAL_SPEND).baseUnit("usd")
                .description("Gasto acumulado estimado del asistente; es una estimacion y no"
                        + " sustituye la factura de AWS")
                .register(registry);
        Gauge.builder(BusinessMetricNames.AI_PROPOSAL_SPEND_TODAY, this,
                guarda -> guarda.spentToday().doubleValue()).baseUnit("usd")
                .description("Gasto estimado del dia en curso, contra el que se aplica el tope")
                .register(registry);
    }

    @Override
    public synchronized Optional<SpendReservation> reserve(BigDecimal estimatedUsd) {
        if (estimatedUsd == null || estimatedUsd.signum() <= 0) {
            // ERROR y no WARN: no es una anomalia aislada de esta peticion sino un
            // defecto determinista del llamante que va a rechazar el 100 % de las
            // reservas hasta que alguien cambie el codigo. Nada lo reintenta y nada
            // lo recupera; el producto queda vendiendo sin IA sin que nadie lo sepa.
            log.error("Reserva rechazada: la estimacion de coste no es utilizable, asi que el"
                    + " asistente no invocara al modelo ni una vez hasta que se corrija");
            return Optional.empty();
        }
        rotarSiCambioElDia();
        BigDecimal despues = gastadoHoy.add(estimatedUsd);
        if (despues.compareTo(dailyCapUsd) > 0) {
            avisarUnaVezPorDia();
            return Optional.empty();
        }
        gastadoHoy = despues;
        return Optional.of(new SpendReservation(UUID.randomUUID().toString(), estimatedUsd));
    }

    /**
     * El ajuste puede ser negativo —la estimacion casi siempre sobra— y nunca deja
     * el acumulado por debajo de cero. Si el real supera al estimado, el exceso se
     * carga: el gasto ocurrio.
     */
    @Override
    public synchronized void reconcile(SpendReservation reservation, BigDecimal actualUsd) {
        if (reservation == null)
            return;
        rotarSiCambioElDia();
        BigDecimal real = actualUsd == null || actualUsd.signum() < 0 ? BigDecimal.ZERO : actualUsd;
        gastadoHoy = maximoConCero(gastadoHoy.subtract(reservation.reservedUsd()).add(real));
        gastoAcumulado.increment(real.doubleValue());
    }

    @Override
    public synchronized void release(SpendReservation reservation) {
        if (reservation == null)
            return;
        rotarSiCambioElDia();
        gastadoHoy = maximoConCero(gastadoHoy.subtract(reservation.reservedUsd()));
    }

    /** Lo gastado hoy, para la metrica y para la alarma del 70 %. */
    public synchronized BigDecimal spentToday() {
        rotarSiCambioElDia();
        return gastadoHoy;
    }

    /**
     * El dia se compara contra el {@link Clock} inyectado, no contra
     * {@code LocalDate.now()}: el caso que cruza medianoche solo aparece en CI y de
     * noche, y {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} esta congelada sobre el arbol
     * entero.
     */
    /**
     * El aviso de cupo agotado, una vez por dia y sin el importe: cuanto falta para
     * el tope es justo el dato que necesita quien lo esta vaciando a proposito.
     */
    private void avisarUnaVezPorDia() {
        if (diaEnCurso.equals(diaDelAviso)) {
            return;
        }
        diaDelAviso = diaEnCurso;
        log.warn("Tope de gasto diario del asistente agotado; las propuestas salen por el camino"
                + " determinista hasta manana. Se avisa una sola vez al dia; el recuento vive en"
                + " ai_proposal_generated_total con ai_outcome=degraded_spend_cap");
    }

    private void rotarSiCambioElDia() {
        LocalDate hoy = LocalDate.now(clock);
        if (!hoy.equals(diaEnCurso)) {
            diaEnCurso = hoy;
            gastadoHoy = BigDecimal.ZERO;
        }
    }

    private static BigDecimal maximoConCero(BigDecimal valor) {
        return valor.signum() < 0 ? BigDecimal.ZERO : valor;
    }
}
