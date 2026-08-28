package com.vetsoftware.app.customercredit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.customercredit.domain.CreditEntryKind;
import com.vetsoftware.app.customercredit.domain.CreditLot;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaCustomerCreditEntryRepository} contra MySQL real.
 *
 * <p>
 * <b>El orden de los lotes es la regla que este adaptador cuida y ninguna otra
 * capa vigila</b>: el consumo empieza por el lote que antes caduca. No es una
 * preferencia —es lo que evita que el saldo del cliente se evapore por un lote
 * que caduco mientras se gastaba otro que no caducaba nunca—, y vive en un
 * {@code Comparator} del adaptador, no en el SQL, asi que no hay indice ni
 * {@code CHECK} que la sostenga. Un lote sin fecha va el ultimo, y dos lotes
 * que caducan el mismo dia se desempatan por el id del asiento.
 *
 * <p>
 * La otra mitad es el <b>neteo</b>: el remanente de un lote es su abono mas los
 * consumos y caducidades que lo nombran —que van en negativo, de ahi que la
 * resta sea una suma—, y un lote agotado desaparece de la lista en vez de salir
 * con remanente cero. Como el neteo se hace en Java sobre lo que devuelven dos
 * consultas, el unico sitio donde se puede comprobar de verdad es aqui.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCustomerCreditEntryRepository — lotes de saldo a favor contra MySQL real")
class CustomerCreditEntryPersistenceIT extends AbstractDataJpaTest {

    /** Documento de cobro al que se aplica el saldo. Id propio de este archivo. */
    private static final Long DOCUMENTO = 8300L;
    private static final Long OTRO_DOCUMENTO = 8301L;

    /**
     * Tercer documento, y no es relleno: {@code origin_marker} es
     * {@code company|entry_kind|origin_kind|documento|lote}, asi que dos consumos
     * del MISMO lote contra el MISMO documento colisionan en {@code uq_cce_origin}.
     * El senuelo de prefijo parecido necesita documento propio para no morir por
     * esa constraint antes de probar lo que prueba.
     */
    private static final Long TERCER_DOCUMENTO = 8302L;

