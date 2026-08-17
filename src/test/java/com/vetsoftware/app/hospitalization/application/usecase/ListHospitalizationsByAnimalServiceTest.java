package com.vetsoftware.app.hospitalization.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListHospitalizationsByAnimalService")
class ListHospitalizationsByAnimalServiceTest {

    @Mock
    private HospitalizationRepository repository;

    @InjectMocks
    private ListHospitalizationsByAnimalService service;

    @Test
    @DisplayName("mapea el contenido de la pagina y conserva los metadatos de paginacion")
    void mapea_el_contenido_y_conserva_los_metadatos() {
        PageResult<Hospitalization> pagina = new PageResult<>(
                List.of(HospitalizationMother.internado()), 1, 10, 25L, 3);
        when(repository.findAllByAnimalIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                HospitalizationMother.COMPANY_ID, "gastro", 1, 10)).thenReturn(pagina);

        PageResult<HospitalizationDto> resultado = service.listByAnimal(
                HospitalizationMother.ANIMAL_ID, HospitalizationMother.COMPANY_ID, "gastro", 1, 10);

        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.content().get(0).id())
                .isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
        assertThat(resultado.page()).isEqualTo(1);
        assertThat(resultado.pageSize()).isEqualTo(10);
        assertThat(resultado.totalElements()).isEqualTo(25L);
        assertThat(resultado.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("el filtro de texto nulo se propaga tal cual al repositorio")
    void el_filtro_nulo_se_propaga_tal_cual() {
        when(repository.findAllByAnimalIdAndCompanyId(HospitalizationMother.ANIMAL_ID,
                HospitalizationMother.COMPANY_ID, null, 0, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        PageResult<HospitalizationDto> resultado = service.listByAnimal(
                HospitalizationMother.ANIMAL_ID, HospitalizationMother.COMPANY_ID, null, 0, 20);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isZero();
    }
}
