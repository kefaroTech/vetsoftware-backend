package com.vetsoftware.app.companysettings.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import com.vetsoftware.app.companysettings.application.port.out.CompanySettingRepository;
import com.vetsoftware.app.companysettings.testsupport.CompanySettingMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCompanySettingsService")
class ListCompanySettingsServiceTest {

    @Mock
    private CompanySettingRepository repository;

    @InjectMocks
    private ListCompanySettingsService service;

    @Test
    @DisplayName("mapea cada ajuste de la empresa a su dto")
    void mapea_cada_ajuste_de_la_empresa_a_su_dto() {
        when(repository.findByCompany(CompanySettingMother.COMPANY_ID)).thenReturn(List.of(
                CompanySettingMother.ajusteExistente(1L, CompanySettingMother.COMPANY_ID, "a", "1"),
                CompanySettingMother.ajusteExistente(2L, CompanySettingMother.COMPANY_ID, "b",
                        "2")));

        List<CompanySettingDto> resultado = service.listByCompany(CompanySettingMother.COMPANY_ID);

        assertThat(resultado).extracting(CompanySettingDto::propertyName).containsExactly("a", "b");
        assertThat(resultado).extracting(CompanySettingDto::value).containsExactly("1", "2");
    }

    @Test
    @DisplayName("una empresa sin ajustes devuelve una lista vacia")
    void una_empresa_sin_ajustes_devuelve_una_lista_vacia() {
        when(repository.findByCompany(CompanySettingMother.OTRA_COMPANY_ID)).thenReturn(List.of());

        List<CompanySettingDto> resultado = service
                .listByCompany(CompanySettingMother.OTRA_COMPANY_ID);

        assertThat(resultado).isEmpty();
    }
}
