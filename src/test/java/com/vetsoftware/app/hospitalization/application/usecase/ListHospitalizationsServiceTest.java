package com.vetsoftware.app.hospitalization.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListHospitalizationsService")
class ListHospitalizationsServiceTest {

    @Mock
    private HospitalizationRepository repository;

    @InjectMocks
    private ListHospitalizationsService service;

    @Test
    @DisplayName("mapea cada hospitalizacion a su DTO conservando el orden del repositorio")
    void mapea_cada_hospitalizacion_conservando_el_orden() {
        when(repository.findAll()).thenReturn(List.of(HospitalizationMother.internado(),
                HospitalizationMother.ambulatorioSinConsulta()));

        List<HospitalizationDto> resultado = service.listAll();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).reason()).isEqualTo("Gastroenteritis aguda");
        assertThat(resultado.get(0).consultation()).isNotNull();
        assertThat(resultado.get(1).reason()).isEqualTo("Control post quirurgico");
        assertThat(resultado.get(1).consultation()).isNull();
    }

    @Test
    @DisplayName("sin hospitalizaciones devuelve lista vacia, nunca null")
    void sin_hospitalizaciones_devuelve_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
