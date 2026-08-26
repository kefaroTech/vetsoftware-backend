package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.port.out.MedicamentPrescriptionChildrenQueryPort;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
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

/**
 * Pausa (baja logica) en el vademecum de PLATAFORMA.
 *
 * <p>
 * Es la consecuencia mas fea del filtro de #590: sin
 * {@code filter(Medicament::isGeneral)}, un DELETE de plataforma con el id del
 * medicamento PRIVADO de una clinica devolveria 204, lo pausaria, y la clinica
 * dejaria de verlo en su catalogo sin una sola traza de que hubiera pasado
 * nada.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteGlobalMedicamentService")
class DeleteGlobalMedicamentServiceTest {

    private static final Long ID = MedicamentMother.MEDICAMENT_ID;

    @Mock
    private MedicamentRepository repository;
    @Mock
    private MedicamentPrescriptionChildrenQueryPort childrenQueryPort;

    @InjectMocks
    private DeleteGlobalMedicamentService service;

    @Test
    @DisplayName("pausa el medicamento global cuando existe y no tiene recetas activas")
    void pausa_el_global_sin_recetas_activas() {
        when(repository.findById(ID)).thenReturn(Optional.of(MedicamentMother.activoGeneral()));
        when(childrenQueryPort.existsActiveByMedicamentId(ID)).thenReturn(false);

        service.execute(ID);

        verify(repository).delete(ID);
    }

    @Nested
    @DisplayName("Validaciones — no debe escribir")
    class Validaciones {

        @Test
        @DisplayName("lanza MedicamentNotFoundException si no existe")
        void lanza_not_found_si_no_existe() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ID))
                    .isInstanceOf(MedicamentNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).delete(any());
            verifyNoInteractions(childrenQueryPort);
        }

        /**
         * La comprobacion de recetas activas no se acota a ninguna empresa a proposito:
         * un global lo receta cualquier tenant, asi que pausarlo mientras una receta
         * viva lo referencia dejaria esa receta apuntando a un medicamento que ya no
         * esta en ningun catalogo.
         */
        @Test
        @DisplayName("con una receta activa de cualquier tenant no se pausa")
        void con_recetas_activas_no_se_pausa() {
            when(repository.findById(ID)).thenReturn(Optional.of(MedicamentMother.activoGeneral()));
            when(childrenQueryPort.existsActiveByMedicamentId(ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(ID))
                    .isInstanceOf(MedicamentHasActiveChildrenException.class)
                    .hasMessageContaining(String.valueOf(ID))
                    .hasMessageContaining("medicamentPrescription");

            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Tenancy — la consola de plataforma no pausa el catalogo de una clinica")
    class Tenancy {

        /**
         * El escenario de #590: 204, la fila con {@code enabled = false} por el
         * {@code @SQLDelete} y la clinica dejando de ver su medicamento. El corte esta
         * en la carga, y ni el chequeo de recetas la protegeria, porque va despues.
         */
        @Test
        @DisplayName("el medicamento PRIVADO de una clinica no se pausa: 404 y ni se miran sus recetas")
        void el_medicamento_privado_de_una_clinica_no_se_pausa() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(MedicamentMother.activoDeEmpresa()));

            assertThatThrownBy(() -> service.execute(ID))
                    .isInstanceOf(MedicamentNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ID));

            verify(repository, never()).delete(any());
            verifyNoInteractions(childrenQueryPort);
        }
    }
}
