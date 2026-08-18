package com.vetsoftware.app.deworming.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import com.vetsoftware.app.deworming.testsupport.DewormingMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateDewormingService")
class ReactivateDewormingServiceTest {

    @Mock
    private DewormingRepository repository;
    @InjectMocks
    private ReactivateDewormingService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva con el companyId del contexto y devuelve el DTO releido")
        void reactiva_y_devuelve_el_dto_releido() {
            when(repository.reactivate(DewormingMother.DEWORMING_ID, DewormingMother.COMPANY_ID))
                    .thenReturn(1);
            when(repository.findByIdAndCompanyId(DewormingMother.DEWORMING_ID,
                    DewormingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DewormingMother.desparasitacionValida()));

            DewormingDto dto = service.execute(DewormingMother.DEWORMING_ID,
                    DewormingMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(DewormingMother.DEWORMING_ID);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("sin filas afectadas la desparasitacion no existe y no vuelve a leer")
        void sin_filas_afectadas_no_existe() {
            when(repository.reactivate(99L, DewormingMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(99L, DewormingMother.COMPANY_ID))
                    .isInstanceOf(DewormingNotFoundException.class)
                    .hasMessageContaining("Deworming not found: 99");

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }

    @Nested
    @DisplayName("tenancy")
    class Tenancy {

        /**
         * BE-fix3: antes {@code reactivate(id)} no filtraba por empresa y un id ajeno
         * se reactivaba igual. Ahora el companyId viaja hasta el UPDATE nativo: cero
         * filas afectadas es la respuesta tanto para "no existe" como para "es de otro
         * tenant", y el servicio no vuelve a leer.
         */
        @Test
        @DisplayName("una desparasitacion de otra empresa no se reactiva: 404 y no relee")
        void desparasitacion_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(DewormingMother.DEWORMING_ID,
                    DewormingMother.OTRA_CLINICA.id())).thenReturn(0);

            assertThatThrownBy(() -> service.execute(DewormingMother.DEWORMING_ID,
                    DewormingMother.OTRA_CLINICA.id()))
                    .isInstanceOf(DewormingNotFoundException.class)
                    .hasMessageContaining("Deworming not found: " + DewormingMother.DEWORMING_ID);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }
}
