package com.vetsoftware.app.dunning.infrastructure.scheduling;

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
import com.vetsoftware.app.dunning.application.dto.DunningBatchResult;
import com.vetsoftware.app.dunning.application.port.in.ProcessDunningBatchUseCase;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
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
 * El job nocturno de cobranza, que hasta ahora no tenia ni un caso.
 *
 * <p>
 * <b>El caso que vale la clase entera es
 * {@link SinPrincipal#el_worker_ve_ROLE_SYSTEM_aunque_nadie_haya_iniciado_sesion}.</b>
 * Un proceso programado corre <b>sin principal</b>: el
 * {@code SecurityContextHolder} esta vacio cuando el scheduler dispara el
 * metodo. {@code ProcessDunningBatchUseCase} esta cerrado a
 * {@code hasRole('SYSTEM')}, asi que sin el {@link SystemAuthRunner}
 * envolviendo la llamada el barrido se cae <b>cada noche</b> con un
 * {@code AccessDeniedException} — y se cae en silencio, porque nadie mira el
 * log de un job que nadie espera. El sintoma que llega al cliente no es un
 * error: es que la mora deja de evaluarse y los contratos morosos nunca bajan a
 * solo lectura. Por eso el runner que se usa aqui es el <b>real</b>, no un
 * doble: lo que se comprueba es que el principal se instala de verdad.
 *
 * <p>
 * Y su reverso, {@link SinPrincipal#el_contexto_queda_como_estaba_al_terminar}:
 * el hilo del scheduler se reutiliza, asi que un ROLE_SYSTEM que se quedara
 * pegado convertiria el siguiente job en un principal de plataforma que nadie
 * concedio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DunningEvaluationJob — barrido nocturno bajo principal SYSTEM")
class DunningEvaluationJobTest {

    private static final int TAMANO_LOTE = 3;

    @Mock
    private ProcessDunningBatchUseCase worker;
    @Captor
    private ArgumentCaptor<Long> cursorCaptor;

    private CapturaDeObservacion captura;
    private DunningEvaluationJob job;

    @BeforeEach
    void montar() {
        captura = new CapturaDeObservacion();
        job = new DunningEvaluationJob(worker, new SystemAuthRunner(), captura.telemetria(),
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
        @DisplayName("el worker ve ROLE_SYSTEM aunque nadie haya iniciado sesion: sin eso el"
                + " barrido se cae cada noche con AccessDenied y la mora deja de evaluarse")
        void el_worker_ve_ROLE_SYSTEM_aunque_nadie_haya_iniciado_sesion() {
            SecurityContextHolder.clearContext();
            List<String> autoridadesVistas = new ArrayList<>();
            // Lote vacio a proposito: con la condicion de parada correcta un lote no
            // vacio encadenaria otra vuelta y las autoridades se contarian dos veces.
            // Lo que este caso mide es el principal, no el numero de vueltas.
            when(worker.processBatchAfter(0L, TAMANO_LOTE)).thenAnswer(invocacion -> {
                Authentication actual = SecurityContextHolder.getContext().getAuthentication();
                actual.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .forEach(autoridadesVistas::add);
                return new DunningBatchResult(0, 0L);
            });

            job.runDunning();

            assertThat(autoridadesVistas).containsExactly("ROLE_SYSTEM");
        }

        @Test
        @DisplayName("el contexto queda como estaba al terminar: el hilo del scheduler se"
                + " reutiliza y un ROLE_SYSTEM pegado seria un principal que nadie concedio")
        void el_contexto_queda_como_estaba_al_terminar() {
            SecurityContextHolder.clearContext();
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new DunningBatchResult(0, 0L));

            job.runDunning();

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Paginacion por cursor")
    class Paginacion {

        /**
         * Deliberadamente indiferente a la condicion de parada: encadena con un lote
         * lleno y termina con uno <b>vacio</b>, que cierra el barrido tanto con el
         * {@code == batchSize} de hoy como con el {@code > 0} que pide #468. Asi la
         * cobertura del avance del cursor —lo unico que este caso comprueba— sobrevive
         * al arreglo en vez de tener que reescribirse con el.
         */
        @Test
        @DisplayName("avanza el cursor con el último id de la vuelta anterior y para al"
                + " llegar un lote vacío")
        void avanza_el_cursor_con_el_ultimo_id_de_la_vuelta_anterior() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new DunningBatchResult(TAMANO_LOTE, 13L));
            when(worker.processBatchAfter(13L, TAMANO_LOTE))
                    .thenReturn(new DunningBatchResult(0, 13L));

            job.runDunning();

            verify(worker, times(2)).processBatchAfter(cursorCaptor.capture(), eq(TAMANO_LOTE));
            assertThat(cursorCaptor.getAllValues()).containsExactly(0L, 13L);
            verifyNoMoreInteractions(worker);
        }

        /**
         * <b>Este caso sustituye a {@code un_lote_incompleto_cierra_el_barrido}, que
         * fijaba el defecto de #468 como si fuera la conducta deseada</b> — su
         * {@code @DisplayName} decia literalmente «un lote incompleto cierra el barrido
         * en una sola vuelta», que es la descripcion del fallo, no de un requisito.
         *
         * <p>
         * La consulta que alimenta el lote es
         * {@code ... AND id > :afterId ORDER BY id LIMIT :batchSize FOR UPDATE SKIP
         * LOCKED}. {@code SKIP LOCKED} <b>descarta en silencio</b> las filas que otra
         * transaccion tiene bloqueadas, asi que un lote corto significa «habia filas
         * ocupadas», no «se acabo el trabajo». Con dos instancias en ECS —el despliegue
         * normal— la segunda recibe un lote corto en su primera vuelta y da el barrido
         * por terminado: esa noche no se evalua ninguna factura con id posterior. Lo
         * que duele no es que un moroso tarde un dia mas en bajar a solo lectura, sino
         * que <b>un cliente que ya pago sigue sin poder escribir hasta la noche
         * siguiente</b>, y el job reporta {@code success}.
         *
         * <p>
         * La unica condicion de parada correcta es el lote <b>vacio</b>. Termina igual
         * de seguro: el cursor crece de forma estrictamente monotona porque la consulta
         * filtra {@code id > :afterId} y ordena por {@code id}, asi que el
         * {@code lastId} de un lote no vacio siempre supera al cursor anterior.
         *
         * <p>
         * <b>Nacio deshabilitado y ya no lo esta.</b> Se escribio exigiendo la conducta
         * correcta cuando {@code DunningEvaluationJob.executeDunning} todavia cerraba
         * el barrido con {@code while (batch.processed() == batchSize)}; el arreglo de
         * #468 sustituyo esa condicion y el {@code @Disabled} se fue con el, en el
         * mismo cambio.
         */
        @Test
        @DisplayName("un lote corto NO cierra el barrido: con SKIP LOCKED significa filas"
                + " ocupadas, no fin del trabajo")
        void un_lote_corto_no_cierra_el_barrido() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new DunningBatchResult(1, 11L));
            when(worker.processBatchAfter(11L, TAMANO_LOTE))
                    .thenReturn(new DunningBatchResult(0, 11L));

            job.runDunning();

            verify(worker, times(2)).processBatchAfter(cursorCaptor.capture(), eq(TAMANO_LOTE));
            assertThat(cursorCaptor.getAllValues()).containsExactly(0L, 11L);
            verifyNoMoreInteractions(worker);
        }

        /**
         * <b>La otra mitad de #468: con «lote vacio» como condicion de parada hay que
         * demostrar que el barrido TERMINA.</b> Lo garantiza el cursor, que crece de
         * forma estrictamente monotona porque la consulta filtra {@code id > :afterId}
         * y ordena por {@code id}. Pero eso es una propiedad del <em>adaptador</em>, no
         * del bucle: una implementacion del puerto que devolviera filas sin mover el
         * cursor dejaria al job girando para siempre dentro del scheduler, reteniendo
         * un hilo y una conexion por vuelta y sin que nadie lo viera hasta que el pool
         * se agotara. La guarda lo corta en la primera vuelta.
         */
        @Test
        @DisplayName("un cursor que no avanza corta el barrido en vez de girar para"
                + " siempre, y se reporta como partial_failure")
        void un_cursor_que_no_avanza_corta_el_barrido() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new DunningBatchResult(1, 0L));

            job.runDunning();

            verify(worker).processBatchAfter(0L, TAMANO_LOTE);
            verifyNoMoreInteractions(worker);
            assertThat(captura.resultado()).isEqualTo("partial_failure");
        }

        /**
         * La tercera salida: aunque el cursor avance siempre, nada impide que entren
         * facturas nuevas mientras el barrido corre. El tope de vueltas acota la
         * ejecucion a {@code MAX_VUELTAS * batchSize} filas por noche. Este caso simula
         * una fuente inagotable —cada vuelta devuelve un lote lleno y avanza el cursor—
         * y comprueba que el job termina de todos modos.
         */
        @Test
        @DisplayName("un trabajo inagotable se corta en el tope de vueltas y no se"
                + " reporta como exito")
        void un_trabajo_inagotable_se_corta_en_el_tope_de_vueltas() {
            when(worker.processBatchAfter(anyLong(), anyInt()))
                    .thenAnswer(invocacion -> new DunningBatchResult(TAMANO_LOTE,
                            invocacion.getArgument(0, Long.class) + TAMANO_LOTE));

            job.runDunning();

            // 10_000 es MAX_VUELTAS de DunningEvaluationJob. El acoplamiento es
            // deliberado: si alguien sube el tope, este caso obliga a decir por que.
            verify(worker, times(10_000)).processBatchAfter(anyLong(), anyInt());
            assertThat(captura.resultado()).isEqualTo("partial_failure");
        }
    }

    @Nested
    @DisplayName("Telemetria")
    class Telemetria {

        @Test
        @DisplayName("una noche sin facturas vencidas se reporta como no_work, no como exito")
        void una_noche_sin_vencidas_se_reporta_no_work() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new DunningBatchResult(0, 0L));

            job.runDunning();

            assertThat(captura.resultado()).isEqualTo("no_work");
            assertThat(captura.nombreDelJob()).isEqualTo("subscription.dunning");
        }

        @Test
        @DisplayName("una noche con trabajo se reporta como success")
        void una_noche_con_trabajo_se_reporta_success() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenReturn(new DunningBatchResult(2, 12L));
            when(worker.processBatchAfter(12L, TAMANO_LOTE))
                    .thenReturn(new DunningBatchResult(0, 12L));

            job.runDunning();

            assertThat(captura.resultado()).isEqualTo("success");
        }

        @Test
        @DisplayName("un fallo del worker se reporta como error y se propaga, no se traga")
        void un_fallo_del_worker_se_reporta_error_y_se_propaga() {
            when(worker.processBatchAfter(0L, TAMANO_LOTE))
                    .thenThrow(new IllegalStateException("base no disponible"));

            assertThatThrownBy(() -> job.runDunning()).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("base no disponible");

            assertThat(captura.resultado()).isEqualTo("error");
        }
    }

    @Nested
    @DisplayName("Configuracion")
    class Configuracion {

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("un tamano de lote no positivo revienta al construir el bean, no en"
                + " mitad de la noche: un LIMIT 0 no evaluaria ni una factura")
        void un_tamano_de_lote_no_positivo_revienta_al_construir(int batchSize) {
            // Con la condicion de parada por lote vacio un batchSize de cero ya no
            // cuelga —LIMIT 0 devuelve cero filas y el barrido termina en la primera
            // vuelta—, pero termina sin hacer nada y en silencio, todas las noches.
            // Por eso el bean sigue negandose a construirse.
            assertThatThrownBy(() -> new DunningEvaluationJob(worker, new SystemAuthRunner(),
                    captura.telemetria(), batchSize)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("batchSize");

            verifyNoInteractions(worker);
        }
    }

    /**
     * Registro de observacion que se queda con las etiquetas de baja cardinalidad
     * que el job emite. Es el unico modo de afirmar el {@code Outcome}, que
     * {@code runDunning} no devuelve.
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
