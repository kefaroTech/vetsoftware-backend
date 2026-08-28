package com.vetsoftware.app.accountingaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountingaccount.domain.AccountClass;
import com.vetsoftware.app.accountingaccount.domain.AccountingAccount;
import com.vetsoftware.app.accountingaccount.testsupport.AccountingAccountMother;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 */
@DisplayName("AccountingAccountJpaMapper")
class AccountingAccountJpaMapperTest {

    private final AccountingAccountJpaMapper mapper = new AccountingAccountJpaMapper();

    @Nested
    @DisplayName("toJpa - dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar() {
            AccountingAccount cuenta = AccountingAccountMother.cuentaPostable();

            AccountingAccountJpaEntity entity = mapper.toJpa(cuenta);

            assertThat(entity.getId()).isEqualTo(cuenta.getId());
            assertThat(entity.getCode()).isEqualTo(cuenta.getCode());
            assertThat(entity.getName()).isEqualTo(cuenta.getName());
            assertThat(entity.getAccountClass()).isEqualTo(cuenta.getAccountClass());
            assertThat(entity.getParentCode()).isEqualTo(cuenta.getParentCode());
            assertThat(entity.getAccountLevel()).isEqualTo((byte) cuenta.getAccountLevel());
            assertThat(entity.isPostable()).isEqualTo(cuenta.isPostable());
            assertThat(entity.isRequiresThirdParty()).isEqualTo(cuenta.isRequiresThirdParty());
            assertThat(entity.getValidFrom()).isEqualTo(cuenta.getValidFrom());
            assertThat(entity.getValidTo()).isEqualTo(cuenta.getValidTo());
            assertThat(entity.getCreatedDate()).isEqualTo(cuenta.getCreatedDate());
            assertThat(entity.isEnabled()).isEqualTo(cuenta.isEnabled());
        }

        @Test
        @DisplayName("copia la version de una cuenta ya persistida: sin esto el merge seria INSERT")
        void copia_la_version_de_una_cuenta_persistida() {
            AccountingAccount cuenta = AccountingAccountMother.cuentaPostable();

            AccountingAccountJpaEntity entity = mapper.toJpa(cuenta);

            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getVersion()).isNotNull().isEqualTo(cuenta.getVersion());
        }

        @Test
        @DisplayName("una cuenta nueva sin id deja la version nula: decide Hibernate el INSERT")
        void cuenta_nueva_sin_id_deja_la_version_nula() {
            AccountingAccount nueva = AccountingAccount.create("110507", "Bancos",
                    AccountClass.ASSET, "1105", 6, true, false, LocalDate.of(2026, 1, 1), null,
                    LocalDateTime.of(2026, 1, 1, 9, 0));

            AccountingAccountJpaEntity entity = mapper.toJpa(nueva);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("toDomain - entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("la ida y vuelta dominio -> entidad -> dominio no pierde nada, con version")
        void la_ida_y_vuelta_no_pierde_nada() {
            AccountingAccount original = AccountingAccountMother.cuentaPostable();

            AccountingAccountJpaEntity entity = mapper.toJpa(original);
            AccountingAccount vuelta = mapper.toDomain(entity);

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }

        @Test
        @DisplayName("el nivel viaja como byte y vuelve como int intacto en el dominio")
        void el_nivel_byte_vuelve_a_int_sin_perder_el_valor() {
            AccountingAccountJpaEntity entity = mapper
                    .toJpa(AccountingAccountMother.cuentaPostable());

            AccountingAccount cuenta = mapper.toDomain(entity);

            assertThat(cuenta.getAccountLevel()).isEqualTo(6);
        }
    }
}
