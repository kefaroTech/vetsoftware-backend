package com.vetsoftware.app.subscription.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.subscription.application.dto.SubscriptionLifecycleBatchResult;
import com.vetsoftware.app.subscription.application.usecase.SubscriptionLifecycleWorker;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * El barrido diario que cierra cancelaciones, fines de prueba y vigencias de
 * línea. Hasta ahora no tenía ni un caso.
 *
 * <p>
 * <b>Lo que vale la clase entera</b> es que el worker vea {@code ROLE_SYSTEM}:
 * un proceso programado corre <b>sin principal</b>, y todo lo que el worker
 * toca está cerrado a la plataforma. Sin el {@link SystemAuthRunner}
 * envolviendo cada vuelta, el barrido se cae cada noche con un
 * {@code AccessDeniedException} y se cae en silencio: lo que llega al cliente
 * no es un error, es que las bajas pedidas nunca surten efecto y los contratos
 * en prueba nunca pasan a activos. Por eso el runner es el <b>real</b> y no un
 * doble.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionLifecycleJob — barrido diario bajo principal SYSTEM")
class SubscriptionLifecycleJobTest {

    private static final int TAMANO_LOTE = 3;

    @Mock
    private SubscriptionLifecycleWorker worker;
    @Captor
    private ArgumentCaptor<Long> cursorCaptor;

    private CapturaDeObservacion captura;
    private SubscriptionLifecycleJob job;

    @BeforeEach
    void montar() {
        captura = new CapturaDeObservacion();
        job = new SubscriptionLifecycleJob(worker, new SystemAuthRunner(), captura.telemetria(),
                TAMANO_LOTE);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Corre sin principal")
    class SinPrincipal {

        @Test
        @DisplayName("el worker ve ROLE_SYSTEM aunque nadie haya iniciado sesion")
        void el_worker_ve_role_system() {
            SecurityContextHolder.clearContext();
            List<String> autoridadesVistas = new ArrayList<>();
            // Lote vacio a proposito: con la condicion de parada correcta un lote no
            // vacio encadenaria otra vuelta y las autoridades se contarian dos veces.
            // Lo que este caso mide es el principal, no el numero de vueltas.
            when(worker.processBatchAfter(0L, TAMANO_LOTE)).thenAnswer(invocacion -> {
                Authentication actual = SecurityContextHolder.getContext().getAuthentication();
                actual.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .forEach(autoridadesVistas::add);
                return new SubscriptionLifecycleBatchResult(0, 0L);
            });

            job.runLifecycle();

            assertThat(autoridadesVistas).containsExactly("ROLE_SYSTEM");
        }

        @Test
        @DisplayName("el contexto queda como estaba: el hilo del scheduler se reutiliza")
        void el_contexto_queda_como_estaba() {
            SecurityContextHolder.clearContext();
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(0, 0L));

            job.runLifecycle();

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Paginacion por cursor")
    class Paginacion {

        @Test
        @DisplayName("encadena vueltas hasta el lote vacio y avanza con el ultimo id")
        void encadena_vueltas_hasta_el_lote_vacio() {
            // Avanzar por id y no por offset: un contrato que cambia de estado a mitad
            // del barrido desplazaria a los demas y alguno se quedaria sin procesar
            // hasta el dia siguiente.
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(TAMANO_LOTE, 13L));
            when(worker.processBatchAfter(13L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(2, 15L));
            when(worker.processBatchAfter(15L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(0, 15L));

            job.runLifecycle();

            verify(worker, times(3)).processBatchAfter(cursorCaptor.capture(), eq(TAMANO_LOTE));
            assertThat(cursorCaptor.getAllValues()).containsExactly(0L, 13L, 15L);
            verifyNoMoreInteractions(worker);
        }

        /**
         * <b>Este caso SUSTITUYE a {@code un_lote_incompleto_cierra_el_barrido}, que
         * fijaba el defecto de #468 como si fuera la conducta deseada.</b> Su
         * {@code @DisplayName} decia literalmente «un lote incompleto cierra el barrido
         * en una sola vuelta»: eso no era un requisito, era la descripcion del fallo, y
         * un test que afirma un defecto es peor que no tener test —da falsa seguridad y
         * bloquea el arreglo, porque corregir el codigo lo ponia en rojo—.
         *
         * <p>
         * Lo que de verdad ocurre: el lote sale de
         * {@code SubscriptionJpaRepository.lockLifecycleBatchAfter}, que termina en
         * {@code LIMIT :batchSize FOR UPDATE SKIP LOCKED}, y <b>{@code SKIP LOCKED}
         * descarta en silencio las filas bloqueadas por otra transaccion</b>. Un lote
         * corto significa «habia contratos ocupados», no «se acabo el trabajo». Con dos
         * instancias en ECS —el despliegue normal— la segunda recibe un lote corto en
         * su primera vuelta y con la condicion vieja daba el dia por terminado: los
         * contratos con id posterior se quedaban 24 horas sin procesar, una baja pedida
         * no surtia efecto, un contrato en prueba no pasaba a activo, y el job
         * reportaba {@code success}.
         *
         * <p>
         * La unica condicion de parada correcta es el lote <b>vacio</b>.
         */
        @Test
        @DisplayName("un lote corto NO cierra el barrido: con SKIP LOCKED significa"
                + " contratos ocupados, no fin del trabajo")
        void un_lote_corto_no_cierra_el_barrido() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(1, 11L));
            when(worker.processBatchAfter(11L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(0, 11L));

            job.runLifecycle();

            verify(worker, times(2)).processBatchAfter(cursorCaptor.capture(), eq(TAMANO_LOTE));
            assertThat(cursorCaptor.getAllValues()).containsExactly(0L, 11L);
            verifyNoMoreInteractions(worker);
        }

        /**
         * <b>La contrapartida de parar solo con el lote vacio: hay que demostrar que el
         * barrido TERMINA.</b> Lo garantiza el cursor, que crece de forma estrictamente
         * monotona porque la consulta filtra {@code s.id > :afterId} y ordena por
         * {@code s.id}. Pero eso es una propiedad del <em>adaptador</em>, no del bucle:
         * una implementacion del puerto que devolviera filas sin mover el cursor
         * dejaria al job girando para siempre dentro del scheduler, reteniendo un hilo
         * y una conexion por vuelta. La guarda lo corta en la primera vuelta.
         */
        @Test
        @DisplayName("un cursor que no avanza corta el barrido en vez de girar para"
                + " siempre, y se reporta como partial_failure")
        void un_cursor_que_no_avanza_corta_el_barrido() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(1, 0L));

            job.runLifecycle();

            verify(worker).processBatchAfter(0L, TAMANO_LOTE);
            verifyNoMoreInteractions(worker);
            assertThat(captura.resultado()).isEqualTo("partial_failure");
        }

        /**
         * La tercera salida: aunque el cursor avance siempre, nada impide que entren
         * contratos nuevos mientras el barrido corre. El tope de vueltas acota la
         * ejecucion a {@code MAX_VUELTAS * batchSize} filas por dia. Este caso simula
         * una fuente inagotable —cada vuelta devuelve un lote lleno y avanza el cursor—
         * y comprueba que el job termina de todos modos.
         */
        @Test
        @DisplayName("un trabajo inagotable se corta en el tope de vueltas y no se"
                + " reporta como exito")
        void un_trabajo_inagotable_se_corta_en_el_tope_de_vueltas() {
            when(worker.processBatchAfter(anyLong(), anyInt()))
                    .thenAnswer(invocacion -> new SubscriptionLifecycleBatchResult(TAMANO_LOTE,
                            invocacion.getArgument(0, Long.class) + TAMANO_LOTE));

            job.runLifecycle();

            // 10_000 es MAX_VUELTAS de SubscriptionLifecycleJob. El acoplamiento es
            // deliberado: si alguien sube el tope, este caso obliga a decir por que.
            verify(worker, times(10_000)).processBatchAfter(anyLong(), anyInt());
            assertThat(captura.resultado()).isEqualTo("partial_failure");
        }
    }

