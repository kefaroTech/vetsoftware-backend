package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaHospitalizationChildrenQueryPort (company) — adaptador sobre HospitalizationJpaRepository")
class JpaHospitalizationChildrenQueryPortTest {

    @Mock
    private HospitalizationJpaRepository hospitalizationJpaRepository;

    private JpaHospitalizationChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaHospitalizationChildrenQueryPort(hospitalizationJpaRepository);
    }

    @Nested
    @DisplayName("existencia de hospitalizaciones activas")
    class ExistenciaDeHospitalizacionesActivas {

        @Test
        @DisplayName("delega en el repositorio de hospitalizaciones por el id de la empresa")
        void delega_en_el_repositorio_de_hospitalizaciones() {
            when(hospitalizationJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin hospitalizaciones devuelve falso")
        void una_empresa_sin_hospitalizaciones_devuelve_falso() {
            when(hospitalizationJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
