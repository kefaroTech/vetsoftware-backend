package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageAlreadyEndedException;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageNotFoundException;
import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EndExternalInvoicingOutageService")
class EndExternalInvoicingOutageServiceTest {

    @Mock
    private ExternalInvoicingOutageRepository repository;

    @InjectMocks
    private EndExternalInvoicingOutageService service;

    @Captor
    private ArgumentCaptor<ExternalInvoicingOutage> caidaCaptor;

    @Nested
    @DisplayName("cierre")
    class Cierre {

        @Test
        @DisplayName("pone la hora de fin sobre la caida encontrada")
        void pone_la_hora_de_fin() {
            when(repository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.of(ExternalInvoicingOutageMother.abierta()));
            when(repository.save(any())).thenReturn(ExternalInvoicingOutageMother.cerrada());

            ExternalInvoicingOutageDto dto = service
                    .execute(ExternalInvoicingOutageMother.comandoCerrar());

            verify(repository).save(caidaCaptor.capture());
            ExternalInvoicingOutage guardada = caidaCaptor.getValue();
            assertThat(guardada.getEndedAt()).isEqualTo(ExternalInvoicingOutageMother.ENDED_AT);
            assertThat(guardada.isOpen()).isFalse();
            assertThat(dto.endedAt()).isEqualTo(ExternalInvoicingOutageMother.ENDED_AT);
            assertThat(dto.open()).isFalse();
        }

        @Test
        @DisplayName("una caida ya cerrada no se vuelve a cerrar")
        void una_caida_ya_cerrada_no_se_vuelve_a_cerrar() {
            when(repository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.of(ExternalInvoicingOutageMother.cerrada()));

            assertThatThrownBy(() -> service.execute(ExternalInvoicingOutageMother.comandoCerrar()))
                    .isInstanceOf(ExternalInvoicingOutageAlreadyEndedException.class)
                    .hasMessageContaining(
                            "External invoicing outage " + ExternalInvoicingOutageMother.OUTAGE_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("caida inexistente")
    class Inexistente {

        @Test
        @DisplayName("no escribe nada si la caida no existe")
        void no_escribe_nada_si_la_caida_no_existe() {
            when(repository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ExternalInvoicingOutageMother.comandoCerrar()))
                    .isInstanceOf(ExternalInvoicingOutageNotFoundException.class)
                    .hasMessageContaining("External invoicing outage not found: "
                            + ExternalInvoicingOutageMother.OUTAGE_ID);

            verify(repository, never()).save(any());
        }
    }
}
