package com.vetsoftware.app.dianprovider.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import com.vetsoftware.app.dianprovider.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.dianprovider.application.port.out.DianProviderConfigRepository;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
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
@DisplayName("CreateDianProviderConfigService")
class CreateDianProviderConfigServiceTest {

    @Mock
    private DianProviderConfigRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateDianProviderConfigService service;

    @Captor
    private ArgumentCaptor<DianProviderConfig> configCaptor;

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste la config con la CompanyRef resuelta por el puerto")
        void persiste_la_config_con_la_company_ref_resuelta_por_el_puerto() {
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(companyQueryPort.findById(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.of(DianProviderConfigMother.CLINICA));
            when(repository.save(any())).thenReturn(DianProviderConfigMother.configValida());

            service.execute(DianProviderConfigMother.comandoCrear());

            verify(repository).save(configCaptor.capture());
            DianProviderConfig guardado = configCaptor.getValue();
            // Lo que importa no es que se llamara a save, sino que se guardara ESTO: la
            // company tiene que venir del puerto, no fabricarse a mano con el id del
            // comando.
            assertThat(guardado.getCompany()).isEqualTo(DianProviderConfigMother.CLINICA);
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.getBaseUrl()).isEqualTo("https://api.matias.test");
            assertThat(guardado.getProvider())
                    .isEqualTo(com.vetsoftware.app.dianprovider.domain.ProviderType.MATIAS);
        }

        @Test
        @DisplayName("devuelve el DTO de la config ya persistida")
        void devuelve_el_dto_de_la_config_ya_persistida() {
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(companyQueryPort.findById(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.of(DianProviderConfigMother.CLINICA));
            when(repository.save(any())).thenReturn(DianProviderConfigMother.configValida());

            DianProviderConfigDto dto = service.execute(DianProviderConfigMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(DianProviderConfigMother.CONFIG_ID);
            assertThat(dto.baseUrl()).isEqualTo("https://api.matias.test");
        }
    }

    @Nested
    @DisplayName("la empresa ya tiene config — 0-o-1 por empresa")
    class EmpresaYaConfigurada {

        @Test
        @DisplayName("rechaza la segunda config y no llega a consultar la empresa")
        void rechaza_la_segunda_config_y_no_consulta_la_empresa() {
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.of(DianProviderConfigMother.configValida()));

            assertThatThrownBy(() -> service.execute(DianProviderConfigMother.comandoCrear()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya tiene una configuracion");

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("empresa inexistente")
    class EmpresaInexistente {

        @Test
        @DisplayName("no persiste nada si la empresa no existe")
        void no_persiste_nada_si_la_empresa_no_existe() {
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(companyQueryPort.findById(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DianProviderConfigMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Company not found: " + DianProviderConfigMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("una baseUrl en blanco no llega a persistirse")
        void una_base_url_en_blanco_no_llega_a_persistirse() {
            when(repository.findByCompanyId(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.empty());
            when(companyQueryPort.findById(DianProviderConfigMother.COMPANY_ID))
                    .thenReturn(Optional.of(DianProviderConfigMother.CLINICA));
            var comando = new com.vetsoftware.app.dianprovider.application.command.CreateDianProviderConfigCommand(
                    com.vetsoftware.app.dianprovider.domain.ProviderType.MATIAS, "   ", "client-id",
                    "client-secret", "user@test.com", "secret-pass", null, "webhook-secret",
                    "RES-001", DianProviderConfigMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("baseUrl is required");

            verify(repository, never()).save(any());
        }
    }
}
