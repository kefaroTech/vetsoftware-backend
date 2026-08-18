package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.testsupport.CompanyMother;
import com.vetsoftware.app.daycare.infrastructure.persistence.DayCareJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaDayCareChildrenQueryPort (company) — adaptador sobre DayCareJpaRepository")
class JpaDayCareChildrenQueryPortTest {

    @Mock
    private DayCareJpaRepository dayCareJpaRepository;

    private JpaDayCareChildrenQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaDayCareChildrenQueryPort(dayCareJpaRepository);
    }

    @Nested
    @DisplayName("existencia de guarderias activas")
    class ExistenciaDeGuarderiasActivas {

        @Test
        @DisplayName("delega en el repositorio de guarderias por el id de la empresa")
        void delega_en_el_repositorio_de_guarderias() {
            when(dayCareJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(true);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isTrue();
        }

        @Test
        @DisplayName("una empresa sin guarderias devuelve falso")
        void una_empresa_sin_guarderias_devuelve_falso() {
            when(dayCareJpaRepository.existsByCompany_Id(CompanyMother.COMPANY_ID))
                    .thenReturn(false);

            boolean resultado = port.existsActiveByCompanyId(CompanyMother.COMPANY_ID);

            assertThat(resultado).isFalse();
        }
    }
}
