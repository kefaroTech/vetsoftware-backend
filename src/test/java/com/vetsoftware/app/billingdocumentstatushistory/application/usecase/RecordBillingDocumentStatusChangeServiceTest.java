package com.vetsoftware.app.billingdocumentstatushistory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentStatusHistoryRepository;
import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import com.vetsoftware.app.billingdocumentstatushistory.domain.SameStatusTransitionException;
import com.vetsoftware.app.billingdocumentstatushistory.testsupport.BillingDocumentStatusHistoryMother;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordBillingDocumentStatusChangeService — apuntar un cambio de estado")
class RecordBillingDocumentStatusChangeServiceTest {

    /**
     * Reloj fijo: el servicio sella {@code occurredAt} y {@code createdDate} con
     * el, y sin fijarlo el caso que afirma el sello se caeria solo al cruzar la
     * medianoche entre dos lineas.
     */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 31, 23, 59, 58);

    private final Clock reloj = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private BillingDocumentStatusHistoryRepository repository;
    @Mock
    private BillingDocumentValidationPort billingDocumentValidationPort;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("guarda el fotograma con el par de estados, el actor y el motivo del comando")
        void guarda_el_fotograma_con_los_datos_del_comando() {
            RecordBillingDocumentStatusChangeService service = servicio();
            when(billingDocumentValidationPort.existsByIdAndCompanyId(
                    BillingDocumentStatusHistoryMother.DOCUMENTO,
                    BillingDocumentStatusHistoryMother.EMPRESA)).thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BillingDocumentStatusHistoryDto guardado = service
                    .execute(BillingDocumentStatusHistoryMother.comando());

            ArgumentCaptor<BillingDocumentStatusHistory> capturado = ArgumentCaptor
                    .forClass(BillingDocumentStatusHistory.class);
            verify(repository).save(capturado.capture());
            assertThat(capturado.getValue().getFromStatus()).isEqualTo(BillingDocumentStatus.DRAFT);
            assertThat(capturado.getValue().getToStatus())
                    .isEqualTo(BillingDocumentStatus.AWAITING_EXTERNAL);
            assertThat(capturado.getValue().getActor())
                    .isEqualTo(BillingDocumentStatusHistoryMother.ACTOR_PERSONA);
            assertThat(capturado.getValue().getReason())
                    .isEqualTo(BillingDocumentStatusHistoryMother.MOTIVO);
            assertThat(guardado.toStatus()).isEqualTo(BillingDocumentStatus.AWAITING_EXTERNAL);
        }

        @Test
        @DisplayName("el momento del cambio lo pone el reloj del servidor, no el comando")
        void el_momento_del_cambio_lo_pone_el_reloj_del_servidor() {
            // El comando no trae occurredAt y no puede traerlo: es la columna por la
            // que se corta a una fecha, y aceptarla del cliente permitiria antedatar un
            // movimiento y reescribir cuantos documentos esperaban factura externa a 31
            // de marzo. El reloj esta fijado justo antes de esa medianoche.
            RecordBillingDocumentStatusChangeService service = servicio();
            when(billingDocumentValidationPort.existsByIdAndCompanyId(
                    BillingDocumentStatusHistoryMother.DOCUMENTO,
                    BillingDocumentStatusHistoryMother.EMPRESA)).thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BillingDocumentStatusHistoryDto guardado = service
                    .execute(BillingDocumentStatusHistoryMother.comando());

            assertThat(guardado.occurredAt()).isEqualTo(AHORA);
            assertThat(guardado.createdDate()).isEqualTo(AHORA);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("no escribe nada si el documento de cobro no existe")
        void no_escribe_nada_si_el_documento_no_existe() {
            RecordBillingDocumentStatusChangeService service = servicio();
            when(billingDocumentValidationPort.existsByIdAndCompanyId(
                    BillingDocumentStatusHistoryMother.DOCUMENTO,
                    BillingDocumentStatusHistoryMother.EMPRESA)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(BillingDocumentStatusHistoryMother.comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Billing document not found: 8500");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("no escribe nada si el cambio no cambia nada")
        void no_escribe_nada_si_el_cambio_no_cambia_nada() {
            // La invariante vive en el dominio, no aqui, y este caso congela que el
            // servicio no la esquiva: la fila no llega al repositorio. Una version que
            // validara despues de guardar dejaria el ruido dentro.
            RecordBillingDocumentStatusChangeService service = servicio();
            when(billingDocumentValidationPort.existsByIdAndCompanyId(
                    BillingDocumentStatusHistoryMother.DOCUMENTO,
                    BillingDocumentStatusHistoryMother.EMPRESA)).thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(BillingDocumentStatusHistoryMother.comandoSinCambio()))
                    .isInstanceOf(SameStatusTransitionException.class);

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("valida el documento con la empresa del comando y no solo por su id")
        void valida_el_documento_con_la_empresa_del_comando() {
            // Con la variante ancha del puerto, una clinica podria colgar un fotograma
            // de la factura de la vecina. El verify con las DOS columnas es lo que caza
            // que alguien cambie la llamada por un existsById a secas.
            RecordBillingDocumentStatusChangeService service = servicio();
            when(billingDocumentValidationPort.existsByIdAndCompanyId(
                    BillingDocumentStatusHistoryMother.DOCUMENTO,
                    BillingDocumentStatusHistoryMother.EMPRESA)).thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(BillingDocumentStatusHistoryMother.comando());

            verify(billingDocumentValidationPort).existsByIdAndCompanyId(
                    BillingDocumentStatusHistoryMother.DOCUMENTO,
                    BillingDocumentStatusHistoryMother.EMPRESA);
        }

        @Test
        @DisplayName("la empresa que se persiste es la del comando, la que inyecto el controller")
        void la_empresa_que_se_persiste_es_la_del_comando() {
            RecordBillingDocumentStatusChangeService service = servicio();
            when(billingDocumentValidationPort.existsByIdAndCompanyId(
                    BillingDocumentStatusHistoryMother.DOCUMENTO,
                    BillingDocumentStatusHistoryMother.EMPRESA)).thenReturn(true);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(BillingDocumentStatusHistoryMother.comando());

            ArgumentCaptor<BillingDocumentStatusHistory> capturado = ArgumentCaptor
                    .forClass(BillingDocumentStatusHistory.class);
            verify(repository).save(capturado.capture());
            assertThat(capturado.getValue().getCompanyId())
                    .isEqualTo(BillingDocumentStatusHistoryMother.EMPRESA);
        }
    }

    private RecordBillingDocumentStatusChangeService servicio() {
        return new RecordBillingDocumentStatusChangeService(repository,
                billingDocumentValidationPort, reloj);
    }
}
