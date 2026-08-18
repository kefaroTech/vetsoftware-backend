package com.vetsoftware.app.withholdingconfig.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingconfig.application.dto.WithholdingConfigDto;
import com.vetsoftware.app.withholdingconfig.application.port.out.WithholdingConfigRepository;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfigNotFoundException;
import com.vetsoftware.app.withholdingconfig.testsupport.WithholdingConfigMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindWithholdingConfigService")
class FindWithholdingConfigServiceTest {

    private static final Long COMPANY_ID = WithholdingConfigMother.COMPANY_ID;

    @Mock
    private WithholdingConfigRepository repository;
    @InjectMocks
    private FindWithholdingConfigService service;

    @Test
    @DisplayName("devuelve la configuracion de la company")
    void devuelve_la_configuracion_de_la_company() {
        when(repository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(WithholdingConfigMother.configValida()));

        WithholdingConfigDto dto = service.findByCompany(COMPANY_ID);

        assertThat(dto.companyId()).isEqualTo(COMPANY_ID);
    }

    @Test
    @DisplayName("company sin configuracion lanza WithholdingConfigNotFoundException")
    void company_sin_configuracion_lanza_excepcion() {
        when(repository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCompany(COMPANY_ID))
                .isInstanceOf(WithholdingConfigNotFoundException.class)
                .hasMessageContaining(String.valueOf(COMPANY_ID));
    }
}
