package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAnimalsByOwnerService")
class ListAnimalsByOwnerServiceTest {

    private static final Long OWNER_ID = 3L;

    @Mock
    private AnimalRepository repository;

    @InjectMocks
    private ListAnimalsByOwnerService service;

    @Test
    @DisplayName("mapea a DTO conservando el orden del repositorio")
    void mapea_a_dto_conservando_el_orden() {
        when(repository.findByOwnerIdAndCompanyId(OWNER_ID, AnimalMother.COMPANY_ID))
                .thenReturn(List.of(AnimalMother.perroSano(7L), AnimalMother.perroSano(4L)));

        List<AnimalDto> resultado = service.listByOwner(OWNER_ID, AnimalMother.COMPANY_ID);

        assertThat(resultado).extracting(AnimalDto::id).containsExactly(7L, 4L);
    }

    @Test
    @DisplayName("un dueno sin animales devuelve lista vacia, no null")
    void un_dueno_sin_animales_devuelve_lista_vacia() {
        when(repository.findByOwnerIdAndCompanyId(OWNER_ID, AnimalMother.COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.listByOwner(OWNER_ID, AnimalMother.COMPANY_ID)).isEmpty();
    }

    @Test
    @DisplayName("consulta siempre acotando por empresa")
    void consulta_siempre_acotando_por_empresa() {
        when(repository.findByOwnerIdAndCompanyId(OWNER_ID, AnimalMother.COMPANY_ID))
                .thenReturn(List.of());

        service.listByOwner(OWNER_ID, AnimalMother.COMPANY_ID);

        verify(repository).findByOwnerIdAndCompanyId(OWNER_ID, AnimalMother.COMPANY_ID);
    }
}
