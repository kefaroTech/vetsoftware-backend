package com.vetsoftware.app.daycare.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import com.vetsoftware.app.daycare.testsupport.DayCareMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateDayCareService")
class ReactivateDayCareServiceTest {

    private static final Long EMPRESA = DayCareMother.CLINICA.id();
    private static final Long OTRA_EMPRESA = DayCareMother.OTRA_CLINICA.id();

    @Mock
    private DayCareRepository repository;

    private ReactivateDayCareService service;

    @BeforeEach
    void crearServicio() {
        service = new ReactivateDayCareService(repository);
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el daycare releido")
        void reactiva_y_devuelve_el_daycare_releido() {
            when(repository.reactivate(5L, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(5L, EMPRESA))
                    .thenReturn(Optional.of(DayCareMother.guarderiaValida()));

            DayCareDto dto = service.execute(5L, EMPRESA);

            assertThat(dto.id()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no relee si la actualizacion no afecta ninguna fila")
        void no_relee_si_no_afecta_ninguna_fila() {
            when(repository.reactivate(5L, EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(5L, EMPRESA))
                    .isInstanceOf(DayCareNotFoundException.class).hasMessageContaining("5");

            verify(repository).reactivate(5L, EMPRESA);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("una estancia de otra empresa no se reactiva y no se relee")
        void estancia_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(5L, OTRA_EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(5L, OTRA_EMPRESA))
                    .isInstanceOf(DayCareNotFoundException.class).hasMessageContaining("5");

            verify(repository).reactivate(5L, OTRA_EMPRESA);
            verifyNoMoreInteractions(repository);
        }
    }
}
