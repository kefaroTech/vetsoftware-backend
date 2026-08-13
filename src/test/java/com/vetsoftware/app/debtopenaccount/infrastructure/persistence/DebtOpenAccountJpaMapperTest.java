package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import com.vetsoftware.app.debtopenaccount.testsupport.DebtOpenAccountMother;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
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
 * Las entidades JPA de OTRAS features (openaccount, employee) se mockean: su
 * constructor es {@code protected} y el vertical slicing impide construirlas
 * desde aqui. En {@code toJpa} solo viajan por referencia, y en el
 * {@code toDomain} de una sola pieza el mapper unicamente les pide accesores,
 * que es exactamente lo que el doble sabe responder.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DebtOpenAccountJpaMapper")
class DebtOpenAccountJpaMapperTest {

    private final DebtOpenAccountJpaMapper mapper = new DebtOpenAccountJpaMapper();

    @Mock
    private OpenAccountJpaEntity openAccountJpa;
    @Mock
    private CompanyJpaEntity companyJpa;
    @Mock
    private EmployeeJpaEntity createdByJpa;
    @Mock
    private EmployeeJpaEntity voidedByJpa;

    private DebtOpenAccountJpaEntity entidadCompleta() {
        DebtOpenAccountJpaEntity entity = new DebtOpenAccountJpaEntity();
        entity.setId(DebtOpenAccountMother.PAYMENT_ID);
        entity.setAmount(new BigDecimal("30000"));
        entity.setPaymentMethod(PaymentMethod.CASH);
        entity.setOpenAccount(openAccountJpa);
        entity.setCreatedBy(createdByJpa);
        entity.setCreatedDate(DebtOpenAccountMother.CREADO);
        entity.setEnabled(true);
        entity.setVoided(false);
        entity.setClientRequestId("req-1");
        return entity;
    }

    /** Deja los dobles respondiendo lo que el mapper les va a preguntar. */
    private void referenciasHidratadas() {
        when(openAccountJpa.getId()).thenReturn(DebtOpenAccountMother.OPEN_ACCOUNT_ID);
        when(openAccountJpa.getCompany()).thenReturn(companyJpa);
        when(companyJpa.getId()).thenReturn(DebtOpenAccountMother.COMPANY_ID);
        when(createdByJpa.getId()).thenReturn(7L);
        when(createdByJpa.getName()).thenReturn("Ana Ruiz");
    }

    @Nested
    @DisplayName("toDomain")
    class ADominio {

        @Test
        @DisplayName("traslada la fila campo a campo")
        void traslada_la_fila_campo_a_campo() {
            referenciasHidratadas();

            DebtOpenAccount abono = mapper.toDomain(entidadCompleta());

            assertThat(abono.getId()).isEqualTo(DebtOpenAccountMother.PAYMENT_ID);
            assertThat(abono.getAmount()).isEqualByComparingTo("30000");
            assertThat(abono.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
            assertThat(abono.getOpenAccount().id())
                    .isEqualTo(DebtOpenAccountMother.OPEN_ACCOUNT_ID);
            assertThat(abono.getOpenAccount().companyId())
                    .isEqualTo(DebtOpenAccountMother.COMPANY_ID);
            assertThat(abono.getCreatedBy().name()).isEqualTo("Ana Ruiz");
            assertThat(abono.getCreatedDate()).isEqualTo(DebtOpenAccountMother.CREADO);
            assertThat(abono.isEnabled()).isTrue();
            assertThat(abono.isVoided()).isFalse();
            assertThat(abono.getVoidedBy()).isNull();
            assertThat(abono.getClientRequestId()).isEqualTo("req-1");
        }

        @Test
        @DisplayName("proyecta el rastro de anulacion")
        void proyecta_el_rastro_de_anulacion() {
            referenciasHidratadas();
            when(voidedByJpa.getId()).thenReturn(8L);
            when(voidedByJpa.getName()).thenReturn("Luis Paz");
            DebtOpenAccountJpaEntity entity = entidadCompleta();
            entity.setVoided(true);
            entity.setVoidedBy(voidedByJpa);
            entity.setVoidedAt(DebtOpenAccountMother.ANULADO);
            entity.setVoidReason("Cobrado por error");

            DebtOpenAccount abono = mapper.toDomain(entity);

            assertThat(abono.isVoided()).isTrue();
            assertThat(abono.getVoidedBy().name()).isEqualTo("Luis Paz");
            assertThat(abono.getVoidedAt()).isEqualTo(DebtOpenAccountMother.ANULADO);
            assertThat(abono.getVoidReason()).isEqualTo("Cobrado por error");
        }

        @Test
        @DisplayName("el empleado sin nombre en la fila no rompe la lectura")
        void el_empleado_sin_nombre_no_rompe_la_lectura() {
            // EmployeeRef de esta feature solo exige el id, asi que una fila historica
            // con el nombre vacio se sigue pudiendo leer en vez de reventar el listado.
            when(openAccountJpa.getId()).thenReturn(DebtOpenAccountMother.OPEN_ACCOUNT_ID);
            when(openAccountJpa.getCompany()).thenReturn(companyJpa);
            when(companyJpa.getId()).thenReturn(DebtOpenAccountMother.COMPANY_ID);
            when(createdByJpa.getId()).thenReturn(7L);
            when(createdByJpa.getName()).thenReturn(null);

            DebtOpenAccount abono = mapper.toDomain(entidadCompleta());

            assertThat(abono.getCreatedBy().name()).isNull();
        }
    }

