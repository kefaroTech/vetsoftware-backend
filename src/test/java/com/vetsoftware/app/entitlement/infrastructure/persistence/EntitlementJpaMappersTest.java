package com.vetsoftware.app.entitlement.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.entitlement.domain.MeasureKind;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.EntitlementSource;
import com.vetsoftware.app.entitlement.domain.SubModuleRef;
import com.vetsoftware.app.entitlement.testsupport.EntitlementMother;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Ida y vuelta dominio &harr; fila de los dos mapeadores del slice, sin base de
 * datos y sin framework.
 *
 * <p>
 * Un mapeador que se deja un campo no rompe nada visible: compila, la fila se
 * guarda y el permiso vuelve con un {@code validUntil} en {@code null}, que es
 * exactamente "no caduca nunca". La prueba que caducaba sola dejaria de
 * caducar, y el sintoma seria un tenant usando gratis un modulo que ya no tiene
 * contratado. Lo mismo con {@code usedQuantity}: perderlo en el mapeo pone a
 * cero el consumo real de una empresa y le regala la diferencia contra su
 * techo.
 *
 * <p>
 * Por eso las aserciones son campo a campo y no {@code isEqualTo} del objeto:
 * ninguna de las dos clases de dominio implementa {@code equals}, asi que una
 * comparacion por identidad pasaria siempre y no probaria nada.
 */
@DisplayName("Mapeadores JPA de entitlements y contadores")
class EntitlementJpaMappersTest {

    private static final Long COMPANY_ID = EntitlementMother.COMPANY_ID;
    private static final LocalDateTime AHORA = EntitlementMother.AHORA;

    private final CompanyEntitlementJpaMapper entitlementMapper = new CompanyEntitlementJpaMapper();
    private final CompanyCapacityJpaMapper capacityMapper = new CompanyCapacityJpaMapper();

    /**
     * Las dos entidades JPA que este mapeador necesita viven en <b>otras</b>
     * features y solo exponen el constructor {@code protected} que exige Hibernate,
     * asi que desde este paquete no se pueden instanciar directamente. El resto de
     * mapeadores del repo se prueban desde el paquete de su propia entidad y por
     * eso nadie se habia topado con esto.
     *
     * <p>
     * La subclase anonima si alcanza el constructor protegido. Mockearlas no es
     * opcion: son entidades, y la regla del CLAUDE.md solo admite dobles de
     * puertos.
     */
    private static CompanyJpaEntity empresa() {
        CompanyJpaEntity company = new CompanyJpaEntity() {
        };
        company.setId(COMPANY_ID);
        return company;
    }

    private static SubModuleJpaEntity submoduloJpa(SubModuleRef ref) {
        SubModuleJpaEntity entity = new SubModuleJpaEntity() {
        };
        entity.setId(ref.id());
        entity.setCode(ref.code());
        entity.setName(ref.name());
        return entity;
    }

    @Nested
    @DisplayName("permisos de empresa")
    class Permisos {

        private CompanyEntitlement permisoCompleto() {
            return new CompanyEntitlement(77L, COMPANY_ID, EntitlementMother.historiaClinica(),
                    AccessLevel.READ_ONLY, EntitlementSource.SUBSCRIPTION,
                    EntitlementMother.SUBSCRIPTION_ID, 900L, AHORA.minusDays(60),
                    AHORA.plusDays(30), AHORA.minusDays(1), AHORA.minusDays(60));
        }

        @Test
        @DisplayName("la ida al JPA no pierde ningun campo del permiso")
        void la_ida_no_pierde_ningun_campo() {
            CompanyEntitlement permiso = permisoCompleto();

            CompanyEntitlementJpaEntity fila = entitlementMapper.toJpa(permiso, empresa(),
                    submoduloJpa(EntitlementMother.historiaClinica()));

            assertThat(fila.getId()).isEqualTo(77L);
            assertThat(fila.getCompany().getId()).isEqualTo(COMPANY_ID);
            assertThat(fila.getSubModule().getCode()).isEqualTo("CLINICAL_HISTORY");
            assertThat(fila.getAccessLevel()).isEqualTo("READ_ONLY");
            assertThat(fila.getSource()).isEqualTo("SUBSCRIPTION");
            assertThat(fila.getSubscriptionId()).isEqualTo(EntitlementMother.SUBSCRIPTION_ID);
            assertThat(fila.getSubscriptionItemId()).isEqualTo(900L);
            assertThat(fila.getValidFrom()).isEqualTo(AHORA.minusDays(60));
            assertThat(fila.getValidUntil()).isEqualTo(AHORA.plusDays(30));
            assertThat(fila.getRecalculatedAt()).isEqualTo(AHORA.minusDays(1));
            assertThat(fila.getCreatedDate()).isEqualTo(AHORA.minusDays(60));
        }

