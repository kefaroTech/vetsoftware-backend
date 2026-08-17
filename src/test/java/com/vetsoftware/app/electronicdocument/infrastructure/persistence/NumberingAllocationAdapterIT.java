package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.electronicdocument.application.port.out.NumberingAllocationPort.AllocatedNumber;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja del consecutivo fiscal contra MySQL real. Es el adaptador mas delicado
 * del proyecto: el numero que entrega va impreso en una factura y la DIAN no
 * perdona ni repeticiones ni saltos.
 *
 * <p>
 * <b>Por que un doble no sirve, punto por punto.</b>
 *
 * <ul>
 * <li>La atomicidad la da un {@code SELECT … LIMIT 1 FOR UPDATE} escrito en SQL
 * <b>nativo</b> —{@code FOR UPDATE} no se expresa en JPQL—. Un doble devuelve
 * el numero siguiente sin bloquear nada, asi que la propiedad que de verdad
 * importa (dos emisiones no se llevan el mismo consecutivo) queda sin
 * comprobar.</li>
 * <li>La consulta filtra {@code enabled = true} <b>a mano</b> porque la
 * {@code @SQLRestriction} de la entidad NO se aplica a las nativas. Si alguien
 * borra ese filtro creyendolo redundante, una resolucion dada de baja vuelve a
 * numerar facturas. Solo la base lo detecta.</li>
 * <li>El fallback sede→empresa es un {@code ORDER BY (branch_id IS NULL), id}:
 * una expresion del motor, no codigo Java.</li>
 * <li>El scope por empresa del contador vive en el {@code WHERE}. Si se cayera,
 * una veterinaria consumiria el rango autorizado de otra.</li>
 * </ul>
 *
 * <p>
 * Las fechas de vigencia se siembran con un rango fijo y ancho porque el
 * adaptador compara contra {@code LocalDate.now()} (deuda registrada: el codigo
 * nuevo inyecta {@code Clock}); asi el caso no depende del dia en que corra.
 */
