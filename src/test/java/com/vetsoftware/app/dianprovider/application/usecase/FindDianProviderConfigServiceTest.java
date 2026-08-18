package com.vetsoftware.app.dianprovider.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import com.vetsoftware.app.dianprovider.application.port.out.DianProviderConfigRepository;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfigNotFoundException;
import com.vetsoftware.app.dianprovider.testsupport.DianProviderConfigMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindDianProviderConfigService")
class FindDianProviderConfigServiceTest {

    @Mock
    private DianProviderConfigRepository repository;

    @InjectMocks
    private FindDianProviderConfigService service;

    @Nested
    @DisplayName("empresa con config")
    class EmpresaConConfig {

        @Test
        @DisplayName("devuelve el DTO de la config de la empresa")
        void devuelve_el_dto_de_la_config_de_la_empresa() {
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.of(DianProviderConfigMother.configValida()));

            DianProviderConfigDto dto = service.findByCompany(DianProviderConfigMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(DianProviderConfigMother.CONFIG_ID);
            assertThat(dto.companyId()).isEqualTo(DianProviderConfigMother.COMPANY_ID);
            assertThat(dto.baseUrl()).isEqualTo("https://api.matias.test");
        }
    }

    @Nested
    @DisplayName("empresa sin config")
    class EmpresaSinConfig {

        @Test
        @DisplayName("propaga DianProviderConfigNotFoundException con el id de la empresa")
        void propaga_not_found_con_el_id_de_la_empresa() {
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findByCompany(DianProviderConfigMother.COMPANY_ID))
                    .isInstanceOf(DianProviderConfigNotFoundException.class)
                    .hasMessageContaining(DianProviderConfigMother.COMPANY_ID.toString());
        }
    }
}
