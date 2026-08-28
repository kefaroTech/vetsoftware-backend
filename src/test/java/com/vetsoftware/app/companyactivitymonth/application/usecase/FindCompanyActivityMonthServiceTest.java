package com.vetsoftware.app.companyactivitymonth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.companyactivitymonth.application.port.out.CompanyActivityMonthRepository;
import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonthNotFoundException;
import com.vetsoftware.app.companyactivitymonth.testsupport.CompanyActivityMonthMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindCompanyActivityMonthService")
class FindCompanyActivityMonthServiceTest {

    @Mock
    private CompanyActivityMonthRepository repository;

    private FindCompanyActivityMonthService service;

    @BeforeEach
    void setUp() {
        service = new FindCompanyActivityMonthService(repository);
    }

    @Nested
    @DisplayName("por id")
    class PorId {

        @Test
        @DisplayName("devuelve la fila cuando existe")
        void devuelve_la_fila_cuando_existe() {
            when(repository.findById(CompanyActivityMonthMother.MONTH_ID))
                    .thenReturn(Optional.of(CompanyActivityMonthMother.pagada()));

            CompanyActivityMonthDto dto = service.findById(CompanyActivityMonthMother.MONTH_ID);

            assertThat(dto.id()).isEqualTo(CompanyActivityMonthMother.MONTH_ID);
        }

        @Test
        @DisplayName("lanza si no existe")
        void lanza_si_no_existe() {
            when(repository.findById(CompanyActivityMonthMother.MONTH_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(CompanyActivityMonthMother.MONTH_ID))
                    .isInstanceOf(CompanyActivityMonthNotFoundException.class)
                    .hasMessageContaining("Company activity month not found: "
                            + CompanyActivityMonthMother.MONTH_ID);
        }
    }

    @Nested
    @DisplayName("por empresa y periodo")
    class PorEmpresaYPeriodo {

        @Test
        @DisplayName("devuelve la fila de esa clinica en ese mes")
        void devuelve_la_fila_de_esa_clinica_en_ese_mes() {
            when(repository.findByCompanyIdAndPeriodKey(CompanyActivityMonthMother.COMPANY_ID,
                    "2026-03")).thenReturn(Optional.of(CompanyActivityMonthMother.pagada()));

            CompanyActivityMonthDto dto = service
                    .findByCompanyIdAndPeriodKey(CompanyActivityMonthMother.COMPANY_ID, "2026-03");

            assertThat(dto.companyId()).isEqualTo(CompanyActivityMonthMother.COMPANY_ID);
        }

        @Test
        @DisplayName("un periodo mal formado no llega a la base")
        void un_periodo_mal_formado_no_llega_a_la_base() {
            assertThatThrownBy(() -> service
                    .findByCompanyIdAndPeriodKey(CompanyActivityMonthMother.COMPANY_ID, "2026-13"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("periodKey must be a month in AAAA-MM format");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("lanza si no hay fila para ese par empresa-mes")
        void lanza_si_no_hay_fila_para_ese_par() {
            when(repository.findByCompanyIdAndPeriodKey(CompanyActivityMonthMother.COMPANY_ID,
                    "2026-03")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .findByCompanyIdAndPeriodKey(CompanyActivityMonthMother.COMPANY_ID, "2026-03"))
                    .isInstanceOf(CompanyActivityMonthNotFoundException.class)
                    .hasMessageContaining("has no activity row for period 2026-03");
        }
    }
}
