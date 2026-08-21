package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.vetsoftware.app.goodsreceipt.application.command.SearchGoodsReceiptsCommand;
import com.vetsoftware.app.goodsreceipt.domain.BranchRef;
import com.vetsoftware.app.goodsreceipt.domain.CompanyRef;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.domain.ProductRef;
import com.vetsoftware.app.goodsreceipt.domain.SupplierRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
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
 * Rodaja de persistencia de la recepcion de mercancia contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Lo que se verifica no es codigo Java,
 * es lo que hace el motor: el cascade + {@code orphanRemoval} que baja las
 * lineas junto con la cabecera y les asigna el {@code AUTO_INCREMENT} —de cuyo
 * <i>orden</i> depende {@code toDomainReusingRefs}, que empareja lineas
 * guardadas y originales por indice—, el redondeo de {@code unit_cost} a
 * {@code DECIMAL(12,2)}, el {@code VARCHAR(30)} donde aterriza el
 * {@code GoodsReceiptStatus}, el {@code @SQLDelete}/{@code @SQLRestriction} que
 * convierte el borrado en un {@code enabled = false} invisible, y el orden
 * total del {@code search} paginado. Con un doble del repositorio el contrato
 * lo definiria el propio test —justo donde vivio BE-01— y las cuatro cosas
 * darian verde sobre una base que no existe.
 *
 * <p>
 * {@code SchemaSeed} no siembra proveedores y {@code goods_receipts} tiene FK a
 * {@code suppliers}, asi que los proveedores (y la sede del tenant ajeno) se
 * insertan aqui por SQL nativo con ids propios que no chocan con los suyos.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaGoodsReceiptRepository — recepciones, lineas y busqueda contra MySQL real")
class GoodsReceiptPersistenceIT extends AbstractDataJpaTest {

    private static final Long SUPPLIER_ID = 960L;
    private static final Long OTRO_SUPPLIER_ID = 961L;
    private static final Long SUPPLIER_AJENO_ID = 962L;
    private static final Long BRANCH_AJENA_ID = 970L;

