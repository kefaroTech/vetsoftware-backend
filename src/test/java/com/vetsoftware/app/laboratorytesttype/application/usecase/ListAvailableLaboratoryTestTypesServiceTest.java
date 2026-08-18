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
@DisplayName("ListAvailableLaboratoryTestTypesService")
class ListAvailableLaboratoryTestTypesServiceTest {

    @Mock
    private LaboratoryTestTypeRepository repository;

    @InjectMocks
    private ListAvailableLaboratoryTestTypesService service;

    @Test
    @DisplayName("proyecta los tipos disponibles para la empresa, propios y generales")
    void proyecta_los_tipos_disponibles_para_la_empresa() {
        when(repository.findAllAvailableForCompany(LaboratoryTestTypeMother.COMPANY_ID))
                .thenReturn(List.of(LaboratoryTestTypeMother.propioDeEmpresa(),
                        LaboratoryTestTypeMother.general()));

        List<LaboratoryTestTypeDto> resultado = service
                .listAvailable(LaboratoryTestTypeMother.COMPANY_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(LaboratoryTestTypeDto::general).containsExactly(false,
                true);
    }

    @Test
    @DisplayName("la fila general sigue siendo accesible desde cualquier empresa")
    void la_fila_general_sigue_siendo_accesible() {
        // Acotar por empresa los caminos de ESCRITURA (update/delete/reactivate) no
        // toco
        // la lectura: el listado de disponibles sigue devolviendo las filas generales,
        // que es lo que debe seguir funcionando.
        when(repository.findAllAvailableForCompany(LaboratoryTestTypeMother.COMPANY_ID))
                .thenReturn(List.of(LaboratoryTestTypeMother.general()));

        List<LaboratoryTestTypeDto> resultado = service
                .listAvailable(LaboratoryTestTypeMother.COMPANY_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().general()).isTrue();
        assertThat(resultado.getFirst().company()).isNull();
    }

    @Test
    @DisplayName("una empresa sin tipos disponibles produce una lista vacia")
    void una_empresa_sin_tipos_disponibles_produce_lista_vacia() {
        when(repository.findAllAvailableForCompany(LaboratoryTestTypeMother.COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.listAvailable(LaboratoryTestTypeMother.COMPANY_ID)).isEmpty();
    }
}
