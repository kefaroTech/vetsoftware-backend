package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.OutageAffectedCompanyDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageCompanyRepository;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.AffectedCompanyAlreadyRegisteredException;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
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
@DisplayName("RegisterAffectedCompanyService (externalinvoicingoutage)")
class RegisterAffectedCompanyServiceTest {

    @Mock
    private ExternalInvoicingOutageCompanyRepository repository;
    @Mock
    private ExternalInvoicingOutageRepository outageRepository;
    @Mock
    private CompanyValidationPort companyValidationPort;

    @InjectMocks
    private RegisterAffectedCompanyService service;

    @Captor
    private ArgumentCaptor<ExternalInvoicingOutageCompany> afectadaCaptor;

    @Nested
    @DisplayName("registro")
    class Registro {

        @Test
        @DisplayName("persiste la clinica alcanzada con los documentos fallidos y la resolucion del comando")
        void persiste_la_clinica_alcanzada() {
            when(outageRepository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.of(ExternalInvoicingOutageMother.abierta()));
            when(companyValidationPort.existsById(ExternalInvoicingOutageMother.COMPANY_ID))
                    .thenReturn(true);
            when(repository.existsByOutageIdAndCompanyId(ExternalInvoicingOutageMother.OUTAGE_ID,
                    ExternalInvoicingOutageMother.COMPANY_ID)).thenReturn(false);
            when(repository.save(any())).thenReturn(ExternalInvoicingOutageMother.afectada());

            OutageAffectedCompanyDto dto = service
                    .execute(ExternalInvoicingOutageMother.comandoRegistrarAfectada());

            verify(repository).save(afectadaCaptor.capture());
            ExternalInvoicingOutageCompany guardada = afectadaCaptor.getValue();
            assertThat(guardada.getId()).isNull();
            assertThat(guardada.getOutageId()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
            assertThat(guardada.getCompanyId()).isEqualTo(ExternalInvoicingOutageMother.COMPANY_ID);
            assertThat(guardada.getFailedDocumentCount())
                    .isEqualTo(ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT);
            assertThat(guardada.getResolvedBy())
                    .isEqualTo(ExternalInvoicingOutageMother.RESOLVED_BY);
            assertThat(dto.companyId()).isEqualTo(ExternalInvoicingOutageMother.COMPANY_ID);
        }

        @Test
        @DisplayName("una clinica ya registrada en el reparto revienta y no vuelve a guardar")
        void una_clinica_ya_registrada_revienta() {
            when(outageRepository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.of(ExternalInvoicingOutageMother.abierta()));
            when(companyValidationPort.existsById(ExternalInvoicingOutageMother.COMPANY_ID))
                    .thenReturn(true);
            when(repository.existsByOutageIdAndCompanyId(ExternalInvoicingOutageMother.OUTAGE_ID,
                    ExternalInvoicingOutageMother.COMPANY_ID)).thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(ExternalInvoicingOutageMother.comandoRegistrarAfectada()))
                    .isInstanceOf(AffectedCompanyAlreadyRegisteredException.class)
                    .hasMessageContaining("Company " + ExternalInvoicingOutageMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class Inexistentes {

        @Test
        @DisplayName("caida inexistente: no consulta ni la empresa ni el duplicado")
        void caida_inexistente() {
            when(outageRepository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ExternalInvoicingOutageMother.comandoRegistrarAfectada()))
                    .isInstanceOf(ExternalInvoicingOutageNotFoundException.class)
                    .hasMessageContaining("External invoicing outage not found: "
                            + ExternalInvoicingOutageMother.OUTAGE_ID);

            verifyNoInteractions(companyValidationPort, repository);
        }

        @Test
        @DisplayName("empresa inexistente: no consulta el duplicado ni persiste")
        void empresa_inexistente() {
            when(outageRepository.findById(ExternalInvoicingOutageMother.OUTAGE_ID))
                    .thenReturn(Optional.of(ExternalInvoicingOutageMother.abierta()));
            when(companyValidationPort.existsById(ExternalInvoicingOutageMother.COMPANY_ID))
                    .thenReturn(false);

            assertThatThrownBy(
                    () -> service.execute(ExternalInvoicingOutageMother.comandoRegistrarAfectada()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + ExternalInvoicingOutageMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }
}
