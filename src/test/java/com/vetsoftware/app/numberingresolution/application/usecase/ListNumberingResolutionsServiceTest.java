package com.vetsoftware.app.numberingresolution.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.testsupport.NumberingResolutionMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListNumberingResolutionsService")
class ListNumberingResolutionsServiceTest {

    @Mock
    private NumberingResolutionRepository repository;

    @InjectMocks
    private ListNumberingResolutionsService service;

    @Nested
    @DisplayName("Listado")
    class Listado {

        @Test
        @DisplayName("lista las resoluciones de la empresa mapeadas a dto")
        void lista_las_resoluciones_de_la_empresa() {
            when(repository.findAllByCompanyId(NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(List.of(NumberingResolutionMother.activaDeEmpresa(),
                            NumberingResolutionMother.activaDeSede()));

            List<NumberingResolutionDto> lista = service
                    .listByCompany(NumberingResolutionMother.COMPANY_ID);

            assertThat(lista).hasSize(2).extracting(NumberingResolutionDto::branchId)
                    .containsExactly(null, NumberingResolutionMother.BRANCH_ID);
        }

        @Test
        @DisplayName("devuelve una lista vacia si la empresa no tiene resoluciones")
        void devuelve_lista_vacia_si_no_hay_resoluciones() {
            when(repository.findAllByCompanyId(NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(List.of());

            assertThat(service.listByCompany(NumberingResolutionMother.COMPANY_ID)).isEmpty();
        }
    }
}
