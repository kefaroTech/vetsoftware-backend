package com.vetsoftware.app.companytaxprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import com.vetsoftware.app.companytaxprofile.testsupport.CompanyTaxProfileMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindCompanyTaxProfileService")
class FindCompanyTaxProfileServiceTest {

    @Mock
    private CompanyTaxProfileRepository repository;

    @InjectMocks
    private FindCompanyTaxProfileService service;

    @Test
    @DisplayName("devuelve el DTO del perfil encontrado")
    void devuelve_el_dto_del_perfil_encontrado() {
        when(repository.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                .thenReturn(Optional.of(CompanyTaxProfileMother.perfilNit()));

        CompanyTaxProfileDto dto = service.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(CompanyTaxProfileMother.PROFILE_ID);
        assertThat(dto.legalName()).isEqualTo(CompanyTaxProfileMother.RAZON_SOCIAL);
    }

    @Test
    @DisplayName("perfil inexistente lanza CompanyTaxProfileNotFoundException")
    void perfil_inexistente_lanza_not_found() {
        when(repository.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                .isInstanceOf(CompanyTaxProfileNotFoundException.class).hasMessageContaining(
                        "not found for company: " + CompanyTaxProfileMother.COMPANY_ID);
    }
}
