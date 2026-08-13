package com.vetsoftware.app.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.inventory.domain.StockBalance;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del saldo contra MySQL real.
 *
 * <p>
 * Lo que se comprueba aqui no existe en el codigo Java: la unicidad de
 * {@code (producto, sede)} la impone un indice de la base, y la version del
 * lock optimista la mueve Hibernate al hacer flush. Con un doble del
 * repositorio las dos cosas darian verde siempre.
 */
@Import(JpaStockBalanceRepository.class)
@DisplayName("JpaStockBalanceRepository — saldo, unicidad y lock contra MySQL real")
class StockBalancePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long PRODUCT = SchemaSeed.PRODUCT_ID;
    private static final Long OTRO_PRODUCT = SchemaSeed.OTRO_PRODUCT_ID;

    @Autowired
    private JpaStockBalanceRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private StockBalance saldo(Long productId, Long branchId, int cantidad, int minimo) {
        return repository
                .save(new StockBalance(null, COMPANY, branchId, productId, cantidad, minimo, null));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y version, y releer conserva los campos")
        void guardar_asigna_id_y_version() {
            StockBalance guardado = saldo(PRODUCT, BRANCH, 12, 5);

            assertThat(guardado.getId()).isNotNull();
            // La version la pone Hibernate, no el dominio: es la que detecta que dos
            // salidas concurrentes tocaron la misma fila.
            assertThat(guardado.getVersion()).isNotNull();

            StockBalance leido = repository.find(PRODUCT, BRANCH).orElseThrow();
            assertThat(leido.getQuantity()).isEqualTo(12);
            assertThat(leido.getMinStock()).isEqualTo(5);
            assertThat(leido.getCompanyId()).isEqualTo(COMPANY);
        }

        @Test
        @DisplayName("una cantidad negativa se persiste: la politica de la empresa la permite")
        void una_cantidad_negativa_se_persiste() {
            // La columna no tiene CHECK: si la empresa permite vender sin stock, el saldo
            // baja de cero y el kardex tiene que poder reflejarlo.
            saldo(PRODUCT, BRANCH, -3, 0);

            assertThat(repository.find(PRODUCT, BRANCH).orElseThrow().getQuantity()).isEqualTo(-3);
        }

        @Test
        @DisplayName("un producto sin saldo en la sede no devuelve nada")
        void un_producto_sin_saldo_no_devuelve_nada() {
            assertThat(repository.find(PRODUCT, BRANCH)).isEmpty();
        }
    }

    @Nested
    @DisplayName("un solo saldo por producto y sede")
    class Unicidad {

        @Test
        @DisplayName("insertar el mismo par dos veces lo rechaza la base")
        void insertar_el_mismo_par_dos_veces_lo_rechaza_la_base() {
            saldo(PRODUCT, BRANCH, 10, 5);

            // Dos filas de saldo para el mismo (producto, sede) partirian el stock en dos
            // y cada operacion movería una: es el peor fallo posible del inventario. El
            // indice unico es la unica red que lo impide de verdad.
            assertThatThrownBy(() -> {
                saldo(PRODUCT, BRANCH, 99, 5);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("el mismo producto en otra sede si es otra fila")
        void el_mismo_producto_en_otra_sede_es_otra_fila() {
            saldo(PRODUCT, BRANCH, 10, 5);
            saldo(PRODUCT, OTRA_BRANCH, 4, 2);

            assertThat(repository.find(PRODUCT, BRANCH).orElseThrow().getQuantity()).isEqualTo(10);
            assertThat(repository.find(PRODUCT, OTRA_BRANCH).orElseThrow().getQuantity())
                    .isEqualTo(4);
        }

        @Test
        @DisplayName("otro producto en la misma sede si es otra fila")
        void otro_producto_en_la_misma_sede_es_otra_fila() {
            saldo(PRODUCT, BRANCH, 10, 5);
            saldo(OTRO_PRODUCT, BRANCH, 7, 3);

            assertThat(repository.find(OTRO_PRODUCT, BRANCH).orElseThrow().getQuantity())
                    .isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("findForUpdate — la lectura que serializa las salidas")
    class Lock {

        @Test
        @DisplayName("devuelve la misma fila que la lectura normal")
        void devuelve_la_misma_fila_que_la_lectura_normal() {
            StockBalance guardado = saldo(PRODUCT, BRANCH, 12, 5);

            Optional<StockBalance> conLock = repository.findForUpdate(PRODUCT, BRANCH);

            // El SELECT ... FOR UPDATE es JPQL con @Lock: si la anotacion se cayera, el
            // test seguiria en verde pero dos ventas simultaneas sobrevenderian. Lo que
            // esto fija es que la consulta existe y devuelve lo mismo; la exclusion
            // mutua en si necesita dos conexiones y no cabe en una rodaja.
            assertThat(conLock).map(StockBalance::getId).contains(guardado.getId());
            assertThat(conLock.orElseThrow().getQuantity()).isEqualTo(12);
        }

        @Test
        @DisplayName("un producto sin saldo no bloquea nada y devuelve vacio")
        void un_producto_sin_saldo_devuelve_vacio() {
            // Es el caso de la primera compra: el caso de uso crea la fila si no esta.
            assertThat(repository.findForUpdate(PRODUCT, BRANCH)).isEmpty();
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("mover el saldo sobre la fila leida no crea una fila nueva")
        void mover_el_saldo_no_crea_una_fila_nueva() {
            StockBalance guardado = saldo(PRODUCT, BRANCH, 10, 5);

            StockBalance recargado = repository.find(PRODUCT, BRANCH).orElseThrow();
            recargado.add(5);
            recargado.subtract(2);
            repository.save(recargado);
            entityManager.flush();

            StockBalance leido = repository.find(PRODUCT, BRANCH).orElseThrow();
            assertThat(leido.getId()).isEqualTo(guardado.getId());
            assertThat(leido.getQuantity()).isEqualTo(13);
        }
    }
}
