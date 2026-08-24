package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Version;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El mapeo de las tres tablas de dinero, que es donde vive la mitad de la
 * TRAMPA 2.
 *
 * <p>
 * Vive en {@code infrastructure.persistence} y no junto al test de dominio a
 * propósito: afirma sobre anotaciones de {@code jakarta.persistence}, y el
 * dominio no puede depender del framework ({@code DOMINIO_SIN_FRAMEWORK}).
 */
@DisplayName("Mapeo JPA de la capa de dinero — lo que ningun codigo puede escribir")
class SubscriptionBillingDocumentJpaEntityTest {

    private static List<String> campos(Class<?> tipo) {
        return Arrays.stream(tipo.getDeclaredFields()).map(Field::getName).toList();
    }

    private static List<String> metodos(Class<?> tipo) {
        return Arrays.stream(tipo.getDeclaredMethods()).map(m -> m.getName()).toList();
    }

    @Nested
    @DisplayName("El saldo calculado — TRAMPA 2")
    class SaldoCalculado {

        @Test
        @DisplayName("balance_amount va de solo lectura: sin insertable=false y updatable=false"
                + " MySQL rechaza el INSERT entero, porque una columna generada no admite valor")
        void la_columna_calculada_es_de_solo_lectura() throws Exception {
            Field campo = SubscriptionBillingDocumentJpaEntity.class
                    .getDeclaredField("balanceAmount");
            Column columna = campo.getAnnotation(Column.class);

            assertThat(columna).isNotNull();
            assertThat(columna.insertable()).isFalse();
            assertThat(columna.updatable()).isFalse();
            assertThat(campo.getAnnotation(Generated.class)).isNotNull();
        }

        @Test
        @DisplayName("no existe setBalanceAmount, y su ausencia es la barrera:"
                + " ni el mapper ni un servicio ni un test pueden escribir el saldo")
        void no_existe_mutador_del_saldo() {
            assertThat(metodos(SubscriptionBillingDocumentJpaEntity.class))
                    .contains("getBalanceAmount").doesNotContain("setBalanceAmount");
        }

        @Test
        @DisplayName("las dos columnas generadas STORED ni se mapean:"
                + " lo que no existe en la entidad no se puede escribir por descuido")
        void los_marcadores_no_se_mapean() {
            assertThat(campos(SubscriptionBillingDocumentJpaEntity.class))
                    .doesNotContain("recurringCycleMarker", "overdueMarker");
        }
    }

    @Nested
    @DisplayName("Bloqueo optimista y borrado logico")
    class BloqueoYBorrado {

        @Test
        @DisplayName("la cabecera lleva @Version: es donde se apoyan las exenciones"
                + " de sus dos tablas hijas")
        void la_cabecera_va_versionada() throws Exception {
            assertThat(SubscriptionBillingDocumentJpaEntity.class.getDeclaredField("version")
                    .getAnnotation(Version.class)).isNotNull();
        }

        @Test
        @DisplayName("ninguna de las tres tablas lleva enabled ni borrado logico:"
                + " un cargo o una factura no se desactivan, se corrigen con otro documento")
        void ninguna_tabla_de_dinero_se_desactiva() {
            List<Class<?>> tablasDeDinero = List.of(SubscriptionBillingDocumentJpaEntity.class,
                    SubscriptionChargeJpaEntity.class,
                    SubscriptionBillingDocumentTaxJpaEntity.class,
                    BillingDocumentSequenceJpaEntity.class);

            for (Class<?> tabla : tablasDeDinero) {
                assertThat(campos(tabla)).as("%s no puede llevar enabled", tabla.getSimpleName())
                        .doesNotContain("enabled");
                assertThat(tabla.getAnnotation(SQLDelete.class))
                        .as("%s no puede tener @SQLDelete", tabla.getSimpleName()).isNull();
                assertThat(tabla.getAnnotation(SQLRestriction.class))
                        .as("%s no puede tener @SQLRestriction", tabla.getSimpleName()).isNull();
            }
        }

        @Test
        @DisplayName("las tres exentas no declaran @Version: es lo que su entrada"
                + " en ENTIDADES_EXENTAS_DE_VERSION afirma por escrito")
        void las_tres_exentas_siguen_sin_version() {
            assertThat(campos(SubscriptionChargeJpaEntity.class)).doesNotContain("version");
            assertThat(campos(SubscriptionBillingDocumentTaxJpaEntity.class))
                    .doesNotContain("version");
            assertThat(campos(BillingDocumentSequenceJpaEntity.class)).doesNotContain("version");
        }
    }

    @Nested
    @DisplayName("Las FK compuestas no se deshacen mapeando la simple")
    class FkCompuestas {

        @Test
        @DisplayName("las referencias entre tablas de dinero son ids pelados, no @ManyToOne:"
                + " la FK compuesta arrastra la empresa y una asociacion de una sola columna"
                + " le pediria a Hibernate la FK simple que deshace esa garantia")
        void las_referencias_son_ids_pelados() {
            assertThat(campos(SubscriptionChargeJpaEntity.class)).contains("companyId",
                    "subscriptionId", "subscriptionItemId", "amendmentId", "billingDocumentId",
                    "voidsChargeId");
            assertThat(campos(SubscriptionBillingDocumentJpaEntity.class)).contains("companyId",
                    "subscriptionId", "correctsDocumentId");

            assertThat(Arrays.stream(SubscriptionChargeJpaEntity.class.getDeclaredFields())
                    .filter(f -> f.getAnnotation(jakarta.persistence.ManyToOne.class) != null)
                    .toList()).isEmpty();
            assertThat(Arrays.stream(SubscriptionBillingDocumentJpaEntity.class.getDeclaredFields())
                    .filter(f -> f.getAnnotation(jakarta.persistence.ManyToOne.class) != null)
                    .toList()).isEmpty();
        }

        @Test
        @DisplayName("el cargo NO tiene columna de impuesto: guarda su base y su tarifa,"
                + " y el importe del IVA vive una sola vez en el desglose")
        void el_cargo_no_guarda_su_impuesto() {
            assertThat(campos(SubscriptionChargeJpaEntity.class))
                    .contains("subtotalAmount", "taxRate", "taxTreatment")
                    .doesNotContain("taxAmount");
            assertThat(campos(SubscriptionBillingDocumentTaxJpaEntity.class))
                    .contains("taxableBase", "taxAmount", "taxRate", "taxTreatment");
        }
    }
}
