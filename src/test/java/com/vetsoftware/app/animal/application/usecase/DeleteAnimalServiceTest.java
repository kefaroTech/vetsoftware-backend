package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.application.port.out.ConsultationChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.DayCareChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.DewormingChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.DiagnosticImagingChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.HospitalizationChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.SpaChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.animal.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.animal.domain.AnimalHasActiveChildrenException;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los nueve tipos de hijo se prueban uno a uno y no en bucle: lo que este test
 * tiene que detectar es precisamente un copiar-pegar que compruebe un puerto y
 * reporte el nombre de otro, y un bucle no distingue eso.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAnimalService")
class DeleteAnimalServiceTest {

    @Mock
    private AnimalRepository repository;
    @Mock
    private VaccinationChildrenQueryPort vaccination;
    @Mock
    private DewormingChildrenQueryPort deworming;
    @Mock
    private SurgeryChildrenQueryPort surgery;
    @Mock
    private HospitalizationChildrenQueryPort hospitalization;
    @Mock
    private DiagnosticImagingChildrenQueryPort diagnosticImaging;
    @Mock
    private LaboratoryTestChildrenQueryPort laboratoryTest;
    @Mock
    private SpaChildrenQueryPort spa;
    @Mock
    private DayCareChildrenQueryPort dayCare;
    @Mock
    private ConsultationChildrenQueryPort consultation;

    @InjectMocks
    private DeleteAnimalService service;

    private void animalExiste() {
        when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                .thenReturn(Optional.of(AnimalMother.perroSano()));
    }

    private void borrar() {
        service.execute(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);
    }

    private void assertBloqueadoPor(String tipoHijo) {
        assertThatThrownBy(DeleteAnimalServiceTest.this::borrar)
                .isInstanceOf(AnimalHasActiveChildrenException.class)
                .hasMessageContaining("Cannot delete animal " + AnimalMother.ANIMAL_ID)
                .hasMessageContaining("has active " + tipoHijo + " children");
        verify(repository, never()).delete(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);
    }

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("sin hijos activos borra acotando por empresa")
        void sin_hijos_activos_borra_acotando_por_empresa() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(deworming.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(surgery.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(hospitalization.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(diagnosticImaging.existsActiveByAnimalId(AnimalMother.ANIMAL_ID))
                    .thenReturn(false);
            when(laboratoryTest.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(spa.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(dayCare.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(consultation.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);

            borrar();

            verify(repository).delete(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("un animal de otra empresa no existe y no se consulta ningun hijo")
        void un_animal_de_otra_empresa_no_existe() {
            when(repository.findByIdAndCompanyId(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(DeleteAnimalServiceTest.this::borrar)
                    .isInstanceOf(AnimalNotFoundException.class)
                    .hasMessageContaining("Animal not found: " + AnimalMother.ANIMAL_ID);

            verifyNoInteractions(vaccination, deworming, surgery, hospitalization,
                    diagnosticImaging, laboratoryTest, spa, dayCare, consultation);
            verify(repository, never()).delete(AnimalMother.ANIMAL_ID, AnimalMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("bloqueo por hijos activos — un tipo por test, en el orden en que se comprueban")
    class BloqueoPorHijos {

        @Test
        @DisplayName("vacunaciones activas")
        void vacunaciones_activas() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(true);

            assertBloqueadoPor("vaccination");
            verifyNoInteractions(deworming, surgery, hospitalization, diagnosticImaging,
                    laboratoryTest, spa, dayCare, consultation);
        }

        @Test
        @DisplayName("desparasitaciones activas")
        void desparasitaciones_activas() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(deworming.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(true);

            assertBloqueadoPor("deworming");
            verifyNoInteractions(surgery, hospitalization, diagnosticImaging, laboratoryTest, spa,
                    dayCare, consultation);
        }

        @Test
        @DisplayName("cirugias activas")
        void cirugias_activas() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(deworming.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(surgery.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(true);

            assertBloqueadoPor("surgery");
            verifyNoInteractions(hospitalization, diagnosticImaging, laboratoryTest, spa, dayCare,
                    consultation);
        }

        @Test
        @DisplayName("hospitalizaciones activas")
        void hospitalizaciones_activas() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(deworming.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(surgery.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(hospitalization.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(true);

            assertBloqueadoPor("hospitalization");
            verifyNoInteractions(diagnosticImaging, laboratoryTest, spa, dayCare, consultation);
        }

        @Test
        @DisplayName("imagenes diagnosticas activas")
        void imagenes_diagnosticas_activas() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(deworming.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(surgery.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(hospitalization.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(diagnosticImaging.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(true);

            assertBloqueadoPor("diagnosticImaging");
            verifyNoInteractions(laboratoryTest, spa, dayCare, consultation);
        }

        @Test
        @DisplayName("examenes de laboratorio activos")
        void examenes_de_laboratorio_activos() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(deworming.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(surgery.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(hospitalization.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(diagnosticImaging.existsActiveByAnimalId(AnimalMother.ANIMAL_ID))
                    .thenReturn(false);
            when(laboratoryTest.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(true);

            assertBloqueadoPor("laboratoryTest");
            verifyNoInteractions(spa, dayCare, consultation);
        }

        @Test
        @DisplayName("servicios de spa activos")
        void servicios_de_spa_activos() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(deworming.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(surgery.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(hospitalization.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(diagnosticImaging.existsActiveByAnimalId(AnimalMother.ANIMAL_ID))
                    .thenReturn(false);
            when(laboratoryTest.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(spa.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(true);

            assertBloqueadoPor("spa");
            verifyNoInteractions(dayCare, consultation);
        }

        @Test
        @DisplayName("guarderia activa")
        void guarderia_activa() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(deworming.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(surgery.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(hospitalization.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(diagnosticImaging.existsActiveByAnimalId(AnimalMother.ANIMAL_ID))
                    .thenReturn(false);
            when(laboratoryTest.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(spa.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(dayCare.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(true);

            assertBloqueadoPor("dayCare");
            verifyNoInteractions(consultation);
        }

        @Test
        @DisplayName("consultas activas — el ultimo puerto que se comprueba")
        void consultas_activas() {
            animalExiste();
            when(vaccination.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(deworming.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(surgery.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(hospitalization.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(diagnosticImaging.existsActiveByAnimalId(AnimalMother.ANIMAL_ID))
                    .thenReturn(false);
            when(laboratoryTest.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(spa.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(dayCare.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(false);
            when(consultation.existsActiveByAnimalId(AnimalMother.ANIMAL_ID)).thenReturn(true);

            assertBloqueadoPor("consultation");
        }
    }
}
