package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.OPEN_ACCOUNT_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmitElectronicDocumentOnCloseService — emision al cerrar/cobrar una cuenta")
class EmitElectronicDocumentOnCloseServiceTest {

    @Mock
    private DocumentBuilder documentBuilder;
    @Mock
    private ClosedAccountEmissionCompleter emissionCompleter;
    @Mock
    private ElectronicDocumentRepository repository;

    private EmitElectronicDocumentOnCloseService service;

    @BeforeEach
    void montar() {
        service = new EmitElectronicDocumentOnCloseService(documentBuilder, emissionCompleter,
                repository);
    }

    @AfterEach
    void limpiarSincronizacion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static EmitElectronicDocumentCommand comando() {
        return new EmitElectronicDocumentCommand(OPEN_ACCOUNT_ID, ElectronicDocumentType.FE_VENTA,
                COMPANY_ID, false);
    }

    @Test
    @DisplayName("una cuenta ya facturada es idempotente: no construye otro documento")
    void cuenta_ya_facturada_es_idempotente() {
        when(repository.existsByOpenAccountId(OPEN_ACCOUNT_ID)).thenReturn(true);

        assertThat(service.execute(comando())).isNull();

        verify(documentBuilder, never()).build(any(), any(), any(),
                org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Nested
    @DisplayName("dentro de una transaccion — registra el afterCommit")
    class DentroDeTransaccion {

        @BeforeEach
        void activarSincronizacion() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @Test
        @DisplayName("construye el documento PENDIENTE y no invoca el completer antes del commit")
        void construye_pendiente_y_difiere_la_emision_a_despues_del_commit() {
            ElectronicDocument documento = facturaPendienteConId(95L);
            when(repository.existsByOpenAccountId(OPEN_ACCOUNT_ID)).thenReturn(false);
            when(documentBuilder.build(OPEN_ACCOUNT_ID, ElectronicDocumentType.FE_VENTA, COMPANY_ID,
                    false)).thenReturn(documento);

            var dto = service.execute(comando());

            assertThat(dto.id()).isEqualTo(95L);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
            verify(emissionCompleter, never()).complete(any(), any());
        }

        @Test
        @DisplayName("al dispararse afterCommit, completa la emision del documento")
        void afterCommit_completa_la_emision() {
            ElectronicDocument documento = facturaPendienteConId(96L);
            when(repository.existsByOpenAccountId(OPEN_ACCOUNT_ID)).thenReturn(false);
            when(documentBuilder.build(any(), any(), any(),
                    org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(documento);

            service.execute(comando());
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager
                    .getSynchronizations();
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            verify(emissionCompleter).complete(96L, COMPANY_ID);
        }

        @Test
        @DisplayName("un fallo del completer en afterCommit se registra y no se propaga al caller")
        void fallo_del_completer_en_after_commit_no_se_propaga() {
            ElectronicDocument documento = facturaPendienteConId(97L);
            when(repository.existsByOpenAccountId(OPEN_ACCOUNT_ID)).thenReturn(false);
            when(documentBuilder.build(any(), any(), any(),
                    org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(documento);
            doThrow(new RuntimeException("MATIAS caido")).when(emissionCompleter).complete(97L,
                    COMPANY_ID);

            service.execute(comando());
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager
                    .getSynchronizations();

            // El propio patron afterCommit exige que la excepcion no suba: la transaccion
            // del cierre ya confirmo y el documento queda PENDIENTE, re-emitible.
            synchronizations.forEach(TransactionSynchronization::afterCommit);
            verify(emissionCompleter).complete(97L, COMPANY_ID);
        }
    }
}
