package com.vetsoftware.app.owner.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.owner.domain.CityRef;
import com.vetsoftware.app.owner.domain.CompanyRef;
import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.Owner;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de propietarios contra MySQL real (BE-10).
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b>
 *
 * <ul>
 * <li><b>La unicidad del documento es por empresa, no global.</b> El indice
 * {@code uq_owners_company_document} es {@code (company_id, document)}: el
 * mismo documento se repite entre dos clinicas sin chocar. Un {@code Map} en
 * memoria por id no distingue esa frontera.</li>
 * <li><b>El soft delete lo hacen dos anotaciones de Hibernate.</b>
 * {@code @SQLDelete} convierte el borrado en {@code UPDATE enabled = false} y
 * {@code @SQLRestriction} esconde la fila de todas las consultas de entidad,
 * menos de la nativa de {@code reactivate}.</li>
 * <li><b>El {@code @EntityGraph} en {@code city, company}</b> es lo que evita
 * el N+1 al listar y buscar: solo se ve pasando por Hibernate.</li>
 * <li><b>La busqueda por termino y el orden de la paginacion</b>
 * ({@code LOWER(...) LIKE}, {@code ORDER BY name, id}) son JPQL que Spring Data
 * parsea al crear el bean: si estuviera mal, el contexto no levantaria.</li>
 * </ul>
 */
