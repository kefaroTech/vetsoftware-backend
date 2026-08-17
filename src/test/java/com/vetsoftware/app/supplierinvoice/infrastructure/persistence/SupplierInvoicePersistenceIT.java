package com.vetsoftware.app.supplierinvoice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplierinvoice.application.command.SearchSupplierInvoicesCommand;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePayment;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import com.vetsoftware.app.supplierinvoice.testsupport.SupplierInvoiceMother;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de la cartera por pagar contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> Lo que sostiene esta feature es
 * dinero, y todo lo que puede descuadrarlo vive en el motor, no en Java:
 *
 * <ul>
 * <li>Los abonos cuelgan por {@code cascade} + {@code orphanRemoval} y el
 * {@code save} del adaptador <em>reconstruye</em> la entidad entera en cada
 * llamada. Que abonar una factura ya persistida actualice su fila en vez de
 * duplicar la coleccion solo lo demuestra Hibernate haciendo el merge de
 * verdad; un doble en memoria guarda la referencia viva y siempre da verde.
 * <li>Los importes son {@code DECIMAL(14,2)}: si un abono perdiera decimales al
 * ir y volver, la cartera quedaria descuadrada por centavos que nadie encuentra
 * despues.
 * <li>La unicidad del numero de factura no es un {@code if} de Java: es un
 * indice unico sobre una columna generada ({@code active_invoice_number}) y una
 * collation {@code ai_ci} que decide si «FAC-001» y «fac-001» son la misma
 * factura.
 * <li>El borrado logico es {@code @SQLDelete} + {@code @SQLRestriction}: SQL
 * que ningun test unitario ejecuta.
 * <li>El orden total del {@code search} (desempate por {@code id}) solo se
 * puede comprobar pidiendo dos paginas consecutivas a la base.
 * </ul>
 *
 * <p>
 * <b>Fechas fijas.</b> Nada de {@code now()}: la cartera se lee por
 * vencimiento, y un test anclado a hoy cambia de resultado al cruzar
 * medianoche.
 *
 * <p>
 * {@link SchemaSeed} no siembra proveedores y {@code supplier_invoices} tiene
 * FK a {@code suppliers}, asi que este test siembra los suyos por SQL nativo
 * con ids propios (960-963) que no chocan con los del seed comun.
 */