        @Test
        @DisplayName("la vuelta reconstruye el permiso identico, con su ventana intacta")
        void la_vuelta_reconstruye_el_permiso_identico() {
            CompanyEntitlement original = permisoCompleto();
            CompanyEntitlementJpaEntity fila = entitlementMapper.toJpa(original, empresa(),
                    submoduloJpa(EntitlementMother.historiaClinica()));

            CompanyEntitlement vuelta = entitlementMapper.toDomain(fila);

            assertThat(vuelta.getId()).isEqualTo(original.getId());
            assertThat(vuelta.getCompanyId()).isEqualTo(original.getCompanyId());
            assertThat(vuelta.getSubModule()).isEqualTo(original.getSubModule());
            assertThat(vuelta.getAccessLevel()).isEqualTo(original.getAccessLevel());
            assertThat(vuelta.getSource()).isEqualTo(original.getSource());
            assertThat(vuelta.getSubscriptionId()).isEqualTo(original.getSubscriptionId());
            assertThat(vuelta.getSubscriptionItemId()).isEqualTo(original.getSubscriptionItemId());
            assertThat(vuelta.getValidFrom()).isEqualTo(original.getValidFrom());
            assertThat(vuelta.getValidUntil()).isEqualTo(original.getValidUntil());
            assertThat(vuelta.getRecalculatedAt()).isEqualTo(original.getRecalculatedAt());
            assertThat(vuelta.getCreatedDate()).isEqualTo(original.getCreatedDate());
        }

        @Test
        @DisplayName("una ventana con fin sobrevive: perderla convertiria el permiso en perpetuo")
        void la_ventana_con_fin_sobrevive_a_la_ida_y_vuelta() {
            CompanyEntitlement conFin = permisoCompleto();

            CompanyEntitlement vuelta = entitlementMapper.toDomain(entitlementMapper.toJpa(conFin,
                    empresa(), submoduloJpa(EntitlementMother.historiaClinica())));

            // Si el mapeo se dejara validUntil, esto seria null y el permiso dejaria de
            // caducar solo: el tenant seguiria entrando a un modulo que ya no paga.
            assertThat(vuelta.getValidUntil()).isNotNull();
            assertThat(vuelta.isActiveAt(AHORA)).isTrue();
            assertThat(vuelta.isActiveAt(AHORA.plusDays(31))).isFalse();
        }

