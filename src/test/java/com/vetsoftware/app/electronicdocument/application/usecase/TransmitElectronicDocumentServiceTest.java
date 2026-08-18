package com.vetsoftware.app.electronicdocument.application.usecase;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.COMPANY_ID;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.command.TransmitElectronicDocumentCommand;
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
@DisplayName("TransmitElectronicDocumentService — retransmite un documento ya construido")
class TransmitElectronicDocumentServiceTest {

    private static final Long DOCUMENT_ID = 101L;

    @Mock
    private ElectronicDocumentRepository repository;
    @Mock
    private DocumentTransmitter transmitter;

    private TransmitElectronicDocumentService service;

    @BeforeEach
    void montar() {
        service = new TransmitElectronicDocumentService(repository, transmitter);
    }

    @Test
    @DisplayName("delega en el transmisor y mapea el resultado a dto")
    void delega_en_el_transmisor() {
        ElectronicDocument pendiente = facturaPendienteConId(DOCUMENT_ID);
        ElectronicDocument validada = facturaValidada(DOCUMENT_ID);
        when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(pendiente));
        when(transmitter.transmit(pendiente)).thenReturn(validada);

        var dto = service.execute(new TransmitElectronicDocumentCommand(DOCUMENT_ID, COMPANY_ID));

        assertThat(dto.id()).isEqualTo(DOCUMENT_ID);
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("documento inexistente lanza ElectronicDocumentNotFoundException")
        void documento_inexistente() {
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new TransmitElectronicDocumentCommand(DOCUMENT_ID, COMPANY_ID)))
                    .hasMessageContaining(String.valueOf(DOCUMENT_ID));
            verifyNoInteractions(transmitter);
        }

        /**
         * El filtro por empresa vive ahora EN la consulta: el documento ajeno no llega
         * a cargarse, asi que no se retransmite nada. El {@code never()} sobre la
         * variante ancha impide reintroducir el {@code findById} + {@code if}
         * posterior.
         */
        @Test
        @DisplayName("documento de otra empresa se reporta como no encontrado")
        void documento_de_otra_empresa_no_encontrado() {
            when(repository.findByIdAndCompanyId(DOCUMENT_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new TransmitElectronicDocumentCommand(DOCUMENT_ID, COMPANY_ID)))
                    .hasMessageContaining(String.valueOf(DOCUMENT_ID));
            verify(repository, never()).findById(DOCUMENT_ID);
            verifyNoInteractions(transmitter);
        }
    }
}
