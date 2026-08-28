package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageNotFoundException;
import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindExternalInvoicingOutageService")
class FindExternalInvoicingOutageServiceTest {

    @Mock
    private ExternalInvoicingOutageRepository repository;

    @InjectMocks
    private FindExternalInvoicingOutageService service;

    @Test
    @DisplayName("devuelve el DTO de la caida encontrada")
    void devuelve_el_dto_de_la_caida_encontrada() {
        when(repository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                .thenReturn(Optional.of(ExternalInvoicingOutageMother.abierta()));

        ExternalInvoicingOutageDto dto = service.execute(ExternalInvoicingOutageMother.OUTAGE_ID);

        assertThat(dto.id()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
        assertThat(dto.open()).isTrue();
    }

    @Test
    @DisplayName("una caida inexistente revienta con 404")
    void una_caida_inexistente_revienta() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(999L))
                .isInstanceOf(ExternalInvoicingOutageNotFoundException.class)
                .hasMessageContaining("External invoicing outage not found: 999");
    }
}
