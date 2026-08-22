package com.vetsoftware.app.company.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.MembershipRef;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
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
 * Company es el tenant raiz: depende de {@code cities} y {@code memberships}
 * por FK, pero no del resto de la cadena de {@code SchemaSeed} (branch,
 * product, categoria...), asi que este test siembra solo su propia ciudad y su
 * propia membresia dejando que el motor asigne el id — igual criterio que
 * {@code CityPersistenceIT}.
 *
 * <p>
 * Lo que un doble no puede probar: el soft delete via {@code @SQLDelete} +
 * {@code @SQLRestriction}, el {@code @EntityGraph} de {@code city} y
 * {@code membership} que evita el N+1 al listar, y que la unicidad del NIT
 * ({@code identifier}) la exige un indice UNIQUE real, no el codigo Java.
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
    private MembershipRef premium;
    private final AtomicInteger nitConsecutivo = new AtomicInteger(1);

    @BeforeEach
    void sembrarCiudadYMembresia() {
        Long countryId = insertar("INSERT INTO countries (name) VALUES ('Colombia (test)')");
        Long stateId = insertar(
                "INSERT INTO states (name, country_id) VALUES ('Cundinamarca (test)', " + countryId
                        + ")");
        Long cityId = insertar(
                "INSERT INTO cities (name, state_id) VALUES ('Bogota (test)', " + stateId + ")");
        Long membershipId = insertar("INSERT INTO memberships (name, status, mandatory) "
                + "VALUES ('Premium (test)', 'ACTIVE', false)");
        bogota = new CityRef(cityId, "Bogota (test)");
        premium = new MembershipRef(membershipId, "Premium (test)", "ACTIVE");
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
                "Calle 123 #45-67", "3001234567", bogota, premium, CREADO, null, true);
    }

    private Company guardar(String nombre) {
        return repository.save(nueva(nombre));
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
            // Los companion VO no se guardan como columnas propias: se reconstruyen desde
            // el @ManyToOne al leer, y sus invariantes rechazan un nombre en blanco.
            assertThat(leida.getCity()).isEqualTo(bogota);
            assertThat(leida.getMembership()).isEqualTo(premium);
        }

        @Test
        @DisplayName("direccion y telefono nulos se conservan nulos, no en blanco")
        void direccion_y_telefono_nulos_se_conservan_nulos() {
            Company guardada = repository.save(
                    new Company(null, "Clinica Norte", "NIT-" + nitConsecutivo.getAndIncrement(),
                            null, null, bogota, premium, CREADO, null, true));

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
                    premium, CREADO, null, true));

            assertThatThrownBy(() -> repository.save(new Company(null, "Clinica Sur", nitCompartido,
                    null, null, bogota, premium, CREADO, null, true)))
                    .isInstanceOf(RuntimeException.class);
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
         */
        @Test
        @DisplayName("tres paginas consecutivas parten el registro sin repetir ni omitir filas")
        void tres_paginas_consecutivas_parten_el_registro() {
            guardar("Delta");
            guardar("Alfa");
            guardar("Charlie");
            guardar("Bravo");
            guardar("Echo");

            PageResult<Company> primera = repository.findAllVisibleTo(null, 0, 2);
            PageResult<Company> segunda = repository.findAllVisibleTo(null, 1, 2);
            PageResult<Company> tercera = repository.findAllVisibleTo(null, 2, 2);

            assertThat(primera.content()).extracting(Company::getName).containsExactly("Alfa",
                    "Bravo");
            assertThat(segunda.content()).extracting(Company::getName).containsExactly("Charlie",
                    "Delta");
            assertThat(tercera.content()).extracting(Company::getName).containsExactly("Echo");
            assertThat(primera.totalElements()).isEqualTo(5L);
            assertThat(primera.totalPages()).isEqualTo(3);
        }

        /**
         * El caso que hace falta el desempate: tres empresas homonimas. Sin
         * {@code id ASC} el nombre no distingue nada y la fila puede saltar de pagina
         * entre dos peticiones.
         */
        @Test
        @DisplayName("con nombres iguales el id desempata y ninguna fila salta de pagina")
        void con_nombres_iguales_el_id_desempata() {
            Company primera = guardar("Clinica Homonima");
            Company segunda = guardar("Clinica Homonima");
            Company tercera = guardar("Clinica Homonima");

            assertThat(repository.findAllVisibleTo(null, 0, 2).content()).extracting(Company::getId)
                    .containsExactly(primera.getId(), segunda.getId());
            assertThat(repository.findAllVisibleTo(null, 1, 2).content()).extracting(Company::getId)
                    .containsExactly(tercera.getId());
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
         */
        @Test
        @DisplayName("un page negativo se normaliza a 0 en vez de reventar dentro de Spring Data")
        void un_page_negativo_se_normaliza_a_cero() {
            Company sembrada = guardar("Clinica Norte");

            PageResult<Company> pagina = repository.findAllVisibleTo(null, -3, 20);

            assertThat(pagina.page()).isZero();
            assertThat(pagina.content()).extracting(Company::getId)
                    .containsExactly(sembrada.getId());
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
                    null, bogota, premium, CREADO, null, true));
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
            guardar("Clinica Sur");

            PageResult<Company> acotada = repository.searchVisibleTo(propia.getId(), "Clinica", 0,
                    20);

            assertThat(acotada.content()).extracting(Company::getId)
                    .containsExactly(propia.getId());
            assertThat(acotada.totalElements()).isEqualTo(1L);
            assertThat(repository.searchVisibleTo(null, "Clinica", 0, 20).totalElements())
                    .isEqualTo(2L);
        }

        @Test
        @DisplayName("un termino vacio devuelve el mismo alcance que el listado")
        void un_termino_vacio_devuelve_el_mismo_alcance_que_el_listado() {
            Company propia = guardar("Clinica Norte");
            guardar("Veterinaria Sur");

            assertThat(repository.searchVisibleTo(propia.getId(), "", 0, 20).content())
                    .extracting(Company::getId).containsExactly(propia.getId());
            assertThat(repository.searchVisibleTo(null, "", 0, 20).totalElements()).isEqualTo(2L);
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
