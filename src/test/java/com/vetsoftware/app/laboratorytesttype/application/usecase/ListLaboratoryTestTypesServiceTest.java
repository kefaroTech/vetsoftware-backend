package com.vetsoftware.app.laboratorytesttype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.testsupport.LaboratoryTestTypeMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListLaboratoryTestTypesService")
class ListLaboratoryTestTypesServiceTest {

    @Mock
    private LaboratoryTestTypeRepository repository;

    @InjectMocks
    private ListLaboratoryTestTypesService service;

    @Test
    @DisplayName("lista global proyecta todos los tipos del repositorio, sin filtrar por empresa")
    void lista_global_proyecta_todos_los_tipos() {
        when(repository.findAll()).thenReturn(List.of(LaboratoryTestTypeMother.propioDeEmpresa(),
                LaboratoryTestTypeMother.general()));

        List<LaboratoryTestTypeDto> resultado = service.listAll();

        assertThat(resultado).extracting(LaboratoryTestTypeDto::id).containsExactly(
                LaboratoryTestTypeMother.TYPE_ID, LaboratoryTestTypeMother.TYPE_ID);
        assertThat(resultado).extracting(LaboratoryTestTypeDto::general).containsExactly(false,
                true);
    }

    @Test
    @DisplayName("un repositorio vacio produce una lista vacia")
    void un_repositorio_vacio_produce_una_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