@Import(JpaNumberingAllocationPort.class)
@DisplayName("JpaNumberingAllocationPort — el consecutivo fiscal contra MySQL real")
class NumberingAllocationAdapterIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;

    private static final ElectronicDocumentType FE = ElectronicDocumentType.FE_VENTA;

    /** Rango de vigencia que contiene cualquier "hoy" razonable. */
    private static final LocalDate VIGENTE_DESDE = LocalDate.of(2020, 1, 1);
    private static final LocalDate VIGENTE_HASTA = LocalDate.of(2099, 12, 31);

    @Autowired
    private JpaNumberingAllocationPort port;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    /**
     * Siembra por SQL nativo: la entidad de {@code numberingresolution} tiene el
     * constructor protegido y su agregado de dominio rechaza los estados limite que
     * aqui hay que provocar (rango agotado, fuera de vigencia).
     */
    private void resolucion(Long companyId, Long branchId, String documentType,
            String resolutionNumber, String prefijo, long desde, long hasta, long actual,
            LocalDate validoDesde, LocalDate validoHasta, boolean activa) {
        // Valores en linea (como SchemaSeed) y no parametros: branch_id nulo como
        // parametro de una consulta nativa obliga a tipar el null a mano.
        entityManager.createNativeQuery("""
                INSERT INTO numbering_resolutions (company_id, branch_id, document_type,
                        resolution_number, resolution_date, prefix, range_from, range_to,
                        valid_from, valid_to, technical_key, current_number, created_date, enabled)
                VALUES (%d, %s, '%s', '%s', '2026-01-01', '%s', %d, %d, '%s', '%s',
                        'clave-tecnica', %d, '2026-01-01 08:00:00', %b)
                """.formatted(companyId, branchId == null ? "NULL" : branchId.toString(),
                documentType, resolutionNumber, prefijo, desde, hasta, validoDesde, validoHasta,
                actual, activa)).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    /** Resolucion de EMPRESA (todas las sedes), vigente, con rango 100..199. */
    private void resolucionDeEmpresa() {
        resolucion(COMPANY, null, "FE_VENTA", "18760000001", "SETP", 100L, 199L, 100L,
                VIGENTE_DESDE, VIGENTE_HASTA, true);
    }

    private Long consecutivoEnBase(String resolutionNumber) {
        Number valor = (Number) entityManager.createNativeQuery(
                "SELECT current_number FROM numbering_resolutions WHERE resolution_number = ?")
                .setParameter(1, resolutionNumber).getSingleResult();
        return valor.longValue();
    }

    @Nested
    @DisplayName("asignacion del consecutivo")
    class Asignacion {

        @Test
        @DisplayName("entrega el consecutivo actual con su resolucion y prefijo")
        void entrega_el_consecutivo_actual_con_resolucion_y_prefijo() {
            resolucionDeEmpresa();

            AllocatedNumber asignado = port.allocate(COMPANY, BRANCH, FE).orElseThrow();

            assertThat(asignado.consecutive()).isEqualTo(100L);
            assertThat(asignado.resolutionNumber()).isEqualTo("18760000001");
            assertThat(asignado.prefix()).isEqualTo("SETP");
        }

        @Test
        @DisplayName("dos asignaciones consecutivas NUNCA devuelven el mismo numero")
        void dos_asignaciones_consecutivas_no_repiten_numero() {
            resolucionDeEmpresa();

            AllocatedNumber primera = port.allocate(COMPANY, BRANCH, FE).orElseThrow();
            AllocatedNumber segunda = port.allocate(COMPANY, BRANCH, FE).orElseThrow();

            // Es LA propiedad del adaptador. Dos facturas con el mismo consecutivo son
            // dos documentos fiscales identicos ante la DIAN.
            assertThat(primera.consecutive()).isEqualTo(100L);
            assertThat(segunda.consecutive()).isEqualTo(101L);
            assertThat(segunda.consecutive()).isNotEqualTo(primera.consecutive());
        }

        @Test
        @DisplayName("el incremento queda persistido, no solo en memoria")
        void el_incremento_queda_persistido() {
            resolucionDeEmpresa();

            port.allocate(COMPANY, BRANCH, FE);
            port.allocate(COMPANY, BRANCH, FE);

            // Si el save no cuajara, al reiniciar el proceso se volveria a entregar el
            // 100 y todos los numeros anteriores se repetirian.
            assertThat(consecutivoEnBase("18760000001")).isEqualTo(102L);
        }

        @Test
        @DisplayName("sin resolucion para el tipo no asigna nada y no falla")
        void sin_resolucion_para_el_tipo_no_asigna_nada() {
            resolucionDeEmpresa();

            // Hay resolucion de FE_VENTA, no de nota credito: el emisor debe poder
            // distinguir "no configurado" de "error".
            assertThat(port.allocate(COMPANY, BRANCH, ElectronicDocumentType.NOTA_CREDITO))
                    .isEmpty();
        }

        @Test
        @DisplayName("una resolucion DESACTIVADA no numera facturas")
        void una_resolucion_desactivada_no_numera() {
            resolucion(COMPANY, null, "FE_VENTA", "18760000009", "OFF", 100L, 199L, 100L,
                    VIGENTE_DESDE, VIGENTE_HASTA, false);

            // La @SQLRestriction de la entidad NO alcanza a las consultas nativas: lo
            // unico que deja fuera a una resolucion dada de baja es el enabled = true
            // escrito a mano en el WHERE.
            assertThat(port.allocate(COMPANY, BRANCH, FE)).isEmpty();
            assertThat(consecutivoEnBase("18760000009")).isEqualTo(100L);
        }

        @Test
        @DisplayName("no toca la resolucion de otra empresa")
        void no_toca_la_resolucion_de_otra_empresa() {
            resolucionDeEmpresa();

            assertThat(port.allocate(OTRA_COMPANY, BRANCH, FE)).isEmpty();
            // Y el contador ajeno sigue intacto: nadie consumio rango de otro tenant.
            assertThat(consecutivoEnBase("18760000001")).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("multi-sucursal: la sede manda sobre la empresa")
    class Sedes {

        @Test
        @DisplayName("con resolucion propia de la sede se usa esa, no la de empresa")
        void con_resolucion_propia_de_la_sede_se_usa_esa() {
            resolucionDeEmpresa();
            resolucion(COMPANY, BRANCH, "FE_VENTA", "18760000002", "SEDE", 500L, 599L, 500L,
                    VIGENTE_DESDE, VIGENTE_HASTA, true);

            AllocatedNumber asignado = port.allocate(COMPANY, BRANCH, FE).orElseThrow();

            // La prioridad la impone un ORDER BY (branch_id IS NULL) del motor. Si se
            // invirtiera, la sede facturaria con el prefijo de otra y la DIAN rechazaria.
            assertThat(asignado.prefix()).isEqualTo("SEDE");
            assertThat(asignado.consecutive()).isEqualTo(500L);
            assertThat(consecutivoEnBase("18760000001")).as("la de empresa no se toco")
                    .isEqualTo(100L);
        }

        @Test
        @DisplayName("una sede sin resolucion propia cae a la de empresa")
        void una_sede_sin_resolucion_propia_cae_a_la_de_empresa() {
            resolucionDeEmpresa();
            resolucion(COMPANY, BRANCH, "FE_VENTA", "18760000002", "SEDE", 500L, 599L, 500L,
                    VIGENTE_DESDE, VIGENTE_HASTA, true);

            AllocatedNumber asignado = port.allocate(COMPANY, OTRA_BRANCH, FE).orElseThrow();

            assertThat(asignado.prefix()).isEqualTo("SETP");
            assertThat(asignado.consecutive()).isEqualTo(100L);
        }

        @Test
        @DisplayName("las dos sedes avanzan su propio contador sin pisarse")
        void las_dos_sedes_avanzan_su_propio_contador() {
            resolucion(COMPANY, BRANCH, "FE_VENTA", "18760000003", "SEDA", 500L, 599L, 500L,
                    VIGENTE_DESDE, VIGENTE_HASTA, true);
            resolucion(COMPANY, OTRA_BRANCH, "FE_VENTA", "18760000004", "SEDB", 800L, 899L, 800L,
                    VIGENTE_DESDE, VIGENTE_HASTA, true);

            port.allocate(COMPANY, BRANCH, FE);
            port.allocate(COMPANY, BRANCH, FE);
            AllocatedNumber norte = port.allocate(COMPANY, OTRA_BRANCH, FE).orElseThrow();

            assertThat(norte.consecutive()).isEqualTo(800L);
            assertThat(consecutivoEnBase("18760000003")).isEqualTo(502L);
            assertThat(consecutivoEnBase("18760000004")).isEqualTo(801L);
        }
    }

    @Nested
    @DisplayName("guardas de vigencia y rango")
    class Guardas {

        @Test
        @DisplayName("una resolucion caducada no numera: falla en vez de emitir")
        void una_resolucion_caducada_no_numera() {
            resolucion(COMPANY, null, "FE_VENTA", "18760000005", "OLD", 100L, 199L, 100L,
                    LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), true);

            assertThatThrownBy(() -> port.allocate(COMPANY, BRANCH, FE))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("vigente");
            assertThat(consecutivoEnBase("18760000005")).isEqualTo(100L);
        }

        @Test
        @DisplayName("una resolucion que aun no empieza tampoco numera")
        void una_resolucion_que_aun_no_empieza_tampoco_numera() {
            resolucion(COMPANY, null, "FE_VENTA", "18760000006", "FUT", 100L, 199L, 100L,
                    LocalDate.of(2099, 1, 1), LocalDate.of(2099, 12, 31), true);

            assertThatThrownBy(() -> port.allocate(COMPANY, BRANCH, FE))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("vigente");
        }

        @Test
        @DisplayName("al pasarse del rango autorizado deja de numerar")
        void al_pasarse_del_rango_autorizado_deja_de_numerar() {
            // Rango de un solo numero: la primera emision lo consume y la siguiente ya
            // estaria fuera del rango que autorizo la DIAN.
            resolucion(COMPANY, null, "FE_VENTA", "18760000007", "TOP", 100L, 100L, 100L,
                    VIGENTE_DESDE, VIGENTE_HASTA, true);

            assertThat(port.allocate(COMPANY, BRANCH, FE).orElseThrow().consecutive())
                    .isEqualTo(100L);

            assertThatThrownBy(() -> port.allocate(COMPANY, BRANCH, FE))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("su rango");
        }
    }

    @Nested
    @DisplayName("peek: leer la resolucion sin consumir consecutivo")
    class Peek {

        @Test
        @DisplayName("devuelve resolucion y prefijo con consecutivo nulo")
        void devuelve_resolucion_y_prefijo_sin_consecutivo() {
            resolucionDeEmpresa();

            AllocatedNumber leido = port.peekActive(COMPANY, BRANCH, FE).orElseThrow();

            // El POS auto-increment lo numera el proveedor: pedir aqui un consecutivo
            // quemaria uno del rango por cada venta que ya numera MATIAS.
            assertThat(leido.consecutive()).isNull();
            assertThat(leido.resolutionNumber()).isEqualTo("18760000001");
            assertThat(leido.prefix()).isEqualTo("SETP");
        }

        @Test
        @DisplayName("no mueve el contador")
        void no_mueve_el_contador() {
            resolucionDeEmpresa();

            port.peekActive(COMPANY, BRANCH, FE);
            port.peekActive(COMPANY, BRANCH, FE);

            assertThat(consecutivoEnBase("18760000001")).isEqualTo(100L);
        }

        @Test
        @DisplayName("tambien exige vigencia")
        void tambien_exige_vigencia() {
            resolucion(COMPANY, null, "FE_VENTA", "18760000008", "OLD", 100L, 199L, 100L,
                    LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), true);

            assertThatThrownBy(() -> port.peekActive(COMPANY, BRANCH, FE))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("vigente");
        }

        @Test
        @DisplayName("sin resolucion activa devuelve vacio")
        void sin_resolucion_activa_devuelve_vacio() {
            assertThat(port.peekActive(COMPANY, BRANCH, FE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("release: recuperar el consecutivo de un rechazo")
    class Release {

        @Test
        @DisplayName("recupera el ultimo numero entregado y devuelve el contador atras")
        void recupera_el_ultimo_numero_entregado() {
            resolucionDeEmpresa();
            Long entregado = port.allocate(COMPANY, BRANCH, FE).orElseThrow().consecutive();

            assertThat(port.release(COMPANY, BRANCH, FE, entregado)).isTrue();

            // El siguiente documento reutiliza el numero del rechazado: sin esto la
            // secuencia fiscal queda con un hueco que hay que justificar ante la DIAN.
            assertThat(consecutivoEnBase("18760000001")).isEqualTo(100L);
            assertThat(port.allocate(COMPANY, BRANCH, FE).orElseThrow().consecutive())
                    .isEqualTo(100L);
        }

        @Test
        @DisplayName("no recupera un numero que ya no es el ultimo")
        void no_recupera_un_numero_que_ya_no_es_el_ultimo() {
            resolucionDeEmpresa();
            port.allocate(COMPANY, BRANCH, FE); // 100
            port.allocate(COMPANY, BRANCH, FE); // 101

            // El 100 ya no es recuperable: el 101 salio detras. Reutilizarlo FUERA DE
            // ORDEN violaria la numeracion ascendente que exige la DIAN.
            assertThat(port.release(COMPANY, BRANCH, FE, 100L)).isFalse();
            assertThat(consecutivoEnBase("18760000001")).isEqualTo(102L);
        }

        @Test
        @DisplayName("liberar dos veces el mismo numero no lo devuelve dos veces")
        void liberar_dos_veces_el_mismo_numero_no_lo_devuelve_dos_veces() {
            resolucionDeEmpresa();
            port.allocate(COMPANY, BRANCH, FE);

            assertThat(port.release(COMPANY, BRANCH, FE, 100L)).isTrue();
            assertThat(port.release(COMPANY, BRANCH, FE, 100L)).isFalse();
            assertThat(consecutivoEnBase("18760000001"))
                    .as("un release idempotente no puede retroceder el rango").isEqualTo(100L);
        }

        @Test
        @DisplayName("sin consecutivo no consulta la base")
        void sin_consecutivo_no_consulta_la_base() {
            resolucionDeEmpresa();

            assertThat(port.release(COMPANY, BRANCH, FE, null)).isFalse();
            assertThat(consecutivoEnBase("18760000001")).isEqualTo(100L);
        }

        @Test
        @DisplayName("sin resolucion activa no hay nada que liberar")
        void sin_resolucion_activa_no_hay_nada_que_liberar() {
            assertThat(port.release(COMPANY, BRANCH, FE, 100L)).isFalse();
        }

        @Test
        @DisplayName("no libera contra la resolucion de otra empresa")
        void no_libera_contra_la_resolucion_de_otra_empresa() {
            resolucionDeEmpresa();
            port.allocate(COMPANY, BRANCH, FE);

            Optional<AllocatedNumber> ajena = port.peekActive(OTRA_COMPANY, BRANCH, FE);

            assertThat(ajena).isEmpty();
            assertThat(port.release(OTRA_COMPANY, BRANCH, FE, 100L)).isFalse();
            assertThat(consecutivoEnBase("18760000001")).isEqualTo(101L);
        }
    }
}