    @Nested
    @DisplayName("toJpa")
    class AJpa {

        @Test
        @DisplayName("copia el abono y engancha las entidades que le pasan")
        void copia_el_abono_y_engancha_las_entidades() {
            DebtOpenAccountJpaEntity entity = mapper.toJpa(DebtOpenAccountMother.abono(),
                    openAccountJpa, createdByJpa, null);

            assertThat(entity.getId()).isEqualTo(DebtOpenAccountMother.PAYMENT_ID);
            assertThat(entity.getOpenAccount()).isSameAs(openAccountJpa);
            assertThat(entity.getCreatedBy()).isSameAs(createdByJpa);
            assertThat(entity.getVoidedBy()).isNull();
            assertThat(entity.getAmount()).isEqualByComparingTo("30000");
            assertThat(entity.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
            assertThat(entity.getCreatedDate()).isEqualTo(DebtOpenAccountMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
            assertThat(entity.isVoided()).isFalse();
        }

        @Test
        @DisplayName("arrastra el rastro de anulacion a la fila")
        void arrastra_el_rastro_de_anulacion() {
            DebtOpenAccountJpaEntity entity = mapper.toJpa(DebtOpenAccountMother.abonoAnulado(),
                    openAccountJpa, createdByJpa, voidedByJpa);

            assertThat(entity.isVoided()).isTrue();
            assertThat(entity.getVoidedBy()).isSameAs(voidedByJpa);
            assertThat(entity.getVoidedAt()).isEqualTo(DebtOpenAccountMother.ANULADO);
            assertThat(entity.getVoidReason()).isEqualTo("Cobrado por error");
        }

        @Test
        @DisplayName("arrastra la idempotency key a la fila: es la que deduplica el cobro")
        void arrastra_la_idempotency_key() {
            DebtOpenAccountJpaEntity entity = mapper.toJpa(
                    DebtOpenAccountMother.abonoConClave("req-7"), openAccountJpa, createdByJpa,
                    null);

            assertThat(entity.getClientRequestId()).isEqualTo("req-7");
        }
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("dominio -> entidad -> dominio conserva el abono")
        void ida_y_vuelta_conserva_el_abono() {
            referenciasHidratadas();
            DebtOpenAccount original = DebtOpenAccountMother.abono();

            DebtOpenAccount vuelta = mapper
                    .toDomain(mapper.toJpa(original, openAccountJpa, createdByJpa, null));

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getAmount()).isEqualByComparingTo(original.getAmount());
            assertThat(vuelta.getPaymentMethod()).isEqualTo(original.getPaymentMethod());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
            assertThat(vuelta.isEnabled()).isEqualTo(original.isEnabled());
            assertThat(vuelta.isVoided()).isEqualTo(original.isVoided());
        }
    }
}
