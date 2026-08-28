package com.vetsoftware.app.entitlement.infrastructure.persistence;

import static com.vetsoftware.app.testsupport.EngineConstraint.assertViolates;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.LimitDimensionRef;
import com.vetsoftware.app.entitlement.domain.MeasureKind;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyCapacityRepository — contadores contratados contra MySQL real")
class CompanyCapacityPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 15, 9, 0);

    @Autowired
    private JpaCompanyCapacityRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        seedCapacity(977L, "BRANCH");
        seedCapacity(978L, "TERMINAL");
    }

    private Long eje(String code) {
        return SchemaSeed.limitDimensionId(entityManager, code);
    }

    private Optional<CompanyCapacity> contadorDe(String code) {
        return repository.findByCompanyIdAndDimension(SchemaSeed.COMPANY_ID, eje(code),
                PeriodKey.SENTINEL);
    }

    private int mover(String code, int delta) {
        return repository.addUsage(SchemaSeed.COMPANY_ID, eje(code), PeriodKey.SENTINEL, delta);
    }

    @Nested
    @DisplayName("El contador sube y comprueba el techo en una sola instrucción (R-LIMIT-01)")
    class SubeYCompruebaEnUnaInstruccion {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"USER", "BRANCH", "TERMINAL"})
        @DisplayName("addUsage incrementa atómicamente el contador de su eje")
        void add_usage_incrementa_el_contador_correcto(String code) {
            int filas = mover(code, 2);
            entityManager.flush();
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(contadorDe(code)).get().satisfies(capacidad -> {
                assertThat(capacidad.getLimitQuantity()).isEqualTo(2);
                assertThat(capacidad.getUsedQuantity()).isEqualTo(2);
                assertThat(capacidad.getDimension().code()).isEqualTo(code);
                assertThat(capacidad.getSubscriptionId()).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
            });
            assertThat(repository.addUsage(SchemaSeed.OTRA_COMPANY_ID, eje(code),
                    PeriodKey.SENTINEL, 1)).isZero();
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"USER", "BRANCH", "TERMINAL"})
        @DisplayName("addUsage no rebasa el límite ni permite dejar el contador negativo")
        void add_usage_respeta_limite_y_piso_de_cero(String code) {
            assertThat(mover(code, 2)).isEqualTo(1);
            assertThat(mover(code, 1)).isZero();
            assertThat(mover(code, -3)).isZero();
            entityManager.flush();
            entityManager.clear();

            assertThat(contadorDe(code)).get().extracting(CompanyCapacity::getUsedQuantity)
                    .isEqualTo(2);
        }

        /**
         * El caso violador de R-LIMIT-01. Las dos altas se piden sobre el mismo ultimo
         * hueco; la comprobacion del techo viaja dentro del propio {@code UPDATE}, asi
         * que la segunda no afecta a ninguna fila. Con un leer-decidir-escribir desde
         * Java las dos habrian leido "queda 1" y las dos habrian entrado.
         */
        @Test
        @DisplayName("dos altas sobre el último hueco de un cupo no pueden colarse las dos")
        void dos_altas_sobre_el_ultimo_hueco_no_pueden_colarse_las_dos() {
            assertThat(mover("USER", 1)).isEqualTo(1);

            int primera = mover("USER", 1);
            int segunda = mover("USER", 1);

            assertThat(primera).isEqualTo(1);
            assertThat(segunda).isZero();
            entityManager.flush();
            entityManager.clear();
            assertThat(contadorDe("USER")).get().extracting(CompanyCapacity::getUsedQuantity)
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("La clave de periodo nunca va vacía (R-LIMIT-05)")
    class ClaveDePeriodo {

        /**
         * Lo que este caso prueba es <strong>el indice unico</strong>, no el centinela.
         * El nombre decia «period_key vacio» y el escenario nunca deja la clave vacia:
         * escribe el centinela, igual que la fila que el andamio ya sembro. La
         * distincion importa porque son dos barandillas distintas —el indice unico y
         * {@code chk_company_capacities_period_key}— y confundirlas deja la segunda sin
         * prueba propia mientras el nombre promete que la tiene.
         *
         * <p>
         * El centinela es la <em>razon</em> de que este indice sirva: si
         * {@code period_key} fuera nulable, estas dos filas convivirian, porque en un
         * indice unico dos NULL no chocan entre si. Que no lo sea es lo que hace que
         * choquen. Los dos casos de mas abajo son los que prueban esa otra mitad.
         */
        @Test
        @DisplayName("R-LIMIT-05 · dos contadores del mismo eje y el mismo periodo chocan contra"
                + " uq_company_capacities")
        void dos_contadores_del_mismo_eje_y_periodo_chocan_contra_el_indice_unico() {
            assertViolates("uq_company_capacities", () -> {
                insertarContador(9101L, eje("USER"), "STOCK", PeriodKey.SENTINEL);
                entityManager.flush();
            });
        }

        /**
         * La otra mitad de la regla, del lado del motor: un eje que no es de flujo no
         * puede llevar un periodo real, y uno de flujo no puede llevar el centinela. Lo
         * impone {@code chk_company_capacities_period_key}.
         */
        @Test
        @DisplayName("un contador que no es de flujo no admite una clave de periodo real")
        void un_contador_que_no_es_de_flujo_no_admite_periodo_real() {
            assertViolates("chk_company_capacities_period_key", () -> {
                insertarContador(9102L, eje("BRANCH"), "STOCK", "2026-03");
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("un contador de flujo no admite el centinela")
        void un_contador_de_flujo_no_admite_el_centinela() {
            assertViolates("chk_company_capacities_period_key", () -> {
                insertarContador(9103L, eje("APPOINTMENT"), "FLOW", PeriodKey.SENTINEL);
                entityManager.flush();
            });
        }

        /**
         * R-LIMIT-04: al entrar el periodo siguiente nace una fila nueva. Dos periodos
         * del mismo eje conviven porque el periodo es parte de la clave — que es
         * justamente lo que el centinela protege en los ejes que no son de flujo.
         */
        @Test
        @DisplayName("dos periodos del mismo eje de flujo sí conviven")
        void dos_periodos_del_mismo_eje_de_flujo_conviven() {
            insertarContador(9104L, eje("APPOINTMENT"), "FLOW", "2026-03");
            insertarContador(9105L, eje("APPOINTMENT"), "FLOW", "2026-04");
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID))
                    .filteredOn(c -> "APPOINTMENT".equals(c.getDimension().code()))
                    .extracting(c -> c.getPeriodKey().value())
                    .containsExactlyInAnyOrder("2026-03", "2026-04");
        }
    }

    @Nested
    @DisplayName("El cupo de flujo se reinicia, no se cierra (R-LIMIT-04)")
    class NacimientoDelPeriodo {

        /**
         * El caso violador contra el motor de verdad. Hasta el arreglo, la fila del
         * periodo entrante no nacia nunca: el {@code UPDATE} afectaba a cero filas y
         * eso era indistinguible de haber topado con el techo, asi que el cupo no se
         * reiniciaba, se <strong>cerraba</strong>.
         *
         * <p>
         * Esta prueba existe sobre todo por el SQL. {@code openPeriod} es un
         * {@code INSERT ... SELECT} sobre la <em>misma</em> tabla en la que inserta,
         * con {@code ON DUPLICATE KEY UPDATE} encima: eso lo acepta o lo rechaza el
         * motor, no el compilador, y un test con dobles no lo sabria nunca.
         */
        @Test
        @DisplayName("el 1 de abril nace la fila 2026-04 heredando el techo ya resuelto de marzo")
        void el_1_de_abril_nace_la_fila_2026_04_heredando_el_techo_de_marzo() {
            insertarContador(9110L, eje("APPOINTMENT"), "FLOW", "2026-03");
            repository.addUsage(SchemaSeed.COMPANY_ID, eje("APPOINTMENT"), "2026-03", 1);
            entityManager.flush();
            entityManager.clear();

            int nacidas = repository.openPeriod(SchemaSeed.COMPANY_ID, eje("APPOINTMENT"),
                    "2026-04", AHORA);
            entityManager.flush();
            entityManager.clear();

            assertThat(nacidas).isEqualTo(1);
            assertThat(repository.findByCompanyIdAndDimension(SchemaSeed.COMPANY_ID,
                    eje("APPOINTMENT"), "2026-04")).get().satisfies(abril -> {
                        // El techo se HEREDA ya resuelto, sin volver a cruzar el contrato.
                        assertThat(abril.getLimitQuantity()).isEqualTo(2);
                        // Y el consumo arranca de cero: eso es reiniciarse.
                        assertThat(abril.getUsedQuantity()).isZero();
                        assertThat(abril.getSubscriptionId()).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
                    });
            // Marzo se queda como estaba: la serie no se mueve, se continua.
            assertThat(repository.findByCompanyIdAndDimension(SchemaSeed.COMPANY_ID,
                    eje("APPOINTMENT"), "2026-03")).get()
                    .extracting(CompanyCapacity::getUsedQuantity).isEqualTo(1);
        }

        /**
         * Dos peticiones simultaneas al entrar el periodo intentan el nacimiento a la
         * vez. La segunda no puede reventar contra {@code uq_company_capacities} ni,
         * mucho menos, pisar el consumo que la primera ya empezo a mover: por eso el
         * {@code ON DUPLICATE KEY UPDATE} asigna la columna a si misma.
         */
        @Test
        @DisplayName("abrir dos veces el mismo periodo no duplica la fila ni pisa el consumo")
        void abrir_dos_veces_el_mismo_periodo_no_duplica_ni_pisa_el_consumo() {
            insertarContador(9111L, eje("APPOINTMENT"), "FLOW", "2026-03");
            entityManager.flush();
            repository.openPeriod(SchemaSeed.COMPANY_ID, eje("APPOINTMENT"), "2026-04", AHORA);
            repository.addUsage(SchemaSeed.COMPANY_ID, eje("APPOINTMENT"), "2026-04", 1);
            entityManager.flush();
            entityManager.clear();

            repository.openPeriod(SchemaSeed.COMPANY_ID, eje("APPOINTMENT"), "2026-04", AHORA);
            entityManager.flush();
            entityManager.clear();

            // NO se afirma sobre el numero devuelto, y no es un descuido. El driver de
            // MySQL conecta con CLIENT_FOUND_ROWS -useAffectedRows viene en false-, asi
            // que un ON DUPLICATE KEY UPDATE que encuentra la fila y la deja igual
            // devuelve 1, filas ENCONTRADAS, y no 0, filas cambiadas. Con eso el contador
            // no distingue «naci» de «ya estaba», que es justo lo que
            // AdjustCompanyCapacityUsageService necesita que signifique: su unico corte es
            // born == 0, o sea «no habia periodo anterior del que heredar». Exigir aqui un
            // 0 era exigir una semantica que ni el driver da ni el llamador usa.
            // Lo observable, que es lo que promete el nombre del caso: ni fila duplicada
            // ni consumo pisado.
            assertThat(filasDelPeriodo(eje("APPOINTMENT"), "2026-04")).isEqualTo(1);
            assertThat(repository.findByCompanyIdAndDimension(SchemaSeed.COMPANY_ID,
                    eje("APPOINTMENT"), "2026-04")).get()
                    .extracting(CompanyCapacity::getUsedQuantity).isEqualTo(1);
        }

        /**
         * Sin periodo anterior no hay techo que heredar: la serie no existe todavia y
         * quien la abre es el recalculo. Inventar una fila aqui --con techo cero--
         * seria peor que el fallo original.
         */
        @Test
        @DisplayName("sin periodo anterior del que heredar no nace ninguna fila")
        void sin_periodo_anterior_no_nace_ninguna_fila() {
            int nacidas = repository.openPeriod(SchemaSeed.COMPANY_ID, eje("APPOINTMENT"),
                    "2026-04", AHORA);
            entityManager.flush();
            entityManager.clear();

            assertThat(nacidas).isZero();
            assertThat(repository.findByCompanyIdAndDimension(SchemaSeed.COMPANY_ID,
                    eje("APPOINTMENT"), "2026-04")).isEmpty();
        }

        /** La empresa vecina no hereda nada: el {@code WHERE} nombra la empresa. */
        @Test
        @DisplayName("el nacimiento no cruza la frontera de empresa")
        void el_nacimiento_no_cruza_la_frontera_de_empresa() {
            insertarContador(9112L, eje("APPOINTMENT"), "FLOW", "2026-03");
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.openPeriod(SchemaSeed.OTRA_COMPANY_ID, eje("APPOINTMENT"),
                    "2026-04", AHORA)).isZero();
        }
    }

    @Nested
    @DisplayName("El sello del consumo lo escribe el recuento, y solo él (R-ENT-13)")
    class SelloDelConsumo {

        /**
         * {@code usage_reconciled_at} existia desde el changeset 314 y no lo escribia
         * nadie: el recalculo no lo toca a proposito y no habia otro escritor, asi que
         * su valor iba a ser nulo para siempre.
         */
        @Test
        @DisplayName("sellar el consumo no toca ni el techo ni el consumo ni el sello del techo")
        void sellar_el_consumo_no_toca_los_demas_campos() {
            repository.addUsage(SchemaSeed.COMPANY_ID, eje("USER"), PeriodKey.SENTINEL, 1);
            entityManager.flush();
            entityManager.clear();
            CompanyCapacity antes = contadorDe("USER").orElseThrow();

            int selladas = repository.markUsageReconciled(SchemaSeed.COMPANY_ID, eje("USER"),
                    PeriodKey.SENTINEL, AHORA);
            entityManager.flush();
            entityManager.clear();

            assertThat(selladas).isEqualTo(1);
            assertThat(contadorDe("USER")).get().satisfies(despues -> {
                assertThat(despues.getUsageReconciledAt()).isEqualTo(AHORA);
                assertThat(despues.getUsedQuantity()).isEqualTo(antes.getUsedQuantity());
                assertThat(despues.getLimitQuantity()).isEqualTo(antes.getLimitQuantity());
                assertThat(despues.getLimitRecalculatedAt())
                        .isEqualTo(antes.getLimitRecalculatedAt());
            });
        }

        @Test
        @DisplayName("el sello no cruza la frontera de empresa")
        void el_sello_no_cruza_la_frontera_de_empresa() {
            assertThat(repository.markUsageReconciled(SchemaSeed.OTRA_COMPANY_ID, eje("USER"),
                    PeriodKey.SENTINEL, AHORA)).isZero();
        }

        /**
         * La consulta que alimenta el barrido. Avanza por cursor de id y no por
         * urgencia: un contador con desvio no se sella, asi que sigue siendo pendiente
         * despues de examinarlo y un orden por urgencia dejaria al barrido girando
         * sobre las mismas filas para siempre.
         */
        @Test
        @DisplayName("el barrido encuentra los no sellados y avanza por cursor")
        void el_barrido_encuentra_los_no_sellados_y_avanza_por_cursor() {
            entityManager.flush();
            entityManager.clear();

            List<CompanyCapacity> primero = repository.findUnreconciled(AHORA, 0L, 1);
            assertThat(primero).hasSize(1);
            Long primerId = primero.get(0).getId();

            List<CompanyCapacity> siguiente = repository.findUnreconciled(AHORA, primerId, 50);
            assertThat(siguiente).isNotEmpty()
                    .allSatisfy(c -> assertThat(c.getId()).isGreaterThan(primerId));

            // Una vez sellado con un instante posterior al corte, sale del conjunto.
            repository.markUsageReconciled(SchemaSeed.COMPANY_ID, eje("USER"), PeriodKey.SENTINEL,
                    AHORA.plusDays(1));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findUnreconciled(AHORA, 0L, 100))
                    .noneSatisfy(c -> assertThat(c.getDimension().code()).isEqualTo("USER"));
        }
    }

    @Nested
    @DisplayName("La copia del tipo de medida va atada al eje (R-LIMIT-22)")
    class CopiaAtadaAlEje {

        /**
         * El caso violador del catalogo. La clave foranea es compuesta contra
         * {@code limit_dimensions(id, measure_kind)}, asi que mover el tipo de medida
         * de un eje que ya tiene contadores colgando muere en el motor. Sin ella, el
         * eje pasaria a FLOW y sus contadores seguirian siendo acumulativos: un cupo
         * mensual que no se reinicia nunca, en silencio.
         */
        @Test
        @DisplayName("cambiar el measure_kind de un eje con contadores colgando muere en el motor")
        void cambiar_measure_kind_del_eje_con_contadores_colgando_muere_en_el_motor() {
            // company_capacities ya NO es lo unico que cuelga de limit_dimensions(id,
            // measure_kind): el changeset 313 siembra el techo de fabrica de USER en
            // catalog_item_limits con la MISMA clave foranea compuesta, y es esa la que el
            // motor evalua primero. El caso se puso rojo nombrando una restriccion que
            // existe y funciona y, peor, habria seguido en verde el dia que
            // fk_company_capacities_dimension desapareciera, porque la vecina la tapaba.
            // Se retira el techo de catalogo para dejar en pie una sola barandilla: la que
            // este caso dice probar.
            entityManager
                    .createNativeQuery(
                            "DELETE FROM catalog_item_limits WHERE limit_dimension_id = :eje")
                    .setParameter("eje", eje("USER")).executeUpdate();
            entityManager.flush();

            assertViolates("fk_company_capacities_dimension", () -> {
                entityManager.createNativeQuery(
                        "UPDATE limit_dimensions SET measure_kind = 'FLOW' WHERE code = 'USER'")
                        .executeUpdate();
                entityManager.flush();
            });
        }

        /**
         * <b>El eje tuvo que cambiar, y ese cambio es el arreglo.</b> Este caso
         * escribia sobre {@code USER}, que es justo el eje del que {@link SchemaSeed}
         * ya siembra un contador con el centinela. La fila nueva chocaba primero contra
         * {@code uq_company_capacities (company_id, limit_dimension_id, period_key)} y
         * la clave foranea compuesta <em>podia no evaluarse nunca</em>: el caso pasaba
         * en verde con la copia del tipo de medida desatada del eje.
         *
         * <p>
         * {@code OWNER} es un eje real del changeset 313 al que el andamio no le cuelga
         * ningun contador, asi que aqui la unica barandilla que puede parar la
         * sentencia es la que el caso dice probar. Es {@code CUMULATIVE} en el catalogo
         * y la fila afirma {@code STOCK}; los dos son «no flujo», asi que
         * {@code chk_company_capacities_period_key} tampoco se interpone.
         */
        @Test
        @DisplayName("R-LIMIT-22 · un contador no puede copiar un tipo de medida que su eje no"
                + " tiene: muere en fk_company_capacities_dimension")
        void un_contador_no_puede_copiar_un_tipo_de_medida_ajeno() {
            assertViolates("fk_company_capacities_dimension", () -> {
                // OWNER es CUMULATIVE en el catálogo; la fila afirma STOCK.
                insertarContador(9106L, eje("OWNER"), "STOCK", PeriodKey.SENTINEL);
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("El recálculo escribe el techo sin pisar el consumo (#648)")
    class RecalculoSinPisarElConsumo {

        private CompanyCapacity techoDe(String code, MeasureKind medida, int techo,
                int usadoLeido) {
            return new CompanyCapacity(null, SchemaSeed.COMPANY_ID,
                    new LimitDimensionRef(eje(code), code, medida,
                            AHORA.toLocalDate().minusYears(1)),
                    PeriodKey.sentinel(), techo, usadoLeido, SchemaSeed.SUBSCRIPTION_ID, AHORA,
                    null, AHORA);
        }

        /**
         * El defecto, reproducido tal cual ocurre en produccion. El recalculo lee el
         * contador (usado = 2), y ANTES de que escriba, un empleado se da de baja
         * (usado = 1). Escribiendo la fila entera con el valor leido, la baja se pierde
         * sin excepcion y sin log, y el cliente se queda con un techo que no puede
         * llenar. La sentencia del techo no nombra {@code used_quantity}, asi que la
         * baja sobrevive.
         */
        @Test
        @DisplayName("una baja que ocurre mientras el recálculo corre no se pierde")
        void una_baja_que_ocurre_mientras_el_recalculo_corre_no_se_pierde() {
            assertThat(mover("USER", 2)).isEqualTo(1);
            entityManager.flush();
            entityManager.clear();

            // El recálculo lee aquí: usado = 2.
            CompanyCapacity leidoPorElRecalculo = contadorDe("USER").orElseThrow();
            assertThat(leidoPorElRecalculo.getUsedQuantity()).isEqualTo(2);

            // Mientras tanto, una baja de empleado.
            assertThat(mover("USER", -1)).isEqualTo(1);
            entityManager.flush();
            entityManager.clear();

            // Y ahora el recálculo escribe el techo con su foto ya vieja.
            repository.upsertCeilings(List.of(
                    techoDe("USER", MeasureKind.STOCK, 5, leidoPorElRecalculo.getUsedQuantity())));
            entityManager.flush();
            entityManager.clear();

            assertThat(contadorDe("USER")).get().satisfies(contador -> {
                assertThat(contador.getLimitQuantity()).isEqualTo(5);
                assertThat(contador.getUsedQuantity()).isEqualTo(1);
            });
        }

        /**
         * R-ENT-13. El recalculo mueve el sello del techo y <strong>no</strong> el del
         * consumo, que sigue siendo nulo porque nadie ha contado las filas reales.
         */
        @Test
        @DisplayName("el recálculo no refresca el sello del consumo")
        void el_recalculo_no_refresca_el_sello_del_consumo() {
            repository.upsertCeilings(List.of(techoDe("USER", MeasureKind.STOCK, 9, 0)));
            entityManager.flush();
            entityManager.clear();

            assertThat(contadorDe("USER")).get().satisfies(contador -> {
                assertThat(contador.getLimitRecalculatedAt()).isEqualTo(AHORA);
                assertThat(contador.getUsageReconciledAt()).isNull();
            });
        }

        /**
         * R-LIMIT-38. Bajar el techo por debajo de lo ya consumido es un estado
         * legitimo. Si la base lo prohibiera, el recalculo de una bajada de plan
         * fallaria entero y el cliente quedaria atrapado en el estado anterior.
         */
        @Test
        @DisplayName("bajar el techo por debajo del consumo no hace fallar el recálculo")
        void bajar_el_techo_por_debajo_del_consumo_no_hace_fallar_el_recalculo() {
            assertThat(mover("USER", 2)).isEqualTo(1);
            entityManager.flush();
            entityManager.clear();

            repository.upsertCeilings(List.of(techoDe("USER", MeasureKind.STOCK, 1, 2)));
            entityManager.flush();
            entityManager.clear();

            assertThat(contadorDe("USER")).get().satisfies(contador -> {
                assertThat(contador.getLimitQuantity()).isEqualTo(1);
                assertThat(contador.getUsedQuantity()).isEqualTo(2);
                assertThat(contador.isExhausted()).isTrue();
            });
        }

        /**
         * Lo que #629 existe para permitir. {@code ANIMAL} no era expresable con la
         * lista cerrada de cuatro unidades; aqui empieza a contarse sin una sola linea
         * de codigo nueva, porque su fila ya esta en el catalogo.
         */
        @Test
        @DisplayName("empezar a contar un eje nuevo es una fila de catálogo, no un despliegue")
        void empezar_a_contar_un_eje_nuevo_es_una_fila_de_catalogo() {
            repository.upsertCeilings(List.of(techoDe("ANIMAL", MeasureKind.CUMULATIVE, 100, 0)));
            entityManager.flush();
            entityManager.clear();

            assertThat(mover("ANIMAL", 1)).isEqualTo(1);
            entityManager.flush();
            entityManager.clear();

            assertThat(contadorDe("ANIMAL")).get().satisfies(contador -> {
                assertThat(contador.getDimension().measureKind()).isEqualTo(MeasureKind.CUMULATIVE);
                assertThat(contador.getLimitQuantity()).isEqualTo(100);
                assertThat(contador.getUsedQuantity()).isEqualTo(1);
            });
        }

        @Test
        @DisplayName("recalcular dos veces seguidas deja exactamente el mismo estado")
        void recalcular_dos_veces_deja_el_mismo_estado() {
            repository.upsertCeilings(List.of(techoDe("USER", MeasureKind.STOCK, 4, 0)));
            repository.upsertCeilings(List.of(techoDe("USER", MeasureKind.STOCK, 4, 0)));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID))
                    .filteredOn(c -> "USER".equals(c.getDimension().code())).singleElement()
                    .extracting(CompanyCapacity::getLimitQuantity).isEqualTo(4);
        }
    }

    private long filasDelPeriodo(Long dimensionId, String periodKey) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM company_capacities"
                        + " WHERE company_id = :companyId AND limit_dimension_id = :dimensionId"
                        + " AND period_key = :periodKey")
                .setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("dimensionId", dimensionId).setParameter("periodKey", periodKey)
                .getSingleResult()).longValue();
    }

    private void insertarContador(Long id, Long dimensionId, String measureKind, String periodKey) {
        entityManager.createNativeQuery("""
                INSERT INTO company_capacities (id, company_id, limit_dimension_id, measure_kind,
                                                period_key, limit_quantity, used_quantity,
                                                subscription_id, limit_recalculated_at,
                                                created_date)
                VALUES (:id, :companyId, :dimensionId, :measureKind, :periodKey, 2, 0,
                        :subscriptionId, NOW(), NOW())
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("dimensionId", dimensionId).setParameter("measureKind", measureKind)
                .setParameter("periodKey", periodKey)
                .setParameter("subscriptionId", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
    }

    private void seedCapacity(Long id, String code) {
        entityManager.createNativeQuery("""
                INSERT INTO company_capacities (id, company_id, limit_dimension_id, measure_kind,
                                                period_key, limit_quantity, used_quantity,
                                                subscription_id, limit_recalculated_at,
                                                created_date)
                VALUES (:id, :companyId, :dimensionId, 'STOCK', :periodKey, 2, 0,
                        :subscriptionId, NOW(), NOW())
                ON DUPLICATE KEY UPDATE used_quantity = 0, limit_quantity = 2
                """).setParameter("id", id).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("dimensionId", eje(code))
                .setParameter("periodKey", PeriodKey.SENTINEL)
                .setParameter("subscriptionId", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();
        entityManager.flush();
    }
}
