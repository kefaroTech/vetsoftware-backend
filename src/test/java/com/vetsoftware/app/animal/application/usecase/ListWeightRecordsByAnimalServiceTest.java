package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.dto.WeightRecordDto;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.animal.testsupport.WeightRecordMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListWeightRecordsByAnimalService")
class ListWeightRecordsByAnimalServiceTest {

    @Mock
    private WeightRecordRepository repository;

    @InjectMocks
    private ListWeightRecordsByAnimalService service;

    @Test
    @DisplayName("conserva el orden del repositorio: la serie ya viene mas reciente primero")
    void conserva_el_orden_del_repositorio() {
        LocalDate marzo = LocalDate.of(2026, 3, 1);
        LocalDate febrero = LocalDate.of(2026, 2, 1);
        when(repository.findByAnimalIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(List.of(WeightRecordMother.manual(new BigDecimal("13.00"), marzo),
                        WeightRecordMother.manual(new BigDecimal("12.50"), febrero)));

        List<WeightRecordDto> serie = service.listByAnimal(AnimalMother.ANIMAL_ID,
                AnimalMother.COMPANY_ID);

        assertThat(serie).extracting(WeightRecordDto::measuredAt).containsExactly(marzo, febrero);
        assertThat(serie).extracting(WeightRecordDto::value)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("13.00"), new BigDecimal("12.50"));
    }

    @Test
    @DisplayName("un animal sin pesos devuelve lista vacia, no null ni excepcion")
    void un_animal_sin_pesos_devuelve_lista_vacia() {
        when(repository.findByAnimalIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.listByAnimal(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID)).isEmpty();
    }

    @Test
    @DisplayName("consulta acotando por empresa")
    void consulta_acotando_por_empresa() {
        when(repository.findByAnimalIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(List.of());

        service.listByAnimal(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);

        verify(repository).findByAnimalIdAndCompanyId(AnimalMother.ANIMAL_ID,
                AnimalMother.COMPANY_ID);
    }
}
