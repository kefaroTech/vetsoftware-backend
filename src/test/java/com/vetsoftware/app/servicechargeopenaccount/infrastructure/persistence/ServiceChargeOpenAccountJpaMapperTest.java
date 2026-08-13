package com.vetsoftware.app.servicechargeopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaEntity;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.testsupport.ServiceChargeOpenAccountMother;
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
 * Las entidades JPA de OTRAS features (animal, service, tax, openaccount,
 * employee) se mockean: su constructor es {@code protected} y el vertical
 * slicing impide construirlas desde aqui. En {@code toJpa} solo viajan por
 * referencia, y en el {@code toDomain} de una sola pieza el mapper unicamente
 * les pide accesores, que es exactamente lo que el doble sabe responder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceChargeOpenAccountJpaMapper")
class ServiceChargeOpenAccountJpaMapperTest {

    private final ServiceChargeOpenAccountJpaMapper mapper = new ServiceChargeOpenAccountJpaMapper();

    @Mock
    private AnimalJpaEntity animalJpa;
    @Mock
    private ServiceJpaEntity serviceJpa;
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

    private ServiceChargeOpenAccountJpaEntity entidadCompleta() {
        ServiceChargeOpenAccountJpaEntity entity = new ServiceChargeOpenAccountJpaEntity();
        entity.setId(ServiceChargeOpenAccountMother.CHARGE_ID);
        entity.setAnimal(animalJpa);
        entity.setService(serviceJpa);
        entity.setUnitPrice(new BigDecimal("11900"));
        entity.setTax(taxJpa);
        entity.setHasTax(true);
        entity.setTaxPercentage(new BigDecimal("19.00"));
        entity.setTaxName("IVA 19%");
        entity.setTaxScheme("IVA");
        entity.setTaxTreatment("GRAVADO");
        entity.setBaseAmount(new BigDecimal("10000.00"));
        entity.setTaxAmount(new BigDecimal("1900.00"));
        entity.setTotalAmount(new BigDecimal("11900.00"));
        entity.setOpenAccount(openAccountJpa);
        entity.setCreatedBy(createdByJpa);
        entity.setCreatedDate(ServiceChargeOpenAccountMother.CREADO);
        entity.setEnabled(true);
        entity.setVoided(false);
        entity.setClientRequestId("req-1");
        return entity;
    }

