package com.vetsoftware.app.promotion.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.promotion.domain.CompanyRef;
import com.vetsoftware.app.promotion.testsupport.PromotionMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCompanyQueryPort (promotion) — adaptador sobre CompanyJpaRepository")
class JpaCompanyQueryPortTest {

    @Mock
    private CompanyJpaRepository companyJpaRepository;
    @Mock
    private CompanyJpaEntity companyEntity;

    private JpaCompanyQueryPort port;

    @BeforeEach
    void crearAdaptador() {
        port = new JpaCompanyQueryPort(companyJpaRepository);
    }

    @Nested
    @DisplayName("busqueda")
    class Busqueda {

        @Test
        @DisplayName("mapea la empresa encontrada a su companion VO")
        void mapea_la_empresa_encontrada_a_su_companion_vo() {
            when(companyEntity.getId()).thenReturn(PromotionMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(PromotionMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(PromotionMother.CLINICA.identifier());
            when(companyJpaRepository.findById(PromotionMother.COMPANY_ID))
                    .thenReturn(Optional.of(companyEntity));

            Optional<CompanyRef> resultado = port.findById(PromotionMother.COMPANY_ID);

            assertThat(resultado).contains(PromotionMother.CLINICA);
        }

        @Test
        @DisplayName("devuelve vacio si la empresa no existe")
        void devuelve_vacio_si_la_empresa_no_existe() {
            when(companyJpaRepository.findById(PromotionMother.COMPANY_ID))
                    .thenReturn(Optional.empty());

            Optional<CompanyRef> resultado = port.findById(PromotionMother.COMPANY_ID);

            assertThat(resultado).isEmpty();
        }
    }
}
