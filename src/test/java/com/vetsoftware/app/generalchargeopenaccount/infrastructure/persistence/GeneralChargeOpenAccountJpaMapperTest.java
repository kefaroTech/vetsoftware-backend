package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.testsupport.GeneralChargeOpenAccountMother;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.tax.domain.TaxScheme;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ida y vuelta dominio &harr; entidad JPA.
 *
 * <p>
 * Las entidades JPA de OTRAS features (tax, openaccount, employee) se mockean:
 * su constructor es {@code protected} y el vertical slicing impide construirlas
 * desde aqui. En {@code toJpa} solo viajan por referencia, y en el
 * {@code toDomain} de una sola pieza el mapper unicamente les pide accesores,
 * que es exactamente lo que el doble sabe responder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GeneralChargeOpenAccountJpaMapper")
class GeneralChargeOpenAccountJpaMapperTest {

    private final GeneralChargeOpenAccountJpaMapper mapper = new GeneralChargeOpenAccountJpaMapper();

    @Mock
    private TaxJpaEntity taxJpa;
    @Mock
    private OpenAccountJpaEntity openAccountJpa;
    @Mock
    private CompanyJpaEntity companyJpa;
    @Mock
    private EmployeeJpaEntity createdByJpa;
    @Mock
    private EmployeeJpaEntity voidedByJpa;

    private GeneralChargeOpenAccountJpaEntity entidadCompleta() {
        GeneralChargeOpenAccountJpaEntity entity = new GeneralChargeOpenAccountJpaEntity();
        entity.setId(GeneralChargeOpenAccountMother.CHARGE_ID);
        entity.setName(GeneralChargeOpenAccountMother.NOMBRE);
        entity.setUnitAmount(new BigDecimal("5950"));
        entity.setQuantity(new BigDecimal("2"));
        entity.setTax(taxJpa);
        entity.setHasTax(true);
        entity.setTaxPercentage(new BigDecimal("19.00"));
        entity.setTaxName("IVA 19%");
        entity.setTaxScheme("IVA");
        entity.setBaseAmount(new BigDecimal("10000.00"));
        entity.setTaxAmount(new BigDecimal("1900.00"));
        entity.setTotalAmount(new BigDecimal("11900.00"));
        entity.setOpenAccount(openAccountJpa);
        entity.setCreatedBy(createdByJpa);
        entity.setCreatedDate(GeneralChargeOpenAccountMother.CREADO);
        entity.setEnabled(true);
        entity.setVoided(false);
        entity.setClientRequestId("req-1");
        return entity;
    }

    /** Deja los dobles respondiendo lo que el mapper les va a preguntar. */
    private void referenciasHidratadas() {
        when(openAccountJpa.getId()).thenReturn(GeneralChargeOpenAccountMother.OPEN_ACCOUNT_ID);
        when(openAccountJpa.getCompany()).thenReturn(companyJpa);
        when(companyJpa.getId()).thenReturn(GeneralChargeOpenAccountMother.COMPANY_ID);
        when(createdByJpa.getId()).thenReturn(7L);
        when(createdByJpa.getName()).thenReturn("Ana Ruiz");
    }

    @Nested
    @DisplayName("toDomain")
    class ADominio {

