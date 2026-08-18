package com.vetsoftware.app.companytaxprofile.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateCompanyTaxProfileService")
class ReactivateCompanyTaxProfileServiceTest {

    @Mock
    private CompanyTaxProfileRepository repository;

    @InjectMocks
    private ReactivateCompanyTaxProfileService service;

    @Test
    @DisplayName("reactiva y devuelve el perfil ya habilitado")
    void reactiva_y_devuelve_el_perfil_ya_habilitado() {
        when(repository.reactivate(CompanyTaxProfileMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                .thenReturn(Optional.of(CompanyTaxProfileMother.perfilNit()));

        CompanyTaxProfileDto dto = service.execute(CompanyTaxProfileMother.COMPANY_ID);

        assertThat(dto.enabled()).isTrue();
        verify(repository).reactivate(CompanyTaxProfileMother.COMPANY_ID);
    }

    @Test
    @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
    void cero_filas_afectadas_es_no_encontrado() {
        when(repository.reactivate(CompanyTaxProfileMother.COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(CompanyTaxProfileMother.COMPANY_ID))
                .isInstanceOf(CompanyTaxProfileNotFoundException.class).hasMessageContaining(
                        "not found for company: " + CompanyTaxProfileMother.COMPANY_ID);

        verify(repository, never()).findByCompanyId(anyLong());
    }

    @Test
    @DisplayName("si el perfil desaparece entre el UPDATE y el SELECT, falla como no-encontrado")
    void si_el_perfil_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(CompanyTaxProfileMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByCompanyId(CompanyTaxProfileMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(CompanyTaxProfileMother.COMPANY_ID))
                .isInstanceOf(CompanyTaxProfileNotFoundException.class);
    }
}
