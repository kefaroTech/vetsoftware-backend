package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductChargeOpenAccount;
import com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother;
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
 * Las entidades JPA de OTRAS features (animal, product, tax, openaccount,
 * employee) se mockean: su constructor es {@code protected} y el vertical
 * slicing impide construirlas desde aqui. En {@code toJpa} solo viajan por
 * referencia, y en el {@code toDomain} de una sola pieza el mapper unicamente
 * les pide accesores, que es exactamente lo que el doble sabe responder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductChargeOpenAccountJpaMapper")
class ProductChargeOpenAccountJpaMapperTest {

    private final ProductChargeOpenAccountJpaMapper mapper = new ProductChargeOpenAccountJpaMapper();

    @Mock
    private AnimalJpaEntity animalJpa;
    @Mock
    private ProductJpaEntity productJpa;
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

    private ProductChargeOpenAccountJpaEntity entidadCompleta() {
        ProductChargeOpenAccountJpaEntity entity = new ProductChargeOpenAccountJpaEntity();
        entity.setId(ProductChargeOpenAccountMother.CHARGE_ID);
        entity.setAnimal(animalJpa);
        entity.setProduct(productJpa);
        entity.setUnitPrice(new BigDecimal("11900"));
        entity.setQuantity(2);
        entity.setTax(taxJpa);
        entity.setHasTax(true);
        entity.setTaxPercentage(new BigDecimal("19.00"));
        entity.setTaxName("IVA 19%");
        entity.setTaxScheme("IVA");
        entity.setTaxTreatment("GRAVADO");
        entity.setBaseAmount(new BigDecimal("20000.00"));
        entity.setTaxAmount(new BigDecimal("3800.00"));
        entity.setTotalAmount(new BigDecimal("23800.00"));
        entity.setOpenAccount(openAccountJpa);
        entity.setCreatedBy(createdByJpa);
        entity.setCreatedDate(ProductChargeOpenAccountMother.CREADO);
        entity.setEnabled(true);
        entity.setVoided(false);
        entity.setVoidedBy(null);
        entity.setVoidedAt(null);
        entity.setVoidReason(null);
        entity.setClientRequestId("req-1");
        return entity;
    }

