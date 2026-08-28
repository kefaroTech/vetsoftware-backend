package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de empresas contra MySQL real.
 *
 * <p>
 * Company es el tenant raiz: depende de {@code cities} por FK, pero no del
 * resto de la cadena de {@code SchemaSeed} (branch, product, categoria...), asi
 * que este test siembra solo su propia ciudad dejando que el motor asigne el id
 * — igual criterio que {@code CityPersistenceIT}. Ya no siembra membresia: esa
 * tabla desaparecio y {@code companies} no tiene {@code membership_id}.
 *
 * <p>
 * Lo que un doble no puede probar: el soft delete via {@code @SQLDelete} +
 * {@code @SQLRestriction}, el {@code @EntityGraph} de {@code city} que evita el
 * N+1 al listar, y que la unicidad del NIT ({@code identifier}) la exige un
 * indice UNIQUE real, no el codigo Java.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyRepository — tenant raiz, soft delete y unicidad del NIT contra MySQL real")
class CompanyPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaCompanyRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private CityRef bogota;
    private final AtomicInteger nitConsecutivo = new AtomicInteger(1);

    @BeforeEach
    void sembrarCiudad() {
        Long countryId = insertar("INSERT INTO countries (name) VALUES ('Colombia (test)')");
        Long stateId = insertar(
                "INSERT INTO states (name, country_id) VALUES ('Cundinamarca (test)', " + countryId
                        + ")");
        Long cityId = insertar(
                "INSERT INTO cities (name, state_id) VALUES ('Bogota (test)', " + stateId + ")");
        bogota = new CityRef(cityId, "Bogota (test)");
        entityManager.flush();
    }

    /** Inserta y devuelve el id autogenerado, en la misma conexion/transaccion. */
    private Long insertar(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
        return ((Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()")
                .getSingleResult()).longValue();
    }

    private Company nueva(String nombre) {
        return new Company(null, nombre, "NIT-" + nitConsecutivo.getAndIncrement(),
                "Calle 123 #45-67", "3001234567", bogota, CREADO, null, true);
    }

    private Company guardar(String nombre) {
        return repository.save(nueva(nombre));
    }

    /**
     * Los ids de una pagina, en el orden exacto en que los devolvio el motor.
     *
     * <p>
     * Es la pieza que permite afirmar contra el listado GLOBAL sin depender de
     * cuantas empresas haya en la tabla: una pagina se compara contra OTRA lectura
     * de la misma consulta, nunca contra una lista escrita a mano.
     */
    private static List<Long> ids(PageResult<Company> pagina) {
        return pagina.content().stream().map(Company::getId).toList();
    }

    /** Soft delete + flush: el UPDATE tiene que llegar a la BD antes de releer. */
    private void deshabilitar(Long id) {
        repository.delete(id);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y las referencias hidratadas")
        void guardar_y_releer_conserva_cada_campo() {
            Company guardada = guardar("Clinica Norte");

            Company leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getName()).isEqualTo("Clinica Norte");
            assertThat(leida.getAddress()).isEqualTo("Calle 123 #45-67");
            assertThat(leida.getContactNumber()).isEqualTo("3001234567");
            assertThat(leida.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leida.isEnabled()).isTrue();
            // El companion VO no se guarda como columnas propias: se reconstruye desde
            // el @ManyToOne al leer, y sus invariantes rechazan un nombre en blanco.
            assertThat(leida.getCity()).isEqualTo(bogota);
        }

        @Test
        @DisplayName("direccion y telefono nulos se conservan nulos, no en blanco")
        void direccion_y_telefono_nulos_se_conservan_nulos() {
            Company guardada = repository.save(
                    new Company(null, "Clinica Norte", "NIT-" + nitConsecutivo.getAndIncrement(),
                            null, null, bogota, CREADO, null, true));

            Company leida = repository.findById(guardada.getId()).orElseThrow();

            assertThat(leida.getAddress()).isNull();
            assertThat(leida.getContactNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("unicidad del NIT")
    class UnicidadDelNit {

        @Test
        @DisplayName("dos empresas con el mismo NIT violan el indice unico")
        void dos_empresas_con_el_mismo_nit_violan_el_indice_unico() {
            String nitCompartido = "NIT-DUPLICADO";
            repository.save(new Company(null, "Clinica Norte", nitCompartido, null, null, bogota,
                    CREADO, null, true));

            assertThatThrownBy(() -> repository.save(new Company(null, "Clinica Sur", nitCompartido,
                    null, null, bogota, CREADO, null, true))).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("findAllVisibleTo(null) trae todas las empresas habilitadas")
        void find_all_visible_to_null_trae_todas_las_habilitadas() {
            Company primera = guardar("Clinica Norte");
            Company segunda = guardar("Clinica Sur");

            assertThat(repository.findAllVisibleTo(null, 0, 20).content())
                    .extracting(Company::getId).contains(primera.getId(), segunda.getId());
        }

        /**
         * El alcance es la barrera, y aqui se comprueba contra la base: con una empresa
         * informada el listado trae esa fila y ninguna otra. Mientras el puerto ofrecio
         * un {@code findAll()} pelado, {@code GET /companies} entregaba el registro de
         * todos los tenants a cualquier empleado con {@code company.read}.
         */
        @Test
        @DisplayName("findAllVisibleTo con empresa trae solo esa, nunca la de otro tenant")
        void find_all_visible_to_acota_a_una_sola_empresa() {
            Company propia = guardar("Clinica Norte");
            Company ajena = guardar("Clinica Sur");

            assertThat(repository.findAllVisibleTo(propia.getId(), 0, 20).content())
                    .extracting(Company::getId).containsExactly(propia.getId());
            assertThat(repository.findAllVisibleTo(ajena.getId(), 0, 20).content())
                    .extracting(Company::getId).doesNotContain(propia.getId());
        }

        /**
         * La empresa propia se sirve como pagina de una fila y no como recurso unico:
         * el alcance no cambia la forma del contrato. Los metadatos salen de la
         * consulta, asi que la pagina 1 viene sin contenido pero sin perder el total
         * —que es justamente la aritmetica que se equivocaba al fabricarla a mano—.
         */
        @Test
        @DisplayName("la empresa propia es una pagina de una fila, y la pagina 1 no pierde el total")
        void la_empresa_propia_es_una_pagina_de_una_fila() {
            Company propia = guardar("Clinica Norte");

            PageResult<Company> primera = repository.findAllVisibleTo(propia.getId(), 0, 20);
            PageResult<Company> segunda = repository.findAllVisibleTo(propia.getId(), 1, 20);

            assertThat(primera.content()).extracting(Company::getId)
                    .containsExactly(propia.getId());
            assertThat(primera.totalElements()).isEqualTo(1L);
            assertThat(primera.totalPages()).isEqualTo(1);
            assertThat(segunda.content()).isEmpty();
            assertThat(segunda.totalElements()).isEqualTo(1L);
        }
    }

    /**
     * Lo que ningun doble puede probar de la paginacion: que el orden que declara
     * el adaptador lo aplica de verdad el motor, y que el tope y la normalizacion
     * del indice llegan hasta el {@code PageRequest} que se ejecuta.
     */
    @Nested
    @DisplayName("paginacion contra la base")
    class Paginacion {

        /**
         * El defecto que esta prueba impide: sin desempate estable, dos filas con el
         * mismo valor de orden pueden salir en dos paginas —o en ninguna—, porque MySQL
         * no garantiza el orden de las empatadas entre dos consultas distintas. Con
         * {@code name ASC, id ASC} la particion es una particion de verdad: las tres
         * paginas concatenadas son el registro entero, sin repetir ni omitir.
         *
         * <p>
         * <b>Por que no se afirma «Alfa, Bravo» a pelo.</b> Este listado es GLOBAL y el
         * contenedor de {@link AbstractDataJpaTest} lo comparte la suite entera, asi
         * que el numero de filas no lo decide este caso: cualquier clase que CONFIRME
         * una empresa —hoy {@code RecordLimitEventRollbackIT}, que corre con
         * {@code NOT_SUPPORTED} y deja sembradas las dos de {@code SchemaSeed}— corre
         * las paginas y pone rojo un aserto posicional. La particion se comprueba
         * contra otra lectura de la misma consulta: las tres paginas de dos en dos
         * tienen que ser, exactamente, los seis primeros elementos del registro
         * partidos en tres. Es mas estricto que la version anterior, no mas laxo — esa
         * solo miraba el caso de tabla vacia.
         */
        @Test
        @DisplayName("tres paginas consecutivas parten el registro sin repetir ni omitir filas")
        void tres_paginas_consecutivas_parten_el_registro() {
            long antes = repository.findAllVisibleTo(null, 0, 1).totalElements();
            Company delta = guardar("Delta");
            Company alfa = guardar("Alfa");
            Company charlie = guardar("Charlie");
            Company bravo = guardar("Bravo");
            Company echo = guardar("Echo");
            Company foxtrot = guardar("Foxtrot");

            List<Long> registro = ids(repository.findAllVisibleTo(null, 0, 200));
            PageResult<Company> primera = repository.findAllVisibleTo(null, 0, 2);
            PageResult<Company> segunda = repository.findAllVisibleTo(null, 1, 2);
            PageResult<Company> tercera = repository.findAllVisibleTo(null, 2, 2);

            assertThat(ids(primera)).containsExactlyElementsOf(registro.subList(0, 2));
            assertThat(ids(segunda)).containsExactlyElementsOf(registro.subList(2, 4));
            assertThat(ids(tercera)).containsExactlyElementsOf(registro.subList(4, 6));
            // El orden que declara el adaptador lo aplica el motor: las seis sembradas
            // salen alfabeticamente aunque se guardaran desordenadas.
            assertThat(registro).containsSubsequence(alfa.getId(), bravo.getId(), charlie.getId(),
                    delta.getId(), echo.getId(), foxtrot.getId());
            // El total crece exactamente en las seis de este caso, y los metadatos
            // siguen siendo coherentes entre si: paginas = techo(elementos / pageSize).
            assertThat(primera.totalElements()).isEqualTo(antes + 6);
            assertThat(primera.totalPages()).isEqualTo((int) ((primera.totalElements() + 1) / 2));
        }

        /**
         * El caso que hace falta el desempate: tres empresas homonimas. Sin
         * {@code id ASC} el nombre no distingue nada y la fila puede saltar de pagina
         * entre dos peticiones.
         *
         * <p>
         * Se afirma sobre el registro entero y no sobre las paginas 0 y 1 porque la
         * POSICION de estas tres en un listado global la decide cuantas empresas haya
         * confirmado otra clase. Lo que el desempate promete es justo lo que se
         * comprueba: las homonimas salen en orden de id, CONSECUTIVAS —no se intercala
         * nada entre ellas— y ninguna aparece dos veces. El aserto posicional anterior
         * era mas debil, porque no decia nada del resto del registro.
         */
        @Test
        @DisplayName("con nombres iguales el id desempata: las homonimas salen consecutivas, en"
                + " orden de id y una sola vez")
        void con_nombres_iguales_el_id_desempata() {
            Company primera = guardar("Clinica Homonima");
            Company segunda = guardar("Clinica Homonima");
            Company tercera = guardar("Clinica Homonima");

            List<Long> registro = ids(repository.findAllVisibleTo(null, 0, 200));

            assertThat(registro).containsSequence(primera.getId(), segunda.getId(),
                    tercera.getId());
            assertThat(registro).containsOnlyOnce(primera.getId(), segunda.getId(),
                    tercera.getId());
        }

        /**
         * El tope lo pone el servidor. Si alguien cambiase {@code Pages.request} por un
         * {@code PageRequest.of} pelado, {@code ?pageSize=100000} devolveria el
         * registro mercantil entero en una sola respuesta y deshaceria VUE-06.
         */
        @Test
        @DisplayName("un pageSize desmedido se topa en el maximo del kernel, no lo fija el cliente")
        void un_page_size_desmedido_se_topa_en_el_maximo() {
            guardar("Clinica Norte");

            assertThat(repository.findAllVisibleTo(null, 0, 100_000).pageSize()).isEqualTo(200);
            assertThat(repository.searchVisibleTo(null, "Clinica", 0, 100_000).pageSize())
                    .isEqualTo(200);
        }

        /**
         * Sin normalizar, {@code ?page=-1} revienta con
         * {@code IllegalArgumentException} desde dentro de Spring Data y el cliente ve
         * un 500 por escribir un numero.
         *
         * <p>
         * «Se normaliza a 0» se comprueba contra la pagina 0 de verdad, no contra una
         * lista de un elemento: pedir {@code -3} tiene que devolver EXACTAMENTE lo
         * mismo que pedir {@code 0}. El aserto anterior confundia «es la pagina cero»
         * con «la tabla solo tiene mi fila», y por eso se caia en cuanto otra clase
         * confirmaba una empresa.
         */
        @Test
        @DisplayName("un page negativo se normaliza a 0 en vez de reventar dentro de Spring Data")
        void un_page_negativo_se_normaliza_a_cero() {
            Company sembrada = guardar("Clinica Norte");

            PageResult<Company> negativa = repository.findAllVisibleTo(null, -3, 20);
            PageResult<Company> cero = repository.findAllVisibleTo(null, 0, 20);

            assertThat(negativa.page()).isZero();
            assertThat(ids(negativa)).containsExactlyElementsOf(ids(cero))
                    .contains(sembrada.getId());
        }
    }

    /**
     * La busqueda, con el mismo reparto de ramas que el listado y el termino
     * añadido al {@code WHERE}.
     */
    @Nested
    @DisplayName("busqueda por nombre e identificador fiscal")
    class Busqueda {

        @Test
        @DisplayName("el termino casa por nombre parcial y sin distinguir mayusculas")
        void el_termino_casa_por_nombre_parcial_e_insensible() {
            Company norte = guardar("Clinica Norte");
            guardar("Veterinaria Sur");

            assertThat(repository.searchVisibleTo(null, "nOrTe", 0, 20).content())
                    .extracting(Company::getId).containsExactly(norte.getId());
        }

        @Test
        @DisplayName("el termino tambien casa por identificador fiscal")
        void el_termino_tambien_casa_por_identificador_fiscal() {
            Company conNit = repository.save(new Company(null, "Clinica Norte", "NIT-BUSCADO", null,
                    null, bogota, CREADO, null, true));
            guardar("Veterinaria Sur");

            assertThat(repository.searchVisibleTo(null, "BUSCADO", 0, 20).content())
                    .extracting(Company::getId).containsExactly(conNit.getId());
        }

        /**
         * <b>El caso que sostiene todo el aislamiento de esta feature.</b> El filtro de
         * empresa se aplica ADEMAS del termino, nunca en su lugar: un empleado de la
         * Clinica Norte que escribe «Veterinaria Sur» recibe una pagina vacia. La
         * segunda asercion es la que impide que este test de verde por el motivo
         * equivocado: sin acotar, esa misma busqueda si encuentra la fila, asi que lo
         * que falta no es el dato — es el derecho.
         */
        @Test
        @DisplayName("buscar el nombre de otra veterinaria devuelve pagina vacia, no su ficha")
        void buscar_otra_veterinaria_devuelve_pagina_vacia() {
            Company propia = guardar("Clinica Norte");
            Company ajena = guardar("Veterinaria Sur");

            PageResult<Company> acotada = repository.searchVisibleTo(propia.getId(),
                    "Veterinaria Sur", 0, 20);

            assertThat(acotada.content()).isEmpty();
            assertThat(acotada.totalElements()).isZero();
            assertThat(repository.searchVisibleTo(null, "Veterinaria Sur", 0, 20).content())
                    .extracting(Company::getId).containsExactly(ajena.getId());
        }

        /**
         * Y al reves: un termino que casa con las dos sigue devolviendo solo la propia.
         * El alcance recorta el resultado del termino, no compite con el.
         */
        @Test
        @DisplayName("un termino que casa con varias empresas se recorta al alcance del empleado")
        void un_termino_que_casa_con_varias_se_recorta_al_alcance() {
            Company propia = guardar("Clinica Norte");
            Company ajena = guardar("Clinica Sur");

            PageResult<Company> acotada = repository.searchVisibleTo(propia.getId(), "Clinica", 0,
                    20);

            assertThat(acotada.content()).extracting(Company::getId)
                    .containsExactly(propia.getId());
            assertThat(acotada.totalElements()).isEqualTo(1L);
            // Sin acotar, ese mismo termino SI alcanza a las dos: lo que falta no es el
            // dato, es el derecho. Por pertenencia y no con un total exacto, que
            // contaria tambien las «Clinica ...» que confirme cualquier otra clase.
            assertThat(ids(repository.searchVisibleTo(null, "Clinica", 0, 20)))
                    .contains(propia.getId(), ajena.getId());
        }

        /**
         * «El mismo alcance que el listado» se comprueba, literalmente, contra el
         * listado: con termino vacio las dos consultas devuelven la misma pagina y el
         * mismo total, sea cual sea el numero de empresas de la tabla. El
         * {@code isEqualTo(2L)} anterior no media esa equivalencia — media que la tabla
         * estuviera vacia, que es otra cosa y ademas no la controla este caso.
         */
        @Test
        @DisplayName("un termino vacio devuelve el mismo alcance que el listado")
        void un_termino_vacio_devuelve_el_mismo_alcance_que_el_listado() {
            Company propia = guardar("Clinica Norte");
            Company ajena = guardar("Veterinaria Sur");

            PageResult<Company> busquedaVacia = repository.searchVisibleTo(null, "", 0, 20);
            PageResult<Company> listado = repository.findAllVisibleTo(null, 0, 20);

            assertThat(repository.searchVisibleTo(propia.getId(), "", 0, 20).content())
                    .extracting(Company::getId).containsExactly(propia.getId());
            assertThat(ids(busquedaVacia)).containsExactlyElementsOf(ids(listado))
                    .contains(propia.getId(), ajena.getId());
            assertThat(busquedaVacia.totalElements()).isEqualTo(listado.totalElements());
        }

        @Test
        @DisplayName("la busqueda tambien parte en paginas con el mismo orden total")
        void la_busqueda_tambien_parte_en_paginas_con_el_mismo_orden() {
            guardar("Clinica Delta");
            guardar("Clinica Alfa");
            guardar("Clinica Bravo");

            assertThat(repository.searchVisibleTo(null, "Clinica", 0, 2).content())
                    .extracting(Company::getName).containsExactly("Clinica Alfa", "Clinica Bravo");
            assertThat(repository.searchVisibleTo(null, "Clinica", 1, 2).content())
                    .extracting(Company::getName).containsExactly("Clinica Delta");
        }
    }

    /**
     * El ARCHIVO: la mitad que faltaba del soft delete. Todo lo que hay debajo es
     * SQL nativo, porque es la unica forma de que una consulta alcance una fila que
     * el {@code @SQLRestriction("enabled = true")} esconde — y esa es exactamente
     * la razon por la que estas pruebas tienen que correr contra MySQL real: un
     * doble del repositorio no ejecuta ni la restriccion que se esquiva ni el
     * {@code WHERE} que la sustituye.
     *
     * <p>
     * <b>Cada prueba siembra una archivada Y una activa.</b> Con solo archivadas en
     * el fixture, una consulta que devolviera la tabla entera pasaria igual de
     * verde: la asercion que muerde es la de la activa que <em>no</em> debe salir.
     */
    @Nested
    @DisplayName("archivo de empresas — findAllDisabledVisibleTo")
    class Archivadas {

        @Test
        @DisplayName("trae la archivada y NO trae la activa: el listado no es la tabla entera")
        void trae_la_archivada_y_no_la_activa() {
            Company archivada = guardar("Clinica Archivada");
            Company activa = guardar("Clinica Activa");
            deshabilitar(archivada.getId());

            PageResult<Company> pagina = repository.findAllDisabledVisibleTo(null, 0, 20);

            assertThat(pagina.content()).extracting(Company::getId).contains(archivada.getId())
                    .doesNotContain(activa.getId());
            // Y la activa sigue estando donde debe: las dos consultas son
            // complementarias, no alternativas.
            assertThat(repository.findAllVisibleTo(null, 0, 20).content())
                    .extracting(Company::getId).contains(activa.getId())
                    .doesNotContain(archivada.getId());
        }

        /**
         * El campo del que cuelga el distintivo de deshabilitada en la consola. Si la
         * consulta nativa lo perdiera por el camino —o el mapeo lo forzara a
         * {@code true}—, la pantalla pintaria el archivo como si estuviera operativo.
         */
        @Test
        @DisplayName("la empresa archivada llega con enabled=false y sus campos intactos")
        void la_archivada_llega_con_enabled_false() {
            Company archivada = guardar("Clinica Archivada");
            deshabilitar(archivada.getId());

            Company leida = repository.findAllDisabledVisibleTo(null, 0, 20).content().stream()
                    .filter(c -> c.getId().equals(archivada.getId())).findFirst().orElseThrow();

            assertThat(leida.isEnabled()).isFalse();
            assertThat(leida.getName()).isEqualTo("Clinica Archivada");
            assertThat(leida.getIdentifier()).isEqualTo(archivada.getIdentifier());
            // La ciudad viaja hidratada pese a que la consulta nativa no admite
            // @EntityGraph: el service corre @Transactional(readOnly = true) y aqui la
            // rodaja tambien esta en transaccion.
            assertThat(leida.getCity().name()).isEqualTo("Bogota (test)");
        }

        /**
         * El {@code AND id = :companyId} de la consulta nativa es TODA la barrera de
         * tenant que hay en este camino: al saltarse el {@code @SQLRestriction} no
         * queda ninguna lectura previa que valide la propiedad de la fila. Si el
         * predicado desapareciera, este test seria el unico que lo notaria — y en
         * produccion la fuga responderia 200.
         */
        @Test
        @DisplayName("acotada a una empresa no devuelve la archivada de otra")
        void acotada_no_devuelve_la_archivada_de_otra() {
            Company archivadaAjena = guardar("Clinica Ajena");
            Company propia = guardar("Clinica Propia");
            deshabilitar(archivadaAjena.getId());
            deshabilitar(propia.getId());

            PageResult<Company> pagina = repository.findAllDisabledVisibleTo(propia.getId(), 0, 20);

            assertThat(pagina.content()).extracting(Company::getId).containsExactly(propia.getId());
            assertThat(pagina.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("acotada a una empresa activa devuelve pagina vacia, no su ficha")
        void acotada_a_una_activa_devuelve_pagina_vacia() {
            Company activa = guardar("Clinica Activa");
            Company archivada = guardar("Clinica Archivada");
            deshabilitar(archivada.getId());

            PageResult<Company> pagina = repository.findAllDisabledVisibleTo(activa.getId(), 0, 20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isZero();
        }

        /**
         * El {@code countQuery} de la consulta nativa cuenta el ARCHIVO, no la tabla.
         * Se comprueba pidiendo paginas de una fila sobre dos archivadas y una activa:
         * el total tiene que decir dos, no tres.
         */
        @Test
        @DisplayName("la paginacion parte el archivo y el total no cuenta las activas")
        void la_paginacion_parte_el_archivo_sin_contar_las_activas() {
            Company primera = guardar("Archivada A");
            Company segunda = guardar("Archivada B");
            Company activa = guardar("Activa C");
            deshabilitar(primera.getId());
            deshabilitar(segunda.getId());

            PageResult<Company> pagina0 = repository.findAllDisabledVisibleTo(null, 0, 1);
            PageResult<Company> pagina1 = repository.findAllDisabledVisibleTo(null, 1, 1);

            assertThat(pagina0.content()).hasSize(1);
            assertThat(pagina1.content()).hasSize(1);
            assertThat(pagina0.content().getFirst().getId())
                    .isNotEqualTo(pagina1.content().getFirst().getId());
            assertThat(pagina0.totalElements()).isEqualTo(2L);
            assertThat(pagina0.content()).extracting(Company::getId).doesNotContain(activa.getId());
            assertThat(pagina1.content()).extracting(Company::getId).doesNotContain(activa.getId());
        }

        /**
         * El {@code ORDER BY} va embebido en el SQL nativo y el {@code Pageable} llega
         * sin {@code Sort}. Con nombres iguales el {@code id} desempata: sin orden
         * total, dos paginas consecutivas pueden repetir u omitir filas.
         */
        @Test
        @DisplayName("en el archivo, con nombres iguales el id desempata y el orden es total")
        void con_nombres_iguales_el_id_desempata_en_el_archivo() {
            Company primera = guardar("Clinica Homonima");
            Company segunda = guardar("Clinica Homonima");
            deshabilitar(primera.getId());
            deshabilitar(segunda.getId());

            PageResult<Company> pagina = repository.findAllDisabledVisibleTo(null, 0, 20);

            assertThat(pagina.content()).extracting(Company::getId)
                    .containsSequence(primera.getId(), segunda.getId());
        }

        /**
         * El tope lo pone el kernel de paginacion: esquivar el {@code @SQLRestriction}
         * no esquiva tambien a {@code Pages}.
         */
        @Test
        @DisplayName("un pageSize desmedido tambien se topa en el maximo al listar el archivo")
        void un_page_size_desmedido_se_topa_en_el_maximo_del_archivo() {
            Company archivada = guardar("Clinica Archivada");
            deshabilitar(archivada.getId());

            assertThat(repository.findAllDisabledVisibleTo(null, 0, 100_000).pageSize())
                    .isEqualTo(200);
        }

        @Test
        @DisplayName("un page negativo tambien se normaliza a 0 al listar el archivo")
        void un_page_negativo_se_normaliza_a_cero_en_el_archivo() {
            Company archivada = guardar("Clinica Archivada");
            deshabilitar(archivada.getId());

            assertThat(repository.findAllDisabledVisibleTo(null, -1, 20).page()).isZero();
        }

        /**
         * El ciclo completo que este listado desbloquea: la empresa aparece en el
         * archivo, se restaura con {@code reactivate} y deja de aparecer. Antes de
         * existir el listado, ese primer paso solo se podia dar sabiendose el id de
         * memoria.
         */
        @Test
        @DisplayName("tras reactivar, la empresa sale del archivo y vuelve al listado activo")
        void tras_reactivar_sale_del_archivo() {
            Company archivada = guardar("Clinica Norte");
            deshabilitar(archivada.getId());
            assertThat(repository.findAllDisabledVisibleTo(null, 0, 20).content())
                    .extracting(Company::getId).contains(archivada.getId());

            repository.reactivate(archivada.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllDisabledVisibleTo(null, 0, 20).content())
                    .extracting(Company::getId).doesNotContain(archivada.getId());
            assertThat(repository.findAllVisibleTo(null, 0, 20).content())
                    .extracting(Company::getId).contains(archivada.getId());
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDelete {

        @Test
        @DisplayName("el listado y la busqueda dejan de ver la empresa deshabilitada")
        void el_listado_no_ve_la_deshabilitada() {
            Company pausada = guardar("Clinica Norte");

            deshabilitar(pausada.getId());

            assertThat(repository.findAllVisibleTo(null, 0, 20).content())
                    .extracting(Company::getId).doesNotContain(pausada.getId());
            assertThat(repository.findById(pausada.getId())).isEmpty();
        }

        /**
         * El {@code @SQLRestriction} tiene que alcanzar tambien a las consultas
         * {@code @Query} nuevas: si la busqueda las esquivara, una empresa dada de baja
         * seguiria siendo encontrable por nombre desde la consola.
         */
        @Test
        @DisplayName("la busqueda por termino tampoco encuentra la empresa deshabilitada")
        void la_busqueda_no_encuentra_la_deshabilitada() {
            Company pausada = guardar("Clinica Norte");

            deshabilitar(pausada.getId());

            PageResult<Company> pagina = repository.searchVisibleTo(null, "Clinica Norte", 0, 20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isZero();
        }
    }
}
