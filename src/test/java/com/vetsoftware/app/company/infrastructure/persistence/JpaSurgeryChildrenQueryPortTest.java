package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSurgeryChildrenQueryPort (company) — adaptador sobre SurgeryJpaRepository")
class JpaSurgeryChildrenQueryPortTest {

    @Mock
    private SurgeryJpaRepository surgeryJpaRepository;

    private JpaSurgeryChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaSurgeryChildrenQueryPort(surgeryJpaRepository);
    }

    @Nested
    @DisplayName("existencia de cirugias activas")
    class ExistenciaDeCirugiasActivas {

        @Test
        @DisplayName("delega en el repositorio de cirugias por el id de la empresa")
        void delega_en_el_repositorio_de_cirugias() {
            when(surgeryJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin cirugias devuelve falso")
        void una_empresa_sin_cirugias_devuelve_falso() {
            when(surgeryJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
