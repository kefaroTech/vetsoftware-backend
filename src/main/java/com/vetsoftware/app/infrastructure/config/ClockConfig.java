package com.vetsoftware.app.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del negocio como bean inyectable, <b>con zona explicita</b>.
 *
 * <p>
 * Existe por la regla de determinismo del CLAUDE.md: un service que llame a
 * {@code LocalDate.now()} directamente no se puede probar sin que el test
 * dependa del reloj de la maquina —y se caiga solo el dia que la medianoche
 * caiga entre dos lineas—. Con el reloj inyectado, el test usa
 * {@code Clock.fixed(...)} y el caso queda fijado para siempre.
 *
 * <p>
 * Se declara una sola vez y en el paquete de configuracion transversal porque
 * no pertenece a ninguna feature: es infraestructura del runtime, como el
 * planificador o el pool de hilos.
 *
 * <h2>Por que la zona esta escrita aqui y no se hereda del contenedor
 * (D-81)</h2>
 *
 * <p>
 * Este bean devolvia {@code Clock.systemDefaultZone()}. La imagen no declara
 * zona, asi que la zona por defecto de la JVM en produccion es UTC y
 * <b>todo</b> lo que preguntaba «que dia es hoy» contestaba cinco horas
 * adelantado. No fallaba: <b>decidia distinto</b>. Una prueba gratuita moria a
 * las 19:00 de su ultimo dia; una cotizacion valida hasta hoy se rechazaba
 * desde las 19:30 y esa misma noche el barrido la marcaba vencida; los dias de
 * gracia de un moroso perdian su ultima tarde; un permiso que empieza el 1 de
 * octubre se concedia a las 19:00 del 30 de septiembre; y un cargo creado a las
 * 23:59 del 31 de marzo quedaba atribuido a abril.
 *
 * <p>
 * <b>La distincion que hace falta</b>: un <i>instante</i> guardado no tiene
 * este problema —Colombia tiene desplazamiento fijo y sin horario de verano,
 * asi que el instante ya prueba a que hora local ocurrio algo—. Lo que si lo
 * tiene es una <i>fecha de calendario derivada del reloj</i>: entre las 19:00 y
 * la medianoche, «hoy» en UTC ya es manana. {@code LocalDate}/
 * {@code LocalDateTime} no llevan zona: son lecturas de reloj de pared, y la
 * pared que importa es la de Bogota.
 *
 * <h2>Por que NO se arregla con la variable de entorno del contenedor</h2>
 *
 * <p>
 * Poner {@code TZ=America/Bogota} moveria la zona por defecto de la JVM entera
 * y corregiria estos sitios de golpe, pero <b>rompe dos medidas que usan
 * horario universal a proposito y lo documentan</b>:
 *
 * <ul>
 * <li>{@code observability.DatabaseAvailabilityProbe} mide la duracion de una
 * racha de caida en segundos transcurridos. Es aritmetica de instantes, no de
 * calendario: la zona no la afecta y {@code Clock.systemUTC()} es la eleccion
 * correcta.</li>
 * <li>{@code observability.business.BusinessGaugeMetrics#loadContingencyExhausted}
 * calcula su umbral de plazo con {@code ZoneId.systemDefault()} <b>a
 * proposito</b>, y su javadoc lo explica: {@code ContingencyRetryJob} decide
 * con un {@code LocalDateTime.now()} pelado y {@code created_date} se persiste
 * con esa misma zona. Mover la zona por defecto desplazaria el umbral cinco
 * horas <i>respecto de las filas ya escritas</i> y la metrica contaria una
 * poblacion distinta de la que el job descarta.</li>
 * </ul>
 *
 * <p>
 * Las dos se protegen solas de este cambio porque <b>construyen su propio
 * {@code Clock.systemUTC()} dentro de su constructor</b> y no consumen este
 * bean. Esa es exactamente la diferencia entre zonar el reloj del negocio
 * —quirurgico, solo alcanza a quien lo inyecta— y mover la zona del proceso
 * —global, alcanza tambien a quien decidio no usarla—.
 *
 * <p>
 * <b>Si alguien vuelve a poner el reloj sin zona</b>, lo detiene
 * {@code ClockConfigTest}: falla si {@link #systemClock()} no declara
 * {@link #BUSINESS_ZONE}, y las pruebas de escenario de cada feature demuestran
 * que la decision cambia con la zona equivocada.
 */
@Configuration
public class ClockConfig {

    /**
     * La zona en la que vive «hoy» para todo el negocio (D-81).
     *
     * <p>
     * Es publica y con nombre porque los tests la necesitan para construir sus
     * {@code Clock.fixed(...)}: un test que escriba {@code -05:00} a mano
     * duplicaria la decision en vez de comprobarla.
     */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");

    /**
     * Reloj del negocio. Zona explicita, nunca {@code systemDefaultZone()}: la
     * imagen no declara zona y heredarla significa decidir en UTC.
     */
    @Bean
    public Clock systemClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