    /** Deja los dobles respondiendo lo que el mapper les va a preguntar. */
    private void referenciasHidratadas() {
        when(animalJpa.getId()).thenReturn(1L);
        when(animalJpa.getName()).thenReturn("Firulais");
        when(animalJpa.getCode()).thenReturn("A-001");
        when(serviceJpa.getId()).thenReturn(2L);
        when(serviceJpa.getName()).thenReturn("Consulta general");
        when(serviceJpa.getPrice()).thenReturn(new BigDecimal("11900"));
        when(openAccountJpa.getId()).thenReturn(ServiceChargeOpenAccountMother.OPEN_ACCOUNT_ID);
        when(openAccountJpa.getCompany()).thenReturn(companyJpa);
        when(companyJpa.getId()).thenReturn(ServiceChargeOpenAccountMother.COMPANY_ID);
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
            when(createdByJpa.getId()).thenReturn(7L);
            when(createdByJpa.getName()).thenReturn("Ana Ruiz");

            ServiceChargeOpenAccount charge = mapper.toDomain(entidadCompleta());

            assertThat(charge.getId()).isEqualTo(ServiceChargeOpenAccountMother.CHARGE_ID);
            assertThat(charge.getAnimal().name()).isEqualTo("Firulais");
            assertThat(charge.getService().name()).isEqualTo("Consulta general");
            assertThat(charge.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTax().id()).isEqualTo(4L);
            assertThat(charge.getTax().scheme()).isEqualTo("IVA");
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(charge.getTaxTreatment()).isEqualTo("GRAVADO");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getOpenAccount().companyId())
                    .isEqualTo(ServiceChargeOpenAccountMother.COMPANY_ID);
            assertThat(charge.getCreatedBy().name()).isEqualTo("Ana Ruiz");
            assertThat(charge.getCreatedDate()).isEqualTo(ServiceChargeOpenAccountMother.CREADO);
            assertThat(charge.isEnabled()).isTrue();
            assertThat(charge.isVoided()).isFalse();
            assertThat(charge.getClientRequestId()).isEqualTo("req-1");
        }

        @Test
        @DisplayName("sin impuesto en la fila, el cargo queda sin TaxRef")
        void sin_impuesto_el_cargo_queda_sin_tax_ref() {
            referenciasHidratadas();
            when(createdByJpa.getId()).thenReturn(7L);
            when(createdByJpa.getName()).thenReturn("Ana Ruiz");
            ServiceChargeOpenAccountJpaEntity entity = entidadCompleta();
            entity.setTax(null);
            entity.setHasTax(false);

            ServiceChargeOpenAccount charge = mapper.toDomain(entity);

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
            when(createdByJpa.getId()).thenReturn(7L);
            when(createdByJpa.getName()).thenReturn("Ana Ruiz");

            ServiceChargeOpenAccount charge = mapper.toDomain(entidadCompleta());

            assertThat(charge.getTax().scheme()).isNull();
        }

        @Test
        @DisplayName("sin creador ni anulador, los refs quedan null en vez de reventar")
        void sin_creador_ni_anulador_los_refs_quedan_null() {
            referenciasHidratadas();
            ServiceChargeOpenAccountJpaEntity entity = entidadCompleta();
            entity.setTax(null);
            entity.setHasTax(false);
            entity.setCreatedBy(null);

            ServiceChargeOpenAccount charge = mapper.toDomain(entity);

            assertThat(charge.getCreatedBy()).isNull();
            assertThat(charge.getVoidedBy()).isNull();
        }

        @Test
        @DisplayName("proyecta el rastro de anulacion")
        void proyecta_el_rastro_de_anulacion() {
            referenciasHidratadas();
            when(createdByJpa.getId()).thenReturn(7L);
            when(createdByJpa.getName()).thenReturn("Ana Ruiz");
            when(voidedByJpa.getId()).thenReturn(8L);
            when(voidedByJpa.getName()).thenReturn("Luis Paz");
            ServiceChargeOpenAccountJpaEntity entity = entidadCompleta();
            entity.setTax(null);
            entity.setHasTax(false);
            entity.setVoided(true);
            entity.setVoidedBy(voidedByJpa);
            entity.setVoidedAt(ServiceChargeOpenAccountMother.ANULADO);
            entity.setVoidReason("Cobrado por error");

            ServiceChargeOpenAccount charge = mapper.toDomain(entity);

            assertThat(charge.isVoided()).isTrue();
            assertThat(charge.getVoidedBy().name()).isEqualTo("Luis Paz");
            assertThat(charge.getVoidedAt()).isEqualTo(ServiceChargeOpenAccountMother.ANULADO);
            assertThat(charge.getVoidReason()).isEqualTo("Cobrado por error");
        }
    }

    @Nested
    @DisplayName("toJpa")
    class AJpa {

        @Test
        @DisplayName("copia el agregado y engancha las entidades que le pasan")
        void copia_el_agregado_y_engancha_las_entidades() {
            ServiceChargeOpenAccountJpaEntity entity = mapper.toJpa(
                    ServiceChargeOpenAccountMother.cargo(), animalJpa, serviceJpa, taxJpa,
                    openAccountJpa, createdByJpa, null);

            assertThat(entity.getId()).isEqualTo(ServiceChargeOpenAccountMother.CHARGE_ID);
            assertThat(entity.getAnimal()).isSameAs(animalJpa);
            assertThat(entity.getService()).isSameAs(serviceJpa);
            assertThat(entity.getTax()).isSameAs(taxJpa);
            assertThat(entity.getOpenAccount()).isSameAs(openAccountJpa);
            assertThat(entity.getCreatedBy()).isSameAs(createdByJpa);
            assertThat(entity.getVoidedBy()).isNull();
            assertThat(entity.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(entity.isHasTax()).isTrue();
            assertThat(entity.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(entity.getTaxName()).isEqualTo("IVA 19%");
            assertThat(entity.getTaxScheme()).isEqualTo("IVA");
            assertThat(entity.getTaxTreatment()).isEqualTo("GRAVADO");
            assertThat(entity.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(entity.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(entity.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(entity.getCreatedDate()).isEqualTo(ServiceChargeOpenAccountMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.isVoided()).isFalse();
        }

        @Test
        @DisplayName("arrastra el rastro de anulacion a la fila")
        void arrastra_el_rastro_de_anulacion() {
            ServiceChargeOpenAccountJpaEntity entity = mapper.toJpa(
                    ServiceChargeOpenAccountMother.cargoAnulado(), animalJpa, serviceJpa, taxJpa,
                    openAccountJpa, createdByJpa, voidedByJpa);

            assertThat(entity.isVoided()).isTrue();
            assertThat(entity.getVoidedBy()).isSameAs(voidedByJpa);
            assertThat(entity.getVoidedAt()).isEqualTo(ServiceChargeOpenAccountMother.ANULADO);
            assertThat(entity.getVoidReason()).isEqualTo("Cobrado por error");
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
            when(createdByJpa.getId()).thenReturn(7L);
            when(createdByJpa.getName()).thenReturn("Ana Ruiz");
            ServiceChargeOpenAccount original = ServiceChargeOpenAccountMother.cargo();

            ServiceChargeOpenAccount vuelta = mapper.toDomain(mapper.toJpa(original, animalJpa,
                    serviceJpa, taxJpa, openAccountJpa, createdByJpa, null));

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getUnitPrice()).isEqualByComparingTo(original.getUnitPrice());
            assertThat(vuelta.getBaseAmount()).isEqualByComparingTo(original.getBaseAmount());
            assertThat(vuelta.getTaxAmount()).isEqualByComparingTo(original.getTaxAmount());
            assertThat(vuelta.getTotalAmount()).isEqualByComparingTo(original.getTotalAmount());
            assertThat(vuelta.getTaxTreatment()).isEqualTo(original.getTaxTreatment());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
        }
    }
}
