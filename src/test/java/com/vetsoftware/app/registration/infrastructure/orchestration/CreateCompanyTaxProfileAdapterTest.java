package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.in.CreateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Único punto de orquestación que conoce la feature companytaxprofile. El
 * dígito de verificación va siempre {@code null}: lo autocalcula el service
 * para NIT; el adaptador no lo toca.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCompanyTaxProfileAdapter")
class CreateCompanyTaxProfileAdapterTest {

    @Mock
    private CreateCompanyTaxProfileUseCase createCompanyTaxProfileUseCase;
    @Mock
    private SystemAuthRunner systemAuthRunner;
    @InjectMocks
    private CreateCompanyTaxProfileAdapter adapter;

    @BeforeEach
    void setUp() {
        when(systemAuthRunner.call(any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    private static CompanyTaxProfileDto dto() {
        return new CompanyTaxProfileDto(1L, null, CompanyDocumentType.NIT, "900123456", "3",
                "Veterinaria Vetrina", TaxRegime.RESPONSABLE_IVA, "fiscal@vetrina.co", null, null,
                List.of(), LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("mapea el NIT sin dígito de verificación (lo autocalcula el service) y sin responsabilidades")
    void mapea_el_perfil_fiscal_sin_dv_ni_responsabilidades() {
        when(createCompanyTaxProfileUseCase.execute(any(CreateCompanyTaxProfileCommand.class)))
                .thenReturn(dto());

        adapter.create(9L, "NIT", "900123456", "Veterinaria Vetrina", "RESPONSABLE_IVA",
                "fiscal@vetrina.co");

        ArgumentCaptor<CreateCompanyTaxProfileCommand> captor = ArgumentCaptor
                .forClass(CreateCompanyTaxProfileCommand.class);
        verify(createCompanyTaxProfileUseCase).execute(captor.capture());
        CreateCompanyTaxProfileCommand command = captor.getValue();
        assertThat(command.companyDocumentType()).isEqualTo(CompanyDocumentType.NIT);
        assertThat(command.companyDocumentId()).isEqualTo("900123456");
        assertThat(command.companyDocumentVerificationDigit()).isNull();
        assertThat(command.legalName()).isEqualTo("Veterinaria Vetrina");
        assertThat(command.taxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
        assertThat(command.fiscalEmail()).isEqualTo("fiscal@vetrina.co");
        assertThat(command.commercialName()).isNull();
        assertThat(command.economicActivityId()).isNull();
        assertThat(command.responsibilityCodes()).isEmpty();
        assertThat(command.companyId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("un tipo de documento distinto de NIT (cédula) también se mapea sin dígito de verificación")
    void un_tipo_de_documento_distinto_de_nit_tambien_se_mapea() {
        when(createCompanyTaxProfileUseCase.execute(any(CreateCompanyTaxProfileCommand.class)))
                .thenReturn(dto());

        adapter.create(9L, "CEDULA_CIUDADANIA", "1020304050", "Orlando Velásquez",
                "NO_RESPONSABLE_IVA", "fiscal@vetrina.co");

        ArgumentCaptor<CreateCompanyTaxProfileCommand> captor = ArgumentCaptor
                .forClass(CreateCompanyTaxProfileCommand.class);
        verify(createCompanyTaxProfileUseCase).execute(captor.capture());
        assertThat(captor.getValue().companyDocumentType())
                .isEqualTo(CompanyDocumentType.CEDULA_CIUDADANIA);
        assertThat(captor.getValue().taxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
    }
}
