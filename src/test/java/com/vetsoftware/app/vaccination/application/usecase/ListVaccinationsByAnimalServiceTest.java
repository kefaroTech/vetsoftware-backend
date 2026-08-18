package com.vetsoftware.app.vaccination.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.domain.Vaccination;
import com.vetsoftware.app.vaccination.testsupport.VaccinationMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListVaccinationsByAnimalService")
class ListVaccinationsByAnimalServiceTest {

    @Mock
    private VaccinationRepository repository;
    @InjectMocks
    private ListVaccinationsByAnimalService service;

    @Test
    @DisplayName("delega en el repositorio y mapea cada fila a su dto")
    void delega_en_el_repositorio_y_mapea_cada_fila() {
        Vaccination vaccination = VaccinationMother.vigente();
        when(repository.findAllByAnimalIdAndCompanyId(VaccinationMother.FIRULAIS.id(),
                VaccinationMother.COMPANY_ID, "rabia", 0, 20))
                .thenReturn(new PageResult<>(List.of(vaccination), 0, 20, 1L, 1));

        PageResult<VaccinationDto> pagina = service.listByAnimal(VaccinationMother.FIRULAIS.id(),
                VaccinationMother.COMPANY_ID, "rabia", 0, 20);

        assertThat(pagina.content()).containsExactly(VaccinationDto.from(vaccination));
        assertThat(pagina.totalElements()).isEqualTo(1L);
    }
}
