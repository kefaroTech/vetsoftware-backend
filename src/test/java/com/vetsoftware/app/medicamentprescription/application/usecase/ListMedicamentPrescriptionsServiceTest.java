package com.vetsoftware.app.medicamentprescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListMedicamentPrescriptionsService")
class ListMedicamentPrescriptionsServiceTest {

    @Mock
    private MedicamentPrescriptionRepository repository;

    @InjectMocks
    private ListMedicamentPrescriptionsService service;

    @Test
    @DisplayName("mapea el contenido a DTO conservando la paginacion del repositorio")
    void mapea_el_contenido_conservando_la_paginacion() {
        PageResult<MedicamentPrescription> pagina = new PageResult<>(
                List.of(MedicamentPrescriptionMother.persistida(1L),
                        MedicamentPrescriptionMother.persistida(2L)),
                2, 20, 57L, 3);
        when(repository.findAll(2, 20)).thenReturn(pagina);

        PageResult<MedicamentPrescriptionDto> resultado = service.listAll(2, 20);

        assertThat(resultado.content()).extracting(MedicamentPrescriptionDto::id)
                .containsExactly(1L, 2L);
        assertThat(resultado.page()).isEqualTo(2);
        assertThat(resultado.pageSize()).isEqualTo(20);
        assertThat(resultado.totalElements()).isEqualTo(57L);
        assertThat(resultado.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("una pagina vacia no es un error")
    void una_pagina_vacia_no_es_un_error() {
        when(repository.findAll(9, 20)).thenReturn(new PageResult<>(List.of(), 9, 20, 57L, 3));

        PageResult<MedicamentPrescriptionDto> resultado = service.listAll(9, 20);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isEqualTo(57L);
    }
}
