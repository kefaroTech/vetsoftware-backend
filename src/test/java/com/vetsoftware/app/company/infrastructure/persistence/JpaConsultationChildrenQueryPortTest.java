package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaConsultationChildrenQueryPort (company) — adaptador sobre ConsultationJpaRepository")
class JpaConsultationChildrenQueryPortTest {

    @Mock
    private ConsultationJpaRepository consultationJpaRepository;

    private JpaConsultationChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaConsultationChildrenQueryPort(consultationJpaRepository);
    }

    @Nested
    @DisplayName("existencia de consultas activas")
    class ExistenciaDeConsultasActivas {

        @Test
        @DisplayName("delega en el repositorio de consultas por el id de la empresa")
        void delega_en_el_repositorio_de_consultas() {
            when(consultationJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin consultas devuelve falso")
        void una_empresa_sin_consultas_devuelve_falso() {
            when(consultationJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
