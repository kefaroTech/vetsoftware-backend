package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * ⛔ <b>Los dos estados en los que el asistente no puede cotizar NADA, y lo
 * único que un operador va a ver de ellos.</b>
 *
 * <p>
 * El SQL de este adaptador lo ejercita {@code SellableCatalogQueryPortIT}
 * contra MySQL real. Lo que esta clase prueba es lo otro: <b>cuándo se escribe
 * el aviso y cuándo se calla</b>, que depende de un reloj y de un contador y no
 * de la base. Con Testcontainers no se podría mover el reloj, así que la
 * ventana quedaría sin comprobar — y una guarda de ruido sin probar es una
 * guarda que termina callándolo todo.
 *
 * <p>
 * <b>Por qué existe la ventana.</b> Antes se avisaba <em>una sola vez por
 * proceso</em>: en un contenedor que lleva días arriba el mensaje está en el
 * arranque y no en la ventana de la petición que falla, así que quien depura
 * mira los últimos minutos, no ve nada y concluye que el asistente está sano.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSellableCatalogQueryPort — los avisos de que no hay nada que cotizar")
class JpaSellableCatalogQueryPortAvisosTest {

    private static final Duration VENTANA = Duration.ofMinutes(5);

    private static final Instant T0 = Instant.parse("2026-08-31T10:00:00Z");

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    /**
     * Un reloj que se puede mover. {@code Clock.fixed} no sirve aquí: con él la
     * ventana no se cierra nunca y la prueba diría que el aviso se repite cuando lo
     * que pasa es que el tiempo no avanza.
     */
    private final AtomicReference<Instant> ahora = new AtomicReference<>(T0);

