package com.vetsoftware.app.accountingexport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
import com.vetsoftware.app.accountingexport.testsupport.AccountingExportMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez.
 *
 * <p>
 * <strong>La version viaja en los dos sentidos</strong>: con {@code null} sobre
 * una entidad que ya tiene id, Hibernate la tomaria por transitoria y
 * escribiria una fila nueva en vez de un {@code merge}, chocando contra
 * {@code uq_accounting_exports_attempt}.
 *
 * <p>
 * <strong>No hay nada que afirmar sobre {@code current_export_marker}</strong>:
 * {@link AccountingExportJpaEntity} no declara ni getter ni setter para esa
 * columna generada — se comprobo leyendo la entidad entera. No existe forma de
 * que este mapper la toque en ningun sentido, asi que un test que "verifique
 * que no se mapea" no tendria nada real que ejercitar.
 */
@DisplayName("AccountingExportJpaMapper")
class AccountingExportJpaMapperTest {

    private final AccountingExportJpaMapper mapper = new AccountingExportJpaMapper();

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna, incluida la version")
        void copia_cada_campo_escalar_en_su_columna_incluida_la_version() {
            AccountingExport export = AccountingExportMother.generado();

            AccountingExportJpaEntity entity = mapper.toJpa(export);

            assertThat(entity.getId()).isEqualTo(AccountingExportMother.EXPORT_ID);
            assertThat(entity.getPeriodKey()).isEqualTo(AccountingExportMother.PERIOD_KEY);
            assertThat(entity.getExportKind()).isEqualTo(AccountingExportMother.KIND);
            assertThat(entity.getAttemptNumber()).isEqualTo(AccountingExportMother.ATTEMPT_NUMBER);
            assertThat(entity.getStatus()).isEqualTo(AccountingExportStatus.GENERATED);
            assertThat(entity.getGeneratedAt()).isEqualTo(AccountingExportMother.GENERATED_AT);
            assertThat(entity.getGeneratedBySystemUserId())
                    .isEqualTo(AccountingExportMother.GENERATED_BY);
            assertThat(entity.getTotalDebit()).isEqualByComparingTo(AccountingExportMother.TOTAL);
            assertThat(entity.getTotalCredit()).isEqualByComparingTo(AccountingExportMother.TOTAL);
            assertThat(entity.getTotalsHash()).isEqualTo(AccountingExportMother.TOTALS_HASH);
            assertThat(entity.getFileRef()).isEqualTo(AccountingExportMother.FILE_REF);
            assertThat(entity.getDeliveredAt()).isNull();
            assertThat(entity.getRejectedAt()).isNull();
            assertThat(entity.getRejectionReason()).isNull();
            assertThat(entity.getCreatedDate()).isEqualTo(AccountingExportMother.CREATED);
            assertThat(entity.getVersion()).isEqualTo(AccountingExportMother.VERSION);
        }

        @Test
        @DisplayName("la version de un export ya resuelto viaja intacta: nunca queda en blanco")
        void la_version_de_un_export_ya_resuelto_viaja_intacta() {
            AccountingExport resuelto = AccountingExportMother.generado()
                    .markDelivered(AccountingExportMother.GENERATED_AT);

            AccountingExportJpaEntity entity = mapper.toJpa(resuelto);

            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getVersion()).isEqualTo(AccountingExportMother.VERSION);
        }
    }

    @Nested
    @DisplayName("toDomain — entidad a dominio")
    class ToDomain {

        @Test
        @DisplayName("reconstruye el agregado completo desde la entidad, incluida la version")
        void reconstruye_el_agregado_completo_desde_la_entidad_incluida_la_version() {
            AccountingExportJpaEntity entity = mapper.toJpa(AccountingExportMother.generado());

            AccountingExport reconstruido = mapper.toDomain(entity);

            assertThat(reconstruido.getId()).isEqualTo(AccountingExportMother.EXPORT_ID);
            assertThat(reconstruido.getStatus()).isEqualTo(AccountingExportStatus.GENERATED);
            assertThat(reconstruido.getVersion()).isEqualTo(AccountingExportMother.VERSION);
        }

        @Test
        @DisplayName("ida y vuelta conserva un desenlace DELIVERED sin perder la version")
        void ida_y_vuelta_conserva_un_desenlace_delivered_sin_perder_la_version() {
            LocalDateTime deliveredAt = AccountingExportMother.GENERATED_AT.plusDays(2);
            AccountingExport entregado = AccountingExportMother.entregado(deliveredAt);

            AccountingExport reconstruido = mapper.toDomain(mapper.toJpa(entregado));

            assertThat(reconstruido.getStatus()).isEqualTo(AccountingExportStatus.DELIVERED);
            assertThat(reconstruido.getDeliveredAt()).isEqualTo(deliveredAt);
            assertThat(reconstruido.getVersion()).isEqualTo(AccountingExportMother.VERSION);
        }
    }
}
