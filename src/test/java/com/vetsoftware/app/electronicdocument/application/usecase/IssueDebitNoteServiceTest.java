package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.EMPLOYEE_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaYaReversada;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.notaDebito;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.IssueDebitNoteCommand;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.DebitNoteReason;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("IssueDebitNoteService — emite nota debito sobre una factura validada")
class IssueDebitNoteServiceTest {

    private static final Long DOCUMENT_ID = 81L;

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private ElectronicDocumentEmitter emitter;
    @Mock
    private TransactionTemplate transactionTemplate;

    private IssueDebitNoteService service;

    @BeforeEach
    void montar() {
        service = new IssueDebitNoteService(repository, emitter, transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer(
                inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    private static IssueDebitNoteCommand comando() {
        return new IssueDebitNoteCommand(DOCUMENT_ID, DebitNoteReason.INTERESES, COMPANY_ID,
                EMPLOYEE_ID, null);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("guarda la nota PENDIENTE y la emite a traves del emisor")
        void guarda_pendiente_y_emite() {
            ElectronicDocument original = facturaValidada(DOCUMENT_ID);
            ElectronicDocument nota = notaDebito(201L);
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(original));
            when(repository.save(any())).thenReturn(nota);
            when(emitter.emit(nota)).thenReturn(nota);

            var dto = service.execute(comando());

            assertThat(dto.id()).isEqualTo(201L);
        }
    }

    @Nested
    @DisplayName("validaciones que abortan sin persistir")
    class Validaciones {

        @Test
        @DisplayName("documento inexistente lanza ElectronicDocumentNotFoundException")
        void documento_inexistente() {
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .hasMessageContaining(String.valueOf(DOCUMENT_ID));
            verifyNoInteractions(emitter);
        }

        /**
         * El filtro por empresa vive ahora EN la consulta: la factura ajena no llega a
         * cargarse. El {@code never()} sobre la variante ancha es lo que impide
         * reintroducir el {@code findById} + {@code if} posterior.
         */
        @Test
        @DisplayName("documento de otra empresa se reporta como no encontrado")
        void documento_de_otra_empresa_no_encontrado() {
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .hasMessageContaining(String.valueOf(DOCUMENT_ID));
            verify(repository, never()).findById(DOCUMENT_ID);
            verify(repository, never()).save(any());
            verifyNoInteractions(emitter);
        }

        @Test
        @DisplayName("no se puede emitir una nota debito sobre otra nota")
        void no_permite_nota_sobre_nota() {
            ElectronicDocument nota = notaDebito(DOCUMENT_ID);
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(nota));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nota debito sobre otra nota");
            verifyNoInteractions(emitter);
        }

        @Test
        @DisplayName("una factura PENDIENTE no admite nota debito")
        void factura_pendiente_no_admite_nota() {
            ElectronicDocument pendiente = facturaPendienteConId(DOCUMENT_ID);
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(pendiente));

            assertThatThrownBy(() -> service.execute(comando())).hasMessageContaining("PENDIENTE");
            verifyNoInteractions(emitter);
        }

        @Test
        @DisplayName("una factura ya reversada no admite nota debito")
        void factura_ya_reversada_no_admite_nota() {
            ElectronicDocument reversada = facturaYaReversada(DOCUMENT_ID);
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(reversada));

            assertThatThrownBy(() -> service.execute(comando()))
                    .hasMessageContaining(String.valueOf(DOCUMENT_ID));
            verifyNoInteractions(emitter);
        }
    }
}
