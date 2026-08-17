package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja del lease que reparte los jobs de DIAN entre replicas, contra MySQL
 * real.
 *
 * <p>
 * <b>Por que un doble no sirve.</b> Este adaptador no tiene logica de negocio:
 * es literalmente una sentencia SQL con {@code FOR UPDATE SKIP LOCKED} y un
 * {@code UPDATE} de marca, escritos en {@code JdbcTemplate} porque
 * {@code SKIP LOCKED} no se expresa en JPQL y porque {@code dian_leased_until}
 * ni siquiera esta mapeada en la entidad — no existe en Java. Doblar el puerto
 * es doblar la unica cosa que hay que probar.
 *
 * <p>
 * Lo que esta en juego: sin el lease, las N tareas Fargate leen la misma lista
 * y transmiten el mismo documento N veces a la DIAN. Los casos de aqui fijan
 * las tres piezas que lo impiden: que reclamar marque de verdad, que lo ya
 * reclamado no vuelva a salir, y que un lote abandonado (replica muerta) vuelva
 * a estar disponible al expirar.
 *
 * <p>
 * <b>Lo que estos casos NO cubren</b>: el {@code SKIP LOCKED} propiamente
 * dicho. Saltarse una fila que otra transaccion tiene bloqueada exige dos
 * conexiones concurrentes, y {@code @DataJpaTest} corre en una sola transaccion
 * con rollback. Lo que si queda cubierto es el filtro de exclusion, que es el
 * que evita el trabajo repetido entre ciclos.
 */