    private static final CompanyRef EMPRESA = new CompanyRef(SchemaSeed.COMPANY_ID,
            "Veterinaria de prueba", "900123456");
    private static final CompanyRef OTRA_EMPRESA = new CompanyRef(SchemaSeed.OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");
    private static final BranchRef SEDE = new BranchRef(SchemaSeed.BRANCH_ID, "Sede Centro");
    private static final BranchRef OTRA_SEDE = new BranchRef(SchemaSeed.OTRA_BRANCH_ID,
            "Sede Norte");
    private static final BranchRef SEDE_AJENA = new BranchRef(BRANCH_AJENA_ID, "Sede Ajena");
    private static final SupplierRef PROVEEDOR = new SupplierRef(SUPPLIER_ID, "Distribuidora Vet");
    private static final SupplierRef OTRO_PROVEEDOR = new SupplierRef(OTRO_SUPPLIER_ID,
            "Insumos Andinos");
    private static final SupplierRef PROVEEDOR_AJENO = new SupplierRef(SUPPLIER_AJENO_ID,
            "Proveedor Ajeno");

    private static final ProductRef VACUNA = new ProductRef(SchemaSeed.PRODUCT_ID,
            "Amoxicilina 500mg", "SKU-100");
    private static final ProductRef JERINGA = new ProductRef(SchemaSeed.OTRO_PRODUCT_ID,
            "Ivermectina 1%", "SKU-200");

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 12);
    private static final LocalDate VENCIMIENTO = LocalDate.of(2027, 1, 31);
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 3, 12, 9, 15);
    private static final BigDecimal COSTO = new BigDecimal("12.50");

    @Autowired
    private JpaGoodsReceiptRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        proveedor(SUPPLIER_ID, "Distribuidora Vet", SchemaSeed.COMPANY_ID);
        proveedor(OTRO_SUPPLIER_ID, "Insumos Andinos", SchemaSeed.COMPANY_ID);
        proveedor(SUPPLIER_AJENO_ID, "Proveedor Ajeno", SchemaSeed.OTRA_COMPANY_ID);
        sedeAjena();
        entityManager.flush();
    }

    /** {@code SchemaSeed} no llega a proveedores y la FK de la cabecera si. */
    private void proveedor(Long id, String nombre, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO suppliers (id, name, company_id, created_date, version, enabled)
                VALUES (:id, '%s', %d, '2026-01-15 08:00:00', 0, true)
                """.formatted(nombre, companyId)).setParameter("id", id).executeUpdate();
    }

    /**
     * Sede propia del tenant ajeno: las dos de SchemaSeed son de la empresa 900.
     */
    private void sedeAjena() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, 'Sede Ajena', 'AJENA', %d, %d)
                """.formatted(SchemaSeed.CITY_ID, SchemaSeed.OTRA_COMPANY_ID))
                .setParameter("id", BRANCH_AJENA_ID).executeUpdate();
    }

    /**
     * Vacia el contexto de persistencia para que la siguiente lectura venga de la
     * base y no de la primera cache: sin esto las aserciones sobre decimales y
     * enums comprobarian el objeto que sigue en memoria, no la columna.
     */
    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Cuenta las lineas con SQL nativo y sin pasar por ninguna entidad: contarlas
     * por el adaptador o por {@code GoodsReceiptLineJpaEntity} haria que un cero
     * pudiera significar «filtradas por {@code @SQLRestriction}» en vez de
     * «borradas de la base», y el test dejaria de distinguir el bug del cascade.
     */
    private long filasDeLinea(Long recepcionId) {
        entityManager.flush();
        Object total = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM goods_receipt_lines WHERE goods_receipt_id = :id
                """).setParameter("id", recepcionId).getSingleResult();
        return ((Number) total).longValue();
    }

    /** Cabecera ya dada de baja, contada tambien sin filtros de Hibernate. */
    private long cabecerasDeshabilitadas(Long recepcionId) {
        entityManager.flush();
        Object total = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM goods_receipts WHERE id = :id AND enabled = false
                """).setParameter("id", recepcionId).getSingleResult();
        return ((Number) total).longValue();
    }

    private GoodsReceiptLine linea(ProductRef producto, String lote, int cantidad,
            BigDecimal costo) {
        return new GoodsReceiptLine(null, producto, null, lote, VENCIMIENTO, cantidad, costo);
    }

    private GoodsReceipt guardar(CompanyRef empresa, BranchRef sede, SupplierRef proveedor,
            LocalDate fecha, GoodsReceiptStatus estado, List<GoodsReceiptLine> lineas) {
        return repository.save(new GoodsReceipt(null, empresa, sede, proveedor, null, fecha,
                "FV-1001", "Entrega parcial", estado, lineas, CREADO, SchemaSeed.EMPLOYEE_ID, null,
                null, null, true));
    }

    /** Recepcion propia por defecto: empresa 900, sede 910, proveedor 960. */
    private GoodsReceipt guardar(LocalDate fecha, GoodsReceiptStatus estado) {
        return guardar(EMPRESA, SEDE, PROVEEDOR, fecha, estado,
                List.of(linea(VACUNA, "LOTE-A", 10, COSTO)));
    }

    private SearchGoodsReceiptsCommand busqueda(Long supplierId, Long branchId,
            GoodsReceiptStatus estado, LocalDate desde, LocalDate hasta, int page, int pageSize) {
        return new SearchGoodsReceiptsCommand(SchemaSeed.COMPANY_ID, supplierId, branchId, estado,
                desde, hasta, page, pageSize);
    }

    @Nested
    @DisplayName("ida y vuelta del agregado con sus lineas")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva la cabecera y cada linea")
        void guardar_y_releer_conserva_la_cabecera_y_cada_linea() {
            GoodsReceipt guardado = guardar(EMPRESA, SEDE, PROVEEDOR, FECHA,
                    GoodsReceiptStatus.DRAFT, List.of(linea(VACUNA, "LOTE-A", 10, COSTO),
                            linea(JERINGA, "LOTE-B", 4, new BigDecimal("3.00"))));
            releerDesdeLaBase();

            GoodsReceipt leido = repository
                    .findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID).orElseThrow();

            assertThat(leido.getCompany()).isEqualTo(EMPRESA);
            assertThat(leido.getBranch()).isEqualTo(SEDE);
            assertThat(leido.getSupplier()).isEqualTo(PROVEEDOR);
            assertThat(leido.getPurchaseOrderId()).isNull();
            assertThat(leido.getReceiptDate()).isEqualTo(FECHA);
            assertThat(leido.getSupplierInvoiceNumber()).isEqualTo("FV-1001");
            assertThat(leido.getNotes()).isEqualTo("Entrega parcial");
            assertThat(leido.getStatus()).isEqualTo(GoodsReceiptStatus.DRAFT);
            assertThat(leido.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leido.getCreatedBy()).isEqualTo(SchemaSeed.EMPLOYEE_ID);
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getLines())
                    .extracting(l -> l.getProduct().id(), GoodsReceiptLine::getLotNumber,
                            GoodsReceiptLine::getExpireDate, GoodsReceiptLine::getQuantityReceived)
                    .containsExactly(tuple(SchemaSeed.PRODUCT_ID, "LOTE-A", VENCIMIENTO, 10),
                            tuple(SchemaSeed.OTRO_PRODUCT_ID, "LOTE-B", VENCIMIENTO, 4));
        }

        @Test
        @DisplayName("el save devuelve el id generado de la cabecera y el de cada linea")
        void el_save_devuelve_el_id_generado_de_la_cabecera_y_de_cada_linea() {
            // toDomainReusingRefs empareja lineas guardadas y originales por indice: si
            // el cascade no devolviera un id por linea, la recepcion recien creada saldria
            // con lineas sin identidad y el front no podria editarlas.
            GoodsReceipt guardado = guardar(EMPRESA, SEDE, PROVEEDOR, FECHA,
                    GoodsReceiptStatus.DRAFT, List.of(linea(VACUNA, "LOTE-A", 10, COSTO),
                            linea(JERINGA, "LOTE-B", 4, new BigDecimal("3.00"))));

            assertThat(guardado.getId()).isNotNull();
            assertThat(guardado.getVersion()).isZero();
            assertThat(guardado.getLines()).extracting(GoodsReceiptLine::getId).doesNotContainNull()
                    .doesNotHaveDuplicates();
            assertThat(guardado.getLines()).extracting(GoodsReceiptLine::getLotNumber)
                    .containsExactly("LOTE-A", "LOTE-B");
        }

        @Test
        @DisplayName("las lineas vuelven en el orden en que se guardaron")
        void las_lineas_vuelven_en_el_orden_en_que_se_guardaron() {
            // El mapper del camino de escritura casa savedLines.get(i) con
            // originalLines.get(i): si la coleccion volviera en otro orden, cada linea
            // recibiria el id de otra y el lote quedaria pegado al producto equivocado.
            GoodsReceipt guardado = guardar(EMPRESA, SEDE, PROVEEDOR, FECHA,
                    GoodsReceiptStatus.DRAFT, List.of(linea(VACUNA, "LOTE-A", 1, COSTO),
                            linea(JERINGA, "LOTE-B", 2, COSTO), linea(VACUNA, "LOTE-C", 3, COSTO)));
            releerDesdeLaBase();

            GoodsReceipt leido = repository
                    .findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID).orElseThrow();

            assertThat(leido.getLines()).extracting(GoodsReceiptLine::getLotNumber)
                    .containsExactly("LOTE-A", "LOTE-B", "LOTE-C");
        }

        @Test
        @DisplayName("el costo unitario sobrevive a DECIMAL(12,2) sin perder la parte entera")
        void el_costo_unitario_sobrevive_a_la_columna_decimal() {
            // 12 digitos con 2 decimales: un costo de importacion con ocho enteros cabe
            // justo. Si la columna encogiera, MySQL lo truncaria y la valuacion del
            // inventario quedaria por debajo de lo que se pago.
            GoodsReceipt guardado = guardar(EMPRESA, SEDE, PROVEEDOR, FECHA,
                    GoodsReceiptStatus.DRAFT,
                    List.of(linea(VACUNA, "LOTE-CARO", 1, new BigDecimal("87654321.99"))));
            releerDesdeLaBase();

            GoodsReceipt leido = repository
                    .findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID).orElseThrow();

            assertThat(leido.getLines()).hasSize(1);
            assertThat(leido.getLines().get(0).getUnitCost()).isEqualByComparingTo("87654321.99");
        }
    }

    @Nested
    @DisplayName("estado")
    class Estado {

        @ParameterizedTest
        @EnumSource(GoodsReceiptStatus.class)
        @DisplayName("cada estado se relee tal cual desde la columna VARCHAR")
        void cada_estado_se_relee_tal_cual(GoodsReceiptStatus estado) {
            // @Enumerated(STRING): si alguien lo pasara a ORDINAL, las filas ya escritas
            // se releerian como el estado equivocado y una recepcion cancelada volveria a
            // afectar el inventario.
            GoodsReceipt guardado = guardar(FECHA, estado);
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .map(GoodsReceipt::getStatus).contains(estado);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("una recepcion de otra empresa no se encuentra por id")
        void una_recepcion_de_otra_empresa_no_se_encuentra_por_id() {
            GoodsReceipt ajena = guardar(OTRA_EMPRESA, SEDE_AJENA, PROVEEDOR_AJENO, FECHA,
                    GoodsReceiptStatus.DRAFT, List.of(linea(VACUNA, "LOTE-X", 1, COSTO)));
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(ajena.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("el listado por empresa no mezcla las del tenant ajeno")
        void el_listado_por_empresa_no_mezcla_las_del_tenant_ajeno() {
            GoodsReceipt propia = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            guardar(OTRA_EMPRESA, SEDE_AJENA, PROVEEDOR_AJENO, FECHA, GoodsReceiptStatus.DRAFT,
                    List.of(linea(VACUNA, "LOTE-X", 1, COSTO)));
            releerDesdeLaBase();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID))
                    .extracting(GoodsReceipt::getId).containsExactly(propia.getId());
        }

        @Test
        @DisplayName("la busqueda paginada tampoco devuelve las del tenant ajeno")
        void la_busqueda_tampoco_devuelve_las_del_tenant_ajeno() {
            GoodsReceipt propia = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            guardar(OTRA_EMPRESA, SEDE_AJENA, PROVEEDOR_AJENO, FECHA, GoodsReceiptStatus.DRAFT,
                    List.of(linea(VACUNA, "LOTE-X", 1, COSTO)));
            releerDesdeLaBase();

            PageResult<GoodsReceipt> pagina = repository
                    .search(busqueda(null, null, null, null, null, 0, 20));

            assertThat(pagina.content()).extracting(GoodsReceipt::getId)
                    .containsExactly(propia.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("listado por empresa")
    class Listado {

        @Test
        @DisplayName("ordena por fecha descendente y desempata por id descendente")
        void ordena_por_fecha_descendente_y_desempata_por_id_descendente() {
            GoodsReceipt vieja = guardar(LocalDate.of(2026, 3, 1), GoodsReceiptStatus.DRAFT);
            GoodsReceipt primeraDelDia = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            GoodsReceipt segundaDelDia = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            releerDesdeLaBase();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID))
                    .extracting(GoodsReceipt::getId)
                    .containsExactly(segundaDelDia.getId(), primeraDelDia.getId(), vieja.getId());
        }
    }

    @Nested
    @DisplayName("busqueda paginada")
    class Busqueda {

        @Test
        @DisplayName("filtra por proveedor")
        void filtra_por_proveedor() {
            GoodsReceipt delProveedor = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            guardar(EMPRESA, SEDE, OTRO_PROVEEDOR, FECHA, GoodsReceiptStatus.DRAFT,
                    List.of(linea(VACUNA, "LOTE-B", 1, COSTO)));
            releerDesdeLaBase();

            assertThat(repository.search(busqueda(SUPPLIER_ID, null, null, null, null, 0, 20))
                    .content()).extracting(GoodsReceipt::getId)
                    .containsExactly(delProveedor.getId());
        }

        @Test
        @DisplayName("filtra por sede: el stock entra a una sede concreta")
        void filtra_por_sede() {
            GoodsReceipt enLaSede = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            guardar(EMPRESA, OTRA_SEDE, PROVEEDOR, FECHA, GoodsReceiptStatus.DRAFT,
                    List.of(linea(VACUNA, "LOTE-B", 1, COSTO)));
            releerDesdeLaBase();

            assertThat(
                    repository.search(busqueda(null, SchemaSeed.BRANCH_ID, null, null, null, 0, 20))
                            .content())
                    .extracting(GoodsReceipt::getId).containsExactly(enLaSede.getId());
        }

        @Test
        @DisplayName("filtra por estado")
        void filtra_por_estado() {
            guardar(FECHA, GoodsReceiptStatus.DRAFT);
            GoodsReceipt confirmada = guardar(FECHA, GoodsReceiptStatus.CONFIRMED);
            releerDesdeLaBase();

            assertThat(repository
                    .search(busqueda(null, null, GoodsReceiptStatus.CONFIRMED, null, null, 0, 20))
                    .content()).extracting(GoodsReceipt::getId).containsExactly(confirmada.getId());
        }

        @Test
        @DisplayName("el rango de fechas incluye sus dos extremos")
        void el_rango_de_fechas_incluye_sus_dos_extremos() {
            // El BETWEEN se arma con >= from y <= to: si alguno perdiera el "igual", el
            // primer y el ultimo dia del mes desaparecerian del informe de compras.
            GoodsReceipt primerDia = guardar(LocalDate.of(2026, 3, 1), GoodsReceiptStatus.DRAFT);
            GoodsReceipt ultimoDia = guardar(LocalDate.of(2026, 3, 31), GoodsReceiptStatus.DRAFT);
            guardar(LocalDate.of(2026, 4, 1), GoodsReceiptStatus.DRAFT);
            releerDesdeLaBase();

            assertThat(repository.search(busqueda(null, null, null, LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31), 0, 20)).content()).extracting(GoodsReceipt::getId)
                    .containsExactly(ultimoDia.getId(), primerDia.getId());
        }

        @Test
        @DisplayName("dos paginas consecutivas ni repiten ni omiten filas")
        void dos_paginas_consecutivas_ni_repiten_ni_omiten_filas() {
            // Las tres comparten fecha: sin el desempate por id el orden lo decidiria el
            // motor y la segunda pagina podria traer una fila que ya salio en la primera.
            GoodsReceipt primera = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            GoodsReceipt segunda = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            GoodsReceipt tercera = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            releerDesdeLaBase();

            PageResult<GoodsReceipt> pagina0 = repository
                    .search(busqueda(null, null, null, null, null, 0, 2));
            PageResult<GoodsReceipt> pagina1 = repository
                    .search(busqueda(null, null, null, null, null, 1, 2));

            assertThat(pagina0.content()).extracting(GoodsReceipt::getId)
                    .containsExactly(tercera.getId(), segunda.getId());
            assertThat(pagina1.content()).extracting(GoodsReceipt::getId)
                    .containsExactly(primera.getId());
            assertThat(pagina0.totalElements()).isEqualTo(3L);
            assertThat(pagina0.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("el total cuenta recepciones, no lineas")
        void el_total_cuenta_recepciones_no_lineas() {
            // La spec hace fetch-join de company/branch/supplier solo en la query de
            // datos: si ese fetch se colara en la de count, cada join multiplicaria el
            // total y el front pintaria paginas vacias al final del listado.
            guardar(EMPRESA, SEDE, PROVEEDOR, FECHA, GoodsReceiptStatus.DRAFT,
                    List.of(linea(VACUNA, "LOTE-A", 1, COSTO), linea(JERINGA, "LOTE-B", 2, COSTO)));
            guardar(EMPRESA, SEDE, PROVEEDOR, FECHA, GoodsReceiptStatus.DRAFT,
                    List.of(linea(VACUNA, "LOTE-C", 3, COSTO), linea(JERINGA, "LOTE-D", 4, COSTO)));
            releerDesdeLaBase();

            PageResult<GoodsReceipt> pagina = repository
                    .search(busqueda(null, null, null, null, null, 0, 20));

            assertThat(pagina.totalElements()).isEqualTo(2L);
            assertThat(pagina.content()).hasSize(2);
        }

        @Test
        @DisplayName("la busqueda trae las lineas de cada recepcion")
        void la_busqueda_trae_las_lineas_de_cada_recepcion() {
            // Las lineas quedan fuera del fetch-join para no romper la paginacion y se
            // hidratan LAZY al mapear: si esa carga se saliera de la transaccion, el
            // listado reventaria con LazyInitializationException en produccion.
            guardar(EMPRESA, SEDE, PROVEEDOR, FECHA, GoodsReceiptStatus.DRAFT,
                    List.of(linea(VACUNA, "LOTE-A", 1, COSTO), linea(JERINGA, "LOTE-B", 2, COSTO)));
            releerDesdeLaBase();

            PageResult<GoodsReceipt> pagina = repository
                    .search(busqueda(null, null, null, null, null, 0, 20));

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.content().get(0).getLines())
                    .extracting(GoodsReceiptLine::getLotNumber).containsExactly("LOTE-A", "LOTE-B");
        }
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("el borrado es logico: la fila deja de verse por id y en el listado")
        void el_borrado_es_logico_y_la_fila_deja_de_verse() {
            // @SQLDelete la marca enabled = false y @SQLRestriction la esconde: si
            // alguna de las dos se cayera, o se perderia el historial de compras o
            // seguirian apareciendo recepciones ya anuladas.
            GoodsReceipt guardada = guardar(FECHA, GoodsReceiptStatus.DRAFT);
            GoodsReceipt viva = guardar(FECHA, GoodsReceiptStatus.DRAFT);

            repository.delete(guardada.getId(), SchemaSeed.COMPANY_ID);
            releerDesdeLaBase();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID))
                    .extracting(GoodsReceipt::getId).containsExactly(viva.getId());
        }

        @Test
        @DisplayName("dar de baja conserva el detalle: las lineas siguen en la base")
        void dar_de_baja_conserva_el_detalle_de_la_recepcion() {
            // El bug: @SQLDelete sobre la cabecera + orphanRemoval sobre la coleccion. Al
            // dar de baja por em.remove, Hibernate emite el DELETE del detalle ANTES de
            // sustituir el DELETE de la raiz por el UPDATE, y dejaba una entrada
            // deshabilitada sin lineas: el stock recibido, los lotes y los costes se
            // perdian sin vuelta atras. El adaptador da la baja por un UPDATE nativo que
            // no despierta la maquina de cascadas.
            GoodsReceipt guardada = guardar(EMPRESA, SEDE, PROVEEDOR, FECHA,
                    GoodsReceiptStatus.DRAFT, List.of(linea(VACUNA, "LOTE-A", 10, COSTO),
                            linea(JERINGA, "LOTE-B", 4, new BigDecimal("3.00"))));
            releerDesdeLaBase();

            repository.delete(guardada.getId(), SchemaSeed.COMPANY_ID);
            releerDesdeLaBase();

            assertThat(filasDeLinea(guardada.getId())).isEqualTo(2L);
            assertThat(cabecerasDeshabilitadas(guardada.getId())).isEqualTo(1L);
        }

        @Test
        @DisplayName("dar de baja una recepcion no toca las lineas de las demas")
        void dar_de_baja_una_recepcion_no_toca_las_lineas_de_las_demas() {
            GoodsReceipt anulada = guardar(EMPRESA, SEDE, PROVEEDOR, FECHA,
                    GoodsReceiptStatus.DRAFT, List.of(linea(VACUNA, "LOTE-A", 10, COSTO)));
            GoodsReceipt viva = guardar(EMPRESA, SEDE, PROVEEDOR, FECHA, GoodsReceiptStatus.DRAFT,
                    List.of(linea(VACUNA, "LOTE-C", 2, COSTO), linea(JERINGA, "LOTE-D", 5, COSTO)));
            releerDesdeLaBase();

            repository.delete(anulada.getId(), SchemaSeed.COMPANY_ID);
            releerDesdeLaBase();

            assertThat(filasDeLinea(viva.getId())).isEqualTo(2L);
            assertThat(repository.findByIdAndCompanyId(viva.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
        }

        @Test
        @DisplayName("la recepcion de otra empresa no se da de baja: el UPDATE no toca ninguna fila")
        void la_recepcion_de_otra_empresa_no_se_da_de_baja() {
            // El AND company_id del softDelete es lo que se comprueba aqui: sin el, este
            // UPDATE por id a secas deshabilitaba la recepcion del otro tenant para quien
            // conociera el id, y la lectura previa del caso de uso no lo impide si alguien
            // reordena el servicio o llama al adaptador desde otro sitio.
            GoodsReceipt ajena = guardar(OTRA_EMPRESA, SEDE_AJENA, PROVEEDOR_AJENO, FECHA,
                    GoodsReceiptStatus.DRAFT, List.of(linea(VACUNA, "LOTE-X", 7, COSTO)));
            releerDesdeLaBase();

            repository.delete(ajena.getId(), SchemaSeed.COMPANY_ID);
            releerDesdeLaBase();

            assertThat(cabecerasDeshabilitadas(ajena.getId())).isZero();
            assertThat(repository.findByIdAndCompanyId(ajena.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isPresent();
        }
    }
}
