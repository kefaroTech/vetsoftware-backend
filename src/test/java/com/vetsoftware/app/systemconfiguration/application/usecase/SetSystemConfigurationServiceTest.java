package com.vetsoftware.app.systemconfiguration.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemconfiguration.application.command.SetSystemConfigurationCommand;
import com.vetsoftware.app.systemconfiguration.application.dto.SystemConfigurationDto;
import com.vetsoftware.app.systemconfiguration.application.port.out.SystemConfigurationRepository;
import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import com.vetsoftware.app.systemconfiguration.testsupport.SystemConfigurationMother;
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
@DisplayName("SetSystemConfigurationService")
class SetSystemConfigurationServiceTest {

    @Mock
    private SystemConfigurationRepository repository;

    @InjectMocks
    private SetSystemConfigurationService service;

    @Nested
    @DisplayName("configuracion ya existente")
    class ConfiguracionExistente {

        @Test
        @DisplayName("actualiza el value de la fila encontrada en vez de crear una nueva")
        void actualiza_el_value_de_la_fila_encontrada() {
            SystemConfiguration existente = SystemConfigurationMother.configuracionExistente();
            when(repository.findByPropertyName(SystemConfigurationMother.PROPERTY_NAME))
                    .thenReturn(Optional.of(existente));
            when(repository.save(existente)).thenReturn(existente);

            SystemConfigurationDto resultado = service.execute(new SetSystemConfigurationCommand(
                    SystemConfigurationMother.PROPERTY_NAME, "47100"));

            ArgumentCaptor<SystemConfiguration> guardado = ArgumentCaptor
                    .forClass(SystemConfiguration.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getValue()).isEqualTo("47100");
            assertThat(guardado.getValue().getId()).isEqualTo(SystemConfigurationMother.CONFIG_ID);
            assertThat(resultado.value()).isEqualTo("47100");
        }

        @Test
        @DisplayName("un value en blanco rechaza la actualizacion sin guardar")
        void un_value_en_blanco_rechaza_la_actualizacion_sin_guardar() {
            SystemConfiguration existente = SystemConfigurationMother.configuracionExistente();
            when(repository.findByPropertyName(SystemConfigurationMother.PROPERTY_NAME))
                    .thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.execute(new SetSystemConfigurationCommand(
                    SystemConfigurationMother.PROPERTY_NAME, "   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("value is required");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("configuracion inexistente")
    class ConfiguracionInexistente {

        @Test
        @DisplayName("crea una configuracion nueva cuando no habia fila previa")
        void crea_una_configuracion_nueva() {
            when(repository.findByPropertyName("nueva.propiedad")).thenReturn(Optional.empty());
            when(repository.save(any(SystemConfiguration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            SystemConfigurationDto resultado = service
                    .execute(new SetSystemConfigurationCommand("nueva.propiedad", "10"));

            ArgumentCaptor<SystemConfiguration> guardado = ArgumentCaptor
                    .forClass(SystemConfiguration.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getId()).isNull();
            assertThat(guardado.getValue().getPropertyName()).isEqualTo("nueva.propiedad");
            assertThat(resultado.value()).isEqualTo("10");
        }
    }
}
