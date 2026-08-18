package com.vetsoftware.app.numberingresolution.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import com.vetsoftware.app.numberingresolution.testsupport.NumberingResolutionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteNumberingResolutionService")
class DeleteNumberingResolutionServiceTest {

    @Mock
    private NumberingResolutionRepository repository;

    @InjectMocks
    private DeleteNumberingResolutionService service;

    @Nested
    @DisplayName("Eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("desactiva la resolucion existente de la empresa")
        void desactiva_la_resolucion_existente() {
            when(repository.findByIdAndCompanyId(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(Optional.of(NumberingResolutionMother.activaDeEmpresa()));

            service.execute(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID);

            verify(repository).delete(NumberingResolutionMother.RESOLUTION_ID);
        }

        @Test
        @DisplayName("sin empresa (SYSTEM) la lectura previa no se acota")
        void sin_empresa_la_lectura_no_se_acota() {
            when(repository.findById(NumberingResolutionMother.RESOLUTION_ID))
                    .thenReturn(Optional.of(NumberingResolutionMother.activaDeEmpresa()));

            service.execute(NumberingResolutionMother.RESOLUTION_ID, null);

            verify(repository).delete(NumberingResolutionMother.RESOLUTION_ID);
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("no elimina una resolucion que no existe")
        void no_elimina_una_resolucion_inexistente() {
            when(repository.findByIdAndCompanyId(999L, NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(999L, NumberingResolutionMother.COMPANY_ID))
                    .isInstanceOf(NumberingResolutionNotFoundException.class)
                    .hasMessageContaining("Numbering resolution not found: 999");

            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("la resolucion de OTRA empresa es 404 y no se borra")
        void resolucion_de_otra_empresa_es_not_found_y_no_borra() {
            // La lectura previa va acotada por empresa: la resolucion DIAN de otro tenant
            // ya no se encuentra, asi que borrarla es un 404 y no un borrado que rompe su
            // numeracion fiscal.
            when(repository.findByIdAndCompanyId(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID))
                    .isInstanceOf(NumberingResolutionNotFoundException.class);

            verify(repository, never()).delete(any());
            verify(repository, never()).findById(any());
        }
    }
}
