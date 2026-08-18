package com.vetsoftware.app.prescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListPrescriptionsService")
class ListPrescriptionsServiceTest {

    @Mock
    private PrescriptionRepository repository;

    @InjectMocks
    private ListPrescriptionsService service;

    @Test
    @DisplayName("mapea la pagina de dominio a una pagina de DTOs")
    void mapea_la_pagina_de_dominio_a_dtos() {
        PageResult<com.vetsoftware.app.prescription.domain.Prescription> pagina = new PageResult<>(
                List.of(PrescriptionMother.persistida()), 0, 20, 1L, 1);
        when(repository.findAll(0, 20)).thenReturn(pagina);

        PageResult<PrescriptionDto> resultado = service.listAll(0, 20);

        assertThat(resultado.content()).extracting(PrescriptionDto::id)
                .containsExactly(PrescriptionMother.PRESCRIPTION_ID);
        assertThat(resultado.totalElements()).isEqualTo(1L);
    }
}
