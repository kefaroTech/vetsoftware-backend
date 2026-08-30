package com.vetsoftware.app.aiproposal.infrastructure.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricCardinalityFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lo que este barrido <b>informa</b>, que es donde estan sus defectos posibles.
 *
 * <p>
 * <b>El fallo que estas pruebas existen para impedir ya ocurrio dos veces en
 * este proyecto</b>, en los jobs de facturacion: una terminacion escrita como
 * {@code == batchSize} dejaba clientes que pagaban sin acceso y el job en
 * verde. La forma del defecto no es "el SQL esta mal" —eso lo caza la rodaja de
 * persistencia— sino "el job dice que fue bien cuando no lo fue", y eso solo se
 * ve afirmando el {@code Outcome}.
 *
 * <p>
 * Hay <b>dos</b> maneras distintas de no terminar y las dos tienen que salir
 * como {@code PARTIAL_FAILURE}: que un paso reviente, y que un paso agote su
 * cupo de lotes dejando filas elegibles sin tratar. La segunda es la sutil: el
 * barrido movio miles de filas, no lanzo nada, y aun asi no cumplio la
 * politica.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiProposalRetentionJob — que informa el barrido de retencion")
class AiProposalRetentionJobTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-08-30T08:55:00Z"),
            ZoneOffset.UTC);

    @Mock
    private ProposalRetentionPort retention;

    private MeterRegistry registry;
    private AiProposalRetentionProperties properties;
    private AiProposalRetentionJob job;

    @BeforeEach
    void montar() {
        registry = new SimpleMeterRegistry();
        properties = new AiProposalRetentionProperties();
        properties.setBatchSize(10);
        properties.setMaxBatchesPerRun(3);
        job = new AiProposalRetentionJob(retention, properties,
                new AiProposalRetentionMetrics(registry),
                new ScheduledJobTelemetry(ObservationRegistry.NOOP), RELOJ);
    }

    private double contador(AiProposalRetentionMetrics.Paso paso) {
        return registry.get(AiProposalRetentionMetrics.ROWS_METRIC)
                .tag(AiProposalRetentionMetrics.STEP_TAG, paso.etiqueta()).counter().count();
    }

    @Nested
    @DisplayName("Desenlace")
    class Desenlace {

        @Test
        @DisplayName("sin nada elegible informa NO_WORK, no exito")
        void sin_nada_elegible_informa_no_work() {
            assertThat(job.aplicarRetencion()).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
        }

        @Test
        @DisplayName("con trabajo hecho y sin incidencias informa SUCCESS")
        void con_trabajo_hecho_informa_success() {
            when(retention.anonymizeProposals(any(), any(), anyInt())).thenReturn(4);

            assertThat(job.aplicarRetencion()).isEqualTo(ScheduledJobTelemetry.Outcome.SUCCESS);
        }

        /**
         * <b>La prueba central.</b> Tres lotes llenos con un cupo de tres significa que
         * quedan filas elegibles sin tratar: el barrido movio 30 filas y <b>no</b>
         * completo la politica. Informarlo como exito es como se llega a una tabla con
         * correos de hace ocho meses y un panel en verde.
         */
        @Test
        @DisplayName("si un paso agota su cupo de lotes informa PARTIAL_FAILURE aunque no falle nada")
        void agotar_el_cupo_es_fallo_parcial() {
            when(retention.anonymizeProposals(any(), any(), anyInt())).thenReturn(10);

            assertThat(job.aplicarRetencion())
                    .isEqualTo(ScheduledJobTelemetry.Outcome.PARTIAL_FAILURE);
            verify(retention, org.mockito.Mockito.times(3)).anonymizeProposals(any(), any(),
                    eq(10));
        }

        @Test
        @DisplayName("un lote incompleto cierra el paso: no se piden lotes de mas")
        void un_lote_incompleto_cierra_el_paso() {
            when(retention.anonymizeProposals(any(), any(), anyInt())).thenReturn(4);

            assertThat(job.aplicarRetencion()).isEqualTo(ScheduledJobTelemetry.Outcome.SUCCESS);
            verify(retention, org.mockito.Mockito.times(1)).anonymizeProposals(any(), any(),
                    anyInt());
        }

        @Test
        @DisplayName("si un paso revienta informa PARTIAL_FAILURE y los otros seis corren igual")
        void un_paso_que_revienta_no_aborta_la_pasada() {
            when(retention.anonymizeProposals(any(), any(), anyInt()))
                    .thenThrow(new IllegalStateException("la base no responde"));

            assertThat(job.aplicarRetencion())
                    .isEqualTo(ScheduledJobTelemetry.Outcome.PARTIAL_FAILURE);
            verify(retention).redactTurns(anyInt());
            verify(retention).redactLineReasons(any(), anyInt());
            verify(retention).purgeLines(any(), anyInt());
            verify(retention).purgeTurns(any(), anyInt());
            verify(retention).purgeAcceptances(any(), anyInt());
            verify(retention).purgeProposals(any(), anyInt());
        }

        @Test
        @DisplayName("si revientan los siete informa FAILURE, que no sella el heartbeat")
        void si_revientan_todos_informa_failure() {
            RuntimeException caida = new IllegalStateException("la base no responde");
            when(retention.anonymizeProposals(any(), any(), anyInt())).thenThrow(caida);
            when(retention.redactTurns(anyInt())).thenThrow(caida);
            when(retention.redactLineReasons(any(), anyInt())).thenThrow(caida);
            when(retention.purgeLines(any(), anyInt())).thenThrow(caida);
            when(retention.purgeTurns(any(), anyInt())).thenThrow(caida);
            when(retention.purgeAcceptances(any(), anyInt())).thenThrow(caida);
            when(retention.purgeProposals(any(), anyInt())).thenThrow(caida);

            ScheduledJobTelemetry.Outcome desenlace = job.aplicarRetencion();

            assertThat(desenlace).isEqualTo(ScheduledJobTelemetry.Outcome.FAILURE);
            assertThat(desenlace.sealsHeartbeat())
                    .as("un barrido que no hizo nada de su trabajo no puede sellar el latido que"
                            + " vigila que corriera")
                    .isFalse();
        }

        /**
         * {@code PARTIAL_FAILURE} si sella: el job corrio, y quien vigila que corriera
         * no es quien vigila que no fallara.
         */
        @Test
        @DisplayName("el fallo parcial sigue sellando el heartbeat: son dos preguntas distintas")
        void el_fallo_parcial_sella_el_heartbeat() {
            assertThat(ScheduledJobTelemetry.Outcome.PARTIAL_FAILURE.sealsHeartbeat()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cortes y orden")
    class CortesYOrden {

        @Test
        @DisplayName("los dos plazos salen de la configuracion, no de constantes")
        void los_plazos_salen_de_la_configuracion() {
            properties.setAnonymizeAfter(Duration.ofDays(30));
            properties.setPurgeAfter(Duration.ofDays(365));
            job = new AiProposalRetentionJob(retention, properties,
                    new AiProposalRetentionMetrics(new SimpleMeterRegistry()),
                    new ScheduledJobTelemetry(ObservationRegistry.NOOP), RELOJ);

            job.aplicarRetencion();

            ArgumentCaptor<LocalDateTime> corteAnonimizacion = ArgumentCaptor
                    .forClass(LocalDateTime.class);
            verify(retention).anonymizeProposals(corteAnonimizacion.capture(), any(), anyInt());
            assertThat(corteAnonimizacion.getValue())
                    .isEqualTo(LocalDateTime.now(RELOJ).minusDays(30));

            ArgumentCaptor<LocalDateTime> cortePurga = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(retention).purgeProposals(cortePurga.capture(), anyInt());
            assertThat(cortePurga.getValue()).isEqualTo(LocalDateTime.now(RELOJ).minusDays(365));
        }

        /**
         * Las FK van {@code ON DELETE RESTRICT}, asi que purgar la cabecera antes de
         * vaciar a sus hijas no es un borrado en cascada sino un error de integridad a
         * las cuatro de la mañana.
         */
        @Test
        @DisplayName("la purga va lineas, turnos y cabecera, en ese orden")
        void la_purga_respeta_el_orden_de_las_fk() {
            job.aplicarRetencion();

            var enOrden = org.mockito.Mockito.inOrder(retention);
            enOrden.verify(retention).purgeLines(any(), anyInt());
            enOrden.verify(retention).purgeTurns(any(), anyInt());
            enOrden.verify(retention).purgeProposals(any(), anyInt());
        }

        @Test
        @DisplayName("la anonimizacion marca la cabecera antes de redactar turnos y motivos")
        void la_anonimizacion_marca_antes_de_redactar() {
            job.aplicarRetencion();

            var enOrden = org.mockito.Mockito.inOrder(retention);
            enOrden.verify(retention).anonymizeProposals(any(), any(), anyInt());
            enOrden.verify(retention).redactTurns(anyInt());
            enOrden.verify(retention).redactLineReasons(any(), anyInt());
        }

        @Test
        @DisplayName("la palanca apagada no es cosa del job: si corre, corre entero")
        void el_job_no_decide_si_corre() {
            properties.setEnabled(false);

            assertThat(job.aplicarRetencion()).isEqualTo(ScheduledJobTelemetry.Outcome.NO_WORK);
            verify(retention).anonymizeProposals(any(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("Metricas")
    class Metricas {

        /**
         * &#9940; El contador es <b>por paso</b> y no un total. Un unico numero deja
         * invisible justo el defecto que motivo esta fase: la cabecera anonimizada y
         * los motivos del prospecto intactos en la tabla de al lado.
         */
        @Test
        @DisplayName("publica un contador por paso, no un total")
        void publica_un_contador_por_paso() {
            when(retention.anonymizeProposals(any(), any(), anyInt())).thenReturn(3);
            when(retention.redactLineReasons(any(), anyInt())).thenReturn(7);

            job.aplicarRetencion();

            assertThat(contador(AiProposalRetentionMetrics.Paso.ANONIMIZAR_PROPUESTAS))
                    .isEqualTo(3);
            assertThat(contador(AiProposalRetentionMetrics.Paso.REDACTAR_MOTIVOS)).isEqualTo(7);
            assertThat(contador(AiProposalRetentionMetrics.Paso.REDACTAR_TURNOS)).isZero();
        }

        @Test
        @DisplayName("publica cuantos pasos agotaron su cupo, que es lo que dice que va perdiendo")
        void publica_los_pasos_con_cupo_agotado() {
            when(retention.anonymizeProposals(any(), any(), anyInt())).thenReturn(10);

            job.aplicarRetencion();

            assertThat(registry.get(AiProposalRetentionMetrics.EXHAUSTED_METRIC).gauge().value())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("una pasada limpia deja el gauge de cupo agotado en cero")
        void una_pasada_limpia_deja_el_gauge_en_cero() {
            job.aplicarRetencion();

            assertThat(registry.get(AiProposalRetentionMetrics.EXHAUSTED_METRIC).gauge().value())
                    .isZero();
        }

        @Test
        @DisplayName("un paso que revienta cuenta cero filas, no deja de contar")
        void un_paso_que_revienta_cuenta_cero() {
            when(retention.redactTurns(anyInt()))
                    .thenThrow(new IllegalStateException("la base no responde"));

            job.aplicarRetencion();

            assertThat(contador(AiProposalRetentionMetrics.Paso.REDACTAR_TURNOS)).isZero();
            assertThat(contador(AiProposalRetentionMetrics.Paso.PURGAR_LINEAS))
                    .as("el fallo de un paso no borra las series de los demas").isZero();
        }
    }

    /**
     * &#9940; <b>La purga de la evidencia de consentimiento.</b>
     * {@code legal_document_acceptances.subject_ref} es un {@code VARCHAR} y no una
     * clave foranea -deliberado: una FK polimorfica ataria la rodaja legal a
     * {@code aiproposal}, a {@code company} y a cualquier sujeto futuro-, asi que
     * la base no arrastra nada al borrar la cabecera. Sin este paso, cada propuesta
     * purgada dejaba una o dos filas de aceptacion apuntando a un id que ya no
     * existe, y el informe de cumplimiento mentia en las dos direcciones: afirmaba
     * consentimientos de titulares cuyos datos se borraron, y el recuento de
     * evidencias dejaba de cuadrar con el de propuestas.
     */
    @Nested
    @DisplayName("La purga de aceptaciones legales")
    class PurgaDeAceptaciones {

        /**
         * &#9940; <b>El orden ES el paso.</b> La aceptacion apunta a la propuesta por
         * su id, en un {@code VARCHAR} sin clave foranea: si la cabecera se borra
         * primero, no queda id por el que reconocer la fila y la evidencia huerfana se
         * queda para siempre. Es la misma leccion de orden que
         * {@code suppressByContactEmail}.
         */
        @Test
        @DisplayName("las aceptaciones se purgan ANTES que la cabecera, que es lo que hace"
                + " reconocible la fila")
        void las_aceptaciones_se_purgan_antes_que_la_cabecera() {
            job.aplicarRetencion();

            var enOrden = org.mockito.Mockito.inOrder(retention);
            enOrden.verify(retention).purgeTurns(any(), anyInt());
            enOrden.verify(retention).purgeAcceptances(any(), anyInt());
            enOrden.verify(retention).purgeProposals(any(), anyInt());
        }

        /**
         * Comparte el predicado exacto de {@code purgeProposals} y no el de la
         * anonimizacion: se borra la evidencia de lo que se va, no la de lo que sigue
         * vivo con el correo ya anonimizado.
         */
        @Test
        @DisplayName("usa el mismo corte de purga que la cabecera, no el de la anonimizacion")
        void usa_el_mismo_corte_que_la_cabecera() {
            job.aplicarRetencion();

            ArgumentCaptor<LocalDateTime> corteAceptaciones = ArgumentCaptor
                    .forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> corteCabecera = ArgumentCaptor
                    .forClass(LocalDateTime.class);
            verify(retention).purgeAcceptances(corteAceptaciones.capture(), anyInt());
            verify(retention).purgeProposals(corteCabecera.capture(), anyInt());
            assertThat(corteAceptaciones.getValue()).isEqualTo(corteCabecera.getValue());
        }

        @Test
        @DisplayName("va por lotes del tamano configurado, como los otros seis pasos")
        void va_por_lotes_del_tamano_configurado() {
            job.aplicarRetencion();

            verify(retention).purgeAcceptances(any(), eq(properties.getBatchSize()));
        }

        /**
         * Un paso que agota su cupo dejo filas elegibles sin tratar: aqui eso significa
         * evidencias de consentimiento que siguen apuntando a propuestas que ya no
         * existen. Informarlo como exito es como se llega a un panel en verde sobre una
         * tabla sucia.
         */
        @Test
        @DisplayName("si la purga de aceptaciones agota su cupo, la pasada no es un exito")
        void agotar_el_cupo_de_aceptaciones_no_es_exito() {
            when(retention.purgeAcceptances(any(), anyInt())).thenReturn(10);

            assertThat(job.aplicarRetencion())
                    .isEqualTo(ScheduledJobTelemetry.Outcome.PARTIAL_FAILURE);
        }

        @Test
        @DisplayName("cuenta sus filas en su propia serie, no en la de la cabecera")
        void cuenta_sus_filas_en_su_propia_serie() {
            when(retention.purgeAcceptances(any(), anyInt())).thenReturn(4);

            job.aplicarRetencion();

            assertThat(contador(AiProposalRetentionMetrics.Paso.PURGAR_ACEPTACIONES)).isEqualTo(4);
            assertThat(contador(AiProposalRetentionMetrics.Paso.PURGAR_PROPUESTAS)).isZero();
        }

        /**
         * &#9940; <b>Que el contador se incremente no significa que la metrica
         * salga.</b> {@code BusinessMetricCardinalityFilter} comprueba la etiqueta
         * {@code retention.step} contra una lista blanca escrita a mano, y un valor que
         * no este declarado hace que Micrometer descarte el medidor: no hay excepcion,
         * no hay arranque fallido, y el panel muestra <b>un hueco indistinguible de "no
         * paso nada"</b>. Esto ya ocurrio con {@code purge_acceptances}.
         *
         * <p>
         * Por eso este caso no usa el registro pelado de los demas sino uno con el
         * filtro puesto, que es como corre produccion: si alguien quita el valor de
         * {@code ALLOWED_VALUES}, {@code find(...).counter()} devuelve {@code null} y
         * el test se pone rojo, en vez de seguir en verde sobre una serie que ya no
         * existe.
         */
        @Test
        @DisplayName("la serie del paso sobrevive al filtro de cardinalidad: sale de verdad, no"
                + " deja un hueco")
        void la_serie_sobrevive_al_filtro_de_cardinalidad() {
            MeterRegistry vigilado = new SimpleMeterRegistry();
            vigilado.config().meterFilter(new BusinessMetricCardinalityFilter());
            AiProposalRetentionJob conFiltro = new AiProposalRetentionJob(retention, properties,
                    new AiProposalRetentionMetrics(vigilado),
                    new ScheduledJobTelemetry(ObservationRegistry.NOOP), RELOJ);
            when(retention.purgeAcceptances(any(), anyInt())).thenReturn(6);

            conFiltro.aplicarRetencion();

            assertThat(vigilado.find(AiProposalRetentionMetrics.ROWS_METRIC)
                    .tag(AiProposalRetentionMetrics.STEP_TAG,
                            AiProposalRetentionMetrics.Paso.PURGAR_ACEPTACIONES.etiqueta())
                    .counter())
                    .as("el filtro de cardinalidad descarto la serie: el panel de retencion"
                            + " mostraria un hueco identico a no haber purgado nada")
                    .isNotNull();
            assertThat(vigilado.get(AiProposalRetentionMetrics.ROWS_METRIC)
                    .tag(AiProposalRetentionMetrics.STEP_TAG,
                            AiProposalRetentionMetrics.Paso.PURGAR_ACEPTACIONES.etiqueta())
                    .counter().count()).isEqualTo(6);
        }

        /**
         * La otra mitad del mismo riesgo: si el filtro descarta el medidor, el descarte
         * queda contado. Un valor mayor que cero aqui es un panel ciego.
         */
        @Test
        @DisplayName("ningun paso del barrido acaba en el contador de descartes por"
                + " cardinalidad")
        void ningun_paso_acaba_descartado() {
            MeterRegistry vigilado = new SimpleMeterRegistry();
            BusinessMetricCardinalityFilter filtro = new BusinessMetricCardinalityFilter();
            vigilado.config().meterFilter(filtro);
            new AiProposalRetentionMetrics(vigilado);
            filtro.bindTo(vigilado);

            assertThat(vigilado.find(BusinessMetricCardinalityFilter.DENIED).functionCounters())
                    .allSatisfy(contador -> assertThat(contador.count()).isZero());
        }
    }
}
