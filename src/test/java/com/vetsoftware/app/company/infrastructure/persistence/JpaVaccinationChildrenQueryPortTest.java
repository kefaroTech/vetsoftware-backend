package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.vaccination.infrastructure.persistence.VaccinationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaVaccinationChildrenQueryPort (company) — adaptador sobre VaccinationJpaRepository")
class JpaVaccinationChildrenQueryPortTest {

    @Mock
    private VaccinationJpaRepository vaccinationJpaRepository;

    private JpaVaccinationChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaVaccinationChildrenQueryPort(vaccinationJpaRepository);
    }

    @Nested
    @DisplayName("existencia de vacunaciones activas")
    class ExistenciaDeVacunacionesActivas {

        @Test
        @DisplayName("delega en el repositorio de vacunaciones por el id de la empresa")
        void delega_en_el_repositorio_de_vacunaciones() {
            when(vaccinationJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin vacunaciones devuelve falso")
        void una_empresa_sin_vacunaciones_devuelve_falso() {
            when(vaccinationJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
