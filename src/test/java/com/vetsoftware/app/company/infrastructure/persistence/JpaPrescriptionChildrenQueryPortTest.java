package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPrescriptionChildrenQueryPort (company) — adaptador sobre PrescriptionJpaRepository")
class JpaPrescriptionChildrenQueryPortTest {

    @Mock
    private PrescriptionJpaRepository prescriptionJpaRepository;

    private JpaPrescriptionChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaPrescriptionChildrenQueryPort(prescriptionJpaRepository);
    }

    @Nested
    @DisplayName("existencia de prescripciones activas")
    class ExistenciaDePrescripcionesActivas {

        @Test
        @DisplayName("delega en el repositorio de prescripciones por el id de la empresa")
        void delega_en_el_repositorio_de_prescripciones() {
            when(prescriptionJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin prescripciones devuelve falso")
        void una_empresa_sin_prescripciones_devuelve_falso() {
            when(prescriptionJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