        @Test
        @DisplayName("el camino de escritura reusa la referencia y no toca el proxy de empresa")
        void el_camino_de_escritura_reusa_la_referencia() {
            CompanyEntitlementJpaEntity fila = entitlementMapper.toJpa(permisoCompleto(), empresa(),
                    submoduloJpa(EntitlementMother.historiaClinica()));
            // Se le quita la empresa a la fila a proposito: la sobrecarga de escritura no
            // debe leerla. Si algun dia la lee, esto revienta con NPE en vez de disparar
            // una consulta silenciosa por cada permiso reinsertado.
            fila.setCompany(null);

            CompanyEntitlement vuelta = entitlementMapper.toDomain(fila, COMPANY_ID,
                    EntitlementMother.historiaClinica());

            assertThat(vuelta.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(vuelta.getSubModule()).isEqualTo(EntitlementMother.historiaClinica());
        }

        @ParameterizedTest
        @EnumSource(AccessLevel.class)
        @DisplayName("los tres niveles de acceso sobreviven a la ida y vuelta")
        void los_tres_niveles_de_acceso_sobreviven(AccessLevel nivel) {
            CompanyEntitlement permiso = new CompanyEntitlement(1L, COMPANY_ID,
                    EntitlementMother.facturacion(), nivel, EntitlementSource.SUBSCRIPTION,
                    EntitlementMother.SUBSCRIPTION_ID, 901L, AHORA.minusDays(1), null, AHORA,
                    AHORA);

            CompanyEntitlement vuelta = entitlementMapper.toDomain(entitlementMapper.toJpa(permiso,
                    empresa(), submoduloJpa(EntitlementMother.facturacion())));

            assertThat(vuelta.getAccessLevel()).isEqualTo(nivel);
        }

        @ParameterizedTest
        @EnumSource(EntitlementSource.class)
        @DisplayName("los cuatro origenes sobreviven: el MANUAL_GRANT no puede volver derivado")
        void los_origenes_sobreviven(EntitlementSource origen) {
            CompanyEntitlement permiso = new CompanyEntitlement(2L, COMPANY_ID,
                    EntitlementMother.facturacion(), AccessLevel.FULL, origen,
                    EntitlementMother.SUBSCRIPTION_ID, 902L, AHORA.minusDays(1), null, AHORA,
                    AHORA);

            CompanyEntitlement vuelta = entitlementMapper.toDomain(entitlementMapper.toJpa(permiso,
                    empresa(), submoduloJpa(EntitlementMother.facturacion())));

            // El origen es lo unico que decide si el proximo recalculo borra la fila.
            // Un MANUAL_GRANT que vuelve como SUBSCRIPTION desaparece en el siguiente
            // cambio de contrato, y quien lo concedio no tiene forma de relacionarlo.
            assertThat(vuelta.getSource()).isEqualTo(origen);
        }
    }

    @Nested
    @DisplayName("contadores de capacidad")
    class Contadores {

        /**
         * La fecha en que nacio el eje. No vive en la fila del contador --que solo
         * copia el id y el tipo de medida-- sino en {@code limit_dimensions}, asi que
         * la pone quien lee. Es lo que permite responder a D-74 sin volver a consultar.
         */
        private static final java.time.LocalDate NACIMIENTO = AHORA.toLocalDate().minusYears(1);

        /**
         * El mapper es de una sola direccion. No hay {@code toJpa} que probar: el techo
         * se escribe con la sentencia de {@code upsertCeiling}, que no nombra la
         * columna del consumo (#648). Ofrecer una conversion de vuelta seria ofrecer
         * justo el camino que hay que evitar, asi que estas pruebas construyen la fila
         * a mano --misma package, constructor accesible-- y afirman sobre la lectura.
         */
        private CompanyCapacityJpaEntity fila(String measureKind, String periodKey, int techo,
                int usado, LocalDateTime selloDelConsumo) {
            CompanyCapacityJpaEntity entity = new CompanyCapacityJpaEntity();
            entity.setId(55L);
            entity.setLimitDimensionId(41L);
            entity.setMeasureKind(measureKind);
            entity.setPeriodKey(periodKey);
            entity.setLimitQuantity(techo);
            entity.setUsedQuantity(usado);
            entity.setSubscriptionId(EntitlementMother.SUBSCRIPTION_ID);
            entity.setLimitRecalculatedAt(AHORA.minusDays(2));
            entity.setUsageReconciledAt(selloDelConsumo);
            entity.setCreatedDate(AHORA.minusDays(90));
            return entity;
        }

