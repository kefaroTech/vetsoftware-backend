package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicingoutage.application.command.NotifyAffectedCompaniesCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
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
@DisplayName("NotifyAffectedCompaniesService")
class NotifyAffectedCompaniesServiceTest {

    @Mock
    private ExternalInvoicingOutageRepository repository;

    @InjectMocks
    private NotifyAffectedCompaniesService service;

    @Captor
    private ArgumentCaptor<ExternalInvoicingOutage> caidaCaptor;

    @Nested
    @DisplayName("aviso")
    class Aviso {

        @Test
        @DisplayName("anota la marca y el contador corregido sobre la caida encontrada")
        void anota_la_marca_y_el_contador_corregido() {
            when(repository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.of(ExternalInvoicingOutageMother.abierta()));
            when(repository.save(any()))
                    .thenReturn(ExternalInvoicingOutageMother.abiertaNotificada());

            ExternalInvoicingOutageDto dto = service
                    .execute(ExternalInvoicingOutageMother.comandoNotificar());

            verify(repository).save(caidaCaptor.capture());
            ExternalInvoicingOutage guardada = caidaCaptor.getValue();
            assertThat(guardada.getNotifiedCompaniesAt())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT);
            assertThat(guardada.getAffectedCompanyCount())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANY_COUNT);
            assertThat(dto.notifiedCompaniesAt())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT);
        }

        @Test
        @DisplayName("es idempotente: un segundo aviso sobre una caida ya notificada sobrescribe la marca")
        void un_segundo_aviso_sobrescribe_la_marca_anterior() {
            when(repository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.of(ExternalInvoicingOutageMother.abiertaNotificada()));
            ExternalInvoicingOutage segundoAviso = ExternalInvoicingOutageMother.abiertaNotificada()
                    .notifyCompanies(
                            ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT.plusHours(3), 45);
            when(repository.save(any())).thenReturn(segundoAviso);
            NotifyAffectedCompaniesCommand comandoSegundoAviso = new NotifyAffectedCompaniesCommand(
                    ExternalInvoicingOutageMother.OUTAGE_ID,
                    ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT.plusHours(3), 45);

            service.execute(comandoSegundoAviso);

            verify(repository).save(caidaCaptor.capture());
            assertThat(caidaCaptor.getValue().getAffectedCompanyCount()).isEqualTo(45);
            assertThat(caidaCaptor.getValue().getNotifiedCompaniesAt())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT.plusHours(3));
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

            assertThatThrownBy(
                    () -> service.execute(ExternalInvoicingOutageMother.comandoNotificar()))
                    .isInstanceOf(ExternalInvoicingOutageNotFoundException.class)
                    .hasMessageContaining("External invoicing outage not found: "
                            + ExternalInvoicingOutageMother.OUTAGE_ID);

            verify(repository, never()).save(any());
        }
    }
}
