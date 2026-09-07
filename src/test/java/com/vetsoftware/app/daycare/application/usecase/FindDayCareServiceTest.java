package com.vetsoftware.app.daycare.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindDayCareService")
class FindDayCareServiceTest {

    @Mock
    private DayCareRepository repository;

    private FindDayCareService service;

    @BeforeEach
    void crearServicio() {
        service = new FindDayCareService(repository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("devuelve el daycare de la empresa pedida")
        void devuelve_el_daycare_de_la_empresa_pedida() {
            when(repository.findByIdAndCompanyId(5L, DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.of(DayCareMother.guarderiaValida()));

            DayCareDto dto = service.findById(5L, DayCareMother.CLINICA.id());

            assertThat(dto.id()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza DayCareNotFoundException si no existe en esa empresa")
        void lanza_not_found_si_no_existe_en_esa_empresa() {
            when(repository.findByIdAndCompanyId(5L, DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(5L, DayCareMother.CLINICA.id()))
                    .isInstanceOf(DayCareNotFoundException.class).hasMessageContaining("5");
        }
    }
}