        @Test
        @DisplayName("la lectura no pierde ningun campo del contador")
        void la_lectura_no_pierde_ningun_campo() {
            CompanyCapacity contador = capacityMapper.toDomain(
                    fila("STOCK", PeriodKey.SENTINEL, 3, 5, null), COMPANY_ID, "USER", NACIMIENTO);

            assertThat(contador.getId()).isEqualTo(55L);
            assertThat(contador.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(contador.getDimension().id()).isEqualTo(41L);
            assertThat(contador.getDimension().code()).isEqualTo("USER");
            assertThat(contador.getDimension().measureKind()).isEqualTo(MeasureKind.STOCK);
            assertThat(contador.getPeriodKey().value()).isEqualTo(PeriodKey.SENTINEL);
            assertThat(contador.getLimitQuantity()).isEqualTo(3);
            assertThat(contador.getUsedQuantity()).isEqualTo(5);
            assertThat(contador.getSubscriptionId()).isEqualTo(EntitlementMother.SUBSCRIPTION_ID);
            assertThat(contador.getLimitRecalculatedAt()).isEqualTo(AHORA.minusDays(2));
            assertThat(contador.getCreatedDate()).isEqualTo(AHORA.minusDays(90));
        }

        @Test
        @DisplayName("el consumo por encima del techo sobrevive: es un estado admitido")
        void el_consumo_por_encima_del_techo_sobrevive() {
            CompanyCapacity contador = capacityMapper.toDomain(
                    fila("STOCK", PeriodKey.SENTINEL, 3, 5, null), COMPANY_ID, "USER", NACIMIENTO);

            // "5 usuarios con un techo de 3" es el estado que el modelo admite a
            // proposito tras una bajada de plan (R-LIMIT-38). Un mapeo que recortara el
            // consumo al techo le regalaria dos usuarios gratis a la empresa.
            assertThat(contador.getUsedQuantity()).isEqualTo(5);
            assertThat(contador.getLimitQuantity()).isEqualTo(3);
            assertThat(contador.isExhausted()).isTrue();
        }

        /**
         * R-ENT-13. Los dos sellos llegan separados y el del consumo puede venir nulo
         * --nadie lo ha comprobado nunca-- sin que eso contamine al del techo.
         */
        @Test
        @DisplayName("los dos sellos viajan separados y el del consumo admite no existir")
        void los_dos_sellos_viajan_separados() {
            CompanyCapacity sinRecuento = capacityMapper.toDomain(
                    fila("STOCK", PeriodKey.SENTINEL, 3, 1, null), COMPANY_ID, "USER", NACIMIENTO);
            CompanyCapacity conRecuento = capacityMapper.toDomain(
                    fila("STOCK", PeriodKey.SENTINEL, 3, 1, AHORA.minusDays(9)), COMPANY_ID, "USER",
                    NACIMIENTO);

            assertThat(sinRecuento.getUsageReconciledAt()).isNull();
            assertThat(sinRecuento.getLimitRecalculatedAt()).isEqualTo(AHORA.minusDays(2));
            assertThat(conRecuento.getUsageReconciledAt()).isEqualTo(AHORA.minusDays(9));
        }

        /**
         * R-LIMIT-05 en la lectura: un contador de flujo trae su periodo real y el
         * dominio lo acepta; el mismo periodo sobre un eje que no es de flujo lo
         * rechaza. Las dos direcciones, porque una sola dejaria pasar la mitad.
         */
        @Test
        @DisplayName("un contador de flujo conserva su clave de periodo")
        void un_contador_de_flujo_conserva_su_clave_de_periodo() {
            CompanyCapacity contador = capacityMapper.toDomain(
                    fila("FLOW", "2026-03", 200, 12, null), COMPANY_ID, "APPOINTMENT", NACIMIENTO);

            assertThat(contador.getPeriodKey().value()).isEqualTo("2026-03");
            assertThat(contador.getPeriodKey().isRealPeriod()).isTrue();
        }

        @Test
        @DisplayName("una fila que no es de flujo con periodo real se rechaza al leerla")
        void una_fila_no_de_flujo_con_periodo_real_se_rechaza() {
            CompanyCapacityJpaEntity corrupta = fila("STOCK", "2026-03", 3, 1, null);

            assertThatThrownBy(
                    () -> capacityMapper.toDomain(corrupta, COMPANY_ID, "USER", NACIMIENTO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must carry the sentinel period key");
        }

        @ParameterizedTest
        @EnumSource(MeasureKind.class)
        @DisplayName("los tres tipos de medida sobreviven a la lectura")
        void los_tres_tipos_de_medida_sobreviven(MeasureKind medida) {
            String periodo = medida.requiresPeriodKey() ? "2026-03" : PeriodKey.SENTINEL;

            CompanyCapacity contador = capacityMapper.toDomain(
                    fila(medida.name(), periodo, 7, 0, null), COMPANY_ID, "X", NACIMIENTO);

            // @EnumSource y no tres tests: un tipo de medida nuevo entra solo en la
            // matriz y falla aqui si el mapeo por nombre deja de cuadrar.
            assertThat(contador.getDimension().measureKind()).isEqualTo(medida);
            assertThat(contador.getLimitQuantity()).isEqualTo(7);
        }
    }
}
