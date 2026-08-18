package com.vetsoftware.app.medicamentprescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteMedicamentPrescriptionService")
class DeleteMedicamentPrescriptionServiceTest {

    @Mock
    private MedicamentPrescriptionRepository repository;

    @InjectMocks
    private DeleteMedicamentPrescriptionService service;

    @Nested
    @DisplayName("companyId presente")
    class ConCompanyId {

        @Test
        @DisplayName("borra la linea cuando existe para esa empresa")
        void borra_la_linea_cuando_existe() {
            when(repository.findByIdAndCompanyId(MedicamentPrescriptionMother.ID,
                    MedicamentPrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.persistida()));

            service.execute(MedicamentPrescriptionMother.ID,
                    MedicamentPrescriptionMother.COMPANY_ID);

            verify(repository).delete(MedicamentPrescriptionMother.ID);
        }

        @Test
        @DisplayName("linea inexistente para esa empresa: no borra")
        void linea_inexistente_no_borra() {
            when(repository.findByIdAndCompanyId(MedicamentPrescriptionMother.ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MedicamentPrescriptionMother.ID,
                    MedicamentPrescriptionMother.COMPANY_ID))
                    .isInstanceOf(MedicamentPrescriptionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(MedicamentPrescriptionMother.ID));

            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("companyId ausente")
    class SinCompanyId {

        @Test
        @DisplayName("busca por id a secas")
        void busca_por_id_a_secas() {
            when(repository.findById(MedicamentPrescriptionMother.ID))
                    .thenReturn(Optional.of(MedicamentPrescriptionMother.persistida()));

            service.execute(MedicamentPrescriptionMother.ID, null);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(repository).delete(MedicamentPrescriptionMother.ID);
        }

        @Test
        @DisplayName("linea inexistente a secas: no borra")
        void linea_inexistente_a_secas_no_borra() {
            when(repository.findById(MedicamentPrescriptionMother.ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MedicamentPrescriptionMother.ID, null))
                    .isInstanceOf(MedicamentPrescriptionNotFoundException.class);

            verify(repository, never()).delete(any());
        }
    }
}
