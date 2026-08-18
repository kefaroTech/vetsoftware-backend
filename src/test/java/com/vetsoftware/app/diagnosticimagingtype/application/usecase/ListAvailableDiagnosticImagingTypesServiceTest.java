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
@DisplayName("ListAvailableDiagnosticImagingTypesService")
class ListAvailableDiagnosticImagingTypesServiceTest {

    @Mock
    private DiagnosticImagingTypeRepository repository;

    @InjectMocks
    private ListAvailableDiagnosticImagingTypesService service;

    @Test
    @DisplayName("listAvailable delega en el repositorio con la empresa del llamador")
    void list_available_delega_en_el_repositorio_con_la_empresa() {
        when(repository.findAllAvailableForCompany(DiagnosticImagingTypeMother.COMPANY_ID))
                .thenReturn(List.of(DiagnosticImagingTypeMother.general(),
                        DiagnosticImagingTypeMother.propiaDeEmpresa()));

        List<DiagnosticImagingTypeDto> resultado = service
                .listAvailable(DiagnosticImagingTypeMother.COMPANY_ID);

        assertThat(resultado).hasSize(2);
    }

    @Test
    @DisplayName("sin tipos disponibles devuelve una lista vacia")
    void sin_tipos_disponibles_devuelve_lista_vacia() {
        when(repository.findAllAvailableForCompany(DiagnosticImagingTypeMother.COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.listAvailable(DiagnosticImagingTypeMother.COMPANY_ID)).isEmpty();
    }
}
