package com.vetsoftware.app.companytaxprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import com.vetsoftware.app.companytaxprofile.testsupport.CompanyTaxProfileMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCompanyTaxProfileService")
class DeleteCompanyTaxProfileServiceTest {

    @Mock
    private CompanyTaxProfileRepository repository;

    @InjectMocks
    private DeleteCompanyTaxProfileService service;

    @Test
    @DisplayName("borra el perfil de la empresa cuando existe")
    void borra_el_perfil_cuando_existe() {
        when(repository.existsByCompanyId(CompanyTaxProfileMother.COMPANY_ID)).thenReturn(true);

        service.execute(CompanyTaxProfileMother.COMPANY_ID);

        verify(repository).delete(CompanyTaxProfileMother.COMPANY_ID);
    }

    @Test
    @DisplayName("perfil inexistente: no ejecuta el borrado")
    void perfil_inexistente_no_ejecuta_el_borrado() {
        when(repository.existsByCompanyId(CompanyTaxProfileMother.COMPANY_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(CompanyTaxProfileMother.COMPANY_ID))
                .isInstanceOf(CompanyTaxProfileNotFoundException.class).hasMessageContaining(
                        "not found for company: " + CompanyTaxProfileMother.COMPANY_ID);

        verify(repository, never()).delete(CompanyTaxProfileMother.COMPANY_ID);
    }
}