        @Test
        @DisplayName("traslada la cabecera y el desglose tributario campo a campo")
        void traslada_la_cabecera_y_el_desglose() {
            referenciasHidratadas();
            when(taxJpa.getId()).thenReturn(4L);
            when(taxJpa.getName()).thenReturn("IVA 19%");
            when(taxJpa.getPercentage()).thenReturn(new BigDecimal("19.00"));
            when(taxJpa.getTaxScheme()).thenReturn(TaxScheme.IVA);

            GeneralChargeOpenAccount charge = mapper.toDomain(entidadCompleta());

            assertThat(charge.getId()).isEqualTo(GeneralChargeOpenAccountMother.CHARGE_ID);
            assertThat(charge.getName()).isEqualTo(GeneralChargeOpenAccountMother.NOMBRE);
            assertThat(charge.getUnitAmount()).isEqualByComparingTo("5950");
            assertThat(charge.getQuantity()).isEqualByComparingTo("2");
            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTax().id()).isEqualTo(4L);
            assertThat(charge.getTax().scheme()).isEqualTo("IVA");
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(charge.getTaxName()).isEqualTo("IVA 19%");
            assertThat(charge.getTaxScheme()).isEqualTo("IVA");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getOpenAccount().companyId())
                    .isEqualTo(GeneralChargeOpenAccountMother.COMPANY_ID);
            assertThat(charge.getCreatedBy().name()).isEqualTo("Ana Ruiz");
            assertThat(charge.getCreatedDate()).isEqualTo(GeneralChargeOpenAccountMother.CREADO);
            assertThat(charge.isEnabled()).isTrue();
            assertThat(charge.isVoided()).isFalse();
            assertThat(charge.getClientRequestId()).isEqualTo("req-1");
        }

        @Test
        @DisplayName("sin impuesto en la fila, el cargo queda sin TaxRef")
        void sin_impuesto_el_cargo_queda_sin_tax_ref() {
            referenciasHidratadas();
            GeneralChargeOpenAccountJpaEntity entity = entidadCompleta();
            entity.setTax(null);
            entity.setHasTax(false);

            GeneralChargeOpenAccount charge = mapper.toDomain(entity);

            assertThat(charge.getTax()).isNull();
            assertThat(charge.isHasTax()).isFalse();
        }

        @Test
        @DisplayName("un impuesto sin esquema no revienta: el esquema queda null")
        void un_impuesto_sin_esquema_no_revienta() {
            referenciasHidratadas();
            when(taxJpa.getId()).thenReturn(4L);
            when(taxJpa.getName()).thenReturn("IVA 19%");
            when(taxJpa.getPercentage()).thenReturn(new BigDecimal("19.00"));
            when(taxJpa.getTaxScheme()).thenReturn(null);

            GeneralChargeOpenAccount charge = mapper.toDomain(entidadCompleta());

            assertThat(charge.getTax().scheme()).isNull();
        }

        @Test
        @DisplayName("sin anulador, el ref queda null en vez de reventar")
        void sin_anulador_el_ref_queda_null() {
            referenciasHidratadas();
            GeneralChargeOpenAccountJpaEntity entity = entidadCompleta();
            entity.setTax(null);
            entity.setHasTax(false);

            GeneralChargeOpenAccount charge = mapper.toDomain(entity);

            assertThat(charge.getVoidedBy()).isNull();
        }

        @Test
        @DisplayName("proyecta el rastro de anulacion")
        void proyecta_el_rastro_de_anulacion() {
            referenciasHidratadas();
            when(voidedByJpa.getId()).thenReturn(8L);
            when(voidedByJpa.getName()).thenReturn("Luis Paz");
            GeneralChargeOpenAccountJpaEntity entity = entidadCompleta();
            entity.setTax(null);
            entity.setHasTax(false);
            entity.setVoided(true);
            entity.setVoidedBy(voidedByJpa);
            entity.setVoidedAt(GeneralChargeOpenAccountMother.ANULADO);
            entity.setVoidReason("Cobrado por error");

            GeneralChargeOpenAccount charge = mapper.toDomain(entity);

            assertThat(charge.isVoided()).isTrue();
            assertThat(charge.getVoidedBy().name()).isEqualTo("Luis Paz");
            assertThat(charge.getVoidedAt()).isEqualTo(GeneralChargeOpenAccountMother.ANULADO);
            assertThat(charge.getVoidReason()).isEqualTo("Cobrado por error");
        }
    }

    @Nested
    @DisplayName("toJpa")
    class AJpa {

        @Test
        @DisplayName("copia el agregado y engancha las entidades que le pasan")
        void copia_el_agregado_y_engancha_las_entidades() {
            GeneralChargeOpenAccountJpaEntity entity = mapper.toJpa(
                    GeneralChargeOpenAccountMother.cargo(), taxJpa, openAccountJpa, createdByJpa,
                    null);

            assertThat(entity.getId()).isEqualTo(GeneralChargeOpenAccountMother.CHARGE_ID);
            assertThat(entity.getTax()).isSameAs(taxJpa);
            assertThat(entity.getOpenAccount()).isSameAs(openAccountJpa);
            assertThat(entity.getCreatedBy()).isSameAs(createdByJpa);
            assertThat(entity.getVoidedBy()).isNull();
            assertThat(entity.getName()).isEqualTo(GeneralChargeOpenAccountMother.NOMBRE);
            assertThat(entity.getUnitAmount()).isEqualByComparingTo("5950");
            assertThat(entity.getQuantity()).isEqualByComparingTo("2");
            assertThat(entity.isHasTax()).isTrue();
            assertThat(entity.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(entity.getTaxName()).isEqualTo("IVA 19%");
            assertThat(entity.getTaxScheme()).isEqualTo("IVA");
            assertThat(entity.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(entity.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(entity.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(entity.getCreatedDate()).isEqualTo(GeneralChargeOpenAccountMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.isVoided()).isFalse();
        }

        @Test
        @DisplayName("arrastra el rastro de anulacion a la fila")
        void arrastra_el_rastro_de_anulacion() {
            GeneralChargeOpenAccountJpaEntity entity = mapper.toJpa(
                    GeneralChargeOpenAccountMother.cargoAnulado(), taxJpa, openAccountJpa,
                    createdByJpa, voidedByJpa);

            assertThat(entity.isVoided()).isTrue();
            assertThat(entity.getVoidedBy()).isSameAs(voidedByJpa);
            assertThat(entity.getVoidedAt()).isEqualTo(GeneralChargeOpenAccountMother.ANULADO);
            assertThat(entity.getVoidReason()).isEqualTo("Cobrado por error");
        }

        @Test
        @DisplayName("un cargo sin impuesto no engancha ninguna fila de impuesto")
        void un_cargo_sin_impuesto_no_engancha_impuesto() {
            GeneralChargeOpenAccountJpaEntity entity = mapper.toJpa(
                    GeneralChargeOpenAccountMother.cargoSinImpuesto(), null, openAccountJpa,
                    createdByJpa, null);

            assertThat(entity.getTax()).isNull();
            assertThat(entity.isHasTax()).isFalse();
            assertThat(entity.getTaxPercentage()).isNull();
            assertThat(entity.getTaxName()).isNull();
            assertThat(entity.getTaxScheme()).isNull();
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio -> entidad -> dominio conserva el contenido fiscal")
        void ida_y_vuelta_conserva_el_contenido_fiscal() {
            referenciasHidratadas();
            when(taxJpa.getId()).thenReturn(4L);
            when(taxJpa.getName()).thenReturn("IVA 19%");
            when(taxJpa.getPercentage()).thenReturn(new BigDecimal("19.00"));
            when(taxJpa.getTaxScheme()).thenReturn(TaxScheme.IVA);
            GeneralChargeOpenAccount original = GeneralChargeOpenAccountMother.cargo();

            GeneralChargeOpenAccount vuelta = mapper
                    .toDomain(mapper.toJpa(original, taxJpa, openAccountJpa, createdByJpa, null));

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getName()).isEqualTo(original.getName());
            assertThat(vuelta.getUnitAmount()).isEqualByComparingTo(original.getUnitAmount());
            assertThat(vuelta.getQuantity()).isEqualByComparingTo(original.getQuantity());
            assertThat(vuelta.getBaseAmount()).isEqualByComparingTo(original.getBaseAmount());
            assertThat(vuelta.getTaxAmount()).isEqualByComparingTo(original.getTaxAmount());
            assertThat(vuelta.getTotalAmount()).isEqualByComparingTo(original.getTotalAmount());
            assertThat(vuelta.getTaxScheme()).isEqualTo(original.getTaxScheme());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
        }
    }
}
