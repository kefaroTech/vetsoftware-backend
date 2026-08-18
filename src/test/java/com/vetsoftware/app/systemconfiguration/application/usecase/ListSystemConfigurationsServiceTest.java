package com.vetsoftware.app.systemconfiguration.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemconfiguration.application.dto.SystemConfigurationDto;
import com.vetsoftware.app.systemconfiguration.application.port.out.SystemConfigurationRepository;
import com.vetsoftware.app.systemconfiguration.testsupport.SystemConfigurationMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSystemConfigurationsService")
class ListSystemConfigurationsServiceTest {

    @Mock
    private SystemConfigurationRepository repository;

    @InjectMocks
    private ListSystemConfigurationsService service;

    @Test
    @DisplayName("mapea cada configuracion a su dto")
    void mapea_cada_configuracion_a_su_dto() {
        when(repository.findAll()).thenReturn(
                List.of(SystemConfigurationMother.configuracionExistente(1L, "uvt", "47065"),
                        SystemConfigurationMother.configuracionExistente(2L, "umbral", "5")));

        List<SystemConfigurationDto> resultado = service.listAll();

        assertThat(resultado).extracting(SystemConfigurationDto::propertyName)
                .containsExactly("uvt", "umbral");
        assertThat(resultado).extracting(SystemConfigurationDto::value).containsExactly("47065",
                "5");
    }

    @Test
    @DisplayName("sin filas configuradas devuelve una lista vacia")
    void sin_filas_configuradas_devuelve_una_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
