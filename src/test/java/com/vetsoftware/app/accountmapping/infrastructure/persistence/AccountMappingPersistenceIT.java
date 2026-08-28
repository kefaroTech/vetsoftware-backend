package com.vetsoftware.app.accountmapping.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountmapping.domain.AccountMapping;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaAccountMappingRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar son las cuatro columnas GENERATED
 * STORED del changeset 343</b>, porque son invisibles desde Java: no estan
 * mapeadas, no aparecen en ningun getter y ningun test de dominio puede
 * tocarlas. Y sin embargo son la correccion entera del modelo.
 *
 * <ul>
 * <li>{@link Unicidad#dos_mapeos_sin_articulo_del_mismo_supuesto_si_chocan()}
 * demuestra que los tres centinelas funcionan: dos {@code VAT_PAYABLE} con
 * articulo, tipo de cargo y tratamiento vacios <b>chocan de verdad</b>. Con
 * {@code NULL} la base habria admitido los dos —en SQL dos {@code NULL} no son
 * iguales— y la unicidad del documento maestro no habria restringido nada para
 * <b>nueve de las doce clases</b>.</li>
 * <li>{@link Unicidad#dos_mapeos_vigentes_del_mismo_supuesto_no_caben()}
 * demuestra la otra mitad: {@code current_mapping_marker} impide que la
 * consulta de resolucion devuelva dos cuentas y el asiento tome la primera que
 * llegue.</li>
 * </ul>
 *
 * <p>
 * <b>El seed no trae plan de cuentas.</b> Las tres claves foraneas apuntan a
 * {@code accounting_accounts(code)}, asi que la raiz y las dos subcuentas
 * asentables se insertan aqui por SQL nativo, con ids del rango <b>8410</b> que
 * ninguna otra rodaja usa.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAccountMappingRepository — el puente concepto/cuenta contra MySQL real")
class AccountMappingPersistenceIT extends AbstractDataJpaTest {

    private static final Long RAIZ_ID = 8410L;
    private static final Long DEBITO_ID = 8411L;
    private static final Long CREDITO_ID = 8412L;
    private static final Long MAPEO_CRUDO = 8413L;

    private static final String DEBITO = "13050501";
    private static final String CREDITO = "24080501";

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate EL_DIA_DEL_ASIENTO = LocalDate.of(2026, 6, 15);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 1, 1, 8, 0, 0);

    @Autowired
    private AccountMappingJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaAccountMappingRepository repository;

    @BeforeEach
    void adaptador() {
        cuenta(RAIZ_ID, "1", null, 1, false);
        cuenta(DEBITO_ID, DEBITO, "1", 6, true);
        cuenta(CREDITO_ID, CREDITO, "1", 6, true);
        entityManager.flush();
        repository = new JpaAccountMappingRepository(springDataRepository,
                new AccountMappingJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el mapeo de IVA y lo recupera con cada campo en su sitio")
        void guarda_el_mapeo_de_iva_y_lo_recupera_campo_a_campo() {
            AccountMapping guardado = repository.save(ivaGenerado("19", null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getMappingKind()).isEqualTo(MappingKind.VAT_PAYABLE);
                assertThat(recuperado.getMappingKey()).isEqualTo("19");
                assertThat(recuperado.getCatalogItemId()).isNull();
                assertThat(recuperado.getChargeType()).isNull();
                assertThat(recuperado.getTaxTreatment()).isNull();
                assertThat(recuperado.getDebitAccountCode()).isEqualTo(DEBITO);
                assertThat(recuperado.getCreditAccountCode()).isEqualTo(CREDITO);
                assertThat(recuperado.getDeferredAccountCode()).isNull();
                assertThat(recuperado.getValidFrom()).isEqualTo(DESDE);
                assertThat(recuperado.getValidTo()).isNull();
                assertThat(recuperado.isEnabled()).isTrue();
                assertThat(recuperado.getVersion()).isNotNull();
            });
        }

        @Test
        @DisplayName("cerrar la vigencia mueve la version: es una edicion, no un insert")
        void cerrar_la_vigencia_mueve_la_version() {
            AccountMapping guardado = repository.save(ivaGenerado("19", null));
            entityManager.flush();
            entityManager.clear();

            AccountMapping cargado = repository.findById(guardado.getId()).orElseThrow();
            repository.save(cargado.close(LocalDate.of(2027, 1, 1)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(cerrado -> {
                assertThat(cerrado.getValidTo()).isEqualTo(LocalDate.of(2027, 1, 1));
                assertThat(cerrado.getVersion()).isEqualTo(1L);
            });
        }
    }

    @Nested
    @DisplayName("Resolucion del mapeo vigente")
    class Resolucion {

        @Test
        @DisplayName("el mapeo sin articulo SE ENCUENTRA, que es lo que el centinela arregla")
        void el_mapeo_sin_articulo_se_encuentra() {
            // EL caso de la feature. Si la consulta comparara contra NULL en vez de
            // contra los centinelas, esta busqueda devolveria cero filas para nueve de
            // las doce clases: el asiento no se generaria y no habria ningun error.
            repository.save(ivaGenerado("19", null));
            entityManager.flush();
            entityManager.clear();

            Optional<AccountMapping> vigente = repository.findEffective(MappingKind.VAT_PAYABLE,
                    "19", null, null, null, EL_DIA_DEL_ASIENTO);

            assertThat(vigente).get()
                    .satisfies(m -> assertThat(m.getDebitAccountCode()).isEqualTo(DEBITO));
        }

        @Test
        @DisplayName("el limite superior es estricto: el dia del cierre ya no aplica")
        void el_limite_superior_es_estricto() {
            repository.save(cerrado("19", EL_DIA_DEL_ASIENTO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findEffective(MappingKind.VAT_PAYABLE, "19", null, null, null,
                    EL_DIA_DEL_ASIENTO)).isEmpty();
            assertThat(repository.findEffective(MappingKind.VAT_PAYABLE, "19", null, null, null,
                    EL_DIA_DEL_ASIENTO.minusDays(1))).isPresent();
        }

        @Test
        @DisplayName("otra subclave no responde por la primera")
        void otra_subclave_no_responde_por_la_primera() {
            repository.save(ivaGenerado("19", null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findEffective(MappingKind.VAT_PAYABLE, "5", null, null, null,
                    EL_DIA_DEL_ASIENTO)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Unicidad")
    class Unicidad {

        @Test
        @DisplayName("dos mapeos sin articulo del mismo supuesto SI chocan, gracias al centinela")
        void dos_mapeos_sin_articulo_del_mismo_supuesto_si_chocan() {
            repository.save(ivaGenerado("19", null));
            entityManager.flush();

            // Con NULL en las tres columnas de afinado, la base habria admitido los dos:
            // en un indice unico dos NULL no chocan. Los centinelas 0 y '-' son lo que
            // hace que uq_account_mappings_case restrinja de verdad.
            EngineConstraint.assertViolates("uq_account_mappings", () -> {
                repository.save(ivaGenerado("19", null));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("dos vigencias abiertas del mismo supuesto no caben")
        void dos_mapeos_vigentes_del_mismo_supuesto_no_caben() {
            repository.save(ivaGenerado("19", null));
            entityManager.flush();

            // Distinta fecha de inicio, asi que uq_account_mappings_case no los ve
            // iguales; lo que los para es current_mapping_marker.
            EngineConstraint.assertViolates("uq_account_mappings_current", () -> {
                repository.save(desde("19", LocalDate.of(2026, 7, 1)));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("cerrar el vigente libera el hueco para su relevo")
        void cerrar_el_vigente_libera_el_hueco() {
            AccountMapping vigente = repository.save(ivaGenerado("19", null));
            entityManager.flush();
            entityManager.clear();

            AccountMapping cargado = repository.findById(vigente.getId()).orElseThrow();
            repository.save(cargado.close(LocalDate.of(2026, 7, 1)));
            entityManager.flush();

            AccountMapping relevo = repository.save(desde("19", LocalDate.of(2026, 7, 1)));
            entityManager.flush();

            assertThat(relevo.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("un afinado sobre una clase que no lo admite lo para chk_..._refine")
        void un_afinado_sobre_una_clase_que_no_lo_admite_lo_para_el_check() {
            // El impuesto generado no viene de algo vendido: no tiene articulo al que
            // apuntar. El dominio ya lo rechaza; esto comprueba el cinturon de debajo.
            EngineConstraint.assertViolates("chk_account_mappings_refine",
                    () -> insertarCrudo(MAPEO_CRUDO, "VAT_PAYABLE", "19", "RECURRING"));
        }

        @Test
        @DisplayName("una subclave vacia la para chk_account_mappings_key")
        void una_subclave_vacia_la_para_el_check_de_la_clave() {
            // La cadena vacia es peor que el nulo: entraria en la unicidad como un valor
            // mas y abriria un segundo mapeo indistinguible del que usa el centinela.
            EngineConstraint.assertViolates("chk_account_mappings_key",
                    () -> insertarCrudo(MAPEO_CRUDO + 1, "VAT_PAYABLE", "", null));
        }

        @Test
        @DisplayName("una clase de mapeo desconocida la para chk_account_mappings_kind")
        void una_clase_desconocida_la_para_el_check_de_clase() {
            EngineConstraint.assertViolates("chk_account_mappings_kind",
                    () -> insertarCrudo(MAPEO_CRUDO + 2, "TAX_OUTPUT", "19", null));
        }
    }

    private static AccountMapping ivaGenerado(String subclave, Long articulo) {
        return AccountMapping.create(MappingKind.VAT_PAYABLE, subclave, articulo, null, null,
                DEBITO, CREDITO, null, DESDE, null, CREADO_EL);
    }

    private static AccountMapping desde(String subclave, LocalDate validFrom) {
        return AccountMapping.create(MappingKind.VAT_PAYABLE, subclave, null, null, null, DEBITO,
                CREDITO, null, validFrom, null, CREADO_EL);
    }

    private static AccountMapping cerrado(String subclave, LocalDate hasta) {
        return AccountMapping.create(MappingKind.VAT_PAYABLE, subclave, null, null, null, DEBITO,
                CREDITO, null, DESDE, hasta, CREADO_EL);
    }

    private void cuenta(Long id, String codigo, String padre, int nivel, boolean asentable) {
        entityManager.createNativeQuery("""
                INSERT INTO accounting_accounts (id, code, name, account_class, parent_code,
                        account_level, postable, requires_third_party, valid_from, created_date,
                        enabled, version)
                VALUES (:id, :codigo, 'Cuenta de andamio', 'ASSET', :padre, :nivel, :asentable,
                        false, '2026-01-01', NOW(6), true, 0)
                """).setParameter("id", id).setParameter("codigo", codigo)
                .setParameter("padre", padre).setParameter("nivel", nivel)
                .setParameter("asentable", asentable).executeUpdate();
    }

    private void insertarCrudo(Long id, String clase, String subclave, String tipoDeCargo) {
        entityManager.createNativeQuery("""
                INSERT INTO account_mappings (id, mapping_kind, mapping_key, charge_type,
                        debit_account_code, credit_account_code, valid_from, created_date,
                        enabled, version)
                VALUES (:id, :clase, :subclave, :tipoDeCargo, :debito, :credito, '2026-01-01',
                        NOW(6), true, 0)
                """).setParameter("id", id).setParameter("clase", clase)
                .setParameter("subclave", subclave).setParameter("tipoDeCargo", tipoDeCargo)
                .setParameter("debito", DEBITO).setParameter("credito", CREDITO).executeUpdate();
        entityManager.flush();
    }
}
