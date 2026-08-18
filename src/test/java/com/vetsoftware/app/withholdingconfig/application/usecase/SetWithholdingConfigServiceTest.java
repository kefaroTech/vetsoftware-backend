package com.vetsoftware.app.withholdingconfig.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingconfig.application.command.SetWithholdingConfigCommand;
import com.vetsoftware.app.withholdingconfig.application.dto.WithholdingConfigDto;
import com.vetsoftware.app.withholdingconfig.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.withholdingconfig.application.port.out.WithholdingConfigRepository;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfig;
import com.vetsoftware.app.withholdingconfig.testsupport.WithholdingConfigMother;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SetWithholdingConfigService — upsert")
class SetWithholdingConfigServiceTest {

    private static final Long COMPANY_ID = WithholdingConfigMother.COMPANY_ID;

    @Mock
    private WithholdingConfigRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @InjectMocks
    private SetWithholdingConfigService service;

    @Nested
    @DisplayName("cuando ya existe una configuracion para la company")
    class Actualiza {

        @Test
        @DisplayName("actualiza el existente sin consultar la company")
        void actualiza_el_existente_sin_consultar_la_company() {
            WithholdingConfig existente = WithholdingConfigMother.configValida();
            when(repository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SetWithholdingConfigCommand command = new SetWithholdingConfigCommand(
                    new BigDecimal("3"), new BigDecimal("4"), new BigDecimal("5"), COMPANY_ID);

            WithholdingConfigDto dto = service.execute(command);

            assertThat(dto.reteFuenteRate()).isEqualByComparingTo("3");
            assertThat(dto.reteIvaRate()).isEqualByComparingTo("4");
            assertThat(dto.reteIcaRate()).isEqualByComparingTo("5");
            verifyNoInteractions(companyQueryPort);

            ArgumentCaptor<WithholdingConfig> captor = ArgumentCaptor
                    .forClass(WithholdingConfig.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existente);
        }
    }

    @Nested
    @DisplayName("cuando no existe configuracion para la company")
    class Crea {

        @Test
        @DisplayName("busca la company y crea una configuracion nueva")
        void busca_la_company_y_crea_una_configuracion_nueva() {
            when(repository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());
            when(companyQueryPort.findById(COMPANY_ID))
                    .thenReturn(Optional.of(WithholdingConfigMother.VETERINARIA_CENTRAL));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SetWithholdingConfigCommand command = new SetWithholdingConfigCommand(BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, COMPANY_ID);

            WithholdingConfigDto dto = service.execute(command);

            assertThat(dto.companyId()).isEqualTo(COMPANY_ID);
            ArgumentCaptor<WithholdingConfig> captor = ArgumentCaptor
                    .forClass(WithholdingConfig.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getId()).isNull();
            assertThat(captor.getValue().getCompany())
                    .isEqualTo(WithholdingConfigMother.VETERINARIA_CENTRAL);
        }

        @Test
        @DisplayName("company inexistente no guarda nada")
        void company_inexistente_no_guarda_nada() {
            when(repository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());
            when(companyQueryPort.findById(COMPANY_ID)).thenReturn(Optional.empty());

            SetWithholdingConfigCommand command = new SetWithholdingConfigCommand(BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, COMPANY_ID);

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }
}
