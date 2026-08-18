package com.vetsoftware.app.clinicalhistory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventTypeCountDto;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import com.vetsoftware.app.clinicalhistory.testsupport.ClinicalHistoryMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetClinicalHistorySummaryService — delega el conteo por tipo")
class GetClinicalHistorySummaryServiceTest {

    private static final Long ANIMAL_ID = ClinicalHistoryMother.ANIMAL_ID;
    private static final Long COMPANY_ID = ClinicalHistoryMother.COMPANY_ID;

    @Mock
    private ClinicalEventRepository repository;

    private GetClinicalHistorySummaryService service;

    @org.junit.jupiter.api.BeforeEach
    void construirServicio() {
        service = new GetClinicalHistorySummaryService(repository);
    }

    @Test
    @DisplayName("devuelve tal cual la lista de conteos del repositorio")
    void devuelve_la_lista_de_conteos_tal_cual() {
        List<ClinicalEventTypeCountDto> conteos = List.of(
                new ClinicalEventTypeCountDto(ClinicalEventType.CONSULTATION, 5L),
                new ClinicalEventTypeCountDto(ClinicalEventType.VACCINATION, 2L));
        when(repository.countByType(ANIMAL_ID, COMPANY_ID)).thenReturn(conteos);

        List<ClinicalEventTypeCountDto> resultado = service.countByType(ANIMAL_ID, COMPANY_ID);

        assertThat(resultado).containsExactlyElementsOf(conteos);
    }

    @Test
    @DisplayName("un animal sin eventos devuelve lista vacía")
    void animal_sin_eventos_devuelve_lista_vacia() {
        when(repository.countByType(ANIMAL_ID, COMPANY_ID)).thenReturn(List.of());

        assertThat(service.countByType(ANIMAL_ID, COMPANY_ID)).isEmpty();
    }
}
