package com.vetsoftware.app.withholdingraterule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingRateRule;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaWithholdingRateRuleRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar son las dos columnas GENERATED
 * STORED del changeset 317</b>, porque son invisibles desde Java: no estan
 * mapeadas, no aparecen en ningun getter y ningun test de dominio puede
 * tocarlas. Sin embargo son la mitad de la correccion del modelo.
 *
 * <ul>
 * <li>{@link Unicidad#dos_tarifas_nacionales_del_mismo_supuesto_si_chocan()}
 * demuestra que el centinela funciona: dos nacionales con el municipio vacio
 * chocan de verdad, cuando con {@code NULL} la base habria admitido las dos —en
 * SQL dos {@code NULL} no son iguales— y la consulta habria devuelto dos filas
 * para la misma vigencia.</li>
 * <li>{@link Unicidad#dos_vigencias_abiertas_del_mismo_supuesto_no_caben()}
 * demuestra la otra mitad: {@code current_rule_marker} impide el solape que la
 * ficha original no impedia.</li>
 * </ul>
 *
 * <p>
 * <b>Aqui NO se insertan municipios, y esa ausencia es el arreglo.</b>
 * Liquibase siembra la DIVIPOLA completa (changeset 114), asi que Bogota
 * (11001) y Medellin (05001) YA existen en {@code cities} con su
 * {@code dane_code}. Insertarlos otra vez chocaba contra
 * {@code uq_cities_dane_code}, que es GLOBAL, y tumbaba los veintidos casos de
 * esta clase en el {@code @BeforeEach}. La clave foranea de
 * {@code municipality_code} apunta a {@code cities.dane_code}, no a un id, asi
 * que basta con nombrar el codigo que ya esta sembrado.
 *
 * <p>
 * <b>Y por eso los codigos son los REALES</b> y no unos sinteticos: el mismo
 * {@code WithholdingRateRuleMother} lo usan los tests unitarios de la rodaja,
 * que afirman {@code "11001"} literalmente. Cambiar la constante para esquivar
 * la unicidad de una tabla arrastraba diecisiete casos unitarios que no tienen
 * nada que ver con la base de datos.
 *
 * <p>
 * <b>Por que el {@code @Import} no va pelado.</b>
 * {@code PersistenceSliceConfig} es infraestructura compartida y todavia no
 * enumera el adaptador ni el mapper de esta feature; hasta que se anadan alli,
 * la rodaja tiene que importarlos, y eso le da una clave de contexto propia y
 * le cuesta un arranque entero. Es deuda conocida, no una decision: mover las
 * dos clases a {@code PersistenceSliceConfig} y dejar aqui el {@code @Import}
 * pelado devuelve la rodaja a la cache de contextos.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaWithholdingRateRuleRepository — tarifas de retencion contra MySQL real")
class WithholdingRateRulePersistenceIT extends AbstractDataJpaTest {

    /**
     * Vigencia propia de este IT, en un ano que NINGUN changeset siembra.
     *
     * <p>
     * Los changesets 361 y 363 insertan tarifas reales, permanentes y ABIERTAS
     * ({@code valid_to = NULL}) para seis trios de (tipo, naturaleza, municipio):
     * ICA + CONSULTING/TECHNICAL_SERVICE/SOFTWARE_LICENSING + Bogota (11001) -361-
     * e INCOME_TAX + SOFTWARE_LICENSING/TECHNICAL_SERVICE/CONSULTING nacional -361
     * la primera, 363 las otras dos: 361 deja ese hueco fuera a proposito ("NO se
     * siembra INCOME_TAX para TECHNICAL_SERVICE ni CONSULTING") y 363 lo cierra
     * despues, con la misma fecha-.
     *
     * <p>
     * Eso son DOS barandillas distintas, y las dos importan aqui:
     * <ul>
     * <li>{@code uq_withholding_rate_rules_case} (tipo, naturaleza,
     * municipality_key, valid_from) — mover {@code valid_from} a {@code DESDE_IT}
     * basta para esquivarla, porque SI mira la fecha.</li>
     * <li>{@code uq_withholding_rate_rules_current}, sobre la columna generada
     * {@code current_rule_marker} = {@code CONCAT(tipo,'|',naturaleza,municipio)}
     * SOLO cuando {@code valid_to IS NULL} — es decir, UNA fila abierta por trio,
     * PASE LO QUE PASE CON LA FECHA. Mover {@code valid_from} no la esquiva: las
     * seis filas sembradas siguen abiertas, y cualquier fila abierta nueva para uno
     * de esos seis trios choca igual, en 2026, en 2029 o en cualquier ano.</li>
     * </ul>
     *
     * <p>
     * Por eso el arreglo tiene DOS partes: desplazar TRES anos (+3) TODAS las
     * fechas de este fichero -no solo {@code DESDE_IT}, para conservar los mismos
     * huecos relativos entre vigencias-, Y ademas evitar los seis trios ocupados
     * cuando el caso necesita una fila ABIERTA: Medellin (05001) o Cali (76001, ver
     * {@link #CALI}) en vez de Bogota para ICA, VAT en vez de INCOME_TAX para
     * nacional -VAT no lo siembra ni 361 ni 363-. Los casos que solo listan (con
     * {@code findAllEnabled}, que no mira {@code valid_to}) usan la tercera salida:
     * insertar la fila ya CERRADA, con lo que {@code current_rule_marker} vale
     * {@code NULL} y la barandilla no aplica.
     *
     * <p>
     * <b>No se toca {@code DESDE_IT}</b> ni {@code .BOGOTA}: media docena de tests
     * unitarios de esta rodaja (Create/Resolve/List/Find*ServiceTest*,
     * WithholdingRateRuleTest, WithholdingRateRuleJpaMapperTest, *ControllerTest*)
     * afirman esos literales sobre dominio, mapper o JSON sin tocar la base de
     * datos, y no tienen nada que ver con esta unicidad.
     */
    private static final LocalDate DESDE_IT = LocalDate.of(2029, 1, 1);
    private static final LocalDate HASTA_IT = LocalDate.of(2030, 1, 1);

    /**
     * Codigo DANE real de Cali (Valle del Cauca), sembrado por el changeset 114
     * igual que Bogota y Medellin. Los casos de ICA que necesitan DOS municipios
     * libres a la vez usan Cali en el papel que antes hacia Bogota: 11001 tiene
     * abierta y permanente la fila ICA + CONSULTING que siembra 361, y CUALQUIER
     * fila abierta nueva para ese mismo trio -aunque cambie la fecha- choca contra
     * uq_withholding_rate_rules_current, que no mira valid_from en absoluto: va por
     * (tipo, naturaleza, municipio) sin fecha, mientras la fila este abierta.
     */
    private static final String CALI = "76001";

    /**
     * El dia con el que se pregunta: dentro de la vigencia de todo lo sembrado en
     * el IT.
     */
    private static final LocalDate EL_DIA_DE_LA_FACTURA = LocalDate.of(2029, 6, 15);

    @Autowired
    private JpaWithholdingRateRuleRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda la tarifa de ICA y la recupera con cada campo en su sitio")
        void guarda_la_tarifa_de_ica_y_la_recupera_campo_a_campo() {
            // Medellin, no Bogota: 361 dejo ICA + CONSULTING + 11001 ABIERTA para
            // siempre, y una fila abierta nueva para ese mismo trio choca contra
            // uq_withholding_rate_rules_current sin importar la fecha que se use.
            WithholdingRateRule guardada = repository.save(ica(WithholdingRateRuleMother.MEDELLIN,
                    WithholdingRateRuleMother.ICA_BOGOTA, DESDE_IT, null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getWithholdingType()).isEqualTo(WithholdingType.ICA);
                assertThat(recuperada.getServiceNature()).isEqualTo(ServiceNature.CONSULTING);
                assertThat(recuperada.getMunicipalityCode())
                        .isEqualTo(WithholdingRateRuleMother.MEDELLIN);
                assertThat(recuperada.getMinimumBaseAmount()).isEqualByComparingTo("213010.00");
                assertThat(recuperada.getMinimumBaseUvt()).isEqualByComparingTo("4.00");
                assertThat(recuperada.getValidFrom()).isEqualTo(DESDE_IT);
                assertThat(recuperada.getValidTo()).isNull();
                assertThat(recuperada.isEnabled()).isTrue();
                assertThat(recuperada.getVersion()).isNotNull();
            });
        }

        @Test
        @DisplayName("el 4,14 por mil sobrevive entero al viaje por la columna DECIMAL(9,6)")
        void el_414_por_mil_sobrevive_entero() {
            // Es EL caso de la decision de tipo del changeset 317. Con cuatro
            // decimales, 0.414000 se corta a 0.4140 y con dos a 0.41: se retendria
            // casi un uno por ciento de menos en cada factura, calculado en
            // silencio y sin un solo error. Si algun dia alguien baja la escala de
            // la columna, este caso lo caza aqui y no cuadrando la cartera.
            WithholdingRateRule guardada = repository.save(ica(WithholdingRateRuleMother.MEDELLIN,
                    new BigDecimal("0.414000"), DESDE_IT, null));
            entityManager.flush();
            entityManager.clear();

            BigDecimal recuperada = repository.findById(guardada.getId()).orElseThrow()
                    .getRatePercent();
            assertThat(recuperada).isEqualByComparingTo("0.414000");
            assertThat(recuperada.scale()).isEqualTo(6);
            assertThat(recuperada).isNotEqualByComparingTo("0.41");
        }

        @Test
        @DisplayName("cerrar la vigencia edita la fila y mueve la version, no inserta otra")
        void cerrar_la_vigencia_edita_la_fila_y_mueve_la_version() {
            // VAT + CONSULTING, no INCOME_TAX + TECHNICAL_SERVICE: 363 dejo ese
            // ultimo trio con una fila ABIERTA y permanente, y esta prueba necesita
            // insertar OTRA fila abierta del mismo trio -eso es justo lo que va a
            // cerrar-. Cualquier fila abierta nueva del trio ya ocupado choca
            // contra uq_withholding_rate_rules_current sin importar la fecha.
            //
            // La tabla tampoco es solo lo que este caso inserta: 361 y 363 la
            // sembraron con seis filas reales y permanentes (es un catalogo
            // global, sin company). "Antes" fija ese punto de partida en vez de
            // asumir tabla vacia, para que la asercion de mas abajo mida lo que
            // este caso realmente anadio y no un cero que ya no es cierto.
            long antes = repository.findAllEnabled(0, 20).totalElements();

            WithholdingRateRule abierta = repository
                    .save(nacional(WithholdingType.VAT, ServiceNature.CONSULTING, DESDE_IT, null));
            entityManager.flush();
            entityManager.clear();

            WithholdingRateRule cerrada = repository
                    .save(repository.findById(abierta.getId()).orElseThrow().close(HASTA_IT));
            entityManager.flush();
            entityManager.clear();

            assertThat(cerrada.getId()).isEqualTo(abierta.getId());
            assertThat(repository.findAllEnabled(0, 20).totalElements()).isEqualTo(antes + 1L);
            assertThat(repository.findById(abierta.getId())).get().satisfies(fila -> {
                assertThat(fila.getValidTo()).isEqualTo(HASTA_IT);
                assertThat(fila.getVersion()).isEqualTo(1L);
            });
        }
    }

    @Nested
    @DisplayName("Unicidad")
    class Unicidad {

        @Test
        @DisplayName("dos tarifas nacionales del mismo supuesto SI chocan, gracias al centinela")
        void dos_tarifas_nacionales_del_mismo_supuesto_si_chocan() {
            // El arreglo numero uno del changeset 317, y el unico sitio donde se
            // puede comprobar. Las dos tienen municipality_code NULL: en un indice
            // unico sobre esa columna, dos NULL no son iguales y la base habria
            // admitido las dos —una al 3,5 y otra al 4 para el mismo servicio y la
            // misma fecha—, la consulta habria devuelto dos filas y el codigo se
            // habria quedado con la primera que llegara. La unicidad se construye
            // sobre municipality_key, que vale '-' y nunca es nula.
            //
            // VAT + CONSULTING, no INCOME_TAX + TECHNICAL_SERVICE: la PRIMERA fila de
            // este caso tiene que entrar limpia para que sea la SEGUNDA la que choca
            // -de eso trata el caso-. Con INCOME_TAX + TECHNICAL_SERVICE la primera ya
            // revienta contra la fila abierta y permanente de 363
            // (uq_withholding_rate_rules_current), y el caso pasaria por el motivo
            // equivocado -o por ninguno, porque ni siquiera llegaria a la segunda-.
            repository
                    .save(nacional(WithholdingType.VAT, ServiceNature.CONSULTING, DESDE_IT, null));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_withholding_rate_rules_case", () -> {
                // Cerrada a proposito: con valid_to escrito su current_rule_marker
                // vale NULL, asi que uq_..._current no puede saltar antes y la
                // unica barandilla que queda es la del supuesto.
                repository.save(nacional(WithholdingType.VAT, ServiceNature.CONSULTING, DESDE_IT,
                        HASTA_IT));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("dos vigencias abiertas del mismo supuesto no caben: current_rule_marker")
        void dos_vigencias_abiertas_del_mismo_supuesto_no_caben() {
            // El arreglo numero dos. Fechas de inicio DISTINTAS, asi que
            // uq_..._case no aplica y la unica que puede parar la segunda es la
            // unicidad del marcador de vigencia abierta. Sin ella nada impedia dos
            // tarifas solapadas para el mismo caso.
            repository.save(nacional(WithholdingType.VAT, ServiceNature.SOFTWARE_LICENSING,
                    DESDE_IT, null));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_withholding_rate_rules_current", () -> {
                repository.save(nacional(WithholdingType.VAT, ServiceNature.SOFTWARE_LICENSING,
                        LocalDate.of(2029, 7, 1), null));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("cerrar la vigente libera el hueco para su relevo")
        void cerrar_la_vigente_libera_el_hueco_para_su_relevo() {
            // La otra cara del caso anterior, y la razon por la que cerrar existe:
            // en cuanto valid_to deja de ser nulo, current_rule_marker pasa a NULL
            // y la siguiente tarifa del mismo supuesto ya entra.
            WithholdingRateRule vieja = repository
                    .save(nacional(WithholdingType.VAT, ServiceNature.CONSULTING, DESDE_IT, null));
            entityManager.flush();
            entityManager.clear();

            repository.save(repository.findById(vieja.getId()).orElseThrow()
                    .close(LocalDate.of(2029, 7, 1)));
            entityManager.flush();

            WithholdingRateRule relevo = repository.save(nacional(WithholdingType.VAT,
                    ServiceNature.CONSULTING, LocalDate.of(2029, 7, 1), null));
            entityManager.flush();

            assertThat(relevo.getId()).isNotEqualTo(vieja.getId());
        }

        @Test
        @DisplayName("dos municipios distintos si pueden tener la misma tarifa a la vez")
        void dos_municipios_distintos_si_pueden_convivir() {
            // Sin este caso, una unicidad demasiado ancha pasaria por buena: el
            // supuesto incluye el municipio, y Cali y Medellin son supuestos
            // distintos.
            //
            // Cali, no Bogota: 11001 tiene la fila ICA + CONSULTING abierta y
            // permanente de 361, y una fila abierta nueva del mismo trio choca
            // contra uq_withholding_rate_rules_current sin importar la fecha.
            //
            // "Antes" descuenta las seis filas permanentes de 361/363: la tabla es
            // un catalogo global sin company y esta clase no es la unica duena de
            // su contenido.
            long antes = repository.findAllEnabled(0, 20).totalElements();

            repository.save(ica(CALI, WithholdingRateRuleMother.ICA_BOGOTA, DESDE_IT, null));
            repository.save(ica(WithholdingRateRuleMother.MEDELLIN,
                    WithholdingRateRuleMother.ICA_BOGOTA, DESDE_IT, null));
            entityManager.flush();

            assertThat(repository.findAllEnabled(0, 20).totalElements()).isEqualTo(antes + 2L);
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("ICA sin municipio lo para chk_withholding_rate_rules_municipality")
        void ica_sin_municipio_lo_para_el_check() {
            // El dominio ya rechaza esta combinacion, asi que la unica forma de
            // comprobar que la base tambien la rechaza —el cinturon bajo el
            // tirante— es escribir la fila por SQL nativo, saltandose el agregado.
            EngineConstraint.assertViolates("chk_withholding_rate_rules_municipality",
                    () -> insertarCruda("ICA", "CONSULTING", null, "0.690000", "2026-01-01"));
        }

        @Test
        @DisplayName("una retencion nacional con municipio lo para el mismo check")
        void una_nacional_con_municipio_lo_para_el_mismo_check() {
            // La otra mitad del CHECK. Sin este caso, uno que solo mirara la rama
            // de ICA pasaria por bueno y habria dos filas para el mismo supuesto
            // nacional —una con municipio y otra sin el— que la unicidad no veria
            // como iguales.
            EngineConstraint.assertViolates("chk_withholding_rate_rules_municipality",
                    () -> insertarCruda("INCOME_TAX", "TECHNICAL_SERVICE",
                            WithholdingRateRuleMother.BOGOTA, "11.000000", "2026-01-01"));
        }

        @Test
        @DisplayName("un municipio que no existe lo para la clave foranea contra cities.dane_code")
        void un_municipio_que_no_existe_lo_para_la_fk() {
            // La FK apunta a dane_code y no a cities.id, que es lo inusual: el
            // changeset 315 alineo esa columna a ascii_bin y la hizo unica justo
            // para que esta clave sea posible.
            EngineConstraint.assertViolates("fk_withholding_rate_rules_municipality",
                    () -> insertarCruda("ICA", "CONSULTING", "99999", "0.690000", "2026-01-01"));
        }

        @Test
        @DisplayName("una tarifa por encima de 100 la para chk_withholding_rate_rules_rate")
        void una_tarifa_por_encima_de_cien_la_para_el_check() {
            EngineConstraint.assertViolates("chk_withholding_rate_rules_rate",
                    () -> insertarCruda("INCOME_TAX", "TECHNICAL_SERVICE", null, "100.000001",
                            "2026-01-01"));
        }

        @Test
        @DisplayName("un tipo de retencion fuera de la lista lo para su CHECK")
        void un_tipo_fuera_de_la_lista_lo_para_su_check() {
            EngineConstraint.assertViolates("chk_withholding_rate_rules_type",
                    () -> insertarCruda("WEALTH_TAX", "TECHNICAL_SERVICE", null, "11.000000",
                            "2026-01-01"));
        }

        @Test
        @DisplayName("una naturaleza de servicio fuera de la lista compartida lo para su CHECK")
        void una_naturaleza_fuera_de_la_lista_lo_para_su_check() {
            // La lista se escribe igual aqui y en catalog_items (229). Este CHECK
            // es la razon por la que una divergencia falla en voz alta en vez de
            // dejar la retencion esperada en cero.
            EngineConstraint.assertViolates("chk_withholding_rate_rules_service_nature",
                    () -> insertarCruda("INCOME_TAX", "CONSULTANCY", null, "11.000000",
                            "2026-01-01"));
        }
    }

    @Nested
    @DisplayName("Resolucion de la tarifa vigente")
    class ResolucionDeLaTarifaVigente {

        @Test
        @DisplayName("encuentra la nacional pese a que su municipio es NULL: el centinela funciona")
        void encuentra_la_nacional_pese_al_municipio_nulo() {
            // Si la consulta comparara municipality_code = NULL, esto devolveria
            // CERO filas para toda retencion nacional: la retencion esperada
            // saldria cero y no habria un solo error que lo delatara.
            //
            // VAT + CONSULTING, no INCOME_TAX + TECHNICAL_SERVICE: 363 dejo ese
            // ultimo trio con una fila ABIERTA y permanente, y esta prueba necesita
            // insertar OTRA fila abierta del mismo trio, lo que choca contra
            // uq_withholding_rate_rules_current sin importar la fecha.
            repository
                    .save(nacional(WithholdingType.VAT, ServiceNature.CONSULTING, DESDE_IT, null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findEffective(WithholdingType.VAT, ServiceNature.CONSULTING, null,
                    EL_DIA_DE_LA_FACTURA)).get()
                    .satisfies(tarifa -> assertThat(tarifa.getRatePercent())
                            .isEqualByComparingTo("11.000000"));
        }

        @Test
        @DisplayName("encuentra la municipal por su codigo y no la confunde con otro municipio")
        void encuentra_la_municipal_y_no_la_confunde() {
            // Cali, no Bogota: 11001 tiene la fila ICA + CONSULTING abierta y
            // permanente de 361, y una fila abierta nueva del mismo trio choca
            // contra uq_withholding_rate_rules_current sin importar la fecha.
            repository.save(ica(CALI, new BigDecimal("0.690000"), DESDE_IT, null));
            repository.save(ica(WithholdingRateRuleMother.MEDELLIN, new BigDecimal("0.414000"),
                    DESDE_IT, null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findEffective(WithholdingType.ICA, ServiceNature.CONSULTING, CALI,
                    EL_DIA_DE_LA_FACTURA)).get()
                    .satisfies(tarifa -> assertThat(tarifa.getRatePercent())
                            .isEqualByComparingTo("0.690000"));
            assertThat(repository.findEffective(WithholdingType.ICA, ServiceNature.CONSULTING,
                    WithholdingRateRuleMother.MEDELLIN, EL_DIA_DE_LA_FACTURA)).get()
                    .satisfies(tarifa -> assertThat(tarifa.getRatePercent())
                            .isEqualByComparingTo("0.414000"));
        }

        @Test
        @DisplayName("una retencion nacional no encuentra la municipal ni al reves")
        void una_nacional_no_encuentra_la_municipal_ni_al_reves() {
            // Medellin, no Bogota: 361 dejo ICA + CONSULTING + 11001 ABIERTA para
            // siempre, y una fila abierta nueva para ese mismo trio choca contra
            // uq_withholding_rate_rules_current sin importar la fecha que se use.
            repository.save(ica(WithholdingRateRuleMother.MEDELLIN,
                    WithholdingRateRuleMother.ICA_BOGOTA, DESDE_IT, null));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findEffective(WithholdingType.ICA, ServiceNature.CONSULTING, null,
                    EL_DIA_DE_LA_FACTURA)).isEmpty();
        }

        @Test
        @DisplayName("el dia de la fecha de fin la tarifa YA no se resuelve: el limite es estricto")
        void el_dia_de_la_fecha_de_fin_ya_no_se_resuelve() {
            // Es lo que permite que la tarifa que se cierra el 1 de julio y la que
            // empieza ese mismo dia se releven sin solaparse un dia entero.
            //
            // VAT + CONSULTING y no INCOME_TAX + TECHNICAL_SERVICE: 363 sembro este
            // ultimo supuesto ABIERTO (sin valid_to) desde 2026-01-01, asi que
            // NINGUNA fecha futura devolveria vacio para el -siempre habria una fila
            // sembrada vigente-. VAT no lo siembra ni 361 ni 363: es el unico par
            // libre para probar de verdad el limite "ya no se resuelve".
            repository.save(nacional(WithholdingType.VAT, ServiceNature.CONSULTING, DESDE_IT,
                    LocalDate.of(2029, 7, 1)));
            entityManager.flush();
            entityManager.clear();

            assertThat(resolverEn(WithholdingType.VAT, ServiceNature.CONSULTING,
                    LocalDate.of(2029, 6, 30))).isPresent();
            assertThat(resolverEn(WithholdingType.VAT, ServiceNature.CONSULTING,
                    LocalDate.of(2029, 7, 1))).isEmpty();
        }

        @Test
        @DisplayName("antes de la fecha de inicio tampoco: el limite inferior es inclusivo")
        void antes_de_la_fecha_de_inicio_tampoco() {
            // Misma razon que el caso anterior para usar VAT + CONSULTING: 363
            // siembra INCOME_TAX + TECHNICAL_SERVICE abierto desde 2026-01-01, y esa
            // fila seguiria siendo vigente en CUALQUIER fecha anterior a 2029 que
            // este caso probara, asi que "isEmpty()" nunca se cumpliria con ese
            // supuesto por mucho que se desplace la fecha propia del IT.
            repository
                    .save(nacional(WithholdingType.VAT, ServiceNature.CONSULTING, DESDE_IT, null));
            entityManager.flush();
            entityManager.clear();

            assertThat(resolverEn(WithholdingType.VAT, ServiceNature.CONSULTING,
                    LocalDate.of(2028, 12, 31))).isEmpty();
            assertThat(resolverEn(WithholdingType.VAT, ServiceNature.CONSULTING, DESDE_IT))
                    .isPresent();
        }

        @Test
        @DisplayName("una tarifa deshabilitada no se resuelve ni aparece en el catalogo")
        void una_tarifa_deshabilitada_no_se_resuelve() {
            // VAT + CONSULTING, no INCOME_TAX + TECHNICAL_SERVICE: 363 siembra ese
            // ultimo supuesto habilitado y abierto, asi que resolverlo SIEMPRE
            // encontraria la fila sembrada -no la de este caso, que va deshabilitada,
            // pero si la real- y la asercion de "isEmpty()" no probaria lo que dice.
            //
            // "Antes" descuenta las seis filas permanentes de 361/363 del recuento
            // del catalogo: la tabla no la posee en exclusiva este test.
            long antes = repository.findAllEnabled(0, 20).totalElements();

            repository.save(new WithholdingRateRule(null, WithholdingType.VAT,
                    ServiceNature.CONSULTING, null, new BigDecimal("11.000000"),
                    WithholdingRateRuleMother.BASE_EN_PESOS, null, null, DESDE_IT, null,
                    WithholdingRateRuleMother.CREADA_EL, false, null));
            entityManager.flush();
            entityManager.clear();

            assertThat(
                    resolverEn(WithholdingType.VAT, ServiceNature.CONSULTING, EL_DIA_DE_LA_FACTURA))
                    .isEmpty();
            assertThat(repository.findAllEnabled(0, 20).totalElements()).isEqualTo(antes);
        }

        @Test
        @DisplayName("entre dos vigencias cerradas solapadas se queda con la mas reciente")
        void entre_dos_cerradas_solapadas_se_queda_con_la_mas_reciente() {
            // uq_..._current solo protege a las abiertas, asi que un historico mal
            // cargado puede dejar dos cerradas que se pisan. La respuesta a «que
            // tarifa aplico» no puede depender del plan que elija el motor.
            repository.save(nacional(WithholdingType.INCOME_TAX, ServiceNature.TECHNICAL_SERVICE,
                    DESDE_IT, LocalDate.of(2029, 12, 1)));
            repository.save(nacionalCon(new BigDecimal("10.000000"), LocalDate.of(2029, 4, 1),
                    LocalDate.of(2029, 12, 1)));
            entityManager.flush();
            entityManager.clear();

            assertThat(resolverNacionalEn(EL_DIA_DE_LA_FACTURA)).get()
                    .satisfies(tarifa -> assertThat(tarifa.getValidFrom())
                            .isEqualTo(LocalDate.of(2029, 4, 1)));
        }
    }

    @Nested
    @DisplayName("Listado")
    class Listado {

        @Test
        @DisplayName("agrupa por supuesto y desempata por id, con los totales de la consulta")
        void agrupa_por_supuesto_y_desempata_por_id() {
            // "Antes" descuenta las seis filas permanentes de 361/363: el catalogo
            // es global y esta clase no es su unica duena, asi que el total real
            // de la consulta ya no es "lo que este caso inserto" a secas.
            long antes = repository.findAllEnabled(0, 20).totalElements();

            // Las tres filas van CERRADAS (con HASTA_IT), no abiertas: este caso solo
            // ejercita findAllEnabled, que no mira valid_to en absoluto, asi que
            // cerrarlas no cambia lo que se prueba. INCOME_TAX + TECHNICAL_SERVICE y
            // ICA + CONSULTING + Bogota SI tienen ya una fila ABIERTA y permanente
            // (363 y 361), y uq_withholding_rate_rules_current no mira valid_from:
            // una fila abierta nueva de cualquiera de esos dos trios chocaria pase
            // lo que pase con la fecha. Cerrada, su current_rule_marker es NULL y la
            // barandilla no aplica.
            WithholdingRateRule vat = repository.save(nacional(WithholdingType.VAT,
                    ServiceNature.SOFTWARE_LICENSING, DESDE_IT, HASTA_IT));
            WithholdingRateRule incomeTax = repository.save(nacional(WithholdingType.INCOME_TAX,
                    ServiceNature.TECHNICAL_SERVICE, DESDE_IT, HASTA_IT));
            WithholdingRateRule icaBogota = repository.save(ica(WithholdingRateRuleMother.BOGOTA,
                    WithholdingRateRuleMother.ICA_BOGOTA, DESDE_IT, HASTA_IT));
            entityManager.flush();
            entityManager.clear();

            PageResult<WithholdingRateRule> pagina = repository.findAllEnabled(0, 20);
            List<Long> propias = List.of(icaBogota.getId(), incomeTax.getId(), vat.getId());

            // El orden del enum en la columna es alfabetico sobre el literal:
            // ICA, INCOME_TAX, VAT. Se filtra a las tres filas de este caso porque
            // 361/363 mezclan mas ICA e INCOME_TAX en la misma consulta global; la
            // relacion de orden entre las TRES propias sigue siendo la que la
            // consulta devolvio, asi que el desempate se sigue probando de verdad.
            assertThat(pagina.content().stream().filter(fila -> propias.contains(fila.getId()))
                    .toList()).extracting(WithholdingRateRule::getWithholdingType).containsExactly(
                            WithholdingType.ICA, WithholdingType.INCOME_TAX, WithholdingType.VAT);
            assertThat(pagina.totalElements()).isEqualTo(antes + 3L);
        }

        @Test
        @DisplayName("la pagina respeta el tamano pedido y reporta el total de la tabla")
        void la_pagina_respeta_el_tamano_y_reporta_el_total() {
            // "Antes" descuenta las seis filas permanentes de 361/363: el total
            // real de la tabla ya no es solo lo que este caso inserta.
            long antes = repository.findAllEnabled(0, 20).totalElements();

            // Cerradas (HASTA_IT), no abiertas: findAllEnabled ignora valid_to, y
            // dos de estos tres trios ya tienen una fila abierta permanente (361 y
            // 363) que uq_withholding_rate_rules_current no dejaria repetir abierta
            // sin importar la fecha.
            repository.save(nacional(WithholdingType.VAT, ServiceNature.SOFTWARE_LICENSING,
                    DESDE_IT, HASTA_IT));
            repository.save(nacional(WithholdingType.INCOME_TAX, ServiceNature.TECHNICAL_SERVICE,
                    DESDE_IT, HASTA_IT));
            repository.save(ica(WithholdingRateRuleMother.BOGOTA,
                    WithholdingRateRuleMother.ICA_BOGOTA, DESDE_IT, HASTA_IT));
            entityManager.flush();
            entityManager.clear();

            long esperado = antes + 3L;
            PageResult<WithholdingRateRule> pagina = repository.findAllEnabled(0, 2);

            assertThat(pagina.content()).hasSize(2);
            // Recalcular el total sobre el contenido paginado es como se acaba
            // reportando «2 de N» en un catalogo mas grande.
            assertThat(pagina.totalElements()).isEqualTo(esperado);
            assertThat(pagina.totalPages()).isEqualTo((int) Math.ceil(esperado / 2.0));
        }
    }

    // --- andamio ------------------------------------------------------------

    private Optional<WithholdingRateRule> resolverNacionalEn(LocalDate dia) {
        return resolverEn(WithholdingType.INCOME_TAX, ServiceNature.TECHNICAL_SERVICE, dia);
    }

    /**
     * Version general de {@link #resolverNacionalEn(LocalDate)}, para los casos que
     * necesitan un supuesto distinto de INCOME_TAX + TECHNICAL_SERVICE -tipico
     * cuando el caso quiere un "isEmpty()" y ese supuesto en concreto ya no puede
     * darlo, porque 363 lo siembra abierto desde 2026-01-01-.
     */
    private Optional<WithholdingRateRule> resolverEn(WithholdingType tipo, ServiceNature naturaleza,
            LocalDate dia) {
        return repository.findEffective(tipo, naturaleza, null, dia);
    }

    private static WithholdingRateRule nacional(WithholdingType tipo, ServiceNature naturaleza,
            LocalDate desde, LocalDate hasta) {
        return new WithholdingRateRule(null, tipo, naturaleza, null, new BigDecimal("11.000000"),
                WithholdingRateRuleMother.BASE_EN_PESOS, WithholdingRateRuleMother.BASE_EN_UVT,
                "Art. 392 ET", desde, hasta, WithholdingRateRuleMother.CREADA_EL, true, null);
    }

    private static WithholdingRateRule nacionalCon(BigDecimal tarifa, LocalDate desde,
            LocalDate hasta) {
        return new WithholdingRateRule(null, WithholdingType.INCOME_TAX,
                ServiceNature.TECHNICAL_SERVICE, null, tarifa,
                WithholdingRateRuleMother.BASE_EN_PESOS, null, "Art. 392 ET", desde, hasta,
                WithholdingRateRuleMother.CREADA_EL, true, null);
    }

    private static WithholdingRateRule ica(String municipio, BigDecimal tarifa, LocalDate desde,
            LocalDate hasta) {
        return new WithholdingRateRule(null, WithholdingType.ICA, ServiceNature.CONSULTING,
                municipio, tarifa, WithholdingRateRuleMother.BASE_EN_PESOS,
                WithholdingRateRuleMother.BASE_EN_UVT, "Acuerdo 65 de 2002", desde, hasta,
                WithholdingRateRuleMother.CREADA_EL, true, null);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para los CHECK que el dominio
     * ya replica: sin ella no habria forma de comprobar que la base tambien los
     * cuida.
     *
     * <p>
     * No nombra {@code municipality_key} ni {@code current_rule_marker}: son
     * columnas generadas y un {@code INSERT} que les diera valor lo rechazaria el
     * motor por otro motivo, y el caso pasaria por la razon equivocada.
     */
    private void insertarCruda(String tipo, String naturaleza, String municipio, String tarifa,
            String desde) {
        entityManager.createNativeQuery("""
                INSERT INTO withholding_rate_rules (withholding_type, service_nature,
                                                    municipality_code, rate_percent,
                                                    minimum_base_uvt, valid_from,
                                                    created_date, enabled, version)
                VALUES (:tipo, :naturaleza, :municipio, :tarifa, 4.00, :desde, NOW(6), true, 0)
                """).setParameter("tipo", tipo).setParameter("naturaleza", naturaleza)
                .setParameter("municipio", municipio).setParameter("tarifa", new BigDecimal(tarifa))
                .setParameter("desde", LocalDate.parse(desde)).executeUpdate();
    }

}
