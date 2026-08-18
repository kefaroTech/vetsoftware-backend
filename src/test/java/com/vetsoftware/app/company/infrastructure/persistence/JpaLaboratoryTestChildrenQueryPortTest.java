package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaLaboratoryTestChildrenQueryPort (company) — adaptador sobre LaboratoryTestJpaRepository")
class JpaLaboratoryTestChildrenQueryPortTest {

    @Mock
    private LaboratoryTestJpaRepository laboratoryTestJpaRepository;

    private JpaLaboratoryTestChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaLaboratoryTestChildrenQueryPort(laboratoryTestJpaRepository);
    }

    @Nested
    @DisplayName("existencia de examenes de laboratorio activos")
    class ExistenciaDeExamenesDeLaboratorioActivos {

        @Test
        @DisplayName("delega en el repositorio de examenes de laboratorio por el id de la empresa")
        void delega_en_el_repositorio_de_examenes_de_laboratorio() {
            when(laboratoryTestJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin examenes de laboratorio devuelve falso")
        void una_empresa_sin_examenes_de_laboratorio_devuelve_falso() {
            when(laboratoryTestJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
