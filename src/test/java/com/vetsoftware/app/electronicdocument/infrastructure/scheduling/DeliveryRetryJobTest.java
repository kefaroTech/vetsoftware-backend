package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.DianJobLeasePort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.usecase.DeliverElectronicDocumentService;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Issue #204. Un documento VALIDADO por la DIAN que se queda sin representacion
 * grafica —porque el render del PDF, el QR o S3 reventaron en el
 * {@code afterCommit} del cierre de cuenta o en el tiempo 3 de la venta POS— no
 * tenia <b>ninguna</b> ruta que reintentara su entrega: las tres candidatas
 * filtran por PENDIENTE. La factura era fiscalmente valida y el cliente no la
 * recibia nunca; el unico remedio era tocar la base de datos a mano.
 *
 * <p>
 * Lo que estos casos fijan es la ruta que no existia, y sus dos limites: que se
 * arriende (para que dos replicas no reentreguen la misma factura) y que se
 * acote (para que un documento roto no se reintente en bucle eternamente).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryRetryJob — re-entrega de documentos VALIDADO sin representacion grafica")
class DeliveryRetryJobTest {

    private static final ScheduledJobTelemetry TELEMETRY = new ScheduledJobTelemetry(
            ObservationRegistry.create());
    private static final long DEADLINE_HOURS = 72;
    private static final int BATCH_SIZE = 25;
    private static final Duration LEASE = Duration.ofMinutes(15);
    /**
     * Reloj fijo: la ventana de plazo es toda la cota que tiene este job, y con el
     * reloj de la maquina el caso limite dependeria del momento en que corre la
     * suite.
     */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 20, 12, 0);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private DianJobLeasePort leasePort;
    @Mock
    private DeliverElectronicDocumentService deliverService;

    private DeliveryRetryJob job;

    @BeforeEach
    void montar() {
        job = new DeliveryRetryJob(repository, leasePort, deliverService, TELEMETRY, RELOJ,
                DEADLINE_HOURS, BATCH_SIZE, LEASE);
    }

    /** Documento con la fecha de creacion que el caso necesita. */
    private static ElectronicDocument documentoCreado(LocalDateTime createdDate) {
        ElectronicDocument document = mock(ElectronicDocument.class);
        when(document.getCreatedDate()).thenReturn(createdDate);
        return document;
    }

    @Nested
    @DisplayName("la ruta de recuperacion que no existia")
    class Recuperacion {

        @Test
        @DisplayName("un VALIDADO sin PDF arrendado se re-entrega")
        void un_validado_sin_pdf_se_reentrega() {
            ElectronicDocument roto = documentoCreado(AHORA.minusHours(2));
            when(leasePort.leaseUndeliveredValidated(BATCH_SIZE, LEASE)).thenReturn(List.of(901L));
            when(repository.findById(901L)).thenReturn(Optional.of(roto));

            job.retryDeliveries();

            // Sin esta llamada el documento sigue siendo una factura valida ante la
            // DIAN que el cliente no recibe y el mostrador no puede reimprimir.
            verify(deliverService).deliverIfValidated(roto);
        }

        /**
         * El job no tiene {@code DocumentTransmitter} entre sus colaboradores, y es
         * deliberado: el documento ya esta validado, y retransmitirlo emitiria un
         * segundo documento fiscal. Lo que se reintenta es la entrega, no la emision.
         */
        @Test
        @DisplayName("arrienda por 'validado sin pdf', no por estado, y nunca lee la lista global")
        void arrienda_por_falta_de_pdf_y_no_por_estado() {
            when(leasePort.leaseUndeliveredValidated(BATCH_SIZE, LEASE)).thenReturn(List.of());

            job.retryDeliveries();

            verify(leasePort).leaseUndeliveredValidated(BATCH_SIZE, LEASE);
            // VALIDADO es el estado terminal feliz: arrendar por estado llenaria el lote
            // de facturas ya entregadas y no alcanzaria nunca a las rotas.
            verify(leasePort, never()).leaseByDianStatus(any(), anyInt(), any());
            verify(repository, never()).findByDianStatus(any());
        }

        @Test
        @DisplayName("un lote vacio no toca el repositorio ni la entrega")
        void lote_vacio_no_toca_nada() {
            when(leasePort.leaseUndeliveredValidated(BATCH_SIZE, LEASE)).thenReturn(List.of());

            job.retryDeliveries();

            verifyNoInteractions(repository, deliverService);
        }

        @Test
        @DisplayName("un documento arrendado que ya no existe se omite sin entregar nada")
        void documento_inexistente_se_omite() {
            when(leasePort.leaseUndeliveredValidated(BATCH_SIZE, LEASE)).thenReturn(List.of(902L));
            when(repository.findById(902L)).thenReturn(Optional.empty());

            job.retryDeliveries();

            verifyNoInteractions(deliverService);
        }

        @Test
        @DisplayName("un fallo de entrega no detiene al resto del lote")
        void un_fallo_no_detiene_el_lote() {
            ElectronicDocument primero = documentoCreado(AHORA.minusHours(1));
            ElectronicDocument segundo = documentoCreado(AHORA.minusHours(1));
            when(leasePort.leaseUndeliveredValidated(BATCH_SIZE, LEASE))
                    .thenReturn(List.of(903L, 904L));
            when(repository.findById(903L)).thenReturn(Optional.of(primero));
            when(repository.findById(904L)).thenReturn(Optional.of(segundo));
            doThrow(new IllegalStateException("S3 no responde")).when(deliverService)
                    .deliverIfValidated(primero);

            job.retryDeliveries();

            // Cada documento se entrega por su cuenta: que uno reviente no puede dejar
            // sin recuperar a los que venian detras en el lote.
            verify(deliverService).deliverIfValidated(segundo);
        }
    }

    @Nested
    @DisplayName("cota de reintentos: la ventana de plazo")
    class Cota {

        @Test
        @DisplayName("un documento que supero la ventana deja de reintentarse solo")
        void superada_la_ventana_no_se_reintenta() {
            ElectronicDocument viejo = documentoCreado(AHORA.minusHours(DEADLINE_HOURS + 1));
            when(viejo.getId()).thenReturn(905L);
            when(leasePort.leaseUndeliveredValidated(BATCH_SIZE, LEASE)).thenReturn(List.of(905L));
            when(repository.findById(905L)).thenReturn(Optional.of(viejo));

            job.retryDeliveries();

            // Sin cota, un documento cuyo PDF nunca se puede generar se reintentaria
            // cada 12 h para siempre, quemando S3 y correo en cada pasada.
            verify(deliverService, never()).deliverIfValidated(any());
        }

        @Test
        @DisplayName("dentro de la ventana se sigue reintentando")
        void dentro_de_la_ventana_se_reintenta() {
            ElectronicDocument reciente = documentoCreado(AHORA.minusHours(DEADLINE_HOURS - 1));
            when(leasePort.leaseUndeliveredValidated(BATCH_SIZE, LEASE)).thenReturn(List.of(906L));
            when(repository.findById(906L)).thenReturn(Optional.of(reciente));

            job.retryDeliveries();

            verify(deliverService).deliverIfValidated(reciente);
        }

        /**
         * Descartar para siempre un documento por un dato ausente seria exactamente el
         * silencio que este job viene a eliminar.
         */
        @Test
        @DisplayName("un documento sin fecha de creacion no se da por agotado")
        void sin_fecha_de_creacion_no_se_agota() {
            ElectronicDocument sinFecha = documentoCreado(null);
            when(leasePort.leaseUndeliveredValidated(BATCH_SIZE, LEASE)).thenReturn(List.of(907L));
            when(repository.findById(907L)).thenReturn(Optional.of(sinFecha));

            job.retryDeliveries();

            verify(deliverService).deliverIfValidated(sinFecha);
        }
    }
}
