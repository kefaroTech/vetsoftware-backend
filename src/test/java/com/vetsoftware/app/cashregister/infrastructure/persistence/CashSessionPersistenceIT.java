package com.vetsoftware.app.cashregister.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.domain.CashMovement;
import com.vetsoftware.app.cashregister.domain.CashMovementType;
import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashReferenceType;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionCount;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de la caja contra MySQL real.
 *
 * <p>
 * El agregado de caja es el unico del proyecto que persiste dos colecciones
 * hijas a la vez (movimientos y conteos). Su {@code save} reconstruye la
 * entidad entera en cada llamada, asi que lo que hay que comprobar es que el
 * append-only no duplica lo ya guardado y que el detalle vuelve del todo. Con
 * un doble en memoria eso siempre da verde, porque el doble guarda la
 * referencia viva y no pasa por Hibernate.
 *
 * <p>
 * Las vistas de resumen ademas son SQL nativo con LEFT JOIN a empleados y una
 * subconsulta de totales de cierre: los nombres que salen ahi no existen en
 * ningun objeto Java.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCashSessionRepository — sesion, movimientos y resumenes contra MySQL real")
class CashSessionPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long BRANCH = SchemaSeed.BRANCH_ID;
    private static final Long OTRA_BRANCH = SchemaSeed.OTRA_BRANCH_ID;
    private static final Long TERMINAL_ID = SchemaSeed.TERMINAL_ID;
    private static final Long OTRO_TERMINAL_ID = SchemaSeed.OTRO_TERMINAL_ID;
    private static final Long EMPLEADO = SchemaSeed.EMPLOYEE_ID;
    private static final Long OTRO_EMPLEADO = SchemaSeed.OTRO_EMPLOYEE_ID;

    private static final LocalDateTime ABIERTA = LocalDateTime.of(2026, 1, 15, 8, 0);
    private static final BigDecimal BASE = new BigDecimal("100000.00");

    @Autowired
    private JpaCashSessionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private CashSession sesion(Long branchId, Long terminalId, String terminal, Long empleado) {
        return repository.save(new CashSession(null, COMPANY, branchId, terminalId, terminal,
                empleado, ABIERTA, BASE, CashSessionStatus.OPEN, null, null, "Turno manana", null,
                List.of(), List.of()));
    }

    private CashSession sesionAbierta() {
        return sesion(BRANCH, TERMINAL_ID, "principal", EMPLEADO);
    }

    private static CashMovement venta(CashPaymentMethod metodo, String monto, Long documentoId) {
        return new CashMovement(null, CashMovementType.SALE_IN, metodo, new BigDecimal(monto),
                CashReferenceType.POS_DOCUMENT, documentoId, EMPLEADO,
                LocalDateTime.of(2026, 1, 15, 11, 30), null);
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y version, y el detalle vuelve entero")
        void guardar_asigna_id_y_el_detalle_vuelve_entero() {
            CashSession guardada = sesionAbierta();

            assertThat(guardada.getId()).isNotNull();
            assertThat(guardada.getVersion()).isNotNull();

            CashSession leida = repository.findByIdAndCompany(guardada.getId(), COMPANY)
                    .orElseThrow();
            assertThat(leida.getBranchId()).isEqualTo(BRANCH);
            assertThat(leida.getTerminal()).isEqualTo("principal");
            assertThat(leida.getOpeningFloat()).isEqualByComparingTo(BASE);
            assertThat(leida.getOpenedAt()).isEqualTo(ABIERTA);
            assertThat(leida.getNote()).isEqualTo("Turno manana");
            assertThat(leida.isOpen()).isTrue();
        }

        @Test
        @DisplayName("los movimientos vuelven con su tipo, metodo y referencia")
        void los_movimientos_vuelven_completos() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(venta(CashPaymentMethod.CASH, "50000.00", 1L));
            repository.save(sesion);

            CashSession leida = repository.findByIdAndCompany(sesion.getId(), COMPANY)
                    .orElseThrow();

            assertThat(leida.getMovements()).singleElement().satisfies(m -> {
                assertThat(m.getType()).isEqualTo(CashMovementType.SALE_IN);
                assertThat(m.getMethod()).isEqualTo(CashPaymentMethod.CASH);
                assertThat(m.getAmount()).isEqualByComparingTo("50000.00");
                assertThat(m.getReferenceType()).isEqualTo(CashReferenceType.POS_DOCUMENT);
                assertThat(m.getReferenceId()).isEqualTo(1L);
                assertThat(m.getId()).as("la base le asigna id al insertarlo").isNotNull();
            });
        }

        @Test
        @DisplayName("una sesion de otra empresa no se puede leer por id")
        void una_sesion_de_otra_empresa_no_se_puede_leer() {
            CashSession guardada = sesionAbierta();

            // La lectura va scoped por empresa: sin eso, adivinar un id daria acceso al
            // arqueo de otro tenant.
            assertThat(repository.findByIdAndCompany(guardada.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("append-only: guardar de nuevo no duplica")
    class AppendOnly {

        @Test
        @DisplayName("volver a guardar con un movimiento nuevo deja dos, no tres")
        void volver_a_guardar_no_duplica_los_movimientos() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(venta(CashPaymentMethod.CASH, "50000.00", 1L));
            CashSession conUno = repository.save(sesion);

            conUno.addMovement(venta(CashPaymentMethod.CARD, "30000.00", 2L));
            repository.save(conUno);

            // El save reconstruye la entidad entera en cada llamada: los movimientos que
            // ya tienen id se re-mapean en vez de insertarse. Si eso fallara, cada cobro
            // duplicaria todo el historico de la sesion y el arqueo saldria al doble.
            CashSession leida = repository.findByIdAndCompany(sesion.getId(), COMPANY)
                    .orElseThrow();
            assertThat(leida.getMovements()).hasSize(2);
            assertThat(leida.expectedByMethod().get(CashPaymentMethod.CASH))
                    .isEqualByComparingTo("150000.00");
            assertThat(leida.expectedByMethod().get(CashPaymentMethod.CARD))
                    .isEqualByComparingTo("30000.00");
        }

        @Test
        @DisplayName("guardar sin cambios no toca el historico")
        void guardar_sin_cambios_no_toca_el_historico() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(venta(CashPaymentMethod.CASH, "50000.00", 1L));
            CashSession conUno = repository.save(sesion);

            repository.save(conUno);

            assertThat(repository.findByIdAndCompany(sesion.getId(), COMPANY).orElseThrow()
                    .getMovements()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("cierre: los conteos se materializan")
    class Cierre {

        @Test
        @DisplayName("guardar el cierre persiste un conteo por metodo con su diferencia")
        void guardar_el_cierre_persiste_los_conteos() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(venta(CashPaymentMethod.CASH, "50000.00", 1L));
            CashSession conVenta = repository.save(sesion);

            conVenta.close(OTRO_EMPLEADO,
                    Map.of(CashPaymentMethod.CASH, new BigDecimal("145000.00")), "Faltan 5.000");
            repository.save(conVenta);

            CashSession leida = repository.findByIdAndCompany(sesion.getId(), COMPANY)
                    .orElseThrow();
            assertThat(leida.getStatus()).isEqualTo(CashSessionStatus.CLOSED);
            assertThat(leida.getClosedByEmployeeId()).isEqualTo(OTRO_EMPLEADO);
            assertThat(leida.getNote()).isEqualTo("Faltan 5.000");
            assertThat(leida.getCounts()).singleElement().satisfies(c -> {
                assertThat(c.getMethod()).isEqualTo(CashPaymentMethod.CASH);
                assertThat(c.getExpectedAmount()).isEqualByComparingTo("150000.00");
                assertThat(c.getCountedAmount()).isEqualByComparingTo("145000.00");
                assertThat(c.difference()).isEqualByComparingTo("-5000.00");
            });
        }

        @Test
        @DisplayName("la diferencia se persiste en su columna, no solo se calcula al leer")
        void la_diferencia_se_persiste_en_su_columna() {
            CashSession sesion = sesionAbierta();
            sesion.close(OTRO_EMPLEADO, Map.of(CashPaymentMethod.CASH, new BigDecimal("95000.00")),
                    null);
            CashSession cerrada = repository.save(sesion);

            // El adaptador escribe difference() en una columna propia: es lo que consulta
            // el reporte de descuadres sin tener que recalcular sobre millones de filas.
            Number diferencia = (Number) entityManager.createNativeQuery("""
                    SELECT difference FROM cash_session_count WHERE session_id = ?
                    """).setParameter(1, cerrada.getId()).getSingleResult();

            assertThat(new BigDecimal(diferencia.toString())).isEqualByComparingTo("-5000.00");
        }
    }

    @Nested
    @DisplayName("una sola caja abierta a la vez")
    class Unicidad {

        @Test
        @DisplayName("reconoce la sesion abierta de la terminal")
        void reconoce_la_sesion_abierta_de_la_terminal() {
            sesionAbierta();

            assertThat(repository.existsOpen(COMPANY, BRANCH, "principal")).isTrue();
            assertThat(repository.existsOpenByTerminalId(COMPANY, BRANCH, TERMINAL_ID)).isTrue();
            assertThat(repository.findOpen(COMPANY, BRANCH, "principal")).isPresent();
        }

        @Test
        @DisplayName("otra terminal de la misma sede no cuenta como abierta")
        void otra_terminal_de_la_misma_sede_no_cuenta() {
            sesionAbierta();

            assertThat(repository.existsOpenByTerminalId(COMPANY, BRANCH, OTRO_TERMINAL_ID))
                    .isFalse();
            assertThat(repository.existsOpen(COMPANY, BRANCH, "caja 2")).isFalse();
        }

        @Test
        @DisplayName("una sesion cerrada deja de contar como abierta")
        void una_sesion_cerrada_deja_de_contar() {
            CashSession sesion = sesionAbierta();
            sesion.close(EMPLEADO, null, null);
            repository.save(sesion);

            // Si el filtro por estado se cayera, nadie podria volver a abrir caja en esa
            // terminal al dia siguiente.
            assertThat(repository.existsOpen(COMPANY, BRANCH, "principal")).isFalse();
            assertThat(repository.findOpen(COMPANY, BRANCH, "principal")).isEmpty();
        }

        @Test
        @DisplayName("reconoce la caja abierta del empleado, este donde este")
        void reconoce_la_caja_abierta_del_empleado() {
            sesionAbierta();

            assertThat(repository.existsOpenByEmployee(COMPANY, EMPLEADO)).isTrue();
            assertThat(repository.existsOpenByEmployee(COMPANY, OTRO_EMPLEADO)).isFalse();
            assertThat(repository.findOpenByEmployee(COMPANY, EMPLEADO)).isPresent();
        }

        @Test
        @DisplayName("sin empleado no se consulta la base: devuelve vacio")
        void sin_empleado_no_se_consulta_la_base() {
            sesionAbierta();

            assertThat(repository.existsOpenByEmployee(COMPANY, null)).isFalse();
            assertThat(repository.findOpenByEmployee(COMPANY, null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("resumenes: los nombres que no existen en Java")
    class Resumenes {

        @Test
        @DisplayName("el resumen de la terminal resuelve sede y responsable")
        void el_resumen_resuelve_sede_y_responsable() {
            sesionAbierta();

            CashSessionView resumen = repository
                    .findOpenSummaryByTerminalId(COMPANY, BRANCH, TERMINAL_ID).orElseThrow();

            // El agregado solo tiene ids: estos nombres salen del LEFT JOIN de la query
            // nativa y no hay forma de comprobarlos sin la base.
            assertThat(resumen.branchName()).isEqualTo("Sede Centro");
            assertThat(resumen.openedByEmployeeName()).isEqualTo("Ana Ruiz");
            assertThat(resumen.terminal()).isEqualTo("principal");
            assertThat(resumen.status()).isEqualTo(CashSessionStatus.OPEN);
            assertThat(resumen.movements()).as("el resumen no arrastra el detalle").isEmpty();
        }

        /**
         * El caso que no se podia escribir mientras la busqueda iba por la cadena, y el
         * motivo de que {@code findOpenSummary(String)} ya no exista.
         *
         * <p>
         * {@code cash_session.terminal} es la foto del codigo en el instante de abrir
         * -lo que salio impreso en el recibo-, no una referencia viva. Se renombra la
         * terminal A, se crea la B reutilizando el codigo liberado, y las dos acaban
         * con sesiones abiertas que declaran el mismo texto. La unicidad que queda
         * ({@code uq_cash_session_terminal_open}, changeset {@code 211_06}) es sobre
         * {@code (company_id, terminal_id, open_marker)}: permite exactamente este
         * estado, asi que la base lo acepta sin rechistar.
         *
         * <p>
         * Aqui se siembra ese estado a proposito y se comprueba lo unico que se puede
         * comprobar: <b>cada identificador recupera su propia sesion</b>, y la busqueda
         * por cadena <b>no puede distinguirlas</b>. Esa segunda asercion es
         * deliberadamente floja -{@code isIn}, no {@code isEqualTo}- porque cual de las
         * dos vuelve depende del plan que elija el motor: afirmar una concreta seria
         * escribir en el test una garantia que la base no da. Es dinero: el arqueo de
         * la caja equivocada.
         */
        @Test
        @DisplayName("dos terminales con el mismo codigo: cada id recupera la suya")
        void dos_terminales_con_el_mismo_codigo_no_se_confunden() {
            CashSession enLaPrincipal = sesion(BRANCH, TERMINAL_ID, "principal", EMPLEADO);
            CashSession enLaSegunda = sesion(BRANCH, OTRO_TERMINAL_ID, "principal", OTRO_EMPLEADO);

            assertThat(repository.findOpenSummaryByTerminalId(COMPANY, BRANCH, TERMINAL_ID))
                    .as("la terminal %s tiene que devolver su propia sesion", TERMINAL_ID)
                    .map(CashSessionView::id).contains(enLaPrincipal.getId());
            assertThat(repository.findOpenSummaryByTerminalId(COMPANY, BRANCH, OTRO_TERMINAL_ID))
                    .as("la terminal %s tiene que devolver su propia sesion", OTRO_TERMINAL_ID)
                    .map(CashSessionView::id).contains(enLaSegunda.getId());

            assertThat(repository.findOpen(COMPANY, BRANCH, "principal").orElseThrow().getId())
                    .as("la busqueda por cadena no puede discriminar: devuelve una arbitraria")
                    .isIn(enLaPrincipal.getId(), enLaSegunda.getId());
        }

        @Test
        @DisplayName("el total de cierre sale de la subconsulta de conteos")
        void el_total_de_cierre_sale_de_la_subconsulta() {
            CashSession sesion = sesionAbierta();
            sesion.close(OTRO_EMPLEADO, Map.of(CashPaymentMethod.CASH, new BigDecimal("95000.00")),
                    null);
            repository.save(sesion);

            CashSessionView fila = repository
                    .search(new SearchCashSessionsQuery(COMPANY, null, null, null, null, 0, 20))
                    .content().getFirst();

            assertThat(fila.closingTotal()).isEqualByComparingTo("95000.00");
            assertThat(fila.closedByEmployeeName()).isEqualTo("Luis Paz");
        }

        @Test
        @DisplayName("una sesion abierta no tiene total de cierre")
        void una_sesion_abierta_no_tiene_total_de_cierre() {
            sesionAbierta();

            assertThat(repository
                    .search(new SearchCashSessionsQuery(COMPANY, null, null, null, null, 0, 20))
                    .content().getFirst().closingTotal()).isNull();
        }
    }

    @Nested
    @DisplayName("historial paginado")
    class Historial {

        @Test
        @DisplayName("filtra por sede y por empleado de apertura")
        void filtra_por_sede_y_por_empleado() {
            sesionAbierta();
            sesion(OTRA_BRANCH, OTRO_TERMINAL_ID, "norte", OTRO_EMPLEADO);

            assertThat(repository
                    .search(new SearchCashSessionsQuery(COMPANY, BRANCH, null, null, null, 0, 20))
                    .content()).hasSize(1);
            assertThat(repository.search(
                    new SearchCashSessionsQuery(COMPANY, null, OTRO_EMPLEADO, null, null, 0, 20))
                    .content()).hasSize(1);
            assertThat(repository
                    .search(new SearchCashSessionsQuery(COMPANY, null, null, null, null, 0, 20))
                    .content()).hasSize(2);
        }

        @Test
        @DisplayName("el hasta es inclusivo por dia")
        void el_hasta_es_inclusivo_por_dia() {
            sesionAbierta();

            // La sesion abrio a las 08:00 del 15: un rango que termina el 15 tiene que
            // incluirla, y para eso el adaptador convierte el `to` al dia siguiente.
            assertThat(
                    repository
                            .search(new SearchCashSessionsQuery(COMPANY, null, null,
                                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15), 0, 20))
                            .content())
                    .hasSize(1);
            assertThat(
                    repository
                            .search(new SearchCashSessionsQuery(COMPANY, null, null,
                                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 14), 0, 20))
                            .content())
                    .isEmpty();
        }

        @Test
        @DisplayName("devuelve los metadatos de la pagina")
        void devuelve_los_metadatos_de_la_pagina() {
            sesionAbierta();
            sesion(OTRA_BRANCH, OTRO_TERMINAL_ID, "norte", OTRO_EMPLEADO);

            var pagina = repository
                    .search(new SearchCashSessionsQuery(COMPANY, null, null, null, null, 0, 1));

            assertThat(pagina.content()).hasSize(1);
            assertThat(pagina.totalElements()).isEqualTo(2L);
            assertThat(pagina.totalPages()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("cajas abiertas visibles")
    class Abiertas {

        @Test
        @DisplayName("un scope null trae todas las sedes de la empresa")
        void un_scope_null_trae_todas_las_sedes() {
            sesionAbierta();
            sesion(OTRA_BRANCH, OTRO_TERMINAL_ID, "norte", OTRO_EMPLEADO);

            assertThat(repository.findOpenSummaries(COMPANY, null)).hasSize(2);
        }

        @Test
        @DisplayName("con scope solo trae las sedes asignadas")
        void con_scope_solo_trae_las_sedes_asignadas() {
            sesionAbierta();
            sesion(OTRA_BRANCH, OTRO_TERMINAL_ID, "norte", OTRO_EMPLEADO);

            assertThat(repository.findOpenSummaries(COMPANY, Set.of(BRANCH)))
                    .extracting(CashSessionView::branchId).containsExactly(BRANCH);
        }

        @Test
        @DisplayName("un scope vacio no trae nada y ni siquiera consulta")
        void un_scope_vacio_no_trae_nada() {
            sesionAbierta();

            // Un empleado sin sedes asignadas no puede ver ninguna caja. Traducirlo a un
            // IN () vacio seria un error de SQL, asi que el adaptador corta antes.
            assertThat(repository.findOpenSummaries(COMPANY, Set.of())).isEmpty();
        }

        @Test
        @DisplayName("una caja cerrada no aparece")
        void una_caja_cerrada_no_aparece() {
            CashSession sesion = sesionAbierta();
            sesion.close(EMPLEADO, null, null);
            repository.save(sesion);

            assertThat(repository.findOpenSummaries(COMPANY, null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("conteos del cierre")
    class Conteos {

        @Test
        @DisplayName("un cierre con varios metodos guarda un conteo por cada uno")
        void un_cierre_con_varios_metodos_guarda_uno_por_cada_uno() {
            CashSession sesion = sesionAbierta();
            sesion.addMovement(venta(CashPaymentMethod.CASH, "50000.00", 1L));
            sesion.addMovement(venta(CashPaymentMethod.CARD, "30000.00", 2L));
            CashSession conVentas = repository.save(sesion);

            conVentas.close(OTRO_EMPLEADO, null, null);
            repository.save(conVentas);

            List<CashSessionCount> conteos = repository.findByIdAndCompany(sesion.getId(), COMPANY)
                    .orElseThrow().getCounts();

            assertThat(conteos).extracting(CashSessionCount::getMethod)
                    .containsExactlyInAnyOrder(CashPaymentMethod.CASH, CashPaymentMethod.CARD);
        }
    }
}
