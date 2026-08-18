package com.vetsoftware.app.vaccination.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
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
@DisplayName("ReactivateVaccinationService")
class ReactivateVaccinationServiceTest {

    private static final Long OTRA_EMPRESA_ID = 77L;

    @Mock
    private VaccinationRepository repository;
    @InjectMocks
    private ReactivateVaccinationService service;

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva con el companyId del contexto y devuelve la vacuna revivida")
        void reactiva_y_devuelve_la_vacuna_revivida() {
            when(repository.reactivate(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID))
                    .thenReturn(Optional.of(VaccinationMother.vigente()));

            VaccinationDto dto = service.execute(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(VaccinationMother.VACCINATION_ID);
        }

        @Test
        @DisplayName("sin filas afectadas lanza VaccinationNotFoundException y no busca la vacuna")
        void sin_filas_afectadas_lanza_not_found_y_no_busca_la_vacuna() {
            when(repository.reactivate(99L, VaccinationMother.COMPANY_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(99L, VaccinationMother.COMPANY_ID))
                    .isInstanceOf(VaccinationNotFoundException.class)
                    .hasMessageContaining("Vaccination not found: 99");

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("reactivada pero no encontrada al releer lanza VaccinationNotFoundException")
        void reactivada_pero_no_encontrada_al_releer_lanza_not_found() {
            when(repository.reactivate(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID)).thenReturn(1);
            when(repository.findByIdAndCompanyId(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(VaccinationMother.VACCINATION_ID,
                    VaccinationMother.COMPANY_ID)).isInstanceOf(VaccinationNotFoundException.class)
                    .hasMessageContaining(
                            "Vaccination not found: " + VaccinationMother.VACCINATION_ID);
        }
    }

    @Nested
    @DisplayName("tenancy")
    class Tenancy {

        /**
         * BE-fix3: antes {@code reactivate(id)} no filtraba por empresa y un id ajeno
         * se reactivaba igual. Ahora el companyId viaja hasta el UPDATE nativo: cero
         * filas afectadas es la respuesta tanto para "no existe" como para "es de otro
         * tenant", y el servicio no vuelve a leer.
         */
        @Test
        @DisplayName("una vacuna de otra empresa no se reactiva: 404 y no relee")
        void vacuna_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(VaccinationMother.VACCINATION_ID, OTRA_EMPRESA_ID))
                    .thenReturn(0);

            assertThatThrownBy(
                    () -> service.execute(VaccinationMother.VACCINATION_ID, OTRA_EMPRESA_ID))
                    .isInstanceOf(VaccinationNotFoundException.class).hasMessageContaining(
                            "Vaccination not found: " + VaccinationMother.VACCINATION_ID);

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }
}