@Import({JpaSupplierInvoiceRepository.class, SupplierInvoiceJpaMapper.class})
@DisplayName("JpaSupplierInvoiceRepository — abonos, cartera y unicidad contra MySQL real")
class SupplierInvoicePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;

    private static final Long PROVEEDOR_ID = 960L;
    private static final Long OTRO_PROVEEDOR_ID = 961L;
    private static final Long PROVEEDOR_AJENO_ID = 962L;
    private static final Long BRANCH_AJENA_ID = 963L;

    private static final CompanyRef EMPRESA = new CompanyRef(COMPANY, "Veterinaria de prueba",
            "900123456");
    private static final CompanyRef EMPRESA_AJENA = new CompanyRef(OTRA_COMPANY,
            "Veterinaria ajena", "900654321");
    private static final BranchRef SEDE = new BranchRef(BRANCH, "Sede Centro");
    private static final BranchRef OTRA_SEDE = new BranchRef(OTRA_BRANCH, "Sede Norte");
    private static final BranchRef SEDE_AJENA = new BranchRef(BRANCH_AJENA_ID, "Sede ajena");
    private static final SupplierRef DISTRIBUIDORA = new SupplierRef(PROVEEDOR_ID,
            "Distribuidora Sur", "800111222");
    private static final SupplierRef INSUMOS = new SupplierRef(OTRO_PROVEEDOR_ID, "Insumos Norte",
            "800333444");
    private static final SupplierRef PROVEEDOR_DE_LA_OTRA = new SupplierRef(PROVEEDOR_AJENO_ID,
            "Proveedor ajeno", "800555666");

    private static final BigDecimal SUBTOTAL = new BigDecimal("1000000.00");
    private static final BigDecimal IMPUESTO = new BigDecimal("190000.00");
    private static final BigDecimal RETENCION = new BigDecimal("25000.00");
    /** subtotal + impuesto − retencion. */
    private static final BigDecimal POR_PAGAR = new BigDecimal("1165000.00");

    private static final LocalDate EMISION = LocalDate.of(2026, 1, 10);
    private static final LocalDate VENCE_FEBRERO = LocalDate.of(2026, 2, 9);

    @Autowired
    private JpaSupplierInvoiceRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        sedeDeLaOtraEmpresa();
        proveedor(PROVEEDOR_ID, "Distribuidora Sur", "800111222", COMPANY);
        proveedor(OTRO_PROVEEDOR_ID, "Insumos Norte", "800333444", COMPANY);
        proveedor(PROVEEDOR_AJENO_ID, "Proveedor ajeno", "800555666", OTRA_COMPANY);
        entityManager.flush();
        entityManager.clear();
    }

    private void proveedor(Long id, String nombre, String nit, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO suppliers (id, name, tax_id, company_id, created_date, version,
                                              enabled)
                VALUES (:id, :name, :taxId, :companyId, '2026-01-15 08:00:00', 0, true)
                """).setParameter("id", id).setParameter("name", nombre).setParameter("taxId", nit)
                .setParameter("companyId", companyId).executeUpdate();
    }

    private void sedeDeLaOtraEmpresa() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO branches (id, name, code, city_id, company_id)
                VALUES (:id, 'Sede ajena', 'AJENA', :cityId, :companyId)
                """).setParameter("id", BRANCH_AJENA_ID).setParameter("cityId", SchemaSeed.CITY_ID)
                .setParameter("companyId", OTRA_COMPANY).executeUpdate();
    }

    // --- helpers de siembra del agregado -------------------------------------

    private SupplierInvoice factura(String numero, LocalDate vencimiento) {
        return repository.save(SupplierInvoiceMother.nueva(EMPRESA, SEDE, DISTRIBUIDORA, numero,
                EMISION, vencimiento, SUBTOTAL, IMPUESTO, RETENCION));
    }

    private SupplierInvoice factura(BranchRef sede, SupplierRef proveedor, String numero,
            LocalDate emision, LocalDate vencimiento) {
        return repository.save(SupplierInvoiceMother.nueva(EMPRESA, sede, proveedor, numero,
                emision, vencimiento, SUBTOTAL, IMPUESTO, RETENCION));
    }

    private SupplierInvoice facturaConAbonos(String numero, LocalDate vencimiento,
            SupplierInvoiceStatus estado, List<SupplierInvoicePayment> abonos) {
        return repository.save(SupplierInvoiceMother.conEstado(EMPRESA, SEDE, DISTRIBUIDORA, numero,
                EMISION, vencimiento, SUBTOTAL, IMPUESTO, RETENCION, estado, abonos));
    }

    private SupplierInvoice facturaDeLaOtraEmpresa(String numero) {
        return repository
                .save(SupplierInvoiceMother.nueva(EMPRESA_AJENA, SEDE_AJENA, PROVEEDOR_DE_LA_OTRA,
                        numero, EMISION, VENCE_FEBRERO, SUBTOTAL, IMPUESTO, RETENCION));
    }

    /** Vacia el contexto de persistencia: obliga a que la lectura vaya a MySQL. */
    private SupplierInvoice releer(Long id) {
        entityManager.flush();
        entityManager.clear();
        return repository.findByIdAndCompanyId(id, COMPANY).orElseThrow();
    }

    /**
     * Cuenta los abonos con SQL nativo y sin pasar por ninguna entidad: contarlos
     * por el adaptador o por {@code SupplierInvoicePaymentJpaEntity} haria que un
     * cero pudiera significar «filtrados por {@code @SQLRestriction}» en vez de
     * «borrados de la base», y el test dejaria de distinguir el bug del cascade.
     */
    private long abonosEnLaBase(Long invoiceId) {
        entityManager.flush();
        Object total = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM supplier_invoice_payments WHERE supplier_invoice_id = :id
                """).setParameter("id", invoiceId).getSingleResult();
        return ((Number) total).longValue();
    }

    /** Cabecera ya dada de baja, contada tambien sin filtros de Hibernate. */
    private long facturasDeshabilitadas(Long invoiceId) {
        entityManager.flush();
        Object total = entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM supplier_invoices WHERE id = :id AND enabled = false
                """).setParameter("id", invoiceId).getSingleResult();
        return ((Number) total).longValue();
    }

    private PageResult<SupplierInvoice> buscar(SearchSupplierInvoicesCommand command) {
        entityManager.flush();
        entityManager.clear();
        return repository.search(command);
    }

    // --- casos ---------------------------------------------------------------

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer devuelve la factura con sus referencias hidratadas")
        void guardar_y_releer_devuelve_la_factura_completa() {
            SupplierInvoice guardada = factura("FAC-001", VENCE_FEBRERO);

            SupplierInvoice leida = releer(guardada.getId());

            assertThat(leida.getCompany()).isEqualTo(EMPRESA);
            assertThat(leida.getBranch()).isEqualTo(SEDE);
            assertThat(leida.getSupplier()).isEqualTo(DISTRIBUIDORA);
            assertThat(leida.getInvoiceNumber()).isEqualTo("FAC-001");
            assertThat(leida.getIssueDate()).isEqualTo(EMISION);
            assertThat(leida.getDueDate()).isEqualTo(VENCE_FEBRERO);
            assertThat(leida.getNotes()).isEqualTo("Compra de insumos");
            assertThat(leida.getCreatedBy()).isEqualTo(SupplierInvoiceMother.AUTOR);
            assertThat(leida.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("el estado viaja como texto y vuelve como el mismo enum")
        void el_estado_vuelve_como_el_mismo_enum() {
            SupplierInvoice anulada = facturaConAbonos("FAC-ANULADA", VENCE_FEBRERO,
                    SupplierInvoiceStatus.CANCELLED, List.of());

            assertThat(releer(anulada.getId()).getStatus())
                    .isEqualTo(SupplierInvoiceStatus.CANCELLED);
        }

        @Test
        @DisplayName("los importes conservan los dos decimales de la columna DECIMAL")
        void los_importes_conservan_los_dos_decimales() {
            // Redondear un centavo aqui descuadra la cartera contra el extracto del
            // proveedor, y el descuadre no aparece hasta la conciliacion.
            SupplierInvoice guardada = repository
                    .save(SupplierInvoiceMother.nueva(EMPRESA, SEDE, DISTRIBUIDORA, "FAC-CENTAVOS",
                            EMISION, VENCE_FEBRERO, new BigDecimal("1234567.89"),
                            new BigDecimal("234567.81"), new BigDecimal("12345.67")));

            SupplierInvoice leida = releer(guardada.getId());

            assertThat(leida.getSubtotal()).isEqualByComparingTo("1234567.89");
            assertThat(leida.getSubtotal().scale()).isEqualTo(2);
            assertThat(leida.getTaxAmount()).isEqualByComparingTo("234567.81");
            assertThat(leida.getWithholdingAmount()).isEqualByComparingTo("12345.67");
            assertThat(leida.getTotal()).isEqualByComparingTo("1469135.70");
            assertThat(leida.payableAmount()).isEqualByComparingTo("1456790.03");
        }

        @Test
        @DisplayName("el total calculado se persiste en su propia columna")
        void el_total_calculado_se_persiste_en_su_columna() {
            // El dominio deriva el total al reconstruirse, asi que la columna solo la ve
            // el libro de compras y los reportes SQL: si el write path dejara de
            // escribirla, ninguna lectura del agregado lo notaria.
            SupplierInvoice guardada = factura("FAC-TOTAL", VENCE_FEBRERO);
            entityManager.flush();

            Object total = entityManager
                    .createNativeQuery("SELECT total FROM supplier_invoices WHERE id = :id")
                    .setParameter("id", guardada.getId()).getSingleResult();

            assertThat((BigDecimal) total).isEqualByComparingTo("1190000.00");
        }
    }

    @Nested
    @DisplayName("abonos: lo que decide el saldo")
    class Abonos {

        @Test
        @DisplayName("la factura viaja con su coleccion de abonos y vuelve ordenada por id")
        void la_factura_viaja_con_sus_abonos() {
            SupplierInvoice guardada = facturaConAbonos("FAC-002", VENCE_FEBRERO,
                    SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("100000.00"),
                            LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER,
                            "TRF-1"),
                            SupplierInvoiceMother.abono(new BigDecimal("65000.00"),
                                    LocalDate.of(2026, 2, 5), SupplierInvoicePaymentMethod.CASH,
                                    null)));

            SupplierInvoice leida = releer(guardada.getId());

            assertThat(leida.getPayments()).extracting(SupplierInvoicePayment::getReference)
                    .containsExactly("TRF-1", null);
            assertThat(leida.getPayments()).extracting(SupplierInvoicePayment::getMethod)
                    .containsExactly(SupplierInvoicePaymentMethod.TRANSFER,
                            SupplierInvoicePaymentMethod.CASH);
            assertThat(leida.getPayments()).extracting(SupplierInvoicePayment::getPaymentDate)
                    .containsExactly(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 5));
            assertThat(leida.paidAmount()).isEqualByComparingTo("165000.00");
            assertThat(leida.balance()).isEqualByComparingTo("1000000.00");
        }

        @Test
        @DisplayName("abonar una factura ya persistida actualiza su fila y no duplica los abonos")
        void abonar_una_factura_persistida_no_duplica_los_abonos() {
            // El save reconstruye la entidad entera en cada llamada. Si el merge no
            // reconociera los abonos que ya tienen id, cada abono nuevo reinsertaria
            // todos los anteriores y el saldo se pagaria solo.
            Long id = factura("FAC-003", VENCE_FEBRERO).getId();

            SupplierInvoice primera = releer(id);
            primera.registerPayment(SupplierInvoiceMother.abono(new BigDecimal("165000.00"),
                    LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER, "TRF-1"),
                    SupplierInvoiceMother.AUTOR, primera.getVersion());
            repository.save(primera);

            SupplierInvoice segunda = releer(id);
            segunda.registerPayment(
                    SupplierInvoiceMother.abono(new BigDecimal("1000000.00"),
                            LocalDate.of(2026, 3, 1), SupplierInvoicePaymentMethod.CASH, null),
                    SupplierInvoiceMother.AUTOR, segunda.getVersion());
            repository.save(segunda);

            assertThat(abonosEnLaBase(id)).isEqualTo(2L);
            SupplierInvoice tercera = releer(id);
            assertThat(tercera.getId()).isEqualTo(id);
            assertThat(tercera.getPayments()).hasSize(2);
            assertThat(tercera.paidAmount()).isEqualByComparingTo(POR_PAGAR);
            assertThat(tercera.getStatus()).isEqualTo(SupplierInvoiceStatus.PAID);
            assertThat(tercera.balance()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("el abono conserva sus centavos al ir y volver")
        void el_abono_conserva_sus_centavos() {
            SupplierInvoice guardada = facturaConAbonos("FAC-004", VENCE_FEBRERO,
                    SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("333333.33"),
                            LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.CARD,
                            "POS-77")));

            SupplierInvoice leida = releer(guardada.getId());

            assertThat(leida.getPayments()).hasSize(1);
            assertThat(leida.getPayments().get(0).getAmount()).isEqualByComparingTo("333333.33");
            assertThat(leida.balance()).isEqualByComparingTo("831666.67");
        }

        @Test
        @DisplayName("el abono vuelve con su id generado, no nulo")
        void el_abono_vuelve_con_su_id_generado() {
            SupplierInvoice guardada = facturaConAbonos("FAC-005", VENCE_FEBRERO,
                    SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("165000.00"),
                            LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.OTHER, null)));

            assertThat(releer(guardada.getId()).getPayments())
                    .extracting(SupplierInvoicePayment::getId).doesNotContainNull();
        }
    }

    @Nested
    @DisplayName("findOutstandingByCompany — la cartera que alimenta el aging")
    class Cartera {

        private SupplierInvoice sembrarCartera() {
            facturaConAbonos("FAC-PAGADA", LocalDate.of(2026, 2, 15), SupplierInvoiceStatus.PAID,
                    List.of(SupplierInvoiceMother.abono(POR_PAGAR, LocalDate.of(2026, 2, 10),
                            SupplierInvoicePaymentMethod.TRANSFER, "TRF-PAGO")));
            facturaConAbonos("FAC-ANULADA", LocalDate.of(2026, 2, 20),
                    SupplierInvoiceStatus.CANCELLED, List.of());
            factura("FAC-MARZO", LocalDate.of(2026, 3, 31));
            return facturaConAbonos("FAC-ENERO", LocalDate.of(2026, 1, 31),
                    SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("165000.00"),
                            LocalDate.of(2026, 1, 20), SupplierInvoicePaymentMethod.CASH, null)));
        }

        @Test
        @DisplayName("solo devuelve las que tienen saldo, ordenadas por vencimiento")
        void solo_las_que_tienen_saldo_ordenadas_por_vencimiento() {
            sembrarCartera();
            entityManager.flush();
            entityManager.clear();

            List<SupplierInvoice> cartera = repository.findOutstandingByCompany(COMPANY, null);

            assertThat(cartera).extracting(SupplierInvoice::getInvoiceNumber)
                    .containsExactly("FAC-ENERO", "FAC-MARZO");
        }

        @Test
        @DisplayName("cada factura vuelve con su saldo real, no con el total")
        void cada_factura_vuelve_con_su_saldo_real() {
            // El aging reparte `balance()`, que sale de restar los abonos ya guardados: si
            // la coleccion no viniera hidratada, la cartera reportaria de mas.
            sembrarCartera();
            entityManager.flush();
            entityManager.clear();

            List<SupplierInvoice> cartera = repository.findOutstandingByCompany(COMPANY, null);

            assertThat(cartera.get(0).balance()).isEqualByComparingTo("1000000.00");
            assertThat(cartera.get(1).balance()).isEqualByComparingTo(POR_PAGAR);
        }

        @Test
        @DisplayName("filtrada por sede deja fuera la cartera de las demas sedes")
        void filtrada_por_sede_deja_fuera_las_demas() {
            factura("FAC-CENTRO", VENCE_FEBRERO);
            factura(OTRA_SEDE, DISTRIBUIDORA, "FAC-NORTE", EMISION, VENCE_FEBRERO);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findOutstandingByCompany(COMPANY, BRANCH))
                    .extracting(SupplierInvoice::getInvoiceNumber).containsExactly("FAC-CENTRO");
        }

        @Test
        @DisplayName("una factura borrada logicamente sale de la cartera")
        void una_factura_borrada_sale_de_la_cartera() {
            Long id = factura("FAC-BAJA", VENCE_FEBRERO).getId();
            factura("FAC-VIVA", VENCE_FEBRERO);

            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findOutstandingByCompany(COMPANY, null))
                    .extracting(SupplierInvoice::getInvoiceNumber).containsExactly("FAC-VIVA");
        }
    }

    @Nested
    @DisplayName("numero de factura: la red contra el doble registro")
    class NumeroDeFactura {

        @Test
        @DisplayName("detecta el numero repetido del mismo proveedor")
        void detecta_el_numero_repetido_del_mismo_proveedor() {
            factura("FAC-100", VENCE_FEBRERO);
            entityManager.flush();

            assertThat(
                    repository.existsByCompanySupplierAndNumber(COMPANY, PROVEEDOR_ID, "FAC-100"))
                    .isTrue();
        }

        @Test
        @DisplayName("la collation de MySQL ignora mayusculas: fac-100 ya existe")
        void la_collation_ignora_mayusculas() {
            // utf8mb4_0900_ai_ci: la comparacion es case-insensitive, asi que registrar
            // «fac-100» tras «FAC-100» se detecta como duplicado. Cambiar la collation de
            // la columna abriria la puerta al doble pago sin tocar una linea de Java.
            factura("FAC-100", VENCE_FEBRERO);
            entityManager.flush();

            assertThat(
                    repository.existsByCompanySupplierAndNumber(COMPANY, PROVEEDOR_ID, "fac-100"))
                    .isTrue();
        }

        @Test
        @DisplayName("el mismo numero es valido para otro proveedor de la misma empresa")
        void el_mismo_numero_vale_para_otro_proveedor() {
            factura("FAC-100", VENCE_FEBRERO);
            entityManager.flush();

            assertThat(repository.existsByCompanySupplierAndNumber(COMPANY, OTRO_PROVEEDOR_ID,
                    "FAC-100")).isFalse();
        }

        @Test
        @DisplayName("el mismo numero es valido para otra empresa")
        void el_mismo_numero_vale_para_otra_empresa() {
            factura("FAC-100", VENCE_FEBRERO);
            entityManager.flush();

            assertThat(repository.existsByCompanySupplierAndNumber(OTRA_COMPANY, PROVEEDOR_AJENO_ID,
                    "FAC-100")).isFalse();
        }

        @Test
        @DisplayName("borrar la factura libera su numero para volver a registrarla")
        void borrar_la_factura_libera_su_numero() {
            // El indice unico cuelga de una columna generada que solo vale mientras
            // enabled = TRUE: sin eso, corregir una factura mal capturada dejaria su
            // numero quemado para siempre.
            Long id = factura("FAC-100", VENCE_FEBRERO).getId();

            repository.delete(id);
            entityManager.flush();

            assertThat(
                    repository.existsByCompanySupplierAndNumber(COMPANY, PROVEEDOR_ID, "FAC-100"))
                    .isFalse();
            assertThat(factura("FAC-100", VENCE_FEBRERO).getId()).isNotNull();
        }

        @Test
        @DisplayName("la base rechaza el duplicado aunque el caso de uso no lo mire")
        void la_base_rechaza_el_duplicado() {
            factura("FAC-100", VENCE_FEBRERO);

            assertThatThrownBy(() -> factura("FAC-100", VENCE_FEBRERO))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("Duplicate entry");
        }
    }

    @Nested
    @DisplayName("search — paginacion con orden total y filtros")
    class Busqueda {

        private SearchSupplierInvoicesCommand pagina(int page, int pageSize) {
            return new SearchSupplierInvoicesCommand(COMPANY, null, null, null, null, null, page,
                    pageSize);
        }

        private void tresFacturasElMismoDia() {
            factura("FAC-A", VENCE_FEBRERO);
            factura("FAC-B", VENCE_FEBRERO);
            factura("FAC-C", VENCE_FEBRERO);
        }

        @Test
        @DisplayName("dos paginas consecutivas no repiten ni omiten filas")
        void dos_paginas_consecutivas_no_repiten_ni_omiten() {
            // Las tres comparten fecha de emision: sin el desempate por id el ORDER BY no
            // es total y MySQL puede devolver la misma fila en las dos paginas.
            tresFacturasElMismoDia();

            PageResult<SupplierInvoice> primera = buscar(pagina(0, 2));
            PageResult<SupplierInvoice> segunda = buscar(pagina(1, 2));

            assertThat(primera.content()).extracting(SupplierInvoice::getInvoiceNumber)
                    .containsExactly("FAC-C", "FAC-B");
            assertThat(segunda.content()).extracting(SupplierInvoice::getInvoiceNumber)
                    .containsExactly("FAC-A");
        }

        @Test
        @DisplayName("los metadatos son los de la consulta, no los de la pagina")
        void los_metadatos_son_los_de_la_consulta() {
            tresFacturasElMismoDia();

            PageResult<SupplierInvoice> primera = buscar(pagina(0, 2));

            assertThat(primera.page()).isZero();
            assertThat(primera.pageSize()).isEqualTo(2);
            assertThat(primera.totalElements()).isEqualTo(3L);
            assertThat(primera.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("ordena por emision descendente antes que por id")
        void ordena_por_emision_descendente() {
            factura(SEDE, DISTRIBUIDORA, "FAC-VIEJA", LocalDate.of(2026, 1, 1), VENCE_FEBRERO);
            factura(SEDE, DISTRIBUIDORA, "FAC-NUEVA", LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 4, 1));

            assertThat(buscar(pagina(0, 20)).content())
                    .extracting(SupplierInvoice::getInvoiceNumber)
                    .containsExactly("FAC-NUEVA", "FAC-VIEJA");
        }

        @Test
        @DisplayName("filtra por proveedor, sede, estado y rango de emision a la vez")
        void filtra_por_todos_los_criterios_a_la_vez() {
            factura(SEDE, DISTRIBUIDORA, "FAC-BUSCADA", LocalDate.of(2026, 2, 10),
                    LocalDate.of(2026, 3, 10));
            factura(SEDE, INSUMOS, "FAC-OTRO-PROVEEDOR", LocalDate.of(2026, 2, 10),
                    LocalDate.of(2026, 3, 10));
            factura(OTRA_SEDE, DISTRIBUIDORA, "FAC-OTRA-SEDE", LocalDate.of(2026, 2, 10),
                    LocalDate.of(2026, 3, 10));
            factura(SEDE, DISTRIBUIDORA, "FAC-FUERA-DE-RANGO", LocalDate.of(2026, 5, 10),
                    LocalDate.of(2026, 6, 10));
            facturaConAbonos("FAC-ANULADA", VENCE_FEBRERO, SupplierInvoiceStatus.CANCELLED,
                    List.of());

            PageResult<SupplierInvoice> resultado = buscar(new SearchSupplierInvoicesCommand(
                    COMPANY, PROVEEDOR_ID, BRANCH, SupplierInvoiceStatus.PENDING,
                    LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), 0, 20));

            assertThat(resultado.content()).extracting(SupplierInvoice::getInvoiceNumber)
                    .containsExactly("FAC-BUSCADA");
            assertThat(resultado.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("cada fila de la pagina llega con sus abonos hidratados")
        void cada_fila_llega_con_sus_abonos() {
            // El spec hace fetch-join solo de los @ManyToOne para no romper la paginacion;
            // los abonos se cargan LAZY al mapear. Si esa carga se perdiera, el listado
            // mostraria saldo completo en facturas ya abonadas.
            facturaConAbonos("FAC-CON-ABONO", VENCE_FEBRERO, SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("165000.00"),
                            LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER,
                            "TRF-1")));

            PageResult<SupplierInvoice> resultado = buscar(pagina(0, 20));

            assertThat(resultado.content()).hasSize(1);
            assertThat(resultado.content().get(0).balance()).isEqualByComparingTo("1000000.00");
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("findByIdAndCompanyId no devuelve la factura de otra empresa")
        void no_devuelve_la_factura_de_otra_empresa() {
            Long ajena = facturaDeLaOtraEmpresa("FAC-AJENA").getId();
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(ajena, COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajena, OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el search de una empresa nunca ve las facturas de la otra")
        void el_search_nunca_ve_las_de_la_otra() {
            facturaDeLaOtraEmpresa("FAC-AJENA");
            factura("FAC-PROPIA", VENCE_FEBRERO);

            assertThat(buscar(
                    new SearchSupplierInvoicesCommand(COMPANY, null, null, null, null, null, 0, 20))
                    .content()).extracting(SupplierInvoice::getInvoiceNumber)
                    .containsExactly("FAC-PROPIA");
        }

        @Test
        @DisplayName("la cartera de una empresa nunca suma la de la otra")
        void la_cartera_nunca_suma_la_de_la_otra() {
            facturaDeLaOtraEmpresa("FAC-AJENA");
            factura("FAC-PROPIA", VENCE_FEBRERO);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findOutstandingByCompany(COMPANY, null))
                    .extracting(SupplierInvoice::getInvoiceNumber).containsExactly("FAC-PROPIA");
        }
    }

    @Nested
    @DisplayName("borrado logico")
    class BorradoLogico {

        @Test
        @DisplayName("delete no borra la fila: la marca deshabilitada y la esconde")
        void delete_marca_la_fila_deshabilitada() {
            // @SQLDelete + @SQLRestriction: la fila sigue en la base para la auditoria,
            // pero ninguna consulta del adaptador vuelve a verla.
            Long id = factura("FAC-BAJA", VENCE_FEBRERO).getId();

            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            Object filas = entityManager.createNativeQuery("""
                    SELECT COUNT(*) FROM supplier_invoices
                     WHERE id = :id AND enabled = false
                    """).setParameter("id", id).getSingleResult();
            assertThat(((Number) filas).longValue()).isEqualTo(1L);
            assertThat(repository.findByIdAndCompanyId(id, COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("dar de baja conserva el detalle: los abonos registrados siguen en la base")
        void dar_de_baja_conserva_los_abonos() {
            // El bug: @SQLDelete sobre la factura + orphanRemoval sobre los abonos. Al
            // darla de baja por em.remove, Hibernate emite el DELETE de
            // supplier_invoice_payments ANTES de sustituir el DELETE de la raiz por el
            // UPDATE, y dejaba una factura deshabilitada sin rastro de lo ya pagado: el
            // proveedor reclama un pago que la base ya no puede demostrar. El adaptador da
            // la baja por un UPDATE nativo que no despierta la maquina de cascadas.
            Long id = facturaConAbonos("FAC-BAJA-CON-ABONOS", VENCE_FEBRERO,
                    SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("100000.00"),
                            LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER,
                            "TRF-1"),
                            SupplierInvoiceMother.abono(new BigDecimal("65000.00"),
                                    LocalDate.of(2026, 2, 5), SupplierInvoicePaymentMethod.CASH,
                                    null)))
                    .getId();
            entityManager.flush();
            entityManager.clear();

            repository.delete(id);
            entityManager.flush();
            entityManager.clear();

            assertThat(abonosEnLaBase(id)).isEqualTo(2L);
            assertThat(facturasDeshabilitadas(id)).isEqualTo(1L);
        }

        @Test
        @DisplayName("dar de baja una factura no toca los abonos de las demas")
        void dar_de_baja_una_factura_no_toca_los_abonos_de_las_demas() {
            Long anulada = facturaConAbonos("FAC-ANULADA-CON-ABONO", VENCE_FEBRERO,
                    SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("100000.00"),
                            LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER,
                            "TRF-1")))
                    .getId();
            Long viva = facturaConAbonos("FAC-VIVA-CON-ABONO", VENCE_FEBRERO,
                    SupplierInvoiceStatus.PARTIAL,
                    List.of(SupplierInvoiceMother.abono(new BigDecimal("50000.00"),
                            LocalDate.of(2026, 2, 2), SupplierInvoicePaymentMethod.CASH, null)))
                    .getId();
            entityManager.flush();
            entityManager.clear();

            repository.delete(anulada);
            entityManager.flush();
            entityManager.clear();

            assertThat(abonosEnLaBase(viva)).isEqualTo(1L);
            assertThat(releer(viva).paidAmount()).isEqualByComparingTo("50000.00");
        }
    }
}
