package com.vetsoftware.app.spa.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import com.vetsoftware.app.spa.testsupport.SpaMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateSpaService")
class ReactivateSpaServiceTest {

    private static final Long EMPRESA = SpaMother.CLINICA.id();
    private static final Long OTRA_EMPRESA = SpaMother.OTRA_CLINICA.id();

    @Mock
    private SpaRepository repository;

    private ReactivateSpaService service;

    @BeforeEach
    void crearServicio() {
        service = new ReactivateSpaService(repository);
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve el spa releido")
        void reactiva_y_devuelve_el_spa_releido() {
            when(repository.reactivate(5L, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(5L, EMPRESA))
                    .thenReturn(Optional.of(SpaMother.spaValido()));

            SpaDto dto = service.execute(5L, EMPRESA);

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
                    .isInstanceOf(SpaNotFoundException.class).hasMessageContaining("5");

            verify(repository).reactivate(5L, EMPRESA);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * En reactivate no hay lectura previa que valide la propiedad: el UPDATE
         * acotado por empresa es la unica barrera, y cero filas es el 404.
         */
        @Test
        @DisplayName("un spa de otra empresa no se reactiva y no se relee")
        void spa_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(5L, OTRA_EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(5L, OTRA_EMPRESA))
                    .isInstanceOf(SpaNotFoundException.class).hasMessageContaining("5");

            verify(repository).reactivate(5L, OTRA_EMPRESA);
            verifyNoMoreInteractions(repository);
        }
    }
}
