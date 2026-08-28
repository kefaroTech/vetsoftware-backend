package com.vetsoftware.app.quote.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogItemRef;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import com.vetsoftware.app.quote.domain.CompanyRef;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteItemType;
import com.vetsoftware.app.quote.domain.QuoteLine;
import com.vetsoftware.app.quote.domain.QuoteLineArithmeticException;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.domain.QuoteSummary;
import com.vetsoftware.app.quote.domain.QuoteTotalsMismatchException;
import com.vetsoftware.app.quote.domain.TaxTreatment;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de la cotizacion contra MySQL real.
 *
 * <p>
 * Cuatro cosas de este adaptador SOLO existen en la base y un doble del
 * repositorio no puede falsearlas, porque responderia lo que el propio test le
 * hubiera dicho:
 *
 * <ul>
 * <li><b>La reverificacion de importes al leer.</b> Los cuatro totales de la
 * cabecera y los cinco de cada linea estan guardados, no calculados al vuelo, y
 * el constructor del dominio —por el que pasa {@code QuoteJpaMapper.toDomain}—
 * vuelve a hacer la cuenta. Una fila editada por SQL para cobrar de mas se
 * delata al reconstruirla. Eso solo se demuestra editando la fila de verdad.
 * <li><b>El desempate de la paginacion.</b> Sin orden total, dos paginas
 * consecutivas repiten u omiten filas cuando varias cotizaciones caen en el
 * mismo {@code created_date}. Con un doble las paginas las inventa el test.
 * <li><b>La baja logica por UPDATE nativo</b> y el {@code @SQLRestriction} que
 * la hace desaparecer de las lecturas posteriores.
 * <li><b>El indice unico de {@code client_request_id}</b>, que es global. Ver
 * {@link Idempotencia}.
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaQuoteRepository — cotización y snapshots contra MySQL real")
class QuotePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 8, 23, 10, 0);
    private static final LocalDate VIGENTE_HASTA = LocalDate.of(2026, 9, 30);

    private static final CompanyRef LA_EMPRESA = new CompanyRef(SchemaSeed.COMPANY_ID,
            "Veterinaria de prueba", "900123456");
    private static final CompanyRef LA_EMPRESA_AJENA = new CompanyRef(SchemaSeed.OTRA_COMPANY_ID,
            "Veterinaria ajena", "900654321");

    @Autowired
    private JpaQuoteRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
    }

    /** Linea de 100.000 gravada al 19 %, sin descuento: total 119.000. */
    private QuoteLine lineaNucleo() {
        return QuoteLine.freeze(1,
                new CatalogItemRef(nucleo, "CORE", "Núcleo", QuoteItemType.MODULE),
                new CatalogPriceRef(new BigDecimal("100000.00"), new BigDecimal("19.00"),
                        TaxTreatment.TAXED, 2),
                1, BigDecimal.ZERO, CREADA);
    }

    private Quote guardar(String numero, String llave, CompanyRef empresa) {
        return guardar(numero, llave, empresa, VIGENTE_HASTA, CREADA);
    }

    private Quote guardar(String numero, String llave, CompanyRef empresa, LocalDate vigenteHasta,
            LocalDateTime creada) {
        Quote quote = Quote.create(numero, empresa, empresa == null ? "Prospecto Sur" : null, null,
                null, null, SchemaSeed.PRICE_LIST_ID, BillingCycle.MONTHLY, vigenteHasta, 0, llave,
                List.of(lineaNucleo()), List.of(), creada);
        return repository.save(quote);
    }

    /**
     * Deja la cotizacion en el estado dado sin pasar por el dominio.
     *
     * <p>
     * Las tres columnas de aceptacion se escriben siempre, no solo cuando el estado
     * es {@code ACCEPTED}: {@code chk_quotes_accepted} exige {@code accepted_at} en
     * cuanto el estado lo es, y un {@code UPDATE} que solo tocara {@code status}
     * revienta con violacion de CHECK antes de llegar a la asercion. Para los demas
     * estados sobran y no molestan.
     */
    private void forzarEstado(Long id, QuoteStatus estado) {
        entityManager
                .createNativeQuery("UPDATE quotes SET status = :estado,"
                        + " accepted_at = '2026-08-20 10:00:00.000000',"
                        + " accepted_by_email = 'gerente@clinica.test',"
                        + " accepted_ip = '203.0.113.9' WHERE id = :id")
                .setParameter("estado", estado.name()).setParameter("id", id).executeUpdate();
    }

    private void vaciarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Guardado y aislamiento por empresa")
    class GuardadoYAislamiento {

        @Test
        @DisplayName("guarda cabecera y línea congelada y aplica aislamiento por empresa")
        void guarda_cabecera_y_linea_congelada() {
            Quote saved = guardar("COT-TEST-0001", "quote-request-1", LA_EMPRESA);
            vaciarContexto();

            assertThat(repository.findByIdAndCompanyId(saved.getId(), COMPANY)).get()
                    .satisfies(read -> {
                        assertThat(read.getStatus()).isEqualTo(QuoteStatus.DRAFT);
                        assertThat(read.getLines()).singleElement()
                                .extracting(QuoteLine::getUnitAmount)
                                .isEqualTo(new BigDecimal("100000.00"));
                    });
            assertThat(repository.findByIdAndCompanyId(saved.getId(), OTRA_COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("guarda las TRES cantidades de la línea, no solo la que se cobra")
        void guarda_las_tres_cantidades_de_la_linea() {
            // included = 2 en la tarifa, pero el articulo es MODULE: a lo que no es
            // capacidad no se le resta nada. Las tres cifras tienen que sobrevivir al
            // viaje de ida y vuelta para que la oferta se explique sin volver a la tarifa.
            Quote saved = guardar("COT-TEST-0002", "quote-request-2", LA_EMPRESA);
            vaciarContexto();

            assertThat(repository.findByIdAndCompanyId(saved.getId(), COMPANY)).get()
                    .extracting(q -> q.getLines().getFirst()).satisfies(linea -> {
                        assertThat(linea.getContractedQuantity()).isEqualTo(1);
                        assertThat(linea.getIncludedQuantity()).isEqualTo(2);
                        assertThat(linea.getQuantity()).isEqualTo(1);
                    });
        }

        @Test
        @DisplayName("una oferta a prospecto se guarda sin empresa y solo se lee por la vía ancha")
        void una_oferta_a_prospecto_se_guarda_sin_empresa() {
            Quote saved = guardar("COT-TEST-0003", "quote-request-3", null);
            vaciarContexto();

            assertThat(repository.findById(saved.getId())).get()
                    .satisfies(read -> assertThat(read.getCompany()).isNull());
            // Ningun WHERE company_id = ? casa jamas con ella: por eso existe la ancha.
            assertThat(repository.findByIdAndCompanyId(saved.getId(), COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Reverificación de importes al leer de la base")
    class ReverificacionDeImportes {

        @Test
        @DisplayName("un total de cabecera manipulado por SQL se delata al reconstruir la fila")
        void un_total_de_cabecera_manipulado_se_delata() {
            Quote saved = guardar("COT-TEST-0010", "quote-request-10", LA_EMPRESA);
            Long id = saved.getId();
            vaciarContexto();

            // El fraude exacto: alguien sube el total sin tocar las lineas. Ninguna
            // constraint lo ve -119.000 y 500.000 son los dos importes validos y
            // positivos- y el cliente acaba firmando un numero que sus renglones no
            // suman.
            entityManager
                    .createNativeQuery("UPDATE quotes SET total_amount = 500000.00 WHERE id = :id")
                    .setParameter("id", id).executeUpdate();
            vaciarContexto();

            assertThatThrownBy(() -> repository.findByIdAndCompanyId(id, COMPANY))
                    .isInstanceOf(QuoteTotalsMismatchException.class)
                    .hasMessageContaining("totalAmount");
        }

        @Test
        @DisplayName("un IVA de cabecera que ya no cuadra con las líneas también se delata")
        void un_iva_de_cabecera_manipulado_se_delata() {
            Quote saved = guardar("COT-TEST-0011", "quote-request-11", LA_EMPRESA);
            Long id = saved.getId();
            vaciarContexto();

            entityManager.createNativeQuery("UPDATE quotes SET tax_amount = 0.00 WHERE id = :id")
                    .setParameter("id", id).executeUpdate();
            vaciarContexto();

            assertThatThrownBy(() -> repository.findByIdAndCompanyId(id, COMPANY))
                    .isInstanceOf(QuoteTotalsMismatchException.class)
                    .hasMessageContaining("taxAmount");
        }

        @Test
        @DisplayName("una línea desactivada por SQL se sigue leyendo: el documento no se vuelve ilegible")
        void una_linea_desactivada_se_sigue_leyendo() {
            // QuoteLineJpaEntity NO lleva @SQLRestriction("enabled = true"), y es una
            // decision escrita, no un olvido: ocultar la linea haria que la cabecera
            // dejase de cuadrar con lo que el codigo ve y la cotizacion entera reventaria
            // al abrirse. Leyendolas todas, el documento sigue legible y el descuadre lo
            // caza la consulta de vigilancia de R5, que si filtra por enabled.
            //
            // Este test fija esa decision: añadir el @SQLRestriction "por consistencia"
            // con el resto del modelo convertiria una alerta de vigilancia en un 500 en
            // la cara del cliente, y aqui se veria.
            Quote saved = guardar("COT-TEST-0012", "quote-request-12", LA_EMPRESA);
            Long id = saved.getId();
            vaciarContexto();

            entityManager
                    .createNativeQuery(
                            "UPDATE quote_lines SET enabled = false WHERE quote_id = :id")
                    .setParameter("id", id).executeUpdate();
            vaciarContexto();

            assertThat(repository.findByIdAndCompanyId(id, COMPANY)).get().satisfies(read -> {
                assertThat(read.getLines()).singleElement().extracting(QuoteLine::isEnabled)
                        .isEqualTo(false);
                assertThat(read.getTotalAmount()).isEqualTo(new BigDecimal("119000.00"));
            });
        }

        @Test
        @DisplayName("un total de línea manipulado se delata con la excepción de la línea")
        void un_total_de_linea_manipulado_se_delata() {
            Quote saved = guardar("COT-TEST-0013", "quote-request-13", LA_EMPRESA);
            Long id = saved.getId();
            vaciarContexto();

            entityManager
                    .createNativeQuery(
                            "UPDATE quote_lines SET line_total = 999999.00 WHERE quote_id = :id")
                    .setParameter("id", id).executeUpdate();
            vaciarContexto();

            assertThatThrownBy(() -> repository.findByIdAndCompanyId(id, COMPANY))
                    .isInstanceOf(QuoteLineArithmeticException.class)
                    .hasMessageContaining("lineTotal");
        }

        @Test
        @DisplayName("una cantidad cobrada que no respeta la resta de R15 se delata al leer")
        void una_cantidad_cobrada_que_no_respeta_r15_se_delata() {
            // Se cambia el tipo a CAPACITY dejando contratada 1 e incluida 2: la cantidad
            // facturable tiene que ser 0, y la fila dice 1. Es exactamente el cobro de una
            // unidad que venia incluida, que ninguna constraint del esquema puede ver.
            Quote saved = guardar("COT-TEST-0014", "quote-request-14", LA_EMPRESA);
            Long id = saved.getId();
            vaciarContexto();

            entityManager
                    .createNativeQuery(
                            "UPDATE quote_lines SET item_type = 'CAPACITY' WHERE quote_id = :id")
                    .setParameter("id", id).executeUpdate();
            vaciarContexto();

            assertThatThrownBy(() -> repository.findByIdAndCompanyId(id, COMPANY))
                    .isInstanceOf(QuoteLineArithmeticException.class)
                    .hasMessageContaining("quantity");
        }

        @Test
        @DisplayName("el listado NO reverifica: la proyección no toca las líneas, y es deliberado")
        void el_listado_no_reverifica_porque_no_toca_las_lineas() {
            // QuoteSummary no lleva lineas, asi que no hay nada contra lo que cuadrar. Es
            // la contrapartida asumida de paginar sin arrastrar colecciones: la corrupcion
            // se ve al abrir el documento, no en el embudo. Queda fijado para que nadie
            // "arregle" el listado creyendo que deberia reventar.
            Quote saved = guardar("COT-TEST-0015", "quote-request-15", LA_EMPRESA);
            vaciarContexto();
            entityManager
                    .createNativeQuery("UPDATE quotes SET total_amount = 500000.00 WHERE id = :id")
                    .setParameter("id", saved.getId()).executeUpdate();
            vaciarContexto();

            PageResult<QuoteSummary> pagina = repository.findAllByCompanyId(COMPANY, 0, 20);

            // El listado del tenant trae ademas la cotizacion aceptada que SchemaSeed
            // siembra para esta misma empresa (COT-TEST-000900), que es una fila legitima
            // suya. Se acota a la manipulada en vez de exigir que la pagina tenga una sola
            // fila: lo que este caso fija es que la proyeccion NO revienta, no cuantas
            // cotizaciones tiene la clinica.
            assertThat(pagina.content()).filteredOn(resumen -> resumen.id().equals(saved.getId()))
                    .singleElement().extracting(QuoteSummary::totalAmount)
                    .isEqualTo(new BigDecimal("500000.00"));
        }
    }

    /**
     * <b>Aquí vive un defecto real, y estos dos casos lo fijan.</b>
     *
     * <p>
     * {@code uq_quotes_client_request} es {@code UNIQUE (client_request_id)}
     * <b>global</b> (migración 239, línea 55-58), mientras que
     * {@code CreateQuoteService} busca la llave <b>acotada por empresa</b> en
     * cuanto hay {@code companyId}. Las dos mitades no dicen lo mismo: la lectura
     * previa de la empresa B no puede ver la fila de la empresa A, así que B pasa
     * el chequeo de idempotencia, sigue adelante y su {@code INSERT} choca contra
     * el índice global.
     *
     * <p>
     * La constraint no está mapeada en {@code GlobalExceptionHandler}, así que sale
     * por la rama genérica: <b>409 {@code DATA_INTEGRITY_VIOLATION}</b> con el
     * texto «Database constraint violation». No es un 500, pero es un error opaco
     * por una llave que la empresa B eligió dentro de su propio espacio de nombres.
     * Y es <b>permanente</b>: cada reintento de B con esa llave falla igual, porque
     * la búsqueda acotada nunca verá la fila que estorba. Un cliente que reintenta
     * con la misma llave —que es justo el contrato de la idempotencia— no sale
     * nunca del error.
     *
     * <p>
     * Las migraciones 243, 252 y 253 documentan que el criterio correcto es
     * {@code UNIQUE (company_id, client_request_id)} y la 252 dice explícitamente
     * que corrigió este mismo error en otra tabla. A {@code quotes} no se le
     * aplicó.
     *
     * <p>
     * Estos tests afirman el comportamiento <b>de hoy</b>, no el deseado: son la
     * prueba reproducible del defecto. Cuando se corrija el índice fallarán, y eso
     * es lo que se quiere — obligan a volver aquí.
     */
    @Nested
    @DisplayName("Idempotencia por client_request_id")
    class Idempotencia {

        @Test
        @DisplayName("la búsqueda acotada por empresa no ve la llave de otra clínica")
        void la_busqueda_acotada_no_ve_la_llave_de_otra_clinica() {
            guardar("COT-TEST-0020", "llave-compartida", LA_EMPRESA);
            vaciarContexto();

            // Esto es lo correcto y es la mitad que SÍ funciona: reutilizar la llave de
            // otra clinica no devuelve su cotizacion, con sus precios y su prueba de
            // aceptacion.
            assertThat(
                    repository.findByClientRequestIdAndCompanyId("llave-compartida", OTRA_COMPANY))
                    .isEmpty();
            assertThat(repository.findByClientRequestIdAndCompanyId("llave-compartida", COMPANY))
                    .isPresent();
        }

        /**
         * Fija el arreglo del defecto #427, y sustituye al test que lo documentaba.
         *
         * <p>
         * El indice era {@code UNIQUE(client_request_id)} a secas -global- mientras la
         * busqueda de idempotencia iba acotada por empresa. Las dos piezas median cosas
         * distintas, y la consecuencia era peor que una fuga: la segunda clinica que
         * reutilizara una llave quedaba <strong>bloqueada de forma permanente</strong>,
         * porque reintentar con la misma llave -que es literalmente el contrato de la
         * idempotencia- nunca salia del conflicto.
         *
         * <p>
         * Lo cierra la columna generada {@code client_request_scope}
         * ({@code COALESCE(company_id, -1)}) con el unico sobre ella y la llave. No se
         * podia usar un compuesto {@code (company_id, client_request_id)} a secas
         * porque {@code company_id} es nulable -se cotiza a prospectos- y MySQL admite
         * varios {@code NULL} en un indice unico, lo que habria roto la deduplicacion
         * de prospectos justo en el caso para el que existe.
         */
        @Test
        @DisplayName("dos empresas pueden usar la misma llave: cada una tiene su propio espacio")
        void dos_empresas_pueden_usar_la_misma_llave() {
            Quote deLaEmpresa = guardar("COT-TEST-0021", "llave-compartida", LA_EMPRESA);
            vaciarContexto();

            Quote deLaAjena = guardar("COT-TEST-0022", "llave-compartida", LA_EMPRESA_AJENA);
            vaciarContexto();

            assertThat(deLaAjena.getId()).isNotEqualTo(deLaEmpresa.getId());
            assertThat(repository.findByClientRequestIdAndCompanyId("llave-compartida", COMPANY))
                    .get().extracting(Quote::getId).isEqualTo(deLaEmpresa.getId());
            assertThat(
                    repository.findByClientRequestIdAndCompanyId("llave-compartida", OTRA_COMPANY))
                    .get().extracting(Quote::getId).isEqualTo(deLaAjena.getId());
        }

        @Test
        @DisplayName("la misma llave repetida DENTRO de una empresa sigue siendo un conflicto")
        void la_misma_llave_repetida_dentro_de_una_empresa_sigue_siendo_conflicto() {
            guardar("COT-TEST-0025", "llave-repetida", LA_EMPRESA);
            vaciarContexto();

            // La mitad que el arreglo NO puede relajar: acotar por empresa no es dejar de
            // deduplicar. Dentro de la misma clinica, la llave sigue siendo unica y el
            // segundo INSERT choca -que es lo que hace que el doble clic no cree dos
            // cotizaciones-.
            assertThatThrownBy(() -> guardar("COT-TEST-0026", "llave-repetida", LA_EMPRESA))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("la búsqueda ancha sí ve la llave de cualquier empresa: es el camino SYSTEM")
        void la_busqueda_ancha_ve_la_llave_de_cualquier_empresa() {
            Quote saved = guardar("COT-TEST-0023", "llave-ancha", LA_EMPRESA);
            vaciarContexto();

            // Es el camino de la oferta a prospecto, restringido a SYSTEM por el
            // @PreAuthorize. Que devuelva la de otra empresa es correcto para SYSTEM y
            // seria una fuga para un empleado, que no puede llegar aqui.
            assertThat(repository.findByClientRequestId("llave-ancha")).get()
                    .extracting(Quote::getId).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("el reintento trae el detalle completo: la misma respuesta que la primera vez")
        void el_reintento_trae_el_detalle_completo() {
            guardar("COT-TEST-0024", "llave-detalle", LA_EMPRESA);
            vaciarContexto();

            assertThat(repository.findByClientRequestIdAndCompanyId("llave-detalle", COMPANY)).get()
                    .satisfies(read -> {
                        assertThat(read.getLines()).hasSize(1);
                        assertThat(read.getTotalAmount()).isEqualTo(new BigDecimal("119000.00"));
                    });
        }
    }

    @Nested
    @DisplayName("Barrido de vencimiento")
    class BarridoDeVencimiento {

        @Test
        @DisplayName("devuelve las vencidas ordenadas por fecha de vigencia y desempatadas por id")
        void devuelve_las_vencidas_ordenadas() {
            Quote tardia = guardar("COT-TEST-0030", "req-30", LA_EMPRESA, LocalDate.of(2026, 8, 20),
                    CREADA);
            Quote temprana = guardar("COT-TEST-0031", "req-31", LA_EMPRESA,
                    LocalDate.of(2026, 8, 10), CREADA);
            Quote mismoDia = guardar("COT-TEST-0032", "req-32", LA_EMPRESA,
                    LocalDate.of(2026, 8, 10), CREADA);
            vaciarContexto();

            List<Quote> vencidas = repository.findExpirable(LocalDate.of(2026, 8, 23), 50);

            // El orden es total: primero la vigencia, y entre las dos del mismo dia manda
            // el id. Sin el desempate, dos ejecuciones del barrido podrian devolver
            // ordenes distintos y el lote acotado dejaria filas sin barrer.
            assertThat(vencidas).extracting(Quote::getId).containsExactly(temprana.getId(),
                    mismoDia.getId(), tardia.getId());
        }

        @Test
        @DisplayName("una cotización que todavía está vigente no entra en el barrido")
        void una_cotizacion_vigente_no_entra_en_el_barrido() {
            guardar("COT-TEST-0033", "req-33", LA_EMPRESA, LocalDate.of(2026, 9, 30), CREADA);
            vaciarContexto();

            assertThat(repository.findExpirable(LocalDate.of(2026, 8, 23), 50)).isEmpty();
        }

        @Test
        @DisplayName("el último día de vigencia todavía no vence: el criterio es estricto")
        void el_ultimo_dia_de_vigencia_todavia_no_vence() {
            guardar("COT-TEST-0034", "req-34", LA_EMPRESA, LocalDate.of(2026, 8, 23), CREADA);
            vaciarContexto();

            // valid_until < today, no <=. Un >= de mas aqui vence las ofertas un dia antes
            // de tiempo y el cliente se encuentra con que el precio ya no se respeta.
            assertThat(repository.findExpirable(LocalDate.of(2026, 8, 23), 50)).isEmpty();
            assertThat(repository.findExpirable(LocalDate.of(2026, 8, 24), 50)).hasSize(1);
        }

        @Test
        @DisplayName("lo ya aceptado, rechazado o vencido no vuelve a moverse por el barrido")
        void lo_ya_resuelto_no_vuelve_a_moverse() {
            Quote aceptada = guardar("COT-TEST-0035", "req-35", LA_EMPRESA,
                    LocalDate.of(2026, 8, 10), CREADA);
            Quote rechazada = guardar("COT-TEST-0036", "req-36", LA_EMPRESA,
                    LocalDate.of(2026, 8, 10), CREADA);
            Quote yaVencida = guardar("COT-TEST-0037", "req-37", LA_EMPRESA,
                    LocalDate.of(2026, 8, 10), CREADA);
            Quote viva = guardar("COT-TEST-0038", "req-38", LA_EMPRESA, LocalDate.of(2026, 8, 10),
                    CREADA);
            vaciarContexto();
            forzarEstado(aceptada.getId(), QuoteStatus.ACCEPTED);
            forzarEstado(rechazada.getId(), QuoteStatus.REJECTED);
            forzarEstado(yaVencida.getId(), QuoteStatus.EXPIRED);
            vaciarContexto();

            assertThat(repository.findExpirable(LocalDate.of(2026, 8, 23), 50))
                    .extracting(Quote::getId).containsExactly(viva.getId());
        }

        @Test
        @DisplayName("el lote acota en la base: pide tres y devuelve dos si ese es el tamaño")
        void el_lote_acota_en_la_base() {
            guardar("COT-TEST-0039", "req-39", LA_EMPRESA, LocalDate.of(2026, 8, 10), CREADA);
            guardar("COT-TEST-0040", "req-40", LA_EMPRESA, LocalDate.of(2026, 8, 11), CREADA);
            guardar("COT-TEST-0041", "req-41", LA_EMPRESA, LocalDate.of(2026, 8, 12), CREADA);
            vaciarContexto();

            assertThat(repository.findExpirable(LocalDate.of(2026, 8, 23), 2)).hasSize(2);
        }

        @Test
        @DisplayName("sin nada que vencer devuelve lista vacía sin lanzar la segunda consulta")
        void sin_nada_que_vencer_devuelve_lista_vacia() {
            assertThat(repository.findExpirable(LocalDate.of(2026, 8, 23), 50)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Listados paginados")
    class ListadosPaginados {

        @Test
        @DisplayName("el desempate por id evita repetir u omitir filas entre páginas consecutivas")
        void el_desempate_por_id_evita_repetir_u_omitir_filas() {
            // Las tres caen en el MISMO created_date, que es el escenario contra el que
            // existe el desempate: con orden solo por fecha, MySQL puede devolverlas en
            // cualquier orden y la pagina 1 repetiria una fila de la pagina 0.
            Quote primera = guardar("COT-TEST-0050", "req-50", LA_EMPRESA, VIGENTE_HASTA, CREADA);
            Quote segunda = guardar("COT-TEST-0051", "req-51", LA_EMPRESA, VIGENTE_HASTA, CREADA);
            Quote tercera = guardar("COT-TEST-0052", "req-52", LA_EMPRESA, VIGENTE_HASTA, CREADA);
            vaciarContexto();

            PageResult<QuoteSummary> pagina0 = repository.findAllByCompanyId(COMPANY, 0, 2);
            PageResult<QuoteSummary> pagina1 = repository.findAllByCompanyId(COMPANY, 1, 2);

            // Mas reciente primero, y a igualdad de fecha el id mayor primero. La cuarta
            // fila es la cotizacion aceptada que SchemaSeed siembra para esta empresa: es
            // suya y tiene que salir, asi que se la nombra en vez de subir el numero a
            // ciegas. Su created_date es 2026-01-01, anterior al de las tres de arriba, y
            // por eso cierra la lista sin partir el trio que demuestra el desempate.
            assertThat(pagina0.content()).extracting(QuoteSummary::id)
                    .containsExactly(tercera.getId(), segunda.getId());
            assertThat(pagina1.content()).extracting(QuoteSummary::id)
                    .containsExactly(primera.getId(), SchemaSeed.QUOTE_ID);
            assertThat(pagina0.totalElements()).isEqualTo(4);
            assertThat(pagina0.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("el listado del tenant no ve las cotizaciones de otra clínica")
        void el_listado_del_tenant_no_ve_las_de_otra_clinica() {
            Quote mia = guardar("COT-TEST-0053", "req-53", LA_EMPRESA);
            Quote ajena = guardar("COT-TEST-0054", "req-54", LA_EMPRESA_AJENA);
            vaciarContexto();

            // Lo que se afirma son las DOS mitades. La de dentro: salen las dos
            // cotizaciones de esta empresa -la del caso y la que siembra SchemaSeed-. La
            // de fuera, que es la que da nombre al caso: no se cuela ninguna de la clinica
            // vecina, ni la que acaba de escribir el test ni la sembrada. Sin el
            // doesNotContain, subir el numero esperado habria tapado justo la fuga que
            // este caso existe para cazar.
            assertThat(repository.findAllByCompanyId(COMPANY, 0, 20).content())
                    .extracting(QuoteSummary::id).containsExactly(mia.getId(), SchemaSeed.QUOTE_ID)
                    .doesNotContain(ajena.getId(), SchemaSeed.OTRA_QUOTE_ID);
        }

        @Test
        @DisplayName("el embudo de plataforma cruza todas las clínicas y las ofertas a prospecto")
        void el_embudo_de_plataforma_cruza_todas_las_clinicas() {
            Quote mia = guardar("COT-TEST-0055", "req-55", LA_EMPRESA);
            Quote ajena = guardar("COT-TEST-0056", "req-56", LA_EMPRESA_AJENA);
            Quote prospecto = guardar("COT-TEST-0057", "req-57", null);
            vaciarContexto();

            // El embudo de plataforma no lleva empresa a proposito, asi que las dos
            // cotizaciones sembradas -una por clinica- son filas legitimas suyas y entran
            // en la cuenta. Se nombran una a una: un hasSize(5) diria lo mismo hoy y
            // dejaria pasar manana una fila de otra procedencia.
            assertThat(repository.findAll(0, 20).content()).extracting(QuoteSummary::id)
                    .containsExactlyInAnyOrder(mia.getId(), ajena.getId(), prospecto.getId(),
                            SchemaSeed.QUOTE_ID, SchemaSeed.OTRA_QUOTE_ID);
        }

        @Test
        @DisplayName("la proyección de listado no trae las líneas: es lo que permite paginar")
        void la_proyeccion_de_listado_no_trae_las_lineas() {
            Quote saved = guardar("COT-TEST-0058", "req-58", LA_EMPRESA);
            vaciarContexto();

            // Acotado a la cotizacion del caso: la empresa tiene ademas la sembrada, y lo
            // que aqui se mira es la FORMA de un resumen, no cuantos hay.
            assertThat(repository.findAllByCompanyId(COMPANY, 0, 20).content())
                    .filteredOn(resumen -> resumen.id().equals(saved.getId())).singleElement()
                    .satisfies(resumen -> {
                        assertThat(resumen.totalAmount()).isEqualTo(new BigDecimal("119000.00"));
                        assertThat(resumen.company().identifier()).isEqualTo("900123456");
                    });
        }
    }

    @Nested
    @DisplayName("Baja lógica")
    class BajaLogica {

        @Test
        @DisplayName("la baja acotada desactiva la fila y la deja invisible para las lecturas")
        void la_baja_acotada_desactiva_y_deja_invisible() {
            Quote saved = guardar("COT-TEST-0060", "req-60", LA_EMPRESA);
            Long id = saved.getId();
            vaciarContexto();

            repository.softDelete(id, COMPANY);
            vaciarContexto();

            assertThat(repository.findByIdAndCompanyId(id, COMPANY)).isEmpty();
            assertThat(repository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("la baja NO borra la fila ni sus líneas: son la prueba de lo que se ofreció")
        void la_baja_no_borra_la_fila_ni_sus_lineas() {
            Quote saved = guardar("COT-TEST-0061", "req-61", LA_EMPRESA);
            Long id = saved.getId();
            vaciarContexto();

            repository.softDelete(id, COMPANY);
            vaciarContexto();

            // El @SQLRestriction la esconde de JPA; en la tabla sigue entera. Un
            // deleteById() habria arrastrado las lineas en cascada antes del @SQLDelete.
            assertThat(valorNativo("SELECT COUNT(*) FROM quotes WHERE id = :id", id)).isEqualTo(1);
            assertThat(valorNativo("SELECT COUNT(*) FROM quote_lines WHERE quote_id = :id", id))
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("la baja mueve la versión para que una edición concurrente no la resucite")
        void la_baja_mueve_la_version() {
            Quote saved = guardar("COT-TEST-0062", "req-62", LA_EMPRESA);
            Long id = saved.getId();
            vaciarContexto();
            long versionAntes = valorNativo("SELECT version FROM quotes WHERE id = :id", id);

            repository.softDelete(id, COMPANY);
            vaciarContexto();

            assertThat(valorNativo("SELECT version FROM quotes WHERE id = :id", id))
                    .isEqualTo(versionAntes + 1);
        }

        @Test
        @DisplayName("la baja acotada no toca la cotización de otra empresa")
        void la_baja_acotada_no_toca_la_de_otra_empresa() {
            Quote ajena = guardar("COT-TEST-0063", "req-63", LA_EMPRESA_AJENA);
            Long id = ajena.getId();
            vaciarContexto();

            repository.softDelete(id, COMPANY);
            vaciarContexto();

            assertThat(repository.findByIdAndCompanyId(id, OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("la sobrecarga ancha es el único camino de baja de una oferta a prospecto")
        void la_sobrecarga_ancha_es_el_unico_camino_para_un_prospecto() {
            Quote prospecto = guardar("COT-TEST-0064", "req-64", null);
            Long id = prospecto.getId();
            vaciarContexto();

            // company_id es NULL: ningun WHERE company_id = ? casa, asi que la acotada no
            // hace nada y la fila seguiria viva.
            repository.softDelete(id, COMPANY);
            vaciarContexto();
            assertThat(repository.findById(id)).isPresent();

            repository.softDelete(id);
            vaciarContexto();
            assertThat(repository.findById(id)).isEmpty();
        }

        private long valorNativo(String sql, Long id) {
            return ((Number) entityManager.createNativeQuery(sql).setParameter("id", id)
                    .getSingleResult()).longValue();
        }
    }
}
