package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.FiscalResponsibility;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxRegime;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del documento electronico contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve.</b> Casi todo lo que este adaptador garantiza
 * vive fuera de Java:
 *
 * <ul>
 * <li>La <b>idempotencia fiscal</b> no la sostiene el {@code exists...} del
 * service —eso es check-then-act, con su ventana de carrera— sino dos indices
 * unicos de la base: {@code uq_electronic_documents_open_account} (un documento
 * por cuenta cerrada) y {@code uq_electronic_documents_request} (company +
 * client_request_id). Un doble en memoria acepta los duplicados sin rechistar,
 * que es exactamente el escenario que emite dos veces a la DIAN y quema dos
 * consecutivos.</li>
 * <li>Ambos indices dependen de que <b>MySQL admita varios NULL</b> en una
 * columna unica: si no fuera asi, la segunda venta POS (sin cuenta y sin key)
 * seria imposible. Eso no se puede afirmar sin el motor.</li>
 * <li>{@code updateDianResult} promete tocar <b>solo</b> las columnas del ciclo
 * DIAN y no reescribir lineas ni pagos. Con un doble que guarda la referencia
 * viva, esa promesa siempre se cumple; contra Hibernate, cambiarla por un
 * {@code mapper.toJpa} duplicaria el detalle fiscal.</li>
 * <li>Las responsabilidades del emisor viajan como <b>una sola columna unida
 * por {@code ';'}</b> y el regimen del adquiriente como texto libre parseado de
 * vuelta: son listas y enums que no existen como tales en la tabla.</li>
 * <li>El listado pagina en <b>dos consultas</b> (ids y luego hidratacion) justo
 * para que el {@code LIMIT} sea real con dos colecciones colgando. En memoria
 * la paginacion siempre parece correcta.</li>
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaElectronicDocumentRepository — inmutabilidad, idempotencia y detalle contra MySQL real")
class ElectronicDocumentPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long EMPLEADO = SchemaSeed.EMPLOYEE_ID;

    /**
     * Cuentas cerradas propias de esta rodaja, en el rango 960+ para no chocar con
     * los ids 900-951 de {@link SchemaSeed}, que no siembra {@code open_accounts}.
     *
     * <p>
     * Hacen falta porque {@code open_account_id} tiene FK real
     * ({@code fk_electronic_documents_open_account} → {@code open_accounts(id)}):
     * con una cuenta inventada la base rechaza el INSERT por la FK y el documento
     * nunca llega al indice unico {@code uq_electronic_documents_open_account}, que
     * es justo lo que estos casos quieren afirmar. La cuenta es del rango alto y
     * esta en estado {@code CLOSE} porque un documento electronico se emite
     * congelando una cuenta ya cerrada.
     */
    private static final Long OWNER = 960L;
    private static final Long CUENTA_CONSULTADA = 961L;
    private static final Long CUENTA_CON_POS = 962L;
    private static final Long CUENTA_SIN_DOCUMENTO = 963L;
    private static final Long CUENTA_DISPUTADA = 964L;

    private static final LocalDate EMISION = LocalDate.of(2026, 1, 15);
    private static final String HORA = "10:15:00-05:00";
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 15, 0);

    private static final IssuerSnapshot EMISOR = new IssuerSnapshot("NIT", "900123456", "7",
            "Veterinaria de prueba S.A.S.", "RESPONSABLE_IVA", "facturacion@test.local",
            List.of("O-13", "O-15"));

    private static final CustomerSnapshot ADQUIRIENTE = new CustomerSnapshot("CEDULA_CIUDADANIA",
            "1020304050", "3", "NATURAL", "Ana Ruiz", "Ana Ruiz", "ana@test.local", "05001",
            TaxRegime.NO_RESPONSABLE_IVA, FiscalResponsibility.NO_APLICA);

    /** Duracion del lease de los jobs DIAN; su valor exacto da igual aqui. */
    private static final Duration LEASE = Duration.ofMinutes(5);

    @Autowired
    private JpaElectronicDocumentRepository repository;

    @Autowired
    private JdbcDianJobLeasePort leasePort;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        propietario();
        cuentaCerrada(CUENTA_CONSULTADA);
        cuentaCerrada(CUENTA_CON_POS);
        cuentaCerrada(CUENTA_SIN_DOCUMENTO);
        cuentaCerrada(CUENTA_DISPUTADA);
        entityManager.flush();
    }

    /** Dueño de las cuentas: {@code open_accounts.owner_id} es NOT NULL con FK. */
    private void propietario() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO owners (id, name, document, document_type, person_type,
                                           withholding_agent, tax_regime, fiscal_responsibility,
                                           city_id, company_id, created_date, enabled)
                VALUES (:id, 'Ana Ruiz', 'CC-960', 'CEDULA_CIUDADANIA', 'NATURAL', false,
                        'NO_RESPONSABLE_IVA', 'NO_APLICA', :ciudad, :empresa,
                        '2026-01-01 08:00:00', true)
                """).setParameter("id", OWNER).setParameter("ciudad", SchemaSeed.CITY_ID)
                .setParameter("empresa", COMPANY).executeUpdate();
    }

    private void cuentaCerrada(Long id) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO open_accounts (id, total_amount, paid_amount,
                                                  outstanding_amount, owner_id, company_id,
                                                  branch_id, created_by_id, created_date, enabled,
                                                  status, closed_by_id, closed_at, reversed,
                                                  version)
                VALUES (:id, 119000.00, 119000.00, 0.00, :owner, :empresa, :sede, :creador,
                        '2026-01-15 09:00:00', true, 'CLOSE', :cerradoPor,
                        '2026-01-15 10:00:00', false, 0)
                """).setParameter("id", id).setParameter("owner", OWNER)
                .setParameter("empresa", COMPANY).setParameter("sede", BRANCH)
                .setParameter("creador", EMPLEADO).setParameter("cerradoPor", EMPLEADO)
                .executeUpdate();
    }

    private static ElectronicDocumentLine linea(int numero, String descripcion, String base,
            String iva, String total) {
        return new ElectronicDocumentLine(null, numero, descripcion, new BigDecimal("1.00"), "94",
                new BigDecimal(base), new BigDecimal(base), TaxCategory.GRAVADO, TaxScheme.IVA,
                new BigDecimal("19.00"), new BigDecimal(iva), new BigDecimal(total));
    }

    private static ElectronicDocumentLine lineaConsulta() {
        return linea(1, "Consulta general", "100000.00", "19000.00", "119000.00");
    }

    private ElectronicDocument documento(Long companyId, Long branchId, Long openAccountId,
            String clientRequestId, DianStatus estado, ElectronicDocumentType tipo,
            List<ElectronicDocumentLine> lineas, List<ElectronicDocumentPayment> pagos) {
        return repository.save(new ElectronicDocument(null, companyId, openAccountId, tipo, null,
                null, null, EMISION, HORA, null, null, null, null, null, null, null, estado, null,
                EMISOR, ADQUIRIENTE, new BigDecimal("100000.00"), new BigDecimal("100000.00"),
                new BigDecimal("119000.00"), new BigDecimal("119000.00"), PaymentForm.CONTADO,
                lineas, pagos, CREADO, null, true, null, null, null, false, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, clientRequestId, EMPLEADO, branchId));
    }

    private ElectronicDocument ventaPos() {
        return documento(COMPANY, BRANCH, null, null, DianStatus.PENDIENTE,
                ElectronicDocumentType.DOC_EQUIV_POS, List.of(lineaConsulta()),
                List.of(new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO,
                        new BigDecimal("119000.00"))));
    }

    @Nested
    @DisplayName("ida y vuelta del documento")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y la cabecera fiscal vuelve entera")
        void guardar_asigna_id_y_la_cabecera_vuelve_entera() {
            ElectronicDocument guardado = ventaPos();

            assertThat(guardado.getId()).isNotNull();

            ElectronicDocument leido = repository.findByIdAndCompanyId(guardado.getId(), COMPANY)
                    .orElseThrow();
            assertThat(leido.getDocumentType()).isEqualTo(ElectronicDocumentType.DOC_EQUIV_POS);
            assertThat(leido.getDianStatus()).isEqualTo(DianStatus.PENDIENTE);
            assertThat(leido.getIssueDate()).isEqualTo(EMISION);
            assertThat(leido.getIssueTime()).isEqualTo(HORA);
            assertThat(leido.getBranchId()).isEqualTo(BRANCH);
            assertThat(leido.getIssuedByEmployeeId()).isEqualTo(EMPLEADO);
            assertThat(leido.getPayableAmount()).isEqualByComparingTo("119000.00");
            assertThat(leido.getPaymentForm()).isEqualTo(PaymentForm.CONTADO);
        }

        @Test
        @DisplayName("las responsabilidades del emisor viajan unidas por ';' y vuelven como lista")
        void las_responsabilidades_del_emisor_vuelven_como_lista() {
            ElectronicDocument guardado = ventaPos();
            entityManager.flush();
            entityManager.clear();

            // En la tabla es UNA columna de texto. Que vuelva a ser una lista de dos
            // codigos depende del join/split del mapper, no de ningun tipo Java.
            Object columna = entityManager
                    .createNativeQuery(
                            "SELECT issuer_responsibilities FROM electronic_documents WHERE id = ?")
                    .setParameter(1, guardado.getId()).getSingleResult();
            assertThat(columna).isEqualTo("O-13;O-15");

            assertThat(repository.findById(guardado.getId()).orElseThrow().getIssuer()
                    .responsibilities()).containsExactly("O-13", "O-15");
        }

        @Test
        @DisplayName("el regimen y la responsabilidad del adquiriente vuelven como enum")
        void el_regimen_del_adquiriente_vuelve_como_enum() {
            ElectronicDocument guardado = ventaPos();

            CustomerSnapshot leido = repository.findById(guardado.getId()).orElseThrow()
                    .getCustomer();

            // Se persisten como texto libre y se reparsean con fromName: si la columna
            // guardara otra cosa, volverian null en silencio.
            assertThat(leido.taxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
            assertThat(leido.fiscalResponsibility()).isEqualTo(FiscalResponsibility.NO_APLICA);
            assertThat(leido.cityDaneCode()).isEqualTo("05001");
        }

        @Test
        @DisplayName("las lineas vuelven ordenadas por numero, se guarden como se guarden")
        void las_lineas_vuelven_ordenadas_por_numero() {
            ElectronicDocument guardado = documento(COMPANY, BRANCH, null, null,
                    DianStatus.PENDIENTE, ElectronicDocumentType.DOC_EQUIV_POS,
                    List.of(linea(3, "Tercera", "30000.00", "5700.00", "35700.00"),
                            linea(1, "Primera", "10000.00", "1900.00", "11900.00"),
                            linea(2, "Segunda", "20000.00", "3800.00", "23800.00")),
                    List.of());

            List<ElectronicDocumentLine> lineas = repository.findById(guardado.getId())
                    .orElseThrow().getLines();

            // La coleccion es un Set y la base no promete orden: el orden fiscal lo
            // impone el mapper al leer. Sin el, el UBL saldria con las lineas barajadas.
            assertThat(lineas).extracting(ElectronicDocumentLine::getLineNumber).containsExactly(1,
                    2, 3);
            assertThat(lineas.getFirst().getDescription()).isEqualTo("Primera");
            assertThat(lineas.getFirst().getId()).as("la base le asigna id a cada linea")
                    .isNotNull();
        }

        @Test
        @DisplayName("los pagos vuelven con su medio y su monto")
        void los_pagos_vuelven_con_su_medio_y_su_monto() {
            ElectronicDocument guardado = documento(COMPANY, BRANCH, null, null,
                    DianStatus.PENDIENTE, ElectronicDocumentType.DOC_EQUIV_POS,
                    List.of(lineaConsulta()),
                    List.of(new ElectronicDocumentPayment(null, PaymentMeans.TARJETA_DEBITO,
                            new BigDecimal("100000.00")),
                            new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO,
                                    new BigDecimal("19000.00"))));

            assertThat(repository.findById(guardado.getId()).orElseThrow().getPayments())
                    .extracting(ElectronicDocumentPayment::getPaymentMeans)
                    .containsExactlyInAnyOrder(PaymentMeans.TARJETA_DEBITO, PaymentMeans.EFECTIVO);
        }

        @Test
        @DisplayName("traer lineas y pagos a la vez no multiplica el documento")
        void traer_lineas_y_pagos_a_la_vez_no_multiplica_el_documento() {
            documento(COMPANY, BRANCH, null, null, DianStatus.CONTINGENCIA,
                    ElectronicDocumentType.FE_VENTA,
                    List.of(linea(1, "Primera", "50000.00", "9500.00", "59500.00"),
                            linea(2, "Segunda", "50000.00", "9500.00", "59500.00")),
                    List.of(new ElectronicDocumentPayment(null, PaymentMeans.EFECTIVO,
                            new BigDecimal("60000.00")),
                            new ElectronicDocumentPayment(null, PaymentMeans.TRANSFERENCIA,
                                    new BigDecimal("59000.00"))));

            List<ElectronicDocument> enContingencia = repository
                    .findByDianStatus(DianStatus.CONTINGENCIA);

            // Dos colecciones en el mismo grafo son un producto cartesiano en SQL: 2x2 = 4
            // filas para UN documento. Si se colara al job de reintento, cada documento se
            // retransmitiria cuatro veces a la DIAN.
            assertThat(enContingencia).hasSize(1);
            assertThat(enContingencia.getFirst().getLines()).hasSize(2);
            assertThat(enContingencia.getFirst().getPayments()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("un documento de otra empresa no se lee por id")
        void un_documento_de_otra_empresa_no_se_lee_por_id() {
            ElectronicDocument guardado = ventaPos();

            // Sin el scope, adivinar un id daria acceso a la facturacion de otro tenant.
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), OTRA_COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el CUFE se busca dentro de la empresa, no globalmente")
        void el_cufe_se_busca_dentro_de_la_empresa() {
            ElectronicDocument guardado = ventaPos();
            guardado.markValidated(null, null, "CUFE-AJENO-001", null, null, null, null, null, null,
                    LocalDateTime.of(2026, 1, 15, 11, 0, 0));
            repository.updateDianResult(guardado);

            assertThat(repository.findByCufe("CUFE-AJENO-001", COMPANY)).isPresent();
            assertThat(repository.findByCufe("CUFE-AJENO-001", OTRA_COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("la cuenta cerrada tambien se consulta dentro de la empresa")
        void la_cuenta_cerrada_se_consulta_dentro_de_la_empresa() {
            documento(COMPANY, BRANCH, CUENTA_CONSULTADA, null, DianStatus.PENDIENTE,
                    ElectronicDocumentType.FE_VENTA, List.of(lineaConsulta()), List.of());

            assertThat(repository.findByOpenAccountId(CUENTA_CONSULTADA, COMPANY)).isPresent();
            assertThat(repository.findByOpenAccountId(CUENTA_CONSULTADA, OTRA_COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("idempotencia que sostiene la base, no el service")
    class Idempotencia {

        @Test
        @DisplayName("reconoce que la cuenta ya tiene documento, y de que tipo")
        void reconoce_que_la_cuenta_ya_tiene_documento() {
            documento(COMPANY, BRANCH, CUENTA_CON_POS, null, DianStatus.PENDIENTE,
                    ElectronicDocumentType.DOC_EQUIV_POS, List.of(lineaConsulta()), List.of());

            assertThat(repository.existsByOpenAccountId(CUENTA_CON_POS)).isTrue();
            // Cuenta real y cerrada igual que la anterior, pero sin documento: lo que
            // distingue a las dos es el documento, no que una de ellas no exista.
            assertThat(repository.existsByOpenAccountId(CUENTA_SIN_DOCUMENTO)).isFalse();
            // La conversion POS->FE se guarda de convertir dos veces filtrando por TIPO:
            // la cuenta ya tiene su POS, lo que no debe existir aun es la FE_VENTA.
            assertThat(repository.existsByOpenAccountIdAndDocumentType(CUENTA_CON_POS,
                    ElectronicDocumentType.DOC_EQUIV_POS)).isTrue();
            assertThat(repository.existsByOpenAccountIdAndDocumentType(CUENTA_CON_POS,
                    ElectronicDocumentType.FE_VENTA)).isFalse();
        }

        @Test
        @DisplayName("recupera la venta ya registrada por su client_request_id")
        void recupera_la_venta_por_su_client_request_id() {
            ElectronicDocument guardado = documento(COMPANY, BRANCH, null, "req-abc-123",
                    DianStatus.PENDIENTE, ElectronicDocumentType.DOC_EQUIV_POS,
                    List.of(lineaConsulta()), List.of());

            assertThat(repository.findByCompanyIdAndClientRequestId(COMPANY, "req-abc-123"))
                    .map(ElectronicDocument::getId).contains(guardado.getId());
            assertThat(repository.findByCompanyIdAndClientRequestId(OTRA_COMPANY, "req-abc-123"))
                    .as("la key es de la empresa, no global").isEmpty();
        }

        @Test
        @DisplayName("dos ventas POS sin cuenta ni key conviven: el unico admite varios NULL")
        void dos_ventas_pos_sin_cuenta_ni_key_conviven() {
            ventaPos();
            ventaPos();

            // Si MySQL deduplicase los NULL, la segunda venta del dia seria imposible.
            assertThat(
                    repository.findAllByCompanyId(COMPANY, null, null, null, 0, 20).totalElements())
                    .isEqualTo(2L);
        }

        @Test
        @DisplayName("la misma key en OTRA empresa si se admite")
        void la_misma_key_en_otra_empresa_si_se_admite() {
            documento(COMPANY, BRANCH, null, "req-compartida", DianStatus.PENDIENTE,
                    ElectronicDocumentType.DOC_EQUIV_POS, List.of(lineaConsulta()), List.of());

            // El indice unico es (company_id, client_request_id): dos tenants pueden
            // generar el mismo UUID sin bloquearse. La sede es la sembrada porque la FK
            // solo exige que exista, no que sea de la misma empresa.
            ElectronicDocument ajeno = documento(OTRA_COMPANY, BRANCH, null, "req-compartida",
                    DianStatus.PENDIENTE, ElectronicDocumentType.DOC_EQUIV_POS,
                    List.of(lineaConsulta()), List.of());

            assertThat(ajeno.getId()).isNotNull();
        }

        @Test
        @DisplayName("un segundo documento para la MISMA cuenta lo rechaza la base")
        void un_segundo_documento_para_la_misma_cuenta_lo_rechaza_la_base() {
            documento(COMPANY, BRANCH, CUENTA_DISPUTADA, null, DianStatus.PENDIENTE,
                    ElectronicDocumentType.FE_VENTA, List.of(lineaConsulta()), List.of());

            // El check del service es check-then-act: dos cierres concurrentes pasan los
            // dos. El indice unico es lo unico que impide emitir dos documentos fiscales
            // (y quemar dos consecutivos) por la misma venta. La cuenta existe de verdad,
            // asi que lo que rechaza el segundo INSERT es
            // uq_electronic_documents_open_account y no la FK a open_accounts.
            assertThatThrownBy(
                    () -> documento(COMPANY, BRANCH, CUENTA_DISPUTADA, null, DianStatus.PENDIENTE,
                            ElectronicDocumentType.FE_VENTA, List.of(lineaConsulta()), List.of()))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uq_electronic_documents_open_account");
        }

        @Test
        @DisplayName("repetir el client_request_id de la empresa lo rechaza la base")
        void repetir_el_client_request_id_lo_rechaza_la_base() {
            documento(COMPANY, BRANCH, null, "req-duplicada", DianStatus.PENDIENTE,
                    ElectronicDocumentType.DOC_EQUIV_POS, List.of(lineaConsulta()), List.of());

            assertThatThrownBy(() -> documento(COMPANY, BRANCH, null, "req-duplicada",
                    DianStatus.PENDIENTE, ElectronicDocumentType.DOC_EQUIV_POS,
                    List.of(lineaConsulta()), List.of()))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("updateDianResult: solo el ciclo de vida DIAN")
    class ActualizacionDian {

        @Test
        @DisplayName("persiste numero, sellos y estado tras validar")
        void persiste_numero_sellos_y_estado() {
            ElectronicDocument guardado = ventaPos();
            guardado.assignNumber("18760000001", "SETP", 990L);
            guardado.markValidated(null, null, "CUFE-OK-001", "CUDE-OK-001", "uuid-001", "<xml/>",
                    "qr-data", "https://qr.dian/001", "s3://facturas/001.pdf",
                    LocalDateTime.of(2026, 1, 15, 11, 30, 0));

            ElectronicDocument actualizado = repository.updateDianResult(guardado);

            assertThat(actualizado.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
            assertThat(actualizado.getPrefix()).isEqualTo("SETP");
            assertThat(actualizado.getConsecutive()).isEqualTo(990L);
            assertThat(actualizado.getResolutionNumber()).isEqualTo("18760000001");
            assertThat(actualizado.getCufe()).isEqualTo("CUFE-OK-001");
            assertThat(actualizado.getQrUrl()).isEqualTo("https://qr.dian/001");
            assertThat(actualizado.getDianValidationDate())
                    .isEqualTo(LocalDateTime.of(2026, 1, 15, 11, 30, 0));
        }

        @Test
        @DisplayName("no reescribe ni duplica las lineas del documento")
        void no_reescribe_ni_duplica_las_lineas() {
            ElectronicDocument guardado = documento(COMPANY, BRANCH, null, null,
                    DianStatus.PENDIENTE, ElectronicDocumentType.FE_VENTA,
                    List.of(linea(1, "Primera", "50000.00", "9500.00", "59500.00"),
                            linea(2, "Segunda", "50000.00", "9500.00", "59500.00")),
                    List.of());
            List<Long> idsOriginales = guardado.getLines().stream()
                    .map(ElectronicDocumentLine::getId).toList();

            guardado.markValidated(null, null, "CUFE-OK-002", null, null, null, null, null, null,
                    LocalDateTime.of(2026, 1, 15, 11, 30, 0));
            repository.updateDianResult(guardado);

            // El contenido fiscal es inmutable. Si esto pasara por mapper.toJpa, cada
            // transmision insertaria las lineas otra vez y el total del UBL se doblaria.
            List<ElectronicDocumentLine> leidas = repository.findById(guardado.getId())
                    .orElseThrow().getLines();
            assertThat(leidas).hasSize(2);
            assertThat(leidas).extracting(ElectronicDocumentLine::getId)
                    .containsExactlyElementsOf(idsOriginales);
        }

        @Test
        @DisplayName("marcar reversada la factura no borra nada: es una marca fiscal")
        void marcar_reversada_no_borra_nada() {
            ElectronicDocument guardado = ventaPos();
            guardado.markValidated(null, null, "CUFE-OK-003", null, null, null, null, null, null,
                    LocalDateTime.of(2026, 1, 15, 11, 30, 0));
            repository.updateDianResult(guardado);
            guardado.markReversed();
            repository.updateDianResult(guardado);

            ElectronicDocument leido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(leido.isReversed()).isTrue();
            assertThat(leido.isEnabled()).as("no es un borrado logico").isTrue();
            assertThat(leido.getLines()).isNotEmpty();
        }

        @Test
        @DisplayName("actualizar un documento inexistente falla en vez de insertar uno nuevo")
        void actualizar_un_documento_inexistente_falla() {
            ElectronicDocument fantasma = new ElectronicDocument(777777L, COMPANY, null,
                    ElectronicDocumentType.FE_VENTA, null, null, null, EMISION, HORA, null, null,
                    null, null, null, null, null, DianStatus.PENDIENTE, null, EMISOR, ADQUIRIENTE,
                    new BigDecimal("100000.00"), new BigDecimal("100000.00"),
                    new BigDecimal("119000.00"), new BigDecimal("119000.00"), PaymentForm.CONTADO,
                    List.of(lineaConsulta()), List.of(), CREADO, null, true, null, null, null,
                    false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, EMPLEADO,
                    BRANCH);

            assertThatThrownBy(() -> repository.updateDianResult(fantasma))
                    .isInstanceOf(ElectronicDocumentNotFoundException.class)
                    .hasMessageContaining("777777");
        }
    }

    @Nested
    @DisplayName("listado paginado con filtros")
    class Listado {

        @Test
        @DisplayName("devuelve los metadatos de la pagina y el mas reciente primero")
        void devuelve_los_metadatos_y_el_mas_reciente_primero() {
            ElectronicDocument primero = ventaPos();
            ElectronicDocument segundo = ventaPos();

            PageResult<ElectronicDocument> pagina = repository.findAllByCompanyId(COMPANY, null,
                    null, null, 0, 1);

            // Orden por id descendente: la facturacion se lee de lo mas reciente hacia
            // atras, y sin orden explicito la paginacion repetiria u omitiria filas.
            assertThat(pagina.content()).extracting(ElectronicDocument::getId)
                    .containsExactly(segundo.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
            assertThat(pagina.totalPages()).isEqualTo(2);
            assertThat(pagina.page()).isZero();
            assertThat(pagina.pageSize()).isEqualTo(1);
            assertThat(repository.findAllByCompanyId(COMPANY, null, null, null, 1, 1).content())
                    .extracting(ElectronicDocument::getId).containsExactly(primero.getId());
        }

        @Test
        @DisplayName("la pagina hidratada trae el detalle completo, no solo la cabecera")
        void la_pagina_hidratada_trae_el_detalle_completo() {
            ventaPos();

            // La consulta que pagina solo devuelve ids: si el segundo paso fallara, el
            // listado saldria con documentos sin lineas y nadie lo notaria hasta la UI.
            assertThat(repository.findAllByCompanyId(COMPANY, null, null, null, 0, 20).content()
                    .getFirst().getLines()).hasSize(1);
        }

        @Test
        @DisplayName("filtra por sede, tipo y estado DIAN de forma independiente")
        void filtra_por_sede_tipo_y_estado() {
            documento(COMPANY, BRANCH, null, null, DianStatus.PENDIENTE,
                    ElectronicDocumentType.DOC_EQUIV_POS, List.of(lineaConsulta()), List.of());
            documento(COMPANY, OTRA_BRANCH, null, null, DianStatus.VALIDADO,
                    ElectronicDocumentType.FE_VENTA, List.of(lineaConsulta()), List.of());

            assertThat(repository.findAllByCompanyId(COMPANY, BRANCH, null, null, 0, 20)
                    .totalElements()).isEqualTo(1L);
            assertThat(repository
                    .findAllByCompanyId(COMPANY, null, ElectronicDocumentType.FE_VENTA, null, 0, 20)
                    .totalElements()).isEqualTo(1L);
            assertThat(
                    repository.findAllByCompanyId(COMPANY, null, null, DianStatus.VALIDADO, 0, 20)
                            .totalElements())
                    .isEqualTo(1L);
            assertThat(
                    repository.findAllByCompanyId(COMPANY, null, null, null, 0, 20).totalElements())
                    .as("sin filtros salen los dos").isEqualTo(2L);
        }

        @Test
        @DisplayName("los filtros se combinan: tipo y estado que no casan no traen nada")
        void los_filtros_se_combinan() {
            documento(COMPANY, BRANCH, null, null, DianStatus.PENDIENTE,
                    ElectronicDocumentType.DOC_EQUIV_POS, List.of(lineaConsulta()), List.of());

            assertThat(repository.findAllByCompanyId(COMPANY, null,
                    ElectronicDocumentType.DOC_EQUIV_POS, DianStatus.VALIDADO, 0, 20).content())
                    .isEmpty();
        }

        @Test
        @DisplayName("el listado no ve los documentos de otra empresa")
        void el_listado_no_ve_los_de_otra_empresa() {
            ventaPos();

            assertThat(
                    repository.findAllByCompanyId(OTRA_COMPANY, null, null, null, 0, 20).content())
                    .isEmpty();
        }

        @Test
        @DisplayName("una pagina fuera de rango sale vacia sin reventar")
        void una_pagina_fuera_de_rango_sale_vacia() {
            ventaPos();

            PageResult<ElectronicDocument> pagina = repository.findAllByCompanyId(COMPANY, null,
                    null, null, 50, 20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }
    }

    /** La columna {@code version} tal cual, sin pasar por Hibernate. */
    private long versionEnLaBase(Long id) {
        return ((Number) entityManager
                .createNativeQuery(
                        "SELECT CAST(version AS SIGNED) FROM electronic_documents WHERE id = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    /**
     * {@code dian_leased_until} no esta mapeada en
     * {@code ElectronicDocumentJpaEntity} —es andamiaje de los jobs, no estado de
     * negocio—, asi que no hay forma de leerla que no sea SQL nativo.
     */
    private Object leaseEnLaBase(Long id) {
        return entityManager
                .createNativeQuery(
                        "SELECT dian_leased_until FROM electronic_documents WHERE id = :id")
                .setParameter("id", id).getSingleResult();
    }

    /**
     * Incidencia #53 — {@code JdbcDianJobLeasePort} es la <b>exencion</b>, y esta
     * fijada aqui a proposito.
     *
     * <p>
     * La campaña añade {@code version = version + 1} a los UPDATE nativos que mutan
     * tablas versionadas. {@code electronic_documents} lo es y el UPDATE del lease
     * la muta, asi que por la regla entraria — y deliberadamente no lo hace: sus
     * filas vienen de un {@code SELECT … FOR UPDATE SKIP LOCKED}, o sea ya
     * serializadas por un lock pesimista sostenido hasta el commit, y
     * {@code dian_leased_until} ni siquiera viaja al dominio, asi que ningun
     * {@code save} cargado antes puede pisarla.
     *
     * <p>
     * Lo que este nido impide es que alguien «arregle» la exencion dentro de seis
     * meses creyendo que fue un olvido. Y hay un motivo extra para escribirlo como
     * test y no solo como comentario: al ir por {@code JdbcTemplate} crudo, esa
     * sentencia es invisible a cualquier regla de ArchUnit que escanee anotaciones.
     * El segundo caso es la consecuencia medible de subir la version ahi: un
     * {@code updateDianResult} en curso moriria con un 409 espurio.
     */
    @Nested
    @DisplayName("el lease de la DIAN esta exento del bump de version, a proposito (#53)")
    class LeaseExentoDelBumpDeVersion {

        @Test
        @DisplayName("tomar el lease marca el documento pero no mueve su version")
        void tomar_el_lease_no_mueve_la_version_del_documento() {
            ElectronicDocument documento = ventaPos();
            Long id = documento.getId();
            entityManager.flush();
            entityManager.clear();
            assertThat(versionEnLaBase(id)).isZero();

            assertThat(leasePort.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE)).contains(id);
            entityManager.clear();

            assertThat(leaseEnLaBase(id)).as("el UPDATE si escribio: la marca esta puesta")
                    .isNotNull();
            assertThat(versionEnLaBase(id))
                    .as("E6_YA_PROTEGIDO: el FOR UPDATE SKIP LOCKED ya serializa estas filas")
                    .isZero();
        }

        @Test
        @DisplayName("renovar el lease no invalida un updateDianResult en curso")
        void renovar_el_lease_no_invalida_un_update_dian_result_en_curso() {
            ElectronicDocument documento = ventaPos();
            Long id = documento.getId();
            entityManager.flush();
            entityManager.clear();

            // 1. Una replica esta transmitiendo: carga el documento y lo deja vivo en
            // el contexto de persistencia, con la version que tenia al leerlo.
            ElectronicDocument enTransmision = repository.findById(id).orElseThrow();

            // 2. El ciclo de jobs vuelve a pasar y renueva el lease sobre esa fila.
            leasePort.leaseByDianStatus(DianStatus.PENDIENTE, 10, LEASE);

            // 3. Llega el resultado de la DIAN y se persiste. Si el lease hubiera
            // subido la version, este UPDATE no encontraria fila que casar y moriria
            // con un 409 CONCURRENT_MODIFICATION sobre un conflicto que no existe,
            // en mitad de una transmision fiscal.
            enTransmision.assignNumber("18760000001", "SETP", 991L);
            enTransmision.markValidated(null, null, "CUFE-LEASE-001", null, null, null, null, null,
                    null, LocalDateTime.of(2026, 1, 15, 11, 30, 0));
            ElectronicDocument actualizado = repository.updateDianResult(enTransmision);
            entityManager.flush();

            assertThat(actualizado.getDianStatus()).isEqualTo(DianStatus.VALIDADO);
            assertThat(actualizado.getCufe()).isEqualTo("CUFE-LEASE-001");

            entityManager.clear();
            assertThat(versionEnLaBase(id))
                    .as("una sola escritura versionada: la del resultado DIAN, no la del lease")
                    .isEqualTo(1L);
            assertThat(leaseEnLaBase(id)).as("y el lease sigue puesto, que es su trabajo")
                    .isNotNull();
        }
    }
}
