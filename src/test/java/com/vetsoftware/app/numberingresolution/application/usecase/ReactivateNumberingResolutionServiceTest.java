package com.vetsoftware.app.numberingresolution.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
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
@DisplayName("ReactivateNumberingResolutionService")
class ReactivateNumberingResolutionServiceTest {

    @Mock
    private NumberingResolutionRepository repository;

    @InjectMocks
    private ReactivateNumberingResolutionService service;

    @Nested
    @DisplayName("Reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve la resolucion ya habilitada")
        void reactiva_y_devuelve_la_resolucion_habilitada() {
            when(repository.reactivate(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID))
                    .thenReturn(Optional.of(NumberingResolutionMother.activaDeEmpresa()));

            NumberingResolutionDto dto = service.execute(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(NumberingResolutionMother.RESOLUTION_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Fallos")
    class Fallos {

        @Test
        @DisplayName("una reactivacion sin filas afectadas no lee la resolucion: lanza directamente")
        void sin_filas_afectadas_no_intenta_leer() {
            when(repository.reactivate(999L, NumberingResolutionMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(999L, NumberingResolutionMother.COMPANY_ID))
                    .isInstanceOf(NumberingResolutionNotFoundException.class)
                    .hasMessageContaining("Numbering resolution not found: 999");

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("una resolucion desaparecida entre el UPDATE y la lectura tambien es 404")
        void desaparecida_entre_reactivar_y_leer_es_not_found() {
            when(repository.reactivate(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID))
                    .isInstanceOf(NumberingResolutionNotFoundException.class)
                    .hasMessageContaining("Numbering resolution not found: "
                            + NumberingResolutionMother.RESOLUTION_ID);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("la resolucion de OTRA empresa es 404 y no se reactiva ni se relee")
        void resolucion_de_otra_empresa_es_not_found_y_no_escribe() {
            // El UPDATE lleva el company_id, asi que la resolucion DIAN de otra empresa
            // no afecta filas: es la unica barrera, porque aqui no hay lectura previa.
            when(repository.reactivate(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(NumberingResolutionMother.RESOLUTION_ID,
                    NumberingResolutionMother.COMPANY_ID))
                    .isInstanceOf(NumberingResolutionNotFoundException.class);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
        }
    }
}
