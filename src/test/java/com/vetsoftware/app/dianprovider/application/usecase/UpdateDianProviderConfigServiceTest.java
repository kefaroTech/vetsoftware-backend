package com.vetsoftware.app.dianprovider.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.dianprovider.application.command.UpdateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import com.vetsoftware.app.dianprovider.application.port.out.DianProviderConfigRepository;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfigNotFoundException;
import com.vetsoftware.app.dianprovider.domain.ProviderType;
import com.vetsoftware.app.dianprovider.testsupport.DianProviderConfigMother;
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
@DisplayName("UpdateDianProviderConfigService")
class UpdateDianProviderConfigServiceTest {

    @Mock
    private DianProviderConfigRepository repository;

    @InjectMocks
    private UpdateDianProviderConfigService service;

    @Captor
    private ArgumentCaptor<DianProviderConfig> configCaptor;

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("aplica los cambios sobre la config existente y la guarda")
        void aplica_los_cambios_sobre_la_config_existente_y_la_guarda() {
            DianProviderConfig existente = DianProviderConfigMother.configValida();
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(DianProviderConfigMother.comandoActualizar());

            verify(repository).save(configCaptor.capture());
            DianProviderConfig guardado = configCaptor.getValue();
            assertThat(guardado.getBaseUrl()).isEqualTo("https://api.matias.test/v2");
            assertThat(guardado.getClientId()).isEqualTo("client-id-2");
            assertThat(guardado.getUsername()).isEqualTo("user2@test.com");
            assertThat(guardado.getNumberingProviderRef()).isEqualTo("RES-002");
            // update() es una mutacion sobre la misma instancia leida: id, company y
            // createdDate no vienen del comando.
            assertThat(guardado.getId()).isEqualTo(DianProviderConfigMother.CONFIG_ID);
            assertThat(guardado.getCompany()).isEqualTo(DianProviderConfigMother.CLINICA);
        }

        @Test
        @DisplayName("devuelve el DTO con los datos ya actualizados")
        void devuelve_el_dto_con_los_datos_ya_actualizados() {
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.of(DianProviderConfigMother.configValida()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DianProviderConfigDto dto = service
                    .execute(DianProviderConfigMother.comandoActualizar());

            assertThat(dto.baseUrl()).isEqualTo("https://api.matias.test/v2");
            assertThat(dto.numberingProviderRef()).isEqualTo("RES-002");
        }
    }

    @Nested
    @DisplayName("empresa sin config")
    class EmpresaSinConfig {

        @Test
        @DisplayName("propaga DianProviderConfigNotFoundException y no guarda nada")
        void propaga_not_found_y_no_guarda_nada() {
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DianProviderConfigMother.comandoActualizar()))
                    .isInstanceOf(DianProviderConfigNotFoundException.class)
                    .hasMessageContaining(DianProviderConfigMother.COMPANY_ID.toString());

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("una baseUrl en blanco no llega a persistirse y no deja la config a medias")
        void una_base_url_en_blanco_no_llega_a_persistirse() {
            DianProviderConfig existente = DianProviderConfigMother.configValida();
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.of(existente));
            UpdateDianProviderConfigCommand comando = new UpdateDianProviderConfigCommand(
                    ProviderType.MATIAS, "   ", "client-id-2", "client-secret-2", "user2@test.com",
                    "secret-pass-2", null, "webhook-secret-2", "RES-002",
                    DianProviderConfigMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("baseUrl is required");

            verify(repository, never()).save(any());
            assertThat(existente.getBaseUrl()).isEqualTo("https://api.matias.test");
        }
    }
}
