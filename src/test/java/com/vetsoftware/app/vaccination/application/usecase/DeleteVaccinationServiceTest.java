package com.vetsoftware.app.vaccination.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
import com.vetsoftware.app.vaccination.testsupport.VaccinationMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteVaccinationService")
class DeleteVaccinationServiceTest {

    private static final Long OTRA_EMPRESA_ID = 77L;

    @Mock
    private VaccinationRepository repository;
    @InjectMocks
    private DeleteVaccinationService service;

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("elimina la vacuna existente en la empresa del contexto")
        void elimina_la_vacuna_existente() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));

            service.execute(VaccinationMother.VACCINATION_ID, VaccinationMother.COMPANY_ID);

            verify(repository).delete(VaccinationMother.VACCINATION_ID);
        }

        @Test
        @DisplayName("vacuna inexistente lanza VaccinationNotFoundException y no elimina nada")
        void vacuna_inexistente_lanza_not_found_y_no_elimina_nada() {
            when(repository.findByIdAndCompanyId(99L, VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(99L, VaccinationMother.COMPANY_ID))
                    .isInstanceOf(VaccinationNotFoundException.class)
                    .hasMessageContaining("Vaccination not found: 99");

            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("tenancy")
    class Tenancy {

        /**
         * BE-fix3: la comprobacion previa de existencia decide si se borra, asi que
         * tiene que ser por (id, empresa). Con un {@code findById} a secas, la vacuna
         * de otro tenant existia, pasaba la guarda y se borraba.
         */
        @Test
        @DisplayName("una vacuna de otra empresa no se borra: 404 y no llega al repositorio")
        void vacuna_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID, OTRA_EMPRESA_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(VaccinationMother.VACCINATION_ID, OTRA_EMPRESA_ID))
                    .isInstanceOf(VaccinationNotFoundException.class).hasMessageContaining(
                            "Vaccination not found: " + VaccinationMother.VACCINATION_ID);

            verify(repository, never()).delete(any());
        }
    }
}