@Import(JdbcDianJobLeasePort.class)
@DisplayName("JdbcDianJobLeasePort — reparto de lotes entre replicas contra MySQL real")
class DianJobLeaseAdapterIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;

    private static final Duration LEASE = Duration.ofMinutes(5);

    /** Lease ya vencido: el documento vuelve a estar disponible. */
    private static final String LEASE_VENCIDO = "2020-01-01 00:00:00";
    /** Lease de una replica que sigue trabajando. */
    private static final String LEASE_VIGENTE = "2099-01-01 00:00:00";

    @Autowired
    private JdbcDianJobLeasePort port;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    /**
     * Siembra por SQL nativo con id explicito: {@code dian_leased_until} es
     * andamiaje de los jobs y no esta mapeada en
     * {@code ElectronicDocumentJpaEntity}, asi que no hay forma de escribirla desde
     * el modelo.
     */
    private void documento(long id, DianStatus estado, String fechaEmision, String leasedUntil) {
        entityManager.createNativeQuery("""
                INSERT INTO electronic_documents (id, company_id, branch_id, document_type,
                        issue_date, issue_time, dian_status, line_extension_amount,
                        tax_exclusive_amount, tax_inclusive_amount, payable_amount,
                        rete_fuente_amount, rete_iva_amount, rete_ica_amount, payment_form,
                        created_date, reversed, enabled, dian_leased_until)
                VALUES (%d, %d, %d, 'FE_VENTA', '%s', '10:15:00-05:00', '%s', 100000.00,
                        100000.00, 119000.00, 119000.00, 0.00, 0.00, 0.00, 'CONTADO',
                        '2026-01-15 10:15:00', false, true, %s)
                """.formatted(id, COMPANY, BRANCH, fechaEmision, estado.name(),
                leasedUntil == null ? "NULL" : "'" + leasedUntil + "'")).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private void pendienteLibre(long id, String fechaEmision) {
        documento(id, DianStatus.PENDIENTE, fechaEmision, null);
    }

    private Object leaseEnBase(long id) {
        return entityManager
                .createNativeQuery(
                        "SELECT dian_leased_until FROM electronic_documents WHERE id = ?")
                .setParameter(1, id).getSingleResult();
    }

    @Nested
    @DisplayName("reclamar un lote")
    class Reclamo {

        @Test
        @DisplayName("devuelve los documentos del estado pedido")
        void devuelve_los_documentos_del_estado_pedido() {
            pendienteLibre(8001L, "2026-01-10");
            pendienteLibre(8002L, "2026-01-11");

            assertThat(port.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE))
                    .containsExactlyInAnyOrder(8001L, 8002L);
        }

        @Test
        @DisplayName("marca como suyos los documentos que reclama")
        void marca_como_suyos_los_documentos_que_reclama() {
            pendienteLibre(8001L, "2026-01-10");

            port.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE);

            // La marca es lo que hace que el reparto funcione: sin ella, el SELECT del
            // ciclo siguiente (o de otra replica) devolveria el mismo documento.
            assertThat(leaseEnBase(8001L)).isNotNull();
        }

        @Test
        @DisplayName("no toca los documentos que no reclamo")
        void no_toca_los_documentos_que_no_reclamo() {
            pendienteLibre(8001L, "2026-01-10");
            documento(8002L, DianStatus.VALIDADO, "2026-01-11", null);

            port.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE);

            assertThat(leaseEnBase(8002L)).isNull();
        }

        @Test
        @DisplayName("sin trabajo disponible devuelve una lista vacia")
        void sin_trabajo_disponible_devuelve_lista_vacia() {
            documento(8001L, DianStatus.VALIDADO, "2026-01-10", null);

            assertThat(port.leaseByDianStatus(DianStatus.CONTINGENCIA, 10, LEASE)).isEmpty();
        }

        @Test
        @DisplayName("no mezcla estados: el job de contingencia no se lleva los pendientes")
        void no_mezcla_estados() {
            pendienteLibre(8001L, "2026-01-10");
            documento(8002L, DianStatus.CONTINGENCIA, "2026-01-11", null);

            assertThat(port.leaseByDianStatus(DianStatus.CONTINGENCIA, 10, LEASE))
                    .containsExactly(8002L);
        }
    }

    @Nested
    @DisplayName("exclusividad: lo reclamado no se vuelve a repartir")
    class Exclusividad {

        @Test
        @DisplayName("un segundo reclamo inmediato no devuelve nada")
        void un_segundo_reclamo_inmediato_no_devuelve_nada() {
            pendienteLibre(8001L, "2026-01-10");
            pendienteLibre(8002L, "2026-01-11");

            List<Long> primero = port.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE);
            List<Long> segundo = port.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE);

            // Es la propiedad que impide que el mismo documento fiscal salga dos veces
            // hacia la DIAN. Si el UPDATE de marca no cuajara, los dos lotes serian
            // identicos y el proveedor recibiria duplicados.
            assertThat(primero).containsExactlyInAnyOrder(8001L, 8002L);
            assertThat(segundo).isEmpty();
        }

        @Test
        @DisplayName("un documento arrendado por otra replica no se reclama")
        void un_documento_arrendado_por_otra_replica_no_se_reclama() {
            documento(8001L, DianStatus.PENDIENTE, "2026-01-10", LEASE_VIGENTE);
            pendienteLibre(8002L, "2026-01-11");

            assertThat(port.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE))
                    .containsExactly(8002L);
        }

        @Test
        @DisplayName("un lease vencido vuelve a estar disponible")
        void un_lease_vencido_vuelve_a_estar_disponible() {
            documento(8001L, DianStatus.PENDIENTE, "2026-01-10", LEASE_VENCIDO);

            // El lease es temporal a proposito: si la replica que lo tomo murio a mitad,
            // su lote tiene que volver a la cola en vez de quedarse bloqueado para
            // siempre.
            assertThat(port.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE))
                    .containsExactly(8001L);
        }

        @Test
        @DisplayName("un documento nunca arrendado (lease nulo) esta disponible")
        void un_documento_nunca_arrendado_esta_disponible() {
            pendienteLibre(8001L, "2026-01-10");

            // El guard es `IS NULL OR < now`: sin la rama del NULL, ningun documento
            // recien creado entraria jamas a un job.
            assertThat(port.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE))
                    .containsExactly(8001L);
        }
    }

    @Nested
    @DisplayName("tamano del lote y orden")
    class Lote {

        @Test
        @DisplayName("respeta el limite pedido")
        void respeta_el_limite_pedido() {
            pendienteLibre(8001L, "2026-01-10");
            pendienteLibre(8002L, "2026-01-11");
            pendienteLibre(8003L, "2026-01-12");

            assertThat(port.leaseByDianStatus(DianStatus.PENDIENTE, 2, LEASE)).hasSize(2);
        }

        @Test
        @DisplayName("empieza por el documento mas antiguo")
        void empieza_por_el_documento_mas_antiguo() {
            pendienteLibre(8001L, "2026-01-12");
            pendienteLibre(8002L, "2026-01-10");
            pendienteLibre(8003L, "2026-01-11");

            // ORDER BY issue_date: el backlog se drena en orden de emision, no por id.
            // Un documento viejo no puede quedarse atras mientras entran nuevos.
            assertThat(port.leaseByDianStatus(DianStatus.PENDIENTE, 1, LEASE))
                    .containsExactly(8002L);
        }

        @Test
        @DisplayName("los documentos fuera del lote quedan sin marcar para el ciclo siguiente")
        void los_documentos_fuera_del_lote_quedan_sin_marcar() {
            pendienteLibre(8001L, "2026-01-10");
            pendienteLibre(8002L, "2026-01-11");

            port.leaseByDianStatus(DianStatus.PENDIENTE, 1, LEASE);

            assertThat(leaseEnBase(8001L)).as("el reclamado").isNotNull();
            assertThat(leaseEnBase(8002L)).as("el que no entro en el lote").isNull();
        }
    }
}
