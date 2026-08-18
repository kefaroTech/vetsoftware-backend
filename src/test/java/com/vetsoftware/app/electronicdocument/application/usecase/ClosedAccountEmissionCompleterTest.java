package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClosedAccountEmissionCompleter — completa la emision del cierre en su propia transaccion")
class ClosedAccountEmissionCompleterTest {

    private static final Long DOCUMENT_ID = 97L;
    private static final Long COMPANY_ID = 9L;
    private static final Long OTHER_COMPANY_ID = 90L;

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private ElectronicDocumentEmitter emitter;
    @Mock
    private DeliverElectronicDocumentService deliverService;

    private ClosedAccountEmissionCompleter completer;

    @BeforeEach
    void montar() {
        completer = new ClosedAccountEmissionCompleter(repository, emitter, deliverService);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("emite el documento y entrega la representacion si quedo validado")
        void emite_y_entrega() {
            ElectronicDocument pendiente = facturaPendienteConId(DOCUMENT_ID);
            ElectronicDocument validada = facturaValidada(DOCUMENT_ID);
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.of(pendiente));
            when(emitter.emit(pendiente)).thenReturn(validada);

            completer.complete(DOCUMENT_ID, COMPANY_ID);

            verify(deliverService).deliverIfValidated(validada);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("documento inexistente lanza ElectronicDocumentNotFoundException")
        void documento_inexistente() {
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> completer.complete(DOCUMENT_ID, COMPANY_ID))
                    .hasMessageContaining(String.valueOf(DOCUMENT_ID));
        }

        /**
         * Esta clase no implementa ningun {@code port/in}: su unica barrera es el
         * {@code companyId} que le pasa su llamador. Con un documento de otra empresa
         * no se emite ni se entrega nada — que es lo grave aqui, porque emitir es
         * irreversible (numeracion fiscal consumida + envio a la DIAN).
         */
        @Test
        @DisplayName("documento de otra empresa no se emite ni se entrega")
        void documento_de_otra_empresa_no_se_emite() {
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, OTHER_COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> completer.complete(DOCUMENT_ID, OTHER_COMPANY_ID))
                    .hasMessageContaining(String.valueOf(DOCUMENT_ID));

            verify(repository, never()).findById(DOCUMENT_ID);
            verifyNoInteractions(emitter, deliverService);
        }
    }
}
