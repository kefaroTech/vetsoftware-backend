package com.vetsoftware.app.electronicdocument.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Documento emitido al cerrar una cuenta, para reimprimir su recibo. Devuelve
 * vacio —no excepcion— cuando la cuenta no genero documento: es un caso normal
 * (cuenta cancelada o empresa sin facturacion), no un error.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindElectronicDocumentByAccountService — recibo de la cuenta cerrada")
class FindElectronicDocumentByAccountServiceTest {

    @Mock
    private ElectronicDocumentRepository repository;
    @InjectMocks
    private FindElectronicDocumentByAccountService service;

    @Test
    @DisplayName("devuelve el documento de la cuenta proyectado a DTO")
    void devuelve_el_documento_de_la_cuenta() {
        when(repository.findByOpenAccountId(100L, 9L))
                .thenReturn(Optional.of(ElectronicDocumentMother.facturaValidada(55L)));

        Optional<ElectronicDocumentDto> dto = service.findByOpenAccount(100L, 9L);

        assertThat(dto).isPresent();
        assertThat(dto.get().id()).isEqualTo(55L);
        assertThat(dto.get().openAccountId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("una cuenta que nunca genero documento devuelve vacio, no lanza")
    void una_cuenta_sin_documento_devuelve_vacio() {
        when(repository.findByOpenAccountId(100L, 9L)).thenReturn(Optional.empty());

        assertThat(service.findByOpenAccount(100L, 9L)).isEmpty();
    }

    @Test
    @DisplayName("acota la busqueda a la empresa del contexto y no escribe nada")
    void acota_la_busqueda_a_la_empresa_y_no_escribe() {
        when(repository.findByOpenAccountId(100L, 77L)).thenReturn(Optional.empty());

        assertThat(service.findByOpenAccount(100L, 77L)).isEmpty();

        verify(repository).findByOpenAccountId(100L, 77L);
        verifyNoMoreInteractions(repository);
    }
}
