package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.ContingencyMonitorPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderResult;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransmissionResultPersister — aplica y persiste el desenlace de una transmision")
class TransmissionResultPersisterTest {

    private static final Long DOCUMENT_ID = 40L;

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private TransmissionLogPort transmissionLog;
    @Mock
    private CreditNoteReversalApplier reversalApplier;
    @Mock
    private NumberAssigner numberAssigner;
    @Mock
    private ContingencyMonitorPort contingencyMonitor;

    private TransmissionResultPersister persister;

    private static ProviderResult resultado(DianStatus status) {
        return new ProviderResult(status, "SETP", 991L,
                status == DianStatus.VALIDADO ? "CUFE-9" : null, null, "uuid-9", "<xml/>", "qr",
                "https://qr", null, "KEY-9",
                status == DianStatus.RECHAZADO ? "documento invalido" : null, null, 200, "{}");
    }

    @org.junit.jupiter.api.BeforeEach
    void montar() {
        persister = new TransmissionResultPersister(repository, transmissionLog, reversalApplier,
                numberAssigner, contingencyMonitor);
    }

    @Nested
    @DisplayName("persist — camino VALIDADO")
    class Validado {

        @Test
        @DisplayName("marca el documento validado, lo persiste y aplica el reverso de nota credito")
        void marca_valida_persiste_y_aplica_reverso() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(repository.updateDianResult(documento)).thenReturn(documento);

            ElectronicDocument saved = persister.persist(documento, resultado(DianStatus.VALIDADO),
                    "MATIAS");

            assertThat(saved.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
            assertThat(saved.getCufe()).isEqualTo("CUFE-9");
            verify(contingencyMonitor).recordOutcome(documento.getCompanyId(), true);
            verify(reversalApplier).applyIfCreditNoteValidated(saved);
        }

        @Test
        @DisplayName("registra la bitacora con el resultado ACCEPTED")
        void registra_bitacora_accepted() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(repository.updateDianResult(documento)).thenReturn(documento);

            persister.persist(documento, resultado(DianStatus.VALIDADO), "MATIAS");

            verify(transmissionLog).record(eq(DOCUMENT_ID), eq("MATIAS"), eq(200), eq("KEY-9"),
                    eq(TransmissionResult.ACCEPTED), eq((String) null));
        }

        @Test
        @DisplayName("un VALIDADO sin cufe ni cude igual se marca (queda registrado para atencion manual)")
        void validado_sin_sello_igual_se_marca() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(repository.updateDianResult(documento)).thenReturn(documento);
            ProviderResult sinSello = new ProviderResult(DianStatus.VALIDADO, "SETP", 991L, null,
                    null, "uuid-9", "<xml/>", "qr", "https://qr", null, "KEY-9", null, null, 200,
                    "{}");

            ElectronicDocument saved = persister.persist(documento, sinSello, "MATIAS");

            assertThat(saved.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
            assertThat(saved.getCufe()).isNull();
            assertThat(saved.getCude()).isNull();
        }

        @Test
        @DisplayName("cuando el proveedor trae su propia fecha de validacion, se respeta en vez de now()")
        void respeta_la_fecha_de_validacion_del_proveedor() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(repository.updateDianResult(documento)).thenReturn(documento);
            LocalDateTime fechaProveedor = LocalDateTime.of(2026, 3, 10, 8, 30);
            ProviderResult conFecha = new ProviderResult(DianStatus.VALIDADO, "SETP", 991L,
                    "CUFE-9", null, "uuid-9", "<xml/>", "qr", "https://qr", null, "KEY-9", null,
                    fechaProveedor, 200, "{}");

            ElectronicDocument saved = persister.persist(documento, conFecha, "MATIAS");

            assertThat(saved.getDianValidationDate()).isEqualTo(fechaProveedor);
        }
    }

    @Nested
    @DisplayName("persist — camino RECHAZADO")
    class Rechazado {

        @Test
        @DisplayName("marca rechazado, libera el consecutivo y no aplica reverso")
        void marca_rechazado_libera_consecutivo() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(repository.updateDianResult(documento)).thenReturn(documento);

            ElectronicDocument saved = persister.persist(documento, resultado(DianStatus.RECHAZADO),
                    "MATIAS");

            assertThat(saved.getDianStatus()).isEqualTo(DianStatus.RECHAZADO);
            verify(numberAssigner).release(documento);
            verify(contingencyMonitor).recordOutcome(documento.getCompanyId(), true);
        }
    }

    @Nested
    @DisplayName("persist — camino CONTINGENCIA")
    class Contingencia {

        @Test
        @DisplayName("marca contingencia y registra el fallo de infraestructura en el monitor")
        void marca_contingencia_y_reporta_infraestructura_caida() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(repository.updateDianResult(documento)).thenReturn(documento);

            ElectronicDocument saved = persister.persist(documento,
                    resultado(DianStatus.CONTINGENCIA), "MATIAS");

            assertThat(saved.getDianStatus()).isEqualTo(DianStatus.CONTINGENCIA);
            verify(contingencyMonitor).recordOutcome(documento.getCompanyId(), false);
            verify(numberAssigner, never()).release(any());
        }
    }

    @Nested
    @DisplayName("persist — camino PENDIENTE (async)")
    class Pendiente {

        @Test
        @DisplayName("no transiciona el estado: el webhook lo completara despues")
        void no_transiciona_estado_pendiente() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(repository.updateDianResult(documento)).thenReturn(documento);

            ElectronicDocument saved = persister.persist(documento, resultado(DianStatus.PENDIENTE),
                    "MATIAS");

            assertThat(saved.getDianStatus()).isEqualTo(DianStatus.PENDIENTE);
            verify(numberAssigner, never()).release(any());
        }
    }

    @Nested
    @DisplayName("persistReconciled — variante de reconciliacion")
    class Reconciliada {

        @Test
        @DisplayName("usa la clave de proveedor ya conocida y no vuelve a evaluar contingencia")
        void usa_clave_conocida_sin_reevaluar_contingencia() {
            ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
            when(repository.updateDianResult(documento)).thenReturn(documento);

            persister.persistReconciled(documento, resultado(DianStatus.VALIDADO), "MATIAS",
                    "KEY-1");

            verify(transmissionLog).record(eq(DOCUMENT_ID), eq("MATIAS"), eq(200), eq("KEY-1"),
                    eq(TransmissionResult.ACCEPTED), eq((String) null));
            verify(contingencyMonitor, never()).recordOutcome(any(),
                    org.mockito.ArgumentMatchers.anyBoolean());
        }
    }

    @ParameterizedTest
    @EnumSource(value = DianStatus.class, names = "NO_ELECTRONICO")
    @DisplayName("NO_ELECTRONICO no es un desenlace de transmision valido")
    void no_electronico_no_es_un_resultado_de_transmision(DianStatus status) {
        ElectronicDocument documento = facturaPendienteConId(DOCUMENT_ID);
        ProviderResult resultado = new ProviderResult(status, null, null, null, null, null, null,
                null, null, null, null, null, null, 200, "{}");
        when(repository.updateDianResult(documento)).thenReturn(documento);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> persister.persist(documento, resultado, "MATIAS"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NO_ELECTRONICO");
    }
}
