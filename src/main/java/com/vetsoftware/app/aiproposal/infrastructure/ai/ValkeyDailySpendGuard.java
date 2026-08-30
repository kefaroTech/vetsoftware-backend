package com.vetsoftware.app.aiproposal.infrastructure.ai;

import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * El tope de gasto diario del modelo, contado <strong>una sola vez para toda la
 * plataforma</strong>.
 *
 * <p>
 * &#9940; <strong>Esto es lo que {@link InProcessDailySpendGuard} no podia
 * ser.</strong> Aquel acumula en un campo del proceso, asi que con N tareas de
 * ECS el techo efectivo es N veces el configurado: cada tarea cree tener el
 * cupo entero. Un techo que se cree global y no lo es es peor que ninguno —da
 * la confianza sin dar la proteccion— y ademas escala con el autoescalado, o
 * sea justo cuando mas trafico hay. El contador vive ahora en Valkey, que es
 * donde el plan (S6.3) dijo desde el principio que tenia que vivir.
 *
 * <p>
 * <strong>{@code INCRBYFLOAT} y no {@code GET} + {@code SET}.</strong> Es
 * atomico en el servidor: N reservas simultaneas se serializan y cada una ve el
 * total de las anteriores. Con leer-sumar-escribir volveria la misma ventana
 * que el contador por proceso, solo que repartida entre tareas.
 *
 * <p>
 * <strong>Reserva primero, comprueba despues, y devuelve lo que sobra.</strong>
 * Se suma el estimado y, si el resultado pasa del tope, se resta y se responde
 * que no. Al reves —comprobar y luego sumar— deja el hueco clasico en el que N
 * llamadas concurrentes leen el contador por debajo del tope y pasan todas. El
 * coste de hacerlo asi es que una reserva rechazada toca dos veces la clave; a
 * cambio, el tope se cumple de verdad.
 *
 * <p>
 * <strong>Fail-closed, y aqui eso incluye a Valkey.</strong> Cualquier fallo
 * del cliente —caida, timeout, respuesta ilegible— responde que NO hay cupo. Es
 * la decision incomoda y es la correcta: degradar de mas cuesta una propuesta
 * determinista, que es una propuesta correcta; degradar de menos cuesta dinero
 * real y sin techo, sobre un endpoint anonimo. {@link #reconcile} y
 * {@link #release}, en cambio, <strong>nunca lanzan</strong>: se ejecutan
 * cuando la llamada ya se hizo y el gasto ya ocurrio, asi que convertir su
 * fallo en un error del usuario no devolveria el dinero y si tumbaria una
 * respuesta buena.
 *
 * <p>
 * <strong>Una clave por dia, con TTL.</strong> {@code ai:spend:AAAA-MM-DD} en
 * la zona del negocio, que sale del {@link Clock} inyectado y no de
 * {@code LocalDate.now()}: el corte de medianoche solo aparece de noche y en
 * CI. El TTL de dos dias hace la rotacion sola —no hace falta ningun barrido— y
 * deja margen para leer el dia anterior si alguien lo necesita.
 *
 * <p>
 * <strong>Se reutiliza la conexion que ya existe.</strong> El bean
 * {@code StatefulRedisConnection<String, byte[]>} lo publica la configuracion
 * del rate limiting y se inyecta <em>por tipo</em>, sin importar ni una clase
 * de otra rodaja. Abrir una segunda conexion a Valkey para esto seria un socket
 * mas y un modo de fallo mas por cada tarea, para el mismo servidor.
 */
@Component
@ConditionalOnProperty(name = "vetsoftware.ai.proposal.spend-guard", havingValue = "valkey", matchIfMissing = true)
public class ValkeyDailySpendGuard implements SpendGuardPort {

    static final String PREFIJO = "ai:spend:";

    /**
     * Dos dias: la clave del dia rota sola y queda margen para consultar la
     * vispera. Menos de un dia arriesga expirar el contador en curso si el reloj
     * del servidor y el de la aplicacion no coinciden del todo.
     */
    static final long TTL_SEGUNDOS = 2L * 24 * 60 * 60;

    private static final DateTimeFormatter DIA = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Logger log = LoggerFactory.getLogger(ValkeyDailySpendGuard.class);

    private final StatefulRedisConnection<String, byte[]> connection;

    private final Clock clock;

    private final BigDecimal dailyCapUsd;

    private final Counter gastoAcumulado;

    /**
     * El dia en el que ya se aviso de que el cupo se agoto. Sin esta guarda, con el
     * tope de dev —USD 0,33— <em>cada</em> peticion de un endpoint publico
     * escribiria el mismo {@code WARN} hasta medianoche, y un canal que grita todos
     * los dias se deja de mirar. Es por proceso a proposito: coordinarlo entre
     * tareas costaria otra clave y otra ida a Valkey para no repetir un log.
     */
    private LocalDate diaDelAviso;

    /**
     * &#9940; <strong>El defecto del tope, extraido a constante para poder
     * atarlo.</strong> {@code LoginRateLimitFilter} deriva de este mismo tope
     * cuantas peticiones de pago permite por IP, y lee la clave con su propio
     * {@code @Value}: si los dos defectos se separan, el filtro calibra su limite
     * contra un presupuesto que no es el que se aplica —que es exactamente la clase
     * de desalineacion que costo tener 20 peticiones por IP contra un tope que
     * financiaba 18—. {@code LoginRateLimitFilterTest} comprueba que coinciden.
     */
    public static final String DEFECTO_TOPE_DIARIO_USD = "0.33";

    public ValkeyDailySpendGuard(StatefulRedisConnection<String, byte[]> rateLimitRedisConnection,
            Clock clock, MeterRegistry registry,
            @Value("${vetsoftware.ai.proposal.daily-spend-cap-usd:" + DEFECTO_TOPE_DIARIO_USD
                    + "}") BigDecimal dailyCapUsd) {
        this.connection = rateLimitRedisConnection;
        this.clock = clock;
        this.dailyCapUsd = dailyCapUsd == null || dailyCapUsd.signum() < 0
                ? BigDecimal.ZERO
                : dailyCapUsd;
        // Pre-registrado a cero para que una alerta increase(...) > 0 funcione desde
        // el primer scrape, en vez de depender de que la serie nazca justo durante el
        // incidente.
        this.gastoAcumulado = Counter.builder(BusinessMetricNames.AI_PROPOSAL_SPEND).baseUnit("usd")
                .description("Gasto acumulado estimado del asistente; es una estimacion y no"
                        + " sustituye la factura de AWS")
                .register(registry);
        Gauge.builder(BusinessMetricNames.AI_PROPOSAL_SPEND_TODAY, this,
                guarda -> guarda.spentToday().doubleValue()).baseUnit("usd")
                .description("Gasto estimado del dia en curso en TODA la plataforma, contra el"
                        + " que se aplica el tope")
                .register(registry);
    }

    @Override
    public Optional<SpendReservation> reserve(BigDecimal estimatedUsd) {
        if (estimatedUsd == null || estimatedUsd.signum() <= 0) {
            // ERROR y no WARN: no es una anomalia de esta peticion sino un defecto
            // determinista del llamante que rechazara el 100 % de las reservas hasta que
            // alguien cambie el codigo. El producto queda vendiendo sin IA sin que nadie
            // lo sepa.
            log.error("Reserva rechazada: la estimacion de coste no es utilizable, asi que el"
                    + " asistente no invocara al modelo ni una vez hasta que se corrija");
            return Optional.empty();
        }
        String clave = claveDelDia();
        double estimado = estimatedUsd.doubleValue();
        Double despues;
        try {
            RedisCommands<String, byte[]> comandos = connection.sync();
            despues = comandos.incrbyfloat(clave, estimado);
            comandos.expire(clave, TTL_SEGUNDOS);
        } catch (RuntimeException fallo) {
            // Fail-closed: sin contador no se puede afirmar que quede cupo.
            log.warn("No se pudo reservar cupo de gasto en Valkey; el asistente degrada al camino"
                    + " determinista: {}", fallo.getMessage());
            return Optional.empty();
        }
        if (despues == null || despues > dailyCapUsd.doubleValue()) {
            devolver(estimado);
            avisarUnaVezPorDia();
            return Optional.empty();
        }
        return Optional.of(new SpendReservation(UUID.randomUUID().toString(), estimatedUsd));
    }

    /**
     * El ajuste puede ser negativo —la estimacion casi siempre sobra— y si el real
     * supera al estimado el exceso se carga: el gasto ocurrio.
     *
     * <p>
     * <strong>No se acota a cero.</strong> El contador por proceso podia hacerlo
     * porque era suyo; aqui la clave es compartida y leerla para corregirla
     * reintroduciria la carrera que {@code INCRBYFLOAT} evita. Un delta negativo
     * solo puede acercar el contador a lo realmente gastado, que es lo que se
     * quiere.
     */
    @Override
    public void reconcile(SpendReservation reservation, BigDecimal actualUsd) {
        if (reservation == null)
            return;
        BigDecimal real = actualUsd == null || actualUsd.signum() < 0 ? BigDecimal.ZERO : actualUsd;
        ajustar(real.subtract(reservation.reservedUsd()).doubleValue(),
                "reconciliar el gasto real");
        gastoAcumulado.increment(real.doubleValue());
    }

    @Override
    public void release(SpendReservation reservation) {
        if (reservation == null)
            return;
        ajustar(-reservation.reservedUsd().doubleValue(), "devolver una reserva sin usar");
    }

    /** Lo gastado hoy en toda la plataforma, para la metrica y para la alarma. */
    public BigDecimal spentToday() {
        try {
            byte[] valor = connection.sync().get(claveDelDia());
            if (valor == null || valor.length == 0)
                return BigDecimal.ZERO;
            return new BigDecimal(new String(valor, StandardCharsets.UTF_8));
        } catch (RuntimeException fallo) {
            // El medidor no puede tumbar un scrape: cero es «no se sabe», y quien corta
            // es reserve, que si es fail-closed.
            return BigDecimal.ZERO;
        }
    }

    private void devolver(double estimado) {
        ajustar(-estimado, "devolver una reserva rechazada");
    }

    /**
     * El ajuste posterior a la invocacion. <strong>Nunca lanza</strong>: el gasto
     * ya ocurrio y convertir su fallo en un error del usuario no lo devolveria. Se
     * registra en {@code WARN} porque el contador queda alto y eso degrada de mas,
     * que es el lado seguro.
     */
    private void ajustar(double delta, String queSeIntentaba) {
        if (delta == 0)
            return;
        try {
            RedisCommands<String, byte[]> comandos = connection.sync();
            String clave = claveDelDia();
            comandos.incrbyfloat(clave, delta);
            comandos.expire(clave, TTL_SEGUNDOS);
        } catch (RuntimeException fallo) {
            log.warn(
                    "No se pudo {} en Valkey; el contador del dia queda por encima de lo real y"
                            + " el asistente degradara antes de tiempo: {}",
                    queSeIntentaba, fallo.getMessage());
        }
    }

    private String claveDelDia() {
        return PREFIJO + DIA.format(LocalDate.now(clock));
    }

    private void avisarUnaVezPorDia() {
        LocalDate hoy = LocalDate.now(clock);
        if (hoy.equals(diaDelAviso))
            return;
        diaDelAviso = hoy;
        log.warn("Tope de gasto diario del asistente agotado para TODA la plataforma; las"
                + " propuestas salen por el camino determinista hasta manana. Se avisa una sola"
                + " vez al dia por proceso; el recuento vive en ai_proposal_generated_total con"
                + " ai_outcome=degraded_spend_cap");
    }
}
