package com.vetsoftware.app.deworming.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteDewormingService")
class DeleteDewormingServiceTest {

    private static final Long COMPANY_ID = DewormingMother.COMPANY_ID;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private DewormingRepository repository;
    @InjectMocks
    private DeleteDewormingService service;

    @Test
    @DisplayName("borra la desparasitacion existente")
    void borra_la_desparasitacion_existente() {
        when(repository.findByIdAndCompanyId(DewormingMother.DEWORMING_ID, COMPANY_ID))
                .thenReturn(Optional.of(DewormingMother.desparasitacionValida()));

        service.execute(DewormingMother.DEWORMING_ID, COMPANY_ID);

        verify(repository).delete(DewormingMother.DEWORMING_ID);
    }

    @Test
    @DisplayName("desparasitacion inexistente: no llega a borrar")
    void desparasitacion_inexistente_no_llega_a_borrar() {
        when(repository.findByIdAndCompanyId(99L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(99L, COMPANY_ID))
                .isInstanceOf(DewormingNotFoundException.class)
                .hasMessageContaining("Deworming not found: 99");

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("sin empresa (camino SYSTEM) comprueba la existencia sin acotar")
    void sin_empresa_comprueba_sin_acotar() {
        when(repository.findById(DewormingMother.DEWORMING_ID))
                .thenReturn(Optional.of(DewormingMother.desparasitacionValida()));

        service.execute(DewormingMother.DEWORMING_ID, null);

        verify(repository, never()).findByIdAndCompanyId(any(), any());
        verify(repository).delete(DewormingMother.DEWORMING_ID);
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * Esta feature se dio por corregida cuando solo lo estaba {@code reactivate}.
         * La comprobacion de existencia acotada es lo unico que separa un 404 de borrar
         * la desparasitacion de un paciente de otro tenant.
         */
        @Test
        @DisplayName("la desparasitacion de otra empresa no se borra")
        void la_desparasitacion_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(DewormingMother.DEWORMING_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DewormingMother.DEWORMING_ID, OTRA_EMPRESA))
                    .isInstanceOf(DewormingNotFoundException.class)
                    .hasMessageContaining("Deworming not found: " + DewormingMother.DEWORMING_ID);

            verify(repository, never()).findById(any());
            verify(repository, never()).delete(any());
        }
    }
}