    @Nested
    @DisplayName("Telemetria")
    class Telemetria {

        @Test
        @DisplayName("un dia sin nada que vencer se reporta como no_work, no como exito")
        void un_dia_sin_trabajo_se_reporta_no_work() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(0, 0L));

            job.runLifecycle();

            assertThat(captura.resultado()).isEqualTo("no_work");
            assertThat(captura.nombreDelJob()).isEqualTo("subscription.lifecycle");
        }

        @Test
        @DisplayName("un dia con trabajo se reporta como success")
        void un_dia_con_trabajo_se_reporta_success() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(2, 12L));
            when(worker.processBatchAfter(12L, TAMANO_LOTE))
                    .thenReturn(new SubscriptionLifecycleBatchResult(0, 12L));

            job.runLifecycle();

            assertThat(captura.resultado()).isEqualTo("success");
        }

        @Test
        @DisplayName("un fallo del worker se reporta como error y se propaga, no se traga")
        void un_fallo_del_worker_se_propaga() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenThrow(new IllegalStateException("base no disponible"));

            assertThatThrownBy(() -> job.runLifecycle()).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("base no disponible");

            assertThat(captura.resultado()).isEqualTo("error");
        }
    }

    @Nested
    @DisplayName("Configuracion")
    class Configuracion {

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("un tamano de lote no positivo revienta al construir el bean")
        void un_tamano_de_lote_no_positivo_revienta(int batchSize) {
            // Con la condicion de parada por lote vacio un batchSize de cero ya no
            // cuelga —LIMIT 0 devuelve cero filas y el barrido termina en la primera
            // vuelta—, pero termina sin hacer nada y en silencio, todos los dias.
            assertThatThrownBy(() -> new SubscriptionLifecycleJob(worker, new SystemAuthRunner(),
                    captura.telemetria(), batchSize)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("batchSize");

            verifyNoInteractions(worker);
        }
    }

    /**
     * Registro de observación que se queda con las etiquetas de baja cardinalidad
     * que el job emite: es el único modo de afirmar el {@code Outcome}, que
     * {@code runLifecycle} no devuelve.
     */
    private static final class CapturaDeObservacion
            implements
                ObservationHandler<Observation.Context> {

        private final ObservationRegistry registry = ObservationRegistry.create();
        private final List<String> etiquetas = new ArrayList<>();

        private CapturaDeObservacion() {
            registry.observationConfig().observationHandler(this);
        }

        ScheduledJobTelemetry telemetria() {
            return new ScheduledJobTelemetry(registry);
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }

        @Override
        public void onStop(Observation.Context context) {
            context.getLowCardinalityKeyValues()
                    .forEach(kv -> etiquetas.add(kv.getKey() + "=" + kv.getValue()));
        }

        @Override
        public void onError(Observation.Context context) {
            onStop(context);
        }

        String resultado() {
            return valorDe("job.outcome");
        }

        String nombreDelJob() {
            return valorDe("job.name");
        }

        private String valorDe(String clave) {
            return etiquetas.stream().filter(e -> e.startsWith(clave + "="))
                    .map(e -> e.substring(clave.length() + 1)).findFirst().orElse(null);
        }
    }
}