@Import({JpaOwnerRepository.class, OwnerJpaMapper.class})
@DisplayName("JpaOwnerRepository — aislamiento por empresa, soft delete y busqueda contra MySQL real")
class OwnerPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;
    private static final Long CIUDAD = SchemaSeed.CITY_ID;
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private JpaOwnerRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Nombre real de {@link #CIUDAD} tras sembrar. {@code SchemaSeed} usa
     * {@code INSERT IGNORE} con un id fijo: si la tabla {@code cities} ya trae ese
     * id desde el baseline de Liquibase (catalogo geografico, no reseteado entre
     * tests porque no es parte de la transaccion con rollback), el nombre real NO
     * es "Medellin" — hay que leerlo, nunca asumirlo.
     */
    private String nombreDeLaCiudadSembrada;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        nombreDeLaCiudadSembrada = (String) entityManager
                .createNativeQuery("SELECT name FROM cities WHERE id = :id")
                .setParameter("id", CIUDAD).getSingleResult();
    }

    private Owner nuevo(String nombre, String documento, Long companyId) {
        return new Owner(null, nombre, nombre.toLowerCase() + "@vet.com", documento,
                OwnerDocumentType.CEDULA_CIUDADANIA, PersonType.NATURAL, null, null,
                "Calle 1 # 2-3", "3001112233", new CityRef(CIUDAD, nombreDeLaCiudadSembrada),
                new CompanyRef(companyId, "Veterinaria de prueba", "900123456"), false,
                TaxRegime.NO_RESPONSABLE_IVA, FiscalResponsibility.NO_APLICA, CREADO, null, true);
    }

    private Owner guardar(String nombre, String documento) {
        return repository.save(nuevo(nombre, documento, EMPRESA));
    }

    private void deshabilitar(Long id) {
        repository.delete(id, EMPRESA);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("unicidad del documento por empresa")
    class UnicidadDelDocumento {

        @Test
        @DisplayName("el mismo documento se admite en dos empresas distintas")
        void el_mismo_documento_se_admite_en_dos_empresas_distintas() {
            guardar("Ana Ruiz", "1020304050");

            Owner enOtraEmpresa = repository.save(nuevo("Ana Ruiz", "1020304050", OTRA_EMPRESA));

            assertThat(enOtraEmpresa.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva cada campo y city/company hidratados")
        void guardar_y_releer_conserva_cada_campo() {
            Owner guardado = guardar("Ana Ruiz", "1020304050");

            Owner leido = repository.findByIdAndCompanyId(guardado.getId(), EMPRESA).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Ana Ruiz");
            assertThat(leido.getDocument()).isEqualTo("1020304050");
            assertThat(leido.getCreatedDate()).isEqualTo(CREADO);
            assertThat(leido.isEnabled()).isTrue();
            // CityRef/CompanyRef no se guardan como tal: se reconstruyen desde el
            // @ManyToOne al leer.
            assertThat(leido.getCity()).isEqualTo(new CityRef(CIUDAD, nombreDeLaCiudadSembrada));
            assertThat(leido.getCompany())
                    .isEqualTo(new CompanyRef(EMPRESA, "Veterinaria de prueba", "900123456"));
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("findByIdAndCompanyId no ve un owner de otra empresa")
        void find_by_id_and_company_id_no_ve_un_owner_de_otra_empresa() {
            Owner deOtraEmpresa = repository.save(nuevo("Ana Ruiz", "1020304050", OTRA_EMPRESA));

            assertThat(repository.findByIdAndCompanyId(deOtraEmpresa.getId(), EMPRESA)).isEmpty();
        }

        @Test
        @DisplayName("findAllByCompanyId no mezcla owners de dos empresas")
        void find_all_by_company_id_no_mezcla_las_empresas() {
            Owner propio = guardar("Ana Ruiz", "1020304050");
            Owner ajeno = repository.save(nuevo("Luis Paz", "2030405060", OTRA_EMPRESA));

            PageResult<Owner> pagina = repository.findAllByCompanyId(EMPRESA, 0, 20);

            assertThat(pagina.content()).extracting(Owner::getId).contains(propio.getId())
                    .doesNotContain(ajeno.getId());
        }
    }

    @Nested
    @DisplayName("busqueda por termino")
    class BusquedaPorTermino {

        @Test
        @DisplayName("searchByCompanyAndTerm encuentra por nombre, email o documento, sin distinguir mayusculas")
        void search_by_company_and_term_encuentra_por_nombre_email_o_documento() {
            Owner porNombre = guardar("Ana Ruiz", "1020304050");
            Owner porDocumento = guardar("Luis Paz", "9988776655");
            guardar("Carlos Vera", "1111111111");

            PageResult<Owner> porNombreResultado = repository.searchByCompanyAndTerm(EMPRESA, "ANA",
                    0, 20);
            PageResult<Owner> porDocumentoResultado = repository.searchByCompanyAndTerm(EMPRESA,
                    "9988776655", 0, 20);

            assertThat(porNombreResultado.content()).extracting(Owner::getId)
                    .containsExactly(porNombre.getId());
            assertThat(porDocumentoResultado.content()).extracting(Owner::getId)
                    .containsExactly(porDocumento.getId());
        }

        @Test
        @DisplayName("la busqueda no cruza el limite de empresa")
        void la_busqueda_no_cruza_el_limite_de_empresa() {
            repository.save(nuevo("Ana Ruiz", "1020304050", OTRA_EMPRESA));

            PageResult<Owner> resultado = repository.searchByCompanyAndTerm(EMPRESA, "ana", 0, 20);

            assertThat(resultado.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("soft delete y reactivacion")
    class SoftDelete {

        @Test
        @DisplayName("el listado deja de ver el owner deshabilitado")
        void el_listado_no_ve_el_deshabilitado() {
            Owner pausado = guardar("Ana Ruiz", "1020304050");

            deshabilitar(pausado.getId());

            assertThat(repository.findAllByCompanyId(EMPRESA, 0, 20).content())
                    .extracting(Owner::getId).doesNotContain(pausado.getId());
            assertThat(repository.findByIdAndCompanyId(pausado.getId(), EMPRESA)).isEmpty();
        }

        @Test
        @DisplayName("reactivar devuelve el owner al listado, acotado a su empresa")
        void reactivar_devuelve_el_owner_al_listado() {
            Owner pausado = guardar("Ana Ruiz", "1020304050");
            deshabilitar(pausado.getId());

            int filas = repository.reactivate(pausado.getId(), EMPRESA);

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(pausado.getId(), EMPRESA)).map(Owner::getId)
                    .contains(pausado.getId());
        }

        @Test
        @DisplayName("reactivar con la empresa equivocada no toca ninguna fila")
        void reactivar_con_la_empresa_equivocada_no_toca_ninguna_fila() {
            Owner pausado = guardar("Ana Ruiz", "1020304050");
            deshabilitar(pausado.getId());

            int filas = repository.reactivate(pausado.getId(), OTRA_EMPRESA);

            assertThat(filas).isZero();
        }
    }
}