    @Nested
    @DisplayName("toJpa")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo del dominio y engancha las asociaciones recibidas")
        void copia_cada_campo_y_engancha_las_asociaciones() {
            ProductChargeOpenAccount charge = ProductChargeOpenAccountMother.cargoAnulado();

            ProductChargeOpenAccountJpaEntity entity = mapper.toJpa(charge, animalJpa, productJpa,
                    taxJpa, openAccountJpa, createdByJpa, voidedByJpa);

            assertThat(entity.getId()).isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
            assertThat(entity.getAnimal()).isSameAs(animalJpa);
            assertThat(entity.getProduct()).isSameAs(productJpa);
            assertThat(entity.getTax()).isSameAs(taxJpa);
            assertThat(entity.getOpenAccount()).isSameAs(openAccountJpa);
            assertThat(entity.getCreatedBy()).isSameAs(createdByJpa);
            assertThat(entity.getVoidedBy()).isSameAs(voidedByJpa);
            assertThat(entity.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(entity.getQuantity()).isEqualTo(1);
            assertThat(entity.isHasTax()).isTrue();
            assertThat(entity.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(entity.getTaxName()).isEqualTo("IVA 19%");
            assertThat(entity.getTaxScheme()).isEqualTo("IVA");
            assertThat(entity.getTaxTreatment()).isEqualTo("GRAVADO");
            assertThat(entity.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(entity.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(entity.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(entity.getCreatedDate()).isEqualTo(ProductChargeOpenAccountMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.isVoided()).isTrue();
            assertThat(entity.getVoidedAt()).isEqualTo(ProductChargeOpenAccountMother.ANULADO);
            assertThat(entity.getVoidReason()).isEqualTo("Cobrado por error");
        }

        @Test
        @DisplayName("un cargo sin impuesto ni anulacion deja esas asociaciones en null")
        void un_cargo_sin_impuesto_ni_anulacion() {
            ProductChargeOpenAccount charge = new ProductChargeOpenAccount(1L,
                    ProductChargeOpenAccountMother.ANIMAL,
                    ProductChargeOpenAccountMother.PRODUCTO_SIN_IMPUESTO, new BigDecimal("5000"),
                    ProductChargeOpenAccountMother.CUENTA, ProductChargeOpenAccountMother.EMPLEADO,
                    ProductChargeOpenAccountMother.CREADO, true);

            ProductChargeOpenAccountJpaEntity entity = mapper.toJpa(charge, animalJpa, productJpa,
                    null, openAccountJpa, createdByJpa, null);

            assertThat(entity.getTax()).isNull();
            assertThat(entity.getVoidedBy()).isNull();
            assertThat(entity.isHasTax()).isFalse();
            assertThat(entity.getTaxPercentage()).isNull();
            assertThat(entity.getTaxScheme()).isNull();
            assertThat(entity.getClientRequestId()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado campo por campo")
        void reconstruye_el_agregado_campo_por_campo() {
            ProductChargeOpenAccount charge = mapper.toDomain(entidadCompleta(),
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    ProductChargeOpenAccountMother.IVA_19, ProductChargeOpenAccountMother.CUENTA,
                    ProductChargeOpenAccountMother.EMPLEADO, null);

            assertThat(charge.getId()).isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
            assertThat(charge.getAnimal()).isEqualTo(ProductChargeOpenAccountMother.ANIMAL);
            assertThat(charge.getProduct()).isEqualTo(ProductChargeOpenAccountMother.PRODUCTO);
            assertThat(charge.getTax()).isEqualTo(ProductChargeOpenAccountMother.IVA_19);
            assertThat(charge.getOpenAccount()).isEqualTo(ProductChargeOpenAccountMother.CUENTA);
            assertThat(charge.getCreatedBy()).isEqualTo(ProductChargeOpenAccountMother.EMPLEADO);
            assertThat(charge.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(charge.getQuantity()).isEqualTo(2);
            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(charge.getTaxName()).isEqualTo("IVA 19%");
            assertThat(charge.getTaxScheme()).isEqualTo("IVA");
            assertThat(charge.getTaxTreatment()).isEqualTo("GRAVADO");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("20000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("3800.00");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("23800.00");
            assertThat(charge.getCreatedDate()).isEqualTo(ProductChargeOpenAccountMother.CREADO);
            assertThat(charge.isEnabled()).isTrue();
            assertThat(charge.isVoided()).isFalse();
            assertThat(charge.getVoidedBy()).isNull();
            assertThat(charge.getClientRequestId()).isEqualTo("req-1");
        }

        @Test
        @DisplayName("la ida y vuelta conserva el dinero y la traza de anulacion")
        void la_ida_y_vuelta_conserva_el_dinero_y_la_anulacion() {
            ProductChargeOpenAccount original = ProductChargeOpenAccountMother.cargoAnulado();

            ProductChargeOpenAccountJpaEntity entity = mapper.toJpa(original, animalJpa, productJpa,
                    taxJpa, openAccountJpa, createdByJpa, voidedByJpa);
            ProductChargeOpenAccount vuelta = mapper.toDomain(entity,
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    ProductChargeOpenAccountMother.IVA_19, ProductChargeOpenAccountMother.CUENTA,
                    ProductChargeOpenAccountMother.EMPLEADO,
                    ProductChargeOpenAccountMother.OTRO_EMPLEADO);

            assertThat(vuelta.getUnitPrice()).isEqualByComparingTo(original.getUnitPrice());
            assertThat(vuelta.getBaseAmount()).isEqualByComparingTo(original.getBaseAmount());
            assertThat(vuelta.getTaxAmount()).isEqualByComparingTo(original.getTaxAmount());
            assertThat(vuelta.getTotalAmount()).isEqualByComparingTo(original.getTotalAmount());
            assertThat(vuelta.getQuantity()).isEqualTo(original.getQuantity());
            assertThat(vuelta.isVoided()).isTrue();
            assertThat(vuelta.getVoidedAt()).isEqualTo(original.getVoidedAt());
            assertThat(vuelta.getVoidReason()).isEqualTo(original.getVoidReason());
            assertThat(vuelta.getVoidedBy())
                    .isEqualTo(ProductChargeOpenAccountMother.OTRO_EMPLEADO);
        }
    }

    @Nested
    @DisplayName("toDomain derivando los refs de las asociaciones")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("arma cada companion VO leyendo la entidad JPA ya hidratada")
        void arma_cada_companion_vo_desde_la_entidad_hidratada() {
            when(animalJpa.getId()).thenReturn(1L);
            when(animalJpa.getName()).thenReturn("Firulais");
            when(animalJpa.getCode()).thenReturn("A-001");
            when(productJpa.getId()).thenReturn(2L);
            when(productJpa.getName()).thenReturn("Alimento");
            when(productJpa.getCode()).thenReturn("P-001");
            when(productJpa.getSalePrice()).thenReturn(new BigDecimal("11900"));
            when(taxJpa.getId()).thenReturn(4L);
            when(taxJpa.getName()).thenReturn("IVA 19%");
            when(taxJpa.getPercentage()).thenReturn(new BigDecimal("19.00"));
            when(taxJpa.getTaxScheme()).thenReturn(TaxScheme.IVA);
            when(openAccountJpa.getId()).thenReturn(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
            when(openAccountJpa.getCompany()).thenReturn(companyJpa);
            when(companyJpa.getId()).thenReturn(ProductChargeOpenAccountMother.COMPANY_ID);
            when(createdByJpa.getId()).thenReturn(7L);
            when(createdByJpa.getName()).thenReturn("Ana Ruiz");

            ProductChargeOpenAccount charge = mapper.toDomain(entidadCompleta());

            assertThat(charge.getAnimal()).isEqualTo(ProductChargeOpenAccountMother.ANIMAL);
            assertThat(charge.getProduct().id()).isEqualTo(2L);
            assertThat(charge.getProduct().salePrice()).isEqualByComparingTo("11900");
            // El ProductRef del camino de lectura usa el constructor de compat: sin
            // impuesto embebido, porque el desglose ya viaja en columnas propias.
            assertThat(charge.getProduct().hasTax()).isFalse();
            assertThat(charge.getTax()).isEqualTo(ProductChargeOpenAccountMother.IVA_19);
            assertThat(charge.getOpenAccount()).isEqualTo(ProductChargeOpenAccountMother.CUENTA);
            assertThat(charge.getCreatedBy()).isEqualTo(ProductChargeOpenAccountMother.EMPLEADO);
            assertThat(charge.getVoidedBy()).isNull();
        }

        @Test
        @DisplayName("sin impuesto ni anulador deja el TaxRef y el EmployeeRef en null")
        void sin_impuesto_ni_anulador_deja_los_refs_en_null() {
            when(animalJpa.getId()).thenReturn(1L);
            when(animalJpa.getName()).thenReturn("Firulais");
            when(animalJpa.getCode()).thenReturn("A-001");
            when(productJpa.getId()).thenReturn(2L);
            when(productJpa.getName()).thenReturn("Alimento");
            when(productJpa.getCode()).thenReturn("P-001");
            when(productJpa.getSalePrice()).thenReturn(new BigDecimal("11900"));
            when(openAccountJpa.getId()).thenReturn(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
            when(openAccountJpa.getCompany()).thenReturn(companyJpa);
            when(companyJpa.getId()).thenReturn(ProductChargeOpenAccountMother.COMPANY_ID);
            when(createdByJpa.getId()).thenReturn(7L);
            when(createdByJpa.getName()).thenReturn("Ana Ruiz");
            ProductChargeOpenAccountJpaEntity entity = entidadCompleta();
            entity.setTax(null);
            entity.setHasTax(false);
            entity.setTaxPercentage(null);
            entity.setTaxName(null);
            entity.setTaxScheme(null);

            ProductChargeOpenAccount charge = mapper.toDomain(entity);

            assertThat(charge.getTax()).isNull();
            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getVoidedBy()).isNull();
        }

        @Test
        @DisplayName("un impuesto sin esquema no revienta al derivar el nombre del enum")
        void un_impuesto_sin_esquema_no_revienta() {
            when(animalJpa.getId()).thenReturn(1L);
            when(animalJpa.getName()).thenReturn("Firulais");
            when(animalJpa.getCode()).thenReturn("A-001");
            when(productJpa.getId()).thenReturn(2L);
            when(productJpa.getName()).thenReturn("Alimento");
            when(productJpa.getCode()).thenReturn("P-001");
            when(productJpa.getSalePrice()).thenReturn(new BigDecimal("11900"));
            when(taxJpa.getId()).thenReturn(4L);
            when(taxJpa.getName()).thenReturn("IVA 19%");
            when(taxJpa.getPercentage()).thenReturn(new BigDecimal("19.00"));
            when(taxJpa.getTaxScheme()).thenReturn(null);
            when(openAccountJpa.getId()).thenReturn(ProductChargeOpenAccountMother.OPEN_ACCOUNT_ID);
            when(openAccountJpa.getCompany()).thenReturn(companyJpa);
            when(companyJpa.getId()).thenReturn(ProductChargeOpenAccountMother.COMPANY_ID);
            when(createdByJpa.getId()).thenReturn(7L);
            when(createdByJpa.getName()).thenReturn("Ana Ruiz");

            ProductChargeOpenAccount charge = mapper.toDomain(entidadCompleta());

            assertThat(charge.getTax().scheme()).isNull();
        }
    }
}
