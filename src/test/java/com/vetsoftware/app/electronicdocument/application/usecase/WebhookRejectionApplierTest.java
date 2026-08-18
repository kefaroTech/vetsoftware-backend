package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookRejectionApplier — aplica el rechazo reportado por un webhook")
class WebhookRejectionApplierTest {

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private TransmissionLogPort transmissionLog;
    @Mock
    private NumberAssigner numberAssigner;

    private WebhookRejectionApplier applier;

    @BeforeEach
    void montar() {
        applier = new WebhookRejectionApplier(repository, transmissionLog, numberAssigner);
    }

    @Test
    @DisplayName("marca rechazado, libera el consecutivo y deja bitacora con el motivo")
    void marca_rechazado_libera_consecutivo_y_deja_bitacora() {
        ElectronicDocument documento = facturaPendienteConId(100L);

        applier.apply(documento, "MATIAS", "KEY-77", "documento con error de firma");

        assertThat(documento.getDianStatus()).isEqualTo(DianStatus.RECHAZADO);
        verify(numberAssigner).release(documento);
        verify(repository).updateDianResult(documento);
        verify(transmissionLog).record(documento.getId(), "MATIAS", 200, "KEY-77",
                TransmissionResult.REJECTED, "documento con error de firma");
    }
}
