package com.vetsoftware.app.bankreceipt.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.bankreceipt.testsupport.BankReceiptMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaBankReceiptRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar es la comparacion EXACTA de la
 * referencia bancaria</b>, y es lo unico de toda la feature que no se puede
 * comprobar sin el motor: la decision vive en la colacion {@code ascii_bin} de
 * la columna, no en una linea de Java. Bajo la colacion heredada del esquema,
 * {@code TRX-A} y {@code trx-a} serian la misma entrada y
 * {@code uq_bank_receipts_reference} descartaria la segunda consignacion del
 * dia como duplicada.
 * {@link UnicidadDeLaReferencia#dos_capitalizaciones_son_dos_filas()} congela
 * que hoy no lo hace.
 *
 * <p>
 * <b>No hay siembra, y no es un olvido.</b> {@code bank_receipts} no tiene ni
 * una clave foranea —ni siquiera a {@code companies}, porque antes de
 * identificar una entrada no hay cliente—, asi que no hay nada que satisfacer
 * antes de insertar. Las filas de apoyo que necesitan un {@code id} controlado
 * se escriben con SQL nativo en el rango 8700, que ninguna otra rodaja usa.
 *
 * <p>
 * <b>Por que el adaptador se construye a mano.</b>
 * {@code PersistenceSliceConfig} reune los adaptadores de las rodajas para que
 * todas compartan una unica clave de {@code MergedContextConfiguration} y, con
 * ella, un unico contexto cacheado. Declarar aqui un {@code @Import} propio con
 * este adaptador volveria a darle a esta clase una clave unica y un arranque de
 * contexto entero para ella sola. Instanciarlo con el
 * {@code JpaBankReceiptRepository} de Spring Data que la rodaja ya expone
 * cuesta una linea y no ejercita menos SQL: la consulta que se ejecuta contra
 * MySQL es exactamente la misma.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaBankReceiptRepository — el extracto contra MySQL real")
class BankReceiptPersistenceIT extends AbstractDataJpaTest {

    /** Ids del rango reservado a esta rodaja. */
    private static final Long ENTRADA_CRUDA = 8700L;

    private static final LocalDate DIA = LocalDate.of(2026, 3, 5);
    private static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 3, 7, 8, 45, 0);
    private static final LocalDateTime SELLADA_EL = LocalDateTime.of(2026, 3, 9, 16, 20, 30);

    @Autowired
    private BankReceiptJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaBankReceiptRepository repository;

    @BeforeEach
    void adaptador() {
        repository = new JpaBankReceiptRepository(springDataRepository, new BankReceiptJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda la entrada y la recupera con cada fecha e importe en su sitio")
        void guarda_la_entrada_y_la_recupera_campo_a_campo() {
            BankReceipt guardada = repository.save(BankReceiptMother.enLaBandeja());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getBankAccountRef()).isEqualTo(BankReceiptMother.CUENTA);
                assertThat(recuperada.getBankReference()).isEqualTo(BankReceiptMother.REFERENCIA);
                assertThat(recuperada.getReceivedOn()).isEqualTo(BankReceiptMother.RECIBIDA_EL);
                assertThat(recuperada.getAmount()).isEqualByComparingTo("217345.61");
                assertThat(recuperada.getDescription()).isEqualTo(BankReceiptMother.DESCRIPCION);
                assertThat(recuperada.getStatus()).isEqualTo(BankReceiptStatus.UNIDENTIFIED);
                assertThat(recuperada.getIdentifiedAt()).isNull();
                assertThat(recuperada.getCreatedDate()).isEqualTo(BankReceiptMother.CREADA_EL);
                assertThat(recuperada.getVersion()).isZero();
            });
        }

        @Test
        @DisplayName("un importe NEGATIVO entra en la base: el CHECK es amount <> 0")
        void un_importe_negativo_entra_en_la_base() {
            // La otra mitad de la decision que el dominio documenta. Si alguien
            // endureciera el CHECK a `amount > 0` en un changeset futuro, este caso se
            // pone rojo y no la aplicacion en produccion a mitad de una carga.
            BankReceipt cargo = repository
                    .save(BankReceiptMother.conImporte(new BigDecimal("-45000.00")));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(cargo.getId())).get()
                    .satisfies(recuperado -> assertThat(recuperado.getAmount())
                            .isEqualByComparingTo("-45000.00"));
        }

        @Test
        @DisplayName("el ciclo completo: la entrada sale de la bandeja y queda sellada")
        void el_ciclo_completo_deja_la_entrada_sellada() {
            BankReceipt guardada = repository.save(BankReceiptMother.enLaBandeja());
            entityManager.flush();
            entityManager.clear();

            BankReceipt cargada = repository.findById(guardada.getId()).orElseThrow();
            cargada.identify(SELLADA_EL);
            repository.save(cargada);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(resuelta -> {
                assertThat(resuelta.getStatus()).isEqualTo(BankReceiptStatus.IDENTIFIED);
                assertThat(resuelta.getIdentifiedAt()).isEqualTo(SELLADA_EL);
                // La version se movio: el UPDATE paso por el ciclo de Hibernate y no por
                // una escritura masiva que la dejaria intacta.
                assertThat(resuelta.getVersion()).isEqualTo(1L);
            });
        }
    }

    @Nested
    @DisplayName("Unicidad de la referencia")
    class UnicidadDeLaReferencia {

        @Test
        @DisplayName("la misma referencia el mismo dia la para uq_bank_receipts_reference")
        void la_misma_referencia_el_mismo_dia_la_para_la_unicidad() {
            repository.save(BankReceiptMother.recibidaEl(DIA, "TRX-DUPLICADA"));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_bank_receipts_reference", () -> {
                repository.save(BankReceiptMother.recibidaEl(DIA, "TRX-DUPLICADA"));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("la misma referencia en OTRO dia si entra: la unicidad es del par")
        void la_misma_referencia_en_otro_dia_si_entra() {
            // Los bancos reciclan consecutivos. Una unicidad solo por referencia
            // rechazaria el extracto del mes siguiente.
            repository.save(BankReceiptMother.recibidaEl(DIA, "TRX-RECICLADA"));
            repository.save(BankReceiptMother.recibidaEl(DIA.plusMonths(1), "TRX-RECICLADA"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll(0, 20).totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("dos capitalizaciones de la misma referencia son DOS entradas distintas")
        void dos_capitalizaciones_son_dos_filas() {
            // La columna es ascii_bin. Bajo la colacion heredada del esquema estas dos
            // filas serian la misma y la segunda consignacion del dia se descartaria
            // como duplicada: un ingreso real desaparecido del cuadre. Si alguien
            // devuelve la columna a utf8mb4_unicode_ci, este caso se pone rojo.
            BankReceipt minusculas = repository.save(BankReceiptMother.recibidaEl(DIA, "trx-9f2a"));
            BankReceipt mayusculas = repository.save(BankReceiptMother.recibidaEl(DIA, "TRX-9F2A"));
            entityManager.flush();
            entityManager.clear();

            assertThat(minusculas.getId()).isNotEqualTo(mayusculas.getId());
            assertThat(repository.findAll(0, 20).content())
                    .extracting(BankReceipt::getBankReference)
                    .containsExactlyInAnyOrder("trx-9f2a", "TRX-9F2A");
        }

        @Test
        @DisplayName("la comprobacion previa de duplicado tambien distingue mayusculas")
        void la_comprobacion_previa_distingue_mayusculas() {
            // Es el metodo que el service consulta ANTES de insertar. Si respondiera
            // true para la otra capitalizacion, el conflicto legible se convertiria en
            // un rechazo de una entrada perfectamente valida.
            repository.save(BankReceiptMother.recibidaEl(DIA, "trx-9f2a"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.existsByBankReferenceAndReceivedOn("trx-9f2a", DIA)).isTrue();
            assertThat(repository.existsByBankReferenceAndReceivedOn("TRX-9F2A", DIA)).isFalse();
            assertThat(repository.existsByBankReferenceAndReceivedOn("trx-9f2a", DIA.plusDays(1)))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("una entrada en la bandeja con fecha de sellado la para chk_bank_receipts_identified")
        void una_entrada_en_la_bandeja_con_sello_la_para_el_check() {
            // El dominio ya rechaza esta combinacion, asi que la unica forma de
            // comprobar que la base tambien la rechaza —el cinturon bajo el tirante— es
            // escribir la fila por SQL nativo, saltandose el agregado.
            EngineConstraint.assertViolates("chk_bank_receipts_identified",
                    () -> insertarCrudaSellada(ENTRADA_CRUDA, "TRX-CHK-1", "UNIDENTIFIED",
                            new BigDecimal("1000.00")));
        }

        @Test
        @DisplayName("una entrada resuelta sin fecha de sellado la para el mismo check")
        void una_entrada_resuelta_sin_sello_la_para_el_mismo_check() {
            // La otra mitad del bicondicional. Sin este caso, un CHECK que solo mirara
            // la rama de la bandeja pasaria por bueno y una entrada archivada quedaria
            // sin la fecha en que se dejo de buscar.
            EngineConstraint.assertViolates("chk_bank_receipts_identified",
                    () -> insertarCrudaSinSello(ENTRADA_CRUDA + 1, "TRX-CHK-2", "IDENTIFIED",
                            new BigDecimal("1000.00")));
        }

        @Test
        @DisplayName("un importe de cero lo para chk_bank_receipts_amount")
        void un_importe_de_cero_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_bank_receipts_amount",
                    () -> insertarCrudaSinSello(ENTRADA_CRUDA + 2, "TRX-CHK-3", "UNIDENTIFIED",
                            BigDecimal.ZERO));
        }

        @Test
        @DisplayName("un estado fuera de los tres lo para chk_bank_receipts_identified, "
                + "antes de que chk_bank_receipts_status llegue a mirarlo")
        void un_estado_desconocido_lo_para_el_check() {
            // La red real del enum, medida en el motor y no en la intencion del nombre.
            // chk_bank_receipts_identified exige status IN ('UNIDENTIFIED','IDENTIFIED',
            // 'DISCARDED') para que alguna de sus dos ramas pueda evaluar a TRUE: con un
            // valor fuera del enum las dos ramas dan FALSE sin importar identified_at,
            // asi que esta fila TAMBIEN viola chk_bank_receipts_identified. MySQL evalua
            // los CHECK por orden alfabetico del nombre de la restriccion
            // ("identified" antes que "status") y se detiene en la primera violacion:
            // chk_bank_receipts_status nunca llega a evaluarse para esta fila, y no hay
            // forma de construir una fila con status invalido que lo evite —el propio
            // chk_bank_receipts_identified depende de que status sea uno de los tres
            // validos—. La fila muere en el INSERT igual, solo que bajo otro nombre.
            EngineConstraint.assertViolates("chk_bank_receipts_identified",
                    () -> insertarCrudaSinSello(ENTRADA_CRUDA + 3, "TRX-CHK-4", "PENDING",
                            new BigDecimal("1000.00")));
        }
    }

    @Nested
    @DisplayName("La bandeja")
    class LaBandeja {

        @Test
        @DisplayName("solo trae las no identificadas, de la mas antigua a la mas reciente")
        void solo_trae_las_no_identificadas_por_antiguedad() {
            BankReceipt vieja = repository
                    .save(BankReceiptMother.recibidaEl(LocalDate.of(2026, 1, 10), "TRX-VIEJA"));
            BankReceipt reciente = repository
                    .save(BankReceiptMother.recibidaEl(LocalDate.of(2026, 3, 20), "TRX-RECIENTE"));
            BankReceipt archivada = repository
                    .save(BankReceiptMother.recibidaEl(LocalDate.of(2026, 2, 1), "TRX-ARCHIVADA"));
            archivada.discard(SELLADA_EL);
            repository.save(archivada);
            entityManager.flush();
            entityManager.clear();

            PageResult<BankReceipt> bandeja = repository
                    .findAllByStatus(BankReceiptStatus.UNIDENTIFIED, 0, 20);

            // La archivada queda fuera aunque su fecha caiga entre las dos: el filtro es
            // por estado, no por antiguedad.
            assertThat(bandeja.content()).extracting(BankReceipt::getId)
                    .containsExactly(vieja.getId(), reciente.getId());
            assertThat(bandeja.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("dos entradas del mismo dia desempatan por id ascendente")
        void dos_entradas_del_mismo_dia_desempatan_por_id() {
            // Un extracto trae decenas de lineas con la MISMA fecha. Sin desempate el
            // orden lo decide el motor y dos paginas consecutivas repiten u omiten
            // filas: aqui eso es una consignacion perdida del cuadre.
            BankReceipt primera = repository
                    .save(BankReceiptMother.recibidaEl(DIA, "TRX-EMPATE-A"));
            BankReceipt segunda = repository
                    .save(BankReceiptMother.recibidaEl(DIA, "TRX-EMPATE-B"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByStatus(BankReceiptStatus.UNIDENTIFIED, 0, 20).content())
                    .extracting(BankReceipt::getId)
                    .containsExactly(primera.getId(), segunda.getId());
        }

        @Test
        @DisplayName("el listado completo va al reves: lo ultimo que llego primero")
        void el_listado_completo_va_al_reves() {
            BankReceipt vieja = repository
                    .save(BankReceiptMother.recibidaEl(LocalDate.of(2026, 1, 10), "TRX-L-VIEJA"));
            BankReceipt reciente = repository
                    .save(BankReceiptMother.recibidaEl(LocalDate.of(2026, 3, 20), "TRX-L-NUEVA"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll(0, 20).content()).extracting(BankReceipt::getId)
                    .containsExactly(reciente.getId(), vieja.getId());
        }

        @Test
        @DisplayName("la pagina acotada respeta el tope del kernel de paginacion")
        void la_pagina_acotada_respeta_el_tope() {
            repository.save(BankReceiptMother.recibidaEl(DIA, "TRX-TOPE"));
            entityManager.flush();
            entityManager.clear();

            // 100000 no llega a la consulta: Pages.request lo acota a MAX_SIZE.
            assertThat(repository.findAll(0, 100000).pageSize()).isEqualTo(200);
        }
    }

    /**
     * Escritura cruda que se salta el agregado, con fecha de sellado. Solo para los
     * {@code CHECK} que el dominio ya replica: sin ella no habria forma de
     * comprobar que la base tambien los cuida.
     */
    private void insertarCrudaSellada(Long id, String referencia, String estado,
            BigDecimal importe) {
        insertarCruda(id, referencia, estado, ":sello", importe).setParameter("sello", SELLADA_EL)
                .executeUpdate();
    }

    /**
     * La misma escritura cruda sin sellar. El {@code NULL} va como literal y no
     * como parametro: una consulta nativa sin metadatos de tipo no puede inferir el
     * tipo de un {@code null}, y el fallo saldria como un error de binding que no
     * tiene nada que ver con lo que el caso quiere probar.
     */
    private void insertarCrudaSinSello(Long id, String referencia, String estado,
            BigDecimal importe) {
        insertarCruda(id, referencia, estado, "NULL", importe).executeUpdate();
    }

    private jakarta.persistence.Query insertarCruda(Long id, String referencia, String estado,
            String sello, BigDecimal importe) {
        return entityManager.createNativeQuery("""
                INSERT INTO bank_receipts (id, bank_account_ref, bank_reference, received_on,
                                           amount, description, status, identified_at,
                                           created_date, version)
                VALUES (:id, :cuenta, :referencia, :dia, :importe, 'Escritura cruda de prueba',
                        :estado, %s, :creadaEl, 0)
                """.formatted(sello)).setParameter("id", id)
                .setParameter("cuenta", BankReceiptMother.CUENTA)
                .setParameter("referencia", referencia).setParameter("dia", DIA)
                .setParameter("importe", importe).setParameter("estado", estado)
                .setParameter("creadaEl", CREADA_EL);
    }
}
