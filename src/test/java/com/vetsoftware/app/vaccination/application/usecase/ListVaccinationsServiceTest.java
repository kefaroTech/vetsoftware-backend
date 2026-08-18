package com.vetsoftware.app.vaccination.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.testsupport.VaccinationMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListVaccinationsService")
class ListVaccinationsServiceTest {

    @Mock
    private VaccinationRepository repository;
    @InjectMocks
    private ListVaccinationsService service;

    @Test
    @DisplayName("delega en el repositorio y mapea cada fila a su dto")
    void delega_en_el_repositorio_y_mapea_cada_fila() {
        when(repository.findAll())
                .thenReturn(List.of(VaccinationMother.vigente(), VaccinationMother.sinConsulta()));

        List<VaccinationDto> lista = service.listAll();

        assertThat(lista).containsExactly(VaccinationDto.from(VaccinationMother.vigente()),
                VaccinationDto.from(VaccinationMother.sinConsulta()));
    }

    @Test
    @DisplayName("sin vacunas devuelve una lista vacia")
    void sin_vacunas_devuelve_una_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
