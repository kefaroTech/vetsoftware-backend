package com.vetsoftware.app.specie.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import com.vetsoftware.app.specie.domain.Specie;
import com.vetsoftware.app.specie.testsupport.SpecieMother;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSpeciesService")
class ListSpeciesServiceTest {

    @Mock
    private SpecieRepository repository;
    @InjectMocks
    private ListSpeciesService service;

    @Test
    @DisplayName("mapea cada especie del repositorio a su dto, en el mismo orden")
    void mapea_cada_especie_del_repositorio_a_su_dto() {
        Specie gato = new Specie(2L, "Gato", LocalDateTime.of(2026, 1, 15, 10, 30), true);
        when(repository.findAll()).thenReturn(List.of(SpecieMother.perro(), gato));

        List<SpecieDto> dtos = service.listAll();

        assertThat(dtos).extracting(SpecieDto::name).containsExactly("Perro", "Gato");
    }

    @Test
    @DisplayName("un repositorio vacio devuelve una lista vacia")
    void un_repositorio_vacio_devuelve_una_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
