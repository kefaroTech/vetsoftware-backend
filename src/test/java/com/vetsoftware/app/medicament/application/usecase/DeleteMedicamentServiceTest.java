package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.port.out.MedicamentPrescriptionChildrenQueryPort;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.domain.MedicamentHasActiveChildrenException;
import com.vetsoftware.app.medicament.domain.MedicamentNotFoundException;
import com.vetsoftware.app.medicament.testsupport.MedicamentMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteMedicamentService")
class DeleteMedicamentServiceTest {

    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_EMPRESA = 77L;

    @Mock
    private MedicamentRepository repository;
    @Mock
    private MedicamentPrescriptionChildrenQueryPort childrenQueryPort;

    @InjectMocks
    private DeleteMedicamentService service;

    private static Medicament propio() {
        return MedicamentMother.propioDeEmpresa(MedicamentMother.companyRef());
    }

    @Nested
    @DisplayName("validaciones — no debe escribir")
    class Validaciones {

        @Test
        @DisplayName("lanza MedicamentNotFoundException si no existe")
        void lanza_not_found_si_no_existe() {
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L, COMPANY_ID))
                    .isInstanceOf(MedicamentNotFoundException.class).hasMessageContaining("1");

            verify(repository, never()).delete(any());
            verifyNoInteractions(childrenQueryPort);
        }

        @Test
        @DisplayName("lanza MedicamentHasActiveChildrenException si tiene recetas activas")
        void lanza_has_active_children_si_tiene_recetas() {
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(propio()));
            when(childrenQueryPort.existsActiveByMedicamentId(1L)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(1L, COMPANY_ID))
                    .isInstanceOf(MedicamentHasActiveChildrenException.class)
                    .hasMessageContaining("1").hasMessageContaining("medicamentPrescription");

            verify(repository, never()).delete(any());
        }
    }

    @Test
    @DisplayName("borra el medicamento cuando existe y no tiene recetas activas")
    void borra_el_medicamento_sin_recetas_activas() {
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(propio()));
        when(childrenQueryPort.existsActiveByMedicamentId(1L)).thenReturn(false);

        service.execute(1L, COMPANY_ID);

        verify(repository).delete(1L);
    }

    @Test
    @DisplayName("sin empresa (camino SYSTEM) comprueba la existencia sin acotar")
    void sin_empresa_comprueba_sin_acotar() {
        when(repository.findById(1L)).thenReturn(Optional.of(propio()));
        when(childrenQueryPort.existsActiveByMedicamentId(1L)).thenReturn(false);

        service.execute(1L, null);

        verify(repository, never()).findByIdAndCompanyId(any(), any());
        verify(repository).delete(1L);
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * La comprobacion de existencia acotada es lo unico que separa un 404 de borrar
         * el vademecum de otro tenant. Si el corte falla, la fila desaparece y ni el
         * hijo activo la protege: el chequeo de hijos va despues.
         */
        @Test
        @DisplayName("el medicamento de otra empresa no se borra")
        void el_medicamento_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(1L, OTRA_EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L, OTRA_EMPRESA))
                    .isInstanceOf(MedicamentNotFoundException.class).hasMessageContaining("1");

            verify(repository, never()).findById(any());
            verify(repository, never()).delete(any());
            verifyNoInteractions(childrenQueryPort);
        }
    }
}
