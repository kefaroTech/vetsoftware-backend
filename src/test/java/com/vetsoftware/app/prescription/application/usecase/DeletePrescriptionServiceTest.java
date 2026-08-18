package com.vetsoftware.app.prescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.prescription.application.port.out.MedicamentPrescriptionChildrenQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.PrescriptionHasActiveChildrenException;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeletePrescriptionService")
class DeletePrescriptionServiceTest {

    @Mock
    private PrescriptionRepository repository;
    @Mock
    private MedicamentPrescriptionChildrenQueryPort childrenQueryPort;

    @InjectMocks
    private DeletePrescriptionService service;

    @Nested
    @DisplayName("companyId presente")
    class ConCompanyId {

        @Test
        @DisplayName("borra la receta cuando no tiene medicamentos activos")
        void borra_la_receta_sin_hijos_activos() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(childrenQueryPort.existsActiveByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(false);

            service.execute(PrescriptionMother.PRESCRIPTION_ID, PrescriptionMother.COMPANY_ID);

            verify(repository).delete(PrescriptionMother.PRESCRIPTION_ID);
        }

        @Test
        @DisplayName("no borra si tiene medicamentos activos")
        void no_borra_si_tiene_medicamentos_activos() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(childrenQueryPort.existsActiveByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .isInstanceOf(PrescriptionHasActiveChildrenException.class)
                    .hasMessageContaining("medicamentPrescription");

            verify(repository, never()).delete(PrescriptionMother.PRESCRIPTION_ID);
        }

        @Test
        @DisplayName("receta inexistente para esa empresa: no consulta hijos ni borra")
        void receta_inexistente() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .isInstanceOf(PrescriptionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PrescriptionMother.PRESCRIPTION_ID));

            verifyNoInteractions(childrenQueryPort);
        }
    }

    @Nested
    @DisplayName("companyId ausente")
    class SinCompanyId {

        @Test
        @DisplayName("busca por id a secas")
        void busca_por_id_a_secas() {
            when(repository.findById(PrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(childrenQueryPort.existsActiveByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(false);

            service.execute(PrescriptionMother.PRESCRIPTION_ID, null);

            verify(repository, never()).findByIdAndCompanyId(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
            verify(repository).delete(PrescriptionMother.PRESCRIPTION_ID);
        }
    }
}
