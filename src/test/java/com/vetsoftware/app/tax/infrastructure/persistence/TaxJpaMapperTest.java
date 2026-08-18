package com.vetsoftware.app.tax.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.testsupport.TaxMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TaxJpaMapper")
class TaxJpaMapperTest {

    private final TaxJpaMapper mapper = new TaxJpaMapper();

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio a la entidad, incluida la empresa")
        void copia_cada_campo_del_dominio_a_la_entidad() {
            Tax tax = TaxMother.inactivo();
            CompanyJpaEntity company = mock(CompanyJpaEntity.class);

            TaxJpaEntity entity = mapper.toJpa(tax, company);

            assertThat(entity.getId()).isEqualTo(tax.getId());
            assertThat(entity.getName()).isEqualTo(tax.getName());
            assertThat(entity.getPercentage()).isEqualByComparingTo(tax.getPercentage());
            assertThat(entity.getTaxScheme()).isEqualTo(tax.getTaxScheme());
            assertThat(entity.getCompany()).isSameAs(company);
            assertThat(entity.getCreatedDate()).isEqualTo(tax.getCreatedDate());
            assertThat(entity.getUpdatedDate()).isEqualTo(tax.getUpdatedDate());
            assertThat(entity.getUpdatedBy()).isEqualTo(tax.getUpdatedBy());
            assertThat(entity.getVersion()).isEqualTo(tax.getVersion());
            assertThat(entity.isEnabled()).isEqualTo(tax.isEnabled());
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomainDesdeLaEntidad {

        @Test
        @DisplayName("reconstruye el dominio leyendo la empresa desde la asociacion JPA")
        void reconstruye_el_dominio_leyendo_la_empresa_de_la_asociacion() {
            TaxJpaEntity entity = entidad();
            CompanyJpaEntity company = mock(CompanyJpaEntity.class);
            when(company.getId()).thenReturn(TaxMother.COMPANY_ID);
            when(company.getName()).thenReturn("Clinica Norte");
            when(company.getIdentifier()).thenReturn("900123456");
            entity.setCompany(company);

            Tax tax = mapper.toDomain(entity);

            assertThat(tax.getCompany()).isEqualTo(TaxMother.EMPRESA);
        }
    }

    @Nested
    @DisplayName("toDomain con CompanyRef ya resuelto")
    class ToDomainConCompanyRef {

        @Test
        @DisplayName("usa el CompanyRef recibido en lugar de leer la asociacion JPA")
        void usa_el_company_ref_recibido() {
            TaxJpaEntity entity = entidad();
            CompanyRef otraEmpresa = TaxMother.OTRA_EMPRESA;

            Tax tax = mapper.toDomain(entity, otraEmpresa);

            assertThat(tax.getCompany()).isEqualTo(otraEmpresa);
            assertThat(tax.getId()).isEqualTo(TaxMother.TAX_ID);
            assertThat(tax.getName()).isEqualTo("IVA General");
        }
    }

    private static TaxJpaEntity entidad() {
        TaxJpaEntity entity = new TaxJpaEntity();
        entity.setId(TaxMother.TAX_ID);
        entity.setName("IVA General");
        entity.setPercentage(new java.math.BigDecimal("19.00"));
        entity.setTaxScheme(com.vetsoftware.app.tax.domain.TaxScheme.IVA);
        entity.setCreatedDate(TaxMother.CREADO);
        entity.setVersion(0L);
        entity.setEnabled(true);
        return entity;
    }
}
