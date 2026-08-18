package com.vetsoftware.app.daycare.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private static final Long EMPRESA = DayCareMother.CLINICA.id();
    private static final Long OTRA_EMPRESA = DayCareMother.OTRA_CLINICA.id();

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
        @DisplayName("borra el daycare existente de la empresa del actor")
        void borra_el_daycare_existente() {
            when(repository.findByIdAndCompanyId(5L, EMPRESA))
                    .thenReturn(Optional.of(DayCareMother.guarderiaValida()));

            service.execute(5L, EMPRESA);

            verify(repository).delete(5L);
        }

        @Test
        @DisplayName("sin companyId (actor global) busca por id global antes de borrar")
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
        @DisplayName("no borra si el daycare no existe")
        void no_borra_si_el_daycare_no_existe() {
            when(repository.findByIdAndCompanyId(5L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(5L, EMPRESA))
                    .isInstanceOf(DayCareNotFoundException.class).hasMessageContaining("5");

            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("una estancia de otra empresa es un 404 y no se borra nada")
        void estancia_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(5L, OTRA_EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(5L, OTRA_EMPRESA))
                    .isInstanceOf(DayCareNotFoundException.class).hasMessageContaining("5");

            verify(repository, never()).delete(any());
            verify(repository, never()).findById(any());
        }
    }
}
