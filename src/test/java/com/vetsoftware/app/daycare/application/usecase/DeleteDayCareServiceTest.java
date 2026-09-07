package com.vetsoftware.app.daycare.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteDayCareService")
class DeleteDayCareServiceTest {

    @Mock
    private DayCareRepository repository;

    private DeleteDayCareService service;

    @BeforeEach
    void crearServicio() {
        service = new DeleteDayCareService(repository);
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("con companyId busca por empresa antes de borrar")
        void con_company_id_busca_por_empresa() {
            when(repository.findByIdAndCompanyId(5L, DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.of(DayCareMother.guarderiaValida()));

            service.execute(5L, DayCareMother.CLINICA.id());

            verify(repository).delete(5L);
        }

        @Test
        @DisplayName("sin companyId busca por id global antes de borrar")
        void sin_company_id_busca_por_id_global() {
            when(repository.findById(5L)).thenReturn(Optional.of(DayCareMother.guarderiaValida()));

            service.execute(5L, null);

            verify(repository).delete(5L);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no borra si el daycare no existe en esa empresa")
        void no_borra_si_el_daycare_no_existe() {
            when(repository.findByIdAndCompanyId(5L, DayCareMother.CLINICA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(5L, DayCareMother.CLINICA.id()))
                    .isInstanceOf(DayCareNotFoundException.class).hasMessageContaining("5");

            verify(repository, never()).delete(5L);
        }
    }
}
