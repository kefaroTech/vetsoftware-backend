package com.vetsoftware.app.diagnosticimagingtype.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.testsupport.DiagnosticImagingTypeMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDiagnosticImagingTypesService")
class ListDiagnosticImagingTypesServiceTest {

    @Mock
    private DiagnosticImagingTypeRepository repository;

    @InjectMocks
    private ListDiagnosticImagingTypesService service;

    @Test
    @DisplayName("listAll devuelve todos los tipos mapeados a DTO, sin filtrar por empresa")
    void list_all_devuelve_todos_los_tipos_mapeados() {
        when(repository.findAll()).thenReturn(List.of(DiagnosticImagingTypeMother.general(),
                DiagnosticImagingTypeMother.propiaDeEmpresa()));

        List<DiagnosticImagingTypeDto> resultado = service.listAll();

        assertThat(resultado).hasSize(2).extracting(DiagnosticImagingTypeDto::id).containsExactly(
                DiagnosticImagingTypeMother.TYPE_ID, DiagnosticImagingTypeMother.TYPE_ID);
    }

    @Test
    @DisplayName("sin tipos registrados devuelve una lista vacia")
    void sin_tipos_devuelve_lista_vacia() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }
}