    private static final LocalDateTime OCURRIO_EL = LocalDateTime.of(2026, 2, 14, 11, 22, 33);
    private static final LocalDate FECHA_VALOR = LocalDate.of(2026, 2, 20);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 2, 15, 6, 5, 4);

    @Autowired
    private JpaCustomerCreditEntryRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        // Periodos DISTINTOS a proposito. uq_sbd_recurring_cycle es UNIQUE sobre
        // (recurring_cycle_marker, period_start, period_end), y el marcador vale el
        // id del contrato en toda factura recurrente: dos facturas del mismo
        // contrato y el mismo periodo colisionan aunque lleven numero distinto.
        documento(DOCUMENTO, "FV-CREDITO-0001", "2026-02-01", "2026-02-28");
        documento(OTRO_DOCUMENTO, "FV-CREDITO-0002", "2026-03-01", "2026-03-31");
        documento(TERCER_DOCUMENTO, "FV-CREDITO-0003", "2026-04-01", "2026-04-30");
        entityManager.flush();
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el abono y lo recupera con cada fecha, importe y origen en su sitio")
        void guarda_el_abono_y_lo_recupera_campo_a_campo() {
            CustomerCreditEntry guardado = repository.save(CustomerCreditEntry.grant(
                    SchemaSeed.COMPANY_ID, new BigDecimal("83450.75"), CreditOriginKind.CREDIT_NOTE,
                    null, DOCUMENTO, null, OCURRIO_EL, FECHA_VALOR, LocalDate.of(2026, 12, 31),
                    "abono-ida-y-vuelta", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> {
                        assertThat(recuperado.getEntryKind()).isEqualTo(CreditEntryKind.GRANT);
                        assertThat(recuperado.getAmount()).isEqualByComparingTo("83450.75");
                        assertThat(recuperado.getLotEntryId()).isNull();
                        assertThat(recuperado.getOriginKind())
                                .isEqualTo(CreditOriginKind.CREDIT_NOTE);
                        assertThat(recuperado.getOriginDocumentId()).isEqualTo(DOCUMENTO);
                        assertThat(recuperado.getOriginPaymentId()).isNull();
                        assertThat(recuperado.getOriginSubscriptionId()).isNull();
                        // Tres marcas temporales distintas: ocurrio, fecha valor y
                        // creacion. Cruzar dos cualesquiera rompe la asercion.
                        assertThat(recuperado.getOccurredAt()).isEqualTo(OCURRIO_EL);
                        assertThat(recuperado.getValueDate()).isEqualTo(FECHA_VALOR);
                        assertThat(recuperado.getCreatedDate()).isEqualTo(CREADO_EL);
                        assertThat(recuperado.getExpiresOn()).isEqualTo(LocalDate.of(2026, 12, 31));
                    });
        }

        @Test
        @DisplayName("el consumo se guarda en negativo y nombra el lote del que sale")
        void el_consumo_se_guarda_en_negativo_y_nombra_su_lote() {
            CustomerCreditEntry lote = abono(new BigDecimal("100000.00"), null, "lote-consumido");
            entityManager.flush();

            CustomerCreditEntry consumo = repository.save(CustomerCreditEntry.consumption(
                    SchemaSeed.COMPANY_ID, new BigDecimal("40000.00"), lote.getId(), DOCUMENTO,
                    OCURRIO_EL, FECHA_VALOR, "gasto#0", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(consumo.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperado -> {
                        assertThat(recuperado.getEntryKind())
                                .isEqualTo(CreditEntryKind.CONSUMPTION);
                        // El signo es la mitad del modelo: el remanente de un lote se
                        // calcula sumando, no restando.
                        assertThat(recuperado.getAmount()).isEqualByComparingTo("-40000.00");
                        assertThat(recuperado.delta()).isNegative();
                        assertThat(recuperado.getLotEntryId()).isEqualTo(lote.getId());
                        assertThat(recuperado.getOriginKind())
                                .isEqualTo(CreditOriginKind.APPLICATION);
                        assertThat(recuperado.getExpiresOn()).isNull();
                    });
        }
    }

    @Nested
    @DisplayName("Consumo por lotes")
    class ConsumoPorLotes {

        @Test
        @DisplayName("los lotes abiertos salen empezando por el que antes caduca")
        void los_lotes_abiertos_salen_por_caducidad_mas_cercana() {
            // Se crean a proposito en el orden INVERSO al esperado, para que un
            // adaptador que devolviera las filas tal como llegan de la base fallara.
            CustomerCreditEntry sinCaducidad = abono(new BigDecimal("11000.00"), null,
                    "lote-sin-caducidad");
            CustomerCreditEntry tardio = abono(new BigDecimal("22000.00"),
                    LocalDate.of(2026, 12, 31), "lote-tardio");
            CustomerCreditEntry proximo = abono(new BigDecimal("33000.00"),
                    LocalDate.of(2026, 6, 30), "lote-proximo");
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findOpenLotsByCompanyId(SchemaSeed.COMPANY_ID))
                    .extracting(CreditLot::entryId)
                    .containsExactly(proximo.getId(), tardio.getId(), sinCaducidad.getId());
        }

        @Test
        @DisplayName("dos lotes que caducan el mismo dia se desempatan por el id del asiento")
        void dos_lotes_del_mismo_dia_se_desempatan_por_id() {
            CustomerCreditEntry primero = abono(new BigDecimal("5000.00"),
                    LocalDate.of(2026, 6, 30), "empate-a");
            CustomerCreditEntry segundo = abono(new BigDecimal("6000.00"),
                    LocalDate.of(2026, 6, 30), "empate-b");
            entityManager.flush();
            entityManager.clear();

            // Sin desempate el motor puede devolverlos en cualquier orden y el reparto
            // dejaria de ser reproducible entre dos ejecuciones del mismo cobro.
            assertThat(repository.findOpenLotsByCompanyId(SchemaSeed.COMPANY_ID))
                    .extracting(CreditLot::entryId)
                    .containsExactly(primero.getId(), segundo.getId());
        }

        @Test
        @DisplayName("el remanente de un lote descuenta lo ya consumido y lo caducado")
        void el_remanente_descuenta_lo_consumido_y_lo_caducado() {
            CustomerCreditEntry lote = abono(new BigDecimal("100000.00"), LocalDate.of(2026, 6, 30),
                    "lote-neteado");
            entityManager.flush();
            repository.save(CustomerCreditEntry.consumption(SchemaSeed.COMPANY_ID,
                    new BigDecimal("30000.00"), lote.getId(), DOCUMENTO, OCURRIO_EL, FECHA_VALOR,
                    "gasto-a#0", CREADO_EL));
            repository.save(CustomerCreditEntry.expiration(SchemaSeed.COMPANY_ID,
                    new BigDecimal("25000.00"), lote.getId(), OCURRIO_EL, FECHA_VALOR,
                    "caducidad-a#0", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            // 100000 - 30000 - 25000 = 45000. Los tres numeros son distintos entre si
            // y ninguno es cero: confundir el consumo con la caducidad, o sumar en vez
            // de restar, da un resultado distinto de 45000.
            assertThat(repository.findOpenLotsByCompanyId(SchemaSeed.COMPANY_ID)).singleElement()
                    .satisfies(abierto -> {
                        assertThat(abierto.entryId()).isEqualTo(lote.getId());
                        assertThat(abierto.remaining()).isEqualByComparingTo("45000.00");
                    });
        }

        @Test
        @DisplayName("un lote agotado desaparece de los abiertos en vez de salir con cero")
        void un_lote_agotado_desaparece_de_los_abiertos() {
            CustomerCreditEntry agotado = abono(new BigDecimal("70000.00"), null, "lote-agotado");
            CustomerCreditEntry vivo = abono(new BigDecimal("15000.00"), null, "lote-vivo");
            entityManager.flush();
            repository.save(CustomerCreditEntry.consumption(SchemaSeed.COMPANY_ID,
                    new BigDecimal("70000.00"), agotado.getId(), DOCUMENTO, OCURRIO_EL, FECHA_VALOR,
                    "gasto-total#0", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findOpenLotsByCompanyId(SchemaSeed.COMPANY_ID))
                    .extracting(CreditLot::entryId).containsExactly(vivo.getId());
        }

        @Test
        @DisplayName("caducados son los anteriores al dia de corte, y el del mismo dia todavia no")
        void caducados_son_los_anteriores_al_dia_de_corte() {
            CustomerCreditEntry ayer = abono(new BigDecimal("1000.00"), LocalDate.of(2026, 6, 29),
                    "caduco-ayer");
            abono(new BigDecimal("2000.00"), LocalDate.of(2026, 6, 30), "caduca-hoy");
            abono(new BigDecimal("3000.00"), LocalDate.of(2026, 7, 1), "caduca-manana");
            abono(new BigDecimal("4000.00"), null, "no-caduca");
            entityManager.flush();
            entityManager.clear();

            // El lote que caduca EL dia de corte todavia sirve: la frontera es
            // estricta. Un isBefore convertido en isEqualOrBefore le quitaria al
            // cliente un saldo que aun tiene derecho a gastar.
            assertThat(repository.findExpiredLotsByCompanyId(SchemaSeed.COMPANY_ID,
                    LocalDate.of(2026, 6, 30))).extracting(CreditLot::entryId)
                    .containsExactly(ayer.getId());
        }

        @Test
        @DisplayName("los lotes de una empresa no se mezclan con los de otra")
        void los_lotes_no_se_mezclan_entre_empresas() {
            CustomerCreditEntry propio = abono(new BigDecimal("9000.00"), null, "lote-propio");
            repository.save(CustomerCreditEntry.grant(SchemaSeed.OTRA_COMPANY_ID,
                    new BigDecimal("777000.00"), CreditOriginKind.MANUAL, null, null, null,
                    OCURRIO_EL, FECHA_VALOR, null, "lote-ajeno", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            // El importe ajeno es deliberadamente enorme: si se colara, el remanente
            // no seria 9000 y la asercion caeria por dos sitios.
            assertThat(repository.findOpenLotsByCompanyId(SchemaSeed.COMPANY_ID)).singleElement()
                    .satisfies(abierto -> {
                        assertThat(abierto.entryId()).isEqualTo(propio.getId());
                        assertThat(abierto.remaining()).isEqualByComparingTo("9000.00");
                    });
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("los asientos de una misma operacion se recuperan por su prefijo y en orden")
        void los_asientos_de_una_operacion_se_recuperan_por_prefijo() {
            CustomerCreditEntry lote = abono(new BigDecimal("100000.00"), null, "lote-operacion");
            entityManager.flush();
            repository.save(CustomerCreditEntry.consumption(SchemaSeed.COMPANY_ID,
                    new BigDecimal("1000.00"), lote.getId(), DOCUMENTO, OCURRIO_EL, FECHA_VALOR,
                    "operacion-42#0", CREADO_EL));
            repository.save(CustomerCreditEntry.consumption(SchemaSeed.COMPANY_ID,
                    new BigDecimal("2000.00"), lote.getId(), OTRO_DOCUMENTO, OCURRIO_EL,
                    FECHA_VALOR, "operacion-42#1", CREADO_EL));
            // Una operacion distinta que empieza con los mismos caracteres: el
            // prefijo lleva el separador, asi que esta NO debe colarse.
            repository.save(CustomerCreditEntry.consumption(SchemaSeed.COMPANY_ID,
                    new BigDecimal("3000.00"), lote.getId(), TERCER_DOCUMENTO, OCURRIO_EL,
                    FECHA_VALOR, "operacion-420#0", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findOperation(SchemaSeed.COMPANY_ID, "operacion-42"))
                    .extracting(CustomerCreditEntry::getAmount)
                    .containsExactly(new BigDecimal("-1000.00"), new BigDecimal("-2000.00"));
        }

        @Test
        @DisplayName("la misma llave dos veces la para uq_cce_idempotency")
        void la_misma_llave_dos_veces_la_para_la_unicidad() {
            abono(new BigDecimal("1000.00"), null, "llave-repetida");
            entityManager.flush();

            EngineConstraint.assertViolates("uq_cce_idempotency", () -> {
                // Importe distinto: lo unico repetido es la llave.
                abono(new BigDecimal("2000.00"), null, "llave-repetida");
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("aplicar dos veces el mismo lote al mismo documento lo para uq_cce_origin")
        void aplicar_dos_veces_el_mismo_lote_al_mismo_documento_lo_para_el_marcador() {
            CustomerCreditEntry lote = abono(new BigDecimal("100000.00"), null, "lote-marcador");
            entityManager.flush();
            repository.save(CustomerCreditEntry.consumption(SchemaSeed.COMPANY_ID,
                    new BigDecimal("1000.00"), lote.getId(), DOCUMENTO, OCURRIO_EL, FECHA_VALOR,
                    "aplicacion-a#0", CREADO_EL));
            entityManager.flush();

            // Llaves de idempotencia DISTINTAS a proposito: si fueran iguales saltaria
            // uq_cce_idempotency y el caso pasaria por el motivo equivocado, dejando
            // sin red la columna generada origin_marker el dia que alguien la quite.
            EngineConstraint.assertViolates("uq_cce_origin", () -> {
                repository.save(CustomerCreditEntry.consumption(SchemaSeed.COMPANY_ID,
                        new BigDecimal("2000.00"), lote.getId(), DOCUMENTO, OCURRIO_EL, FECHA_VALOR,
                        "aplicacion-b#0", CREADO_EL));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("el mismo lote sobre documentos distintos si se puede aplicar")
        void el_mismo_lote_sobre_documentos_distintos_si_se_puede() {
            CustomerCreditEntry lote = abono(new BigDecimal("100000.00"), null, "lote-dos-docs");
            entityManager.flush();
            repository.save(CustomerCreditEntry.consumption(SchemaSeed.COMPANY_ID,
                    new BigDecimal("1000.00"), lote.getId(), DOCUMENTO, OCURRIO_EL, FECHA_VALOR,
                    "doc-uno#0", CREADO_EL));
            repository.save(CustomerCreditEntry.consumption(SchemaSeed.COMPANY_ID,
                    new BigDecimal("2000.00"), lote.getId(), OTRO_DOCUMENTO, OCURRIO_EL,
                    FECHA_VALOR, "doc-dos#0", CREADO_EL));
            entityManager.flush();
            entityManager.clear();

            // La otra cara de uq_cce_origin: el marcador incluye el documento, asi que
            // un unico marcador por lote habria bloqueado este reparto legitimo.
            assertThat(repository.findOpenLotsByCompanyId(SchemaSeed.COMPANY_ID)).singleElement()
                    .satisfies(abierto -> assertThat(abierto.remaining())
                            .isEqualByComparingTo("97000.00"));
        }
    }

    @Nested
    @DisplayName("Tenancy y listados")
    class TenancyYListados {

        @Test
        @DisplayName("la carga por id no cruza de empresa")
        void la_carga_por_id_no_cruza_de_empresa() {
            CustomerCreditEntry propio = abono(new BigDecimal("1234.00"), null, "tenancy-carga");
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(propio.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(repository.findByIdAndCompanyId(propio.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("el barrido de lo que caduca trae solo los abonos anteriores al corte")
        void el_barrido_de_lo_que_caduca_trae_solo_los_anteriores_al_corte() {
            CustomerCreditEntry pronto = abono(new BigDecimal("1000.00"), LocalDate.of(2026, 5, 1),
                    "expira-pronto");
            abono(new BigDecimal("2000.00"), LocalDate.of(2027, 1, 1), "expira-tarde");
            abono(new BigDecimal("3000.00"), null, "no-expira");
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllExpiringBefore(LocalDate.of(2026, 6, 1), 0, 20).content())
                    .extracting(CustomerCreditEntry::getId).containsExactly(pronto.getId());
        }
    }

    // --- andamio ------------------------------------------------------------

    /** Abono {@code MANUAL}: sin documento de origen, asi que no arrastra FK. */
    private CustomerCreditEntry abono(BigDecimal importe, LocalDate caduca, String llave) {
        return repository.save(
                CustomerCreditEntry.grant(SchemaSeed.COMPANY_ID, importe, CreditOriginKind.MANUAL,
                        null, null, null, OCURRIO_EL, FECHA_VALOR, caduca, llave, CREADO_EL));
    }

    private void documento(Long id, String numero, String inicio, String fin) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_billing_documents (id, document_number, company_id,
                                                            subscription_id, document_kind,
                                                            billing_reason, period_start,
                                                            period_end, issue_status,
                                                            subtotal_amount, tax_amount,
                                                            total_amount, settled_amount,
                                                            created_date, version)
                VALUES (:id, :numero, :companyId, :subscriptionId, 'INVOICE', 'RECURRING_CYCLE',
                        :inicio, :fin, 'DRAFT', 100000.00, 19000.00, 119000.00,
                        0.00, NOW(), 0)
                """).setParameter("id", id).setParameter("numero", numero)
                .setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("subscriptionId", SchemaSeed.SUBSCRIPTION_ID)
                .setParameter("inicio", inicio).setParameter("fin", fin).executeUpdate();
    }
}