    private final Clock reloj = new Clock() {

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora.get();
        }
    };

    private JpaSellableCatalogQueryPort port;

    private Logger canal;

    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void montar() {
        port = new JpaSellableCatalogQueryPort(entityManager, reloj);
        LoggerContext contexto = (LoggerContext) LoggerFactory.getILoggerFactory();
        // Al logger de la clase y no a la raiz: una rodaja de Spring del mismo fork
        // puede haber cargado logback-spring.xml y cambiado la propagacion.
        canal = contexto.getLogger(JpaSellableCatalogQueryPort.class);
        logs = new ListAppender<>();
        logs.setContext(contexto);
        logs.start();
        canal.addAppender(logs);
    }

    @AfterEach
    void desmontar() {
        canal.detachAppender(logs);
        logs.stop();
    }

    private void consultaVacia() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
    }

    private void consultaDeArticulosVacia() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
    }

    private List<ILoggingEvent> avisos(String fragmento) {
        return logs.list.stream().filter(evento -> evento.getFormattedMessage().contains(fragmento))
                .toList();
    }

    @Nested
    @DisplayName("Sin tarifa publicada")
    class SinTarifa {

        @Test
        @DisplayName("la primera peticion escribe el aviso")
        void la_primera_peticion_avisa() {
            consultaVacia();

            assertThat(port.findPublishedPriceListId()).isEmpty();

            assertThat(avisos("PUBLISHED vigente")).singleElement()
                    .satisfies(evento -> assertThat(evento.getLevel()).isEqualTo(Level.WARN));
        }

        /**
         * ⛔ <b>La mitad «no inundar» del equilibrio.</b> Es un endpoint público y
         * anónimo: una línea por petición convertiría el canal en ruido que enseña a
         * ignorarlo, que es exactamente cómo se llega a que nadie lea el aviso.
         */
        @Test
        @DisplayName("dentro de la ventana no repite el aviso, aunque lluevan peticiones")
        void dentro_de_la_ventana_no_repite() {
            consultaVacia();

            port.findPublishedPriceListId();
            ahora.set(T0.plus(Duration.ofMinutes(1)));
            port.findPublishedPriceListId();
            ahora.set(T0.plus(Duration.ofMinutes(4)));
            port.findPublishedPriceListId();

            assertThat(avisos("PUBLISHED vigente")).hasSize(1);
        }

        /**
         * ⛔ <b>La mitad «no se pierde», y es la razón de todo el cambio.</b> Con el
         * aviso único por proceso, esta segunda línea no existía: el mensaje se quedaba
         * en el arranque y la petición que falla tres días después no dejaba rastro
         * ninguno en su propia ventana de tiempo.
         */
        @Test
        @DisplayName("pasada la ventana vuelve a avisar: el rastro cae en la ventana de la"
                + " peticion que falla")
        void pasada_la_ventana_vuelve_a_avisar() {
            consultaVacia();

            port.findPublishedPriceListId();
            ahora.set(T0.plus(VENTANA).plusSeconds(1));
            port.findPublishedPriceListId();

            assertThat(avisos("PUBLISHED vigente")).hasSize(2);
        }

        /**
         * Ninguna petición se pierde: las que se callan se cuentan y el aviso siguiente
         * dice cuántas fueron, así que desde una sola línea se reconstruye el periodo.
         */
        @Test
        @DisplayName("el aviso siguiente dice cuantas peticiones se callaron")
        void el_aviso_siguiente_cuenta_las_silenciadas() {
            consultaVacia();

            port.findPublishedPriceListId();
            ahora.set(T0.plus(Duration.ofMinutes(1)));
            port.findPublishedPriceListId();
            port.findPublishedPriceListId();
            port.findPublishedPriceListId();
            ahora.set(T0.plus(VENTANA).plusSeconds(1));
            port.findPublishedPriceListId();

            assertThat(avisos("PUBLISHED vigente")).last()
                    .satisfies(evento -> assertThat(evento.getFormattedMessage())
                            .contains("Otras 3 peticiones"));
        }
    }

    @Nested
    @DisplayName("Tarifa publicada pero sin articulos vendibles")
    class CatalogoVacio {

        /**
         * ⛔ <b>Este camino era mudo.</b> Compartía desenlace con «no hay tarifa», así
         * que la señal mandaba a publicar una tarifa que ya estaba publicada.
         */
        @Test
        @DisplayName("avisa, y dice explicitamente que NO es el mismo estado que la falta de"
                + " tarifa")
        void avisa_y_se_distingue_de_la_falta_de_tarifa() {
            consultaDeArticulosVacia();

            assertThat(port.loadCatalog(42L, ProposalBillingCycle.MONTHLY)).isEmpty();

            assertThat(avisos("no cuelga de ella ni un")).singleElement().satisfies(evento -> {
                assertThat(evento.getLevel()).isEqualTo(Level.WARN);
                assertThat(evento.getFormattedMessage()).contains("42").contains("MONTHLY")
                        .contains("empty_catalog");
            });
            assertThat(avisos("PUBLISHED vigente")).isEmpty();
        }

        @Test
        @DisplayName("tiene su propia ventana: no se la gasta el aviso del otro camino")
        void tiene_ventana_propia() {
            consultaDeArticulosVacia();

            port.loadCatalog(42L, ProposalBillingCycle.MONTHLY);
            ahora.set(T0.plus(Duration.ofMinutes(1)));
            port.loadCatalog(42L, ProposalBillingCycle.MONTHLY);

            assertThat(avisos("no cuelga de ella ni un")).hasSize(1);

            ahora.set(T0.plus(VENTANA).plusSeconds(1));
            port.loadCatalog(42L, ProposalBillingCycle.MONTHLY);

            assertThat(avisos("no cuelga de ella ni un")).hasSize(2);
        }

        /**
         * Un {@code priceListId} nulo es un error de programación del llamante, no el
         * estado «catálogo vacío». Avisar aquí metería ruido en el canal que existe
         * para una sola cosa.
         */
        @Test
        @DisplayName("un argumento nulo no es el estado de catalogo vacio y no avisa")
        void un_argumento_nulo_no_avisa() {
            assertThat(port.loadCatalog(null, ProposalBillingCycle.MONTHLY)).isEmpty();
            assertThat(port.loadCatalog(42L, null)).isEmpty();

            assertThat(logs.list).isEmpty();
        }
    }
}
