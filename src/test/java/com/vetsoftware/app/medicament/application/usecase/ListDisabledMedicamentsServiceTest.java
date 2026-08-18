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
@DisplayName("ListDisabledMedicamentsService")
class ListDisabledMedicamentsServiceTest {

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private ListDisabledMedicamentsService service;

    @Test
    @DisplayName("delega en findAllDisabledForCompany y mapea a DTO")
    void delega_en_find_all_disabled_for_company() {
        CompanyRef company = new CompanyRef(9L, "Clinica Norte", "900123456");
        Medicament pausado = Medicament.create("Suero", null, company, false);
        pausado.disable();
        when(repository.findAllDisabledForCompany(9L)).thenReturn(List.of(pausado));

        List<com.vetsoftware.app.medicament.application.dto.MedicamentDto> pausados = service
                .listDisabled(9L);

        assertThat(pausados).extracting(dto -> dto.enabled()).containsExactly(false);
    }
}
