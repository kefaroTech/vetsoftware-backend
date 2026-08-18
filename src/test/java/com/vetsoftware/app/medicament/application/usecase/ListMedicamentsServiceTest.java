package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListMedicamentsService")
class ListMedicamentsServiceTest {

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private ListMedicamentsService service;

    @Test
    @DisplayName("mapea la pagina de dominio a una pagina de DTOs conservando los metadatos")
    void mapea_la_pagina_conservando_metadatos() {
        Medicament medicamento = Medicament.create("Amoxicilina", null, null, true);
        when(repository.findAll(0, 20)).thenReturn(PageResult.of(List.of(medicamento), 0, 20, 1L));

        PageResult<MedicamentDto> pagina = service.listAll(0, 20);

        assertThat(pagina.content()).extracting(MedicamentDto::name).containsExactly("Amoxicilina");
        assertThat(pagina.totalElements()).isEqualTo(1L);
    }
}
