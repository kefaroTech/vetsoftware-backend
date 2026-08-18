package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAvailableMedicamentsService")
class ListAvailableMedicamentsServiceTest {

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private ListAvailableMedicamentsService service;

    @Test
    @DisplayName("delega en findAllAvailableForCompany y mapea a DTO")
    void delega_en_find_all_available_for_company() {
        CompanyRef company = new CompanyRef(9L, "Clinica Norte", "900123456");
        Medicament propio = Medicament.create("Suero", null, company, false);
        when(repository.findAllAvailableForCompany(9L)).thenReturn(List.of(propio));

        List<com.vetsoftware.app.medicament.application.dto.MedicamentDto> disponibles = service
                .listAvailable(9L);

        assertThat(disponibles).extracting(dto -> dto.name()).containsExactly("Suero");
    }
}
