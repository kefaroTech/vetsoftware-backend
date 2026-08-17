package com.vetsoftware.app.purchaseorder.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.purchaseorder.application.command.SearchPurchaseOrdersCommand;
import com.vetsoftware.app.purchaseorder.domain.BranchRef;
import com.vetsoftware.app.purchaseorder.domain.CompanyRef;
import com.vetsoftware.app.purchaseorder.domain.ProductRef;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.purchaseorder.domain.SupplierRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de las ordenes de compra contra MySQL real.
 *
 * <p>
 * Cada {@code save} reconstruye el agregado entero y lo manda por merge.
 *
 * <p>
 * Que eso actualice la linea que ya existe y no inserte otra lo dice el motor.
 *
 * <p>
 * Un doble en memoria guarda la referencia viva y siempre daria verde.
 *
 * <p>
 * Lo mismo vale para el soft-delete, las pausadas y el conteo paginado.
 */
@Import({JpaPurchaseOrderRepository.class, PurchaseOrderJpaMapper.class})
@DisplayName("JpaPurchaseOrderRepository — agregado, tenant y pagina contra MySQL real")
class PurchaseOrderPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long PRODUCT = SchemaSeed.PRODUCT_ID;
    private static final Long OTRO_PRODUCT = SchemaSeed.OTRO_PRODUCT_ID;
    private static final Long EMPLEADO = SchemaSeed.EMPLOYEE_ID;

    private static final Long PROVEEDOR_ID = 960L;
    private static final Long PROV_AJENO_ID = 961L;
    private static final Long SEDE_AJENA_ID = 962L;
    private static final Long PROV_ALTERNO_ID = 963L;

    private static final LocalDate FECHA_ORDEN = LocalDate.of(2026, 3, 10);
    private static final LocalDate FECHA_ESPERADA = LocalDate.of(2026, 3, 20);
    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 3, 10, 8, 0);
    private static final BigDecimal COSTO = new BigDecimal("15000.00");

    @Autowired
    private JpaPurchaseOrderRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        sembrarProveedor(PROVEEDOR_ID, "Distribuidora", COMPANY);
        sembrarProveedor(PROV_ALTERNO_ID, "Insumos Centro", COMPANY);
        sembrarProveedor(PROV_AJENO_ID, "Insumos Sur", OTRA_COMPANY);
        sembrarSedeAjena();
        entityManager.flush();
    }

    // SchemaSeed no siembra proveedores ni una sede de la empresa ajena.
    private void sembrarProveedor(Long id, String nombre, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO suppliers (id, name, company_id)
                VALUES (:id, :nombre, :companyId)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("companyId", companyId).executeUpdate();
    }

    private void sembrarSedeAjena() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, 'Sede Ajena', 'AJENA', :cityId, :companyId)
                """).setParameter("id", SEDE_AJENA_ID).setParameter("cityId", SchemaSeed.CITY_ID)
                .setParameter("companyId", OTRA_COMPANY).executeUpdate();
    }

    private static CompanyRef clinica() {
        return new CompanyRef(COMPANY, "Veterinaria de prueba", "900123456");
    }

    private static CompanyRef clinicaAjena() {
        return new CompanyRef(OTRA_COMPANY, "Veterinaria ajena", "900654321");
    }

    private static BranchRef sedeCentro() {
        return new BranchRef(BRANCH, "Sede Centro");
    }

    private static BranchRef sedeNorte() {
        return new BranchRef(OTRA_BRANCH, "Sede Norte");
    }

    private static SupplierRef proveedor() {
        return new SupplierRef(PROVEEDOR_ID, "Distribuidora");
    }

    private static SupplierRef proveedorAlterno() {
        return new SupplierRef(PROV_ALTERNO_ID, "Insumos Centro");
    }

    private static ProductRef amoxicilina() {
        return new ProductRef(PRODUCT, "Amoxicilina 500mg", "SKU-100");
    }

    private static ProductRef ivermectina() {
        return new ProductRef(OTRO_PRODUCT, "Ivermectina 1%", "SKU-200");
    }

    private static PurchaseOrderLine linea(ProductRef producto, int pedida) {
        return new PurchaseOrderLine(null, producto, pedida, COSTO, 0);
    }

    private PurchaseOrder orden(CompanyRef empresa, BranchRef sede, SupplierRef prov,
            PurchaseOrderStatus estado, LocalDate fecha, List<PurchaseOrderLine> lineas) {
        return repository
                .save(new PurchaseOrder(null, empresa, sede, prov, estado, fecha, FECHA_ESPERADA,
                        "Pedido mensual", lineas, CREADA, EMPLEADO, null, null, null, true));
    }

    private PurchaseOrder ordenPropia(PurchaseOrderStatus estado, List<PurchaseOrderLine> lineas) {
        return orden(clinica(), sedeCentro(), proveedor(), estado, FECHA_ORDEN, lineas);
    }

    private PurchaseOrder ordenEnFecha(LocalDate fecha) {
        return orden(clinica(), sedeCentro(), proveedor(), PurchaseOrderStatus.PLACED, fecha,
                List.of(linea(amoxicilina(), 10)));
    }

    private PurchaseOrder ordenEnSede(BranchRef sede) {
        return orden(clinica(), sede, proveedor(), PurchaseOrderStatus.PLACED, FECHA_ORDEN,
                List.of(linea(amoxicilina(), 10)));
    }

    private PurchaseOrder ordenDeProveedor(SupplierRef prov) {
        return orden(clinica(), sedeCentro(), prov, PurchaseOrderStatus.PLACED, FECHA_ORDEN,
                List.of(linea(amoxicilina(), 10)));
    }

    private PurchaseOrder ordenAjena() {
        return orden(clinicaAjena(), new BranchRef(SEDE_AJENA_ID, "Sede Ajena"),
                new SupplierRef(PROV_AJENO_ID, "Insumos Sur"), PurchaseOrderStatus.PLACED,
                FECHA_ORDEN, List.of(linea(amoxicilina(), 5)));
    }

    private static PurchaseOrderLine lineaDe(PurchaseOrder orden, Long productoId) {
        return orden.getLines().stream().filter(l -> productoId.equals(l.getProduct().id()))
                .findFirst().orElseThrow();
    }

    private void recibir(Long ordenId, int cantidad) {
        PurchaseOrder orden = repository.findByIdAndCompanyId(ordenId, COMPANY).orElseThrow();
        orden.receiveLine(lineaDe(orden, PRODUCT).getId(), cantidad);
        orden.recalculateStatusAfterReceipt(EMPLEADO);
        repository.save(orden);
    }

    private SearchPurchaseOrdersCommand busqueda(Long provId, Long sedeId,
            PurchaseOrderStatus estado, LocalDate desde, LocalDate hasta, int pagina, int tam) {
        return new SearchPurchaseOrdersCommand(COMPANY, provId, sedeId, estado, desde, hasta,
                pagina, tam);
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    // Pausa sin pasar por el remove de JPA, que ademas borra en cascada las lineas.
    private void pausarPorSql(Long ordenId) {
        entityManager.createNativeQuery("""
                UPDATE purchase_orders SET enabled = false WHERE id = :id
                """).setParameter("id", ordenId).executeUpdate();
        entityManager.clear();
    }

    private long filasDeLinea(Long ordenId) {
        entityManager.flush();
        Object total = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM purchase_order_lines WHERE purchase_order_id = :id
                """).setParameter("id", ordenId).getSingleResult();
        return ((Number) total).longValue();
    }

    private long filasPausadas(Long ordenId) {
        entityManager.flush();
        Object total = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM purchase_orders WHERE id = :id AND enabled = false
                """).setParameter("id", ordenId).getSingleResult();
        return ((Number) total).longValue();
    }

    @Nested
    @DisplayName("ida y vuelta del agregado con sus lineas")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer devuelve la cabecera y las dos lineas completas")
        void guardar_y_releer_devuelve_el_agregado_completo() {
            List<PurchaseOrderLine> lineas = List.of(linea(amoxicilina(), 10),
                    linea(ivermectina(), 4));
            PurchaseOrder guardada = ordenPropia(PurchaseOrderStatus.DRAFT, lineas);
            sincronizar();

            PurchaseOrder leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getCompany()).isEqualTo(clinica());
            assertThat(leida.getBranch()).isEqualTo(sedeCentro());
            assertThat(leida.getSupplier()).isEqualTo(proveedor());
            assertThat(leida.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
            assertThat(leida.getOrderDate()).isEqualTo(FECHA_ORDEN);
            assertThat(leida.getExpectedDate()).isEqualTo(FECHA_ESPERADA);
            assertThat(leida.getNotes()).isEqualTo("Pedido mensual");
            assertThat(leida.getCreatedDate()).isEqualTo(CREADA);
            assertThat(leida.getCreatedBy()).isEqualTo(EMPLEADO);
            assertThat(leida.getLines()).extracting(l -> l.getProduct().id())
                    .containsExactlyInAnyOrder(PRODUCT, OTRO_PRODUCT);
            assertThat(leida.getLines()).extracting(PurchaseOrderLine::getQuantityOrdered)
                    .containsExactlyInAnyOrder(10, 4);
        }

        @Test
        @DisplayName("el costo unitario conserva los centavos de la columna decimal")
        void el_costo_unitario_conserva_los_centavos() {
            PurchaseOrderLine conCentavos = new PurchaseOrderLine(null, amoxicilina(), 3,
                    new BigDecimal("1234.56"), 0);
            PurchaseOrder guardada = ordenPropia(PurchaseOrderStatus.DRAFT, List.of(conCentavos));
            sincronizar();

            PurchaseOrder leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getLines().get(0).getUnitCost()).isEqualByComparingTo("1234.56");
        }

        @Test
        @DisplayName("sin fecha esperada ni notas las columnas opcionales vuelven nulas")
        void las_columnas_opcionales_vuelven_nulas() {
            PurchaseOrder pelada = repository.save(new PurchaseOrder(null, clinica(), sedeCentro(),
                    proveedor(), PurchaseOrderStatus.DRAFT, FECHA_ORDEN, null, null,
                    List.of(linea(amoxicilina(), 2)), CREADA, EMPLEADO, null, null, null, true));
            sincronizar();

            PurchaseOrder leida = repository.findById(pelada.getId()).orElseThrow();

            assertThat(leida.getExpectedDate()).isNull();
            assertThat(leida.getNotes()).isNull();
            assertThat(leida.getUpdatedDate()).isNull();
            assertThat(leida.getUpdatedBy()).isNull();
        }

        @ParameterizedTest
        @EnumSource(PurchaseOrderStatus.class)
        @DisplayName("cada estado del ciclo de vida cabe en la columna y vuelve igual")
        void cada_estado_vuelve_igual(PurchaseOrderStatus estado) {
            PurchaseOrder guardada = ordenPropia(estado, List.of(linea(amoxicilina(), 10)));
            sincronizar();

            assertThat(repository.findById(guardada.getId()).orElseThrow().getStatus())
                    .isEqualTo(estado);
        }
    }

    @Nested
    @DisplayName("cantidad recibida: el guardado sucesivo cae sobre la linea que ya existe")
    class Recepcion {

        @Test
        @DisplayName("recibir sobre una linea actualiza su fila y no inserta otra")
        void recibir_actualiza_la_fila_y_no_inserta_otra() {
            List<PurchaseOrderLine> lineas = List.of(linea(amoxicilina(), 10),
                    linea(ivermectina(), 4));
            Long id = ordenPropia(PurchaseOrderStatus.PLACED, lineas).getId();
            sincronizar();
            PurchaseOrder emitida = repository.findByIdAndCompanyId(id, COMPANY).orElseThrow();
            Long lineaId = lineaDe(emitida, PRODUCT).getId();

            emitida.receiveLine(lineaId, 4);
            emitida.recalculateStatusAfterReceipt(EMPLEADO);
            repository.save(emitida);
            sincronizar();

            PurchaseOrder releida = repository.findByIdAndCompanyId(id, COMPANY).orElseThrow();
            assertThat(filasDeLinea(id)).isEqualTo(1 + 1);
            assertThat(lineaDe(releida, PRODUCT).getId()).isEqualTo(lineaId);
            assertThat(lineaDe(releida, PRODUCT).getQuantityReceived()).isEqualTo(4);
            assertThat(lineaDe(releida, OTRO_PRODUCT).getQuantityReceived()).isZero();
            assertThat(releida.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        @Test
        @DisplayName("dos recepciones sucesivas acumulan y dejan la orden en RECEIVED")
        void dos_recepciones_sucesivas_acumulan() {
            Long id = ordenPropia(PurchaseOrderStatus.PLACED, List.of(linea(amoxicilina(), 10)))
                    .getId();
            sincronizar();
            recibir(id, 4);
            sincronizar();
            recibir(id, 6);
            sincronizar();

            PurchaseOrder releida = repository.findByIdAndCompanyId(id, COMPANY).orElseThrow();

            assertThat(filasDeLinea(id)).isEqualTo(1);
            assertThat(lineaDe(releida, PRODUCT).getQuantityReceived()).isEqualTo(10);
            assertThat(releida.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        }

        @Test
        @DisplayName("editar el borrador quitando una linea borra su fila, no la deja huerfana")
        void editar_quitando_una_linea_borra_su_fila() {
            List<PurchaseOrderLine> lineas = List.of(linea(amoxicilina(), 10),
                    linea(ivermectina(), 4));
            Long id = ordenPropia(PurchaseOrderStatus.DRAFT, lineas).getId();
            sincronizar();
            PurchaseOrder borrador = repository.findByIdAndCompanyId(id, COMPANY).orElseThrow();
            PurchaseOrderLine sobrevive = lineaDe(borrador, PRODUCT);

            borrador.update(sedeCentro(), proveedor(), FECHA_ORDEN, FECHA_ESPERADA, "Recortado",
                    List.of(sobrevive), EMPLEADO, borrador.getVersion());
            repository.save(borrador);
            sincronizar();

            PurchaseOrder releida = repository.findByIdAndCompanyId(id, COMPANY).orElseThrow();
            assertThat(filasDeLinea(id)).isEqualTo(1);
            assertThat(releida.getLines()).extracting(l -> l.getProduct().id())
                    .containsExactly(PRODUCT);
        }

        @Test
        @DisplayName("la version optimista de la cabecera sube tras aplicar la recepcion")
        void la_version_optimista_sube_tras_la_recepcion() {
            Long id = ordenPropia(PurchaseOrderStatus.PLACED, List.of(linea(amoxicilina(), 10)))
                    .getId();
            sincronizar();
            recibir(id, 3);
            sincronizar();

            assertThat(repository.findByIdAndCompanyId(id, COMPANY).orElseThrow().getVersion())
                    .isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("una orden de otra empresa no se encuentra con la empresa propia")
        void una_orden_ajena_no_se_encuentra_con_la_empresa_propia() {
            Long ajena = ordenAjena().getId();
            sincronizar();

            assertThat(repository.findByIdAndCompanyId(ajena, COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajena, OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el listado por empresa deja fuera las ordenes de la otra empresa")
        void el_listado_por_empresa_deja_fuera_las_ajenas() {
            Long propia = ordenPropia(PurchaseOrderStatus.DRAFT, List.of(linea(amoxicilina(), 10)))
                    .getId();
            ordenAjena();
            sincronizar();

            assertThat(repository.findAllByCompanyId(COMPANY)).extracting(PurchaseOrder::getId)
                    .containsExactly(propia);
        }

        @Test
        @DisplayName("la busqueda sin filtros sigue acotada a la empresa del comando")
        void la_busqueda_sin_filtros_sigue_acotada_a_la_empresa() {
            Long propia = ordenPropia(PurchaseOrderStatus.DRAFT, List.of(linea(amoxicilina(), 10)))
                    .getId();
            ordenAjena();
            sincronizar();

            PageResult<PurchaseOrder> pagina = repository
                    .search(busqueda(null, null, null, null, null, 0, 20));

            assertThat(pagina.totalElements()).isEqualTo(1);
            assertThat(pagina.content()).extracting(PurchaseOrder::getId).containsExactly(propia);
        }

        @Test
        @DisplayName("reactivar una orden de otra empresa no toca ninguna fila")
        void reactivar_una_orden_ajena_no_toca_ninguna_fila() {
            Long ajena = ordenAjena().getId();
            sincronizar();
            pausarPorSql(ajena);

            assertThat(repository.reactivate(ajena, COMPANY)).isZero();
            assertThat(repository.findAllDisabledByCompanyId(OTRA_COMPANY))
                    .extracting(PurchaseOrder::getId).containsExactly(ajena);
        }
    }

    @Nested
    @DisplayName("pausar y reactivar")
    class Pausadas {

        @Test
        @DisplayName("borrar no elimina la fila: la pausa y la saca de las consultas activas")
        void borrar_deja_la_fila_pausada() {
            Long id = ordenPropia(PurchaseOrderStatus.DRAFT, List.of(linea(amoxicilina(), 10)))
                    .getId();
            sincronizar();

            repository.delete(id);
            sincronizar();

            assertThat(filasPausadas(id)).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(id, COMPANY)).isEmpty();
            assertThat(repository.findAllByCompanyId(COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("pausar conserva el detalle: la orden pausada mantiene sus lineas")
        void pausar_conserva_el_detalle_de_la_orden() {
            List<PurchaseOrderLine> lineas = List.of(linea(amoxicilina(), 10),
                    linea(ivermectina(), 4));
            Long id = ordenPropia(PurchaseOrderStatus.DRAFT, lineas).getId();
            sincronizar();

            repository.delete(id);
            sincronizar();

            assertThat(filasDeLinea(id)).isEqualTo(1 + 1);
        }

        @Test
        @DisplayName("el listado de pausadas ve lo que el filtro de Hibernate esconde")
        void el_listado_de_pausadas_ve_lo_que_el_filtro_esconde() {
            Long pausada = ordenPropia(PurchaseOrderStatus.DRAFT, List.of(linea(amoxicilina(), 10)))
                    .getId();
            ordenPropia(PurchaseOrderStatus.PLACED, List.of(linea(ivermectina(), 4)));
            sincronizar();
            pausarPorSql(pausada);

            assertThat(repository.findAllDisabledByCompanyId(COMPANY))
                    .extracting(PurchaseOrder::getId).containsExactly(pausada);
        }

        @Test
        @DisplayName("reactivar devuelve la orden a las consultas activas")
        void reactivar_devuelve_la_orden_a_las_consultas_activas() {
            Long id = ordenPropia(PurchaseOrderStatus.DRAFT, List.of(linea(amoxicilina(), 10)))
                    .getId();
            sincronizar();
            pausarPorSql(id);

            assertThat(repository.reactivate(id, COMPANY)).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(id, COMPANY)).isPresent();
            assertThat(repository.findAllDisabledByCompanyId(COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("busqueda paginada")
    class Busqueda {

        @Test
        @DisplayName("filtra por sede y deja fuera las de la otra sede")
        void filtra_por_sede() {
            Long enCentro = ordenEnSede(sedeCentro()).getId();
            ordenEnSede(sedeNorte());
            sincronizar();

            PageResult<PurchaseOrder> pagina = repository
                    .search(busqueda(null, BRANCH, null, null, null, 0, 20));

            assertThat(pagina.content()).extracting(PurchaseOrder::getId).containsExactly(enCentro);
        }

        @Test
        @DisplayName("filtra por proveedor")
        void filtra_por_proveedor() {
            Long delPrincipal = ordenDeProveedor(proveedor()).getId();
            ordenDeProveedor(proveedorAlterno());
            sincronizar();

            PageResult<PurchaseOrder> pagina = repository
                    .search(busqueda(PROVEEDOR_ID, null, null, null, null, 0, 20));

            assertThat(pagina.content()).extracting(PurchaseOrder::getId)
                    .containsExactly(delPrincipal);
        }

        @Test
        @DisplayName("filtra por estado")
        void filtra_por_estado() {
            Long borrador = ordenPropia(PurchaseOrderStatus.DRAFT,
                    List.of(linea(amoxicilina(), 10))).getId();
            ordenPropia(PurchaseOrderStatus.PLACED, List.of(linea(ivermectina(), 4)));
            sincronizar();

            PageResult<PurchaseOrder> pagina = repository
                    .search(busqueda(null, null, PurchaseOrderStatus.DRAFT, null, null, 0, 20));

            assertThat(pagina.content()).extracting(PurchaseOrder::getId).containsExactly(borrador);
        }

        @Test
        @DisplayName("el rango de fechas incluye los extremos y descarta lo de fuera")
        void el_rango_de_fechas_incluye_los_extremos() {
            Long primero = ordenEnFecha(LocalDate.of(2026, 3, 5)).getId();
            Long ultimo = ordenEnFecha(LocalDate.of(2026, 3, 15)).getId();
            ordenEnFecha(LocalDate.of(2026, 3, 16));
            sincronizar();

            PageResult<PurchaseOrder> pagina = repository.search(busqueda(null, null, null,
                    LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 15), 0, 20));

            assertThat(pagina.content()).extracting(PurchaseOrder::getId).containsExactly(ultimo,
                    primero);
        }

        @Test
        @DisplayName("dos paginas consecutivas ni repiten ni omiten filas")
        void dos_paginas_consecutivas_ni_repiten_ni_omiten() {
            Long antigua = ordenEnFecha(LocalDate.of(2026, 3, 1)).getId();
            Long media = ordenEnFecha(LocalDate.of(2026, 3, 10)).getId();
            Long reciente = ordenEnFecha(LocalDate.of(2026, 3, 20)).getId();
            sincronizar();

            PageResult<PurchaseOrder> primera = repository
                    .search(busqueda(null, null, null, null, null, 0, 2));
            PageResult<PurchaseOrder> segunda = repository
                    .search(busqueda(null, null, null, null, null, 1, 2));

            assertThat(primera.totalElements()).isEqualTo(3);
            assertThat(primera.totalPages()).isEqualTo(2);
            assertThat(primera.content()).extracting(PurchaseOrder::getId).containsExactly(reciente,
                    media);
            assertThat(segunda.content()).extracting(PurchaseOrder::getId).containsExactly(antigua);
        }

        @Test
        @DisplayName("un tamano de pagina desmedido se acota al tope del kernel")
        void un_tamano_de_pagina_desmedido_se_acota() {
            ordenPropia(PurchaseOrderStatus.PLACED, List.of(linea(amoxicilina(), 10)));
            sincronizar();

            PageResult<PurchaseOrder> pagina = repository
                    .search(busqueda(null, null, null, null, null, 0, 100000));

            assertThat(pagina.pageSize()).isEqualTo(200);
        }
    }
}
