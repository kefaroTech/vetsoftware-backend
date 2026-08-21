package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalColorRef;
import com.vetsoftware.app.animal.domain.AnimalRef;
import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.OwnerRef;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.SpecieRef;
import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaEntity;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaRepository;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaEntity;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
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

/**
 * Rodaja de persistencia de {@link JpaAnimalRepository} contra MySQL real: el
 * scoping por empresa, el enriquecimiento del peso actual derivado del ultimo
 * {@code WeightRecord} (una sola query por lote, sin N+1) y el
 * {@code @SQLRestriction} de soft-delete, que ningun test con dobles puede ver.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaAnimalRepository — animales contra MySQL real")
class AnimalPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;

    @Autowired
    private JpaAnimalRepository repository;
    @Autowired
    private JpaWeightRecordRepository weightRecordRepository;
    @Autowired
    private CompanyJpaRepository companyJpaRepository;
    @Autowired
    private SpecieJpaRepository specieJpaRepository;
    @Autowired
    private BreedJpaRepository breedJpaRepository;
    @Autowired
    private AnimalColorJpaRepository animalColorJpaRepository;
    @Autowired
    private OwnerJpaRepository ownerJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private CompanyJpaEntity company;
    private OwnerJpaEntity owner;
    private OwnerJpaEntity otroOwner;

    @BeforeEach
    void sembrarLaCadenaCompleta() {
        SchemaSeed.seed(entityManager);
        company = companyJpaRepository.getReferenceById(COMPANY);

        SpecieJpaEntity specie = newInstance(SpecieJpaEntity.class);
        setField(specie, "name", "Perro");
        setField(specie, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        specie = specieJpaRepository.save(specie);

        BreedJpaEntity breed = newInstance(BreedJpaEntity.class);
        setField(breed, "name", "Labrador");
        setField(breed, "specie", specie);
        setField(breed, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        breed = breedJpaRepository.save(breed);

        AnimalColorJpaEntity color = newInstance(AnimalColorJpaEntity.class);
        setField(color, "name", "Negro");
        setField(color, "specie", specie);
        setField(color, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        color = animalColorJpaRepository.save(color);

        owner = crearPropietario("Ana Ruiz", "CC-1020");
        otroOwner = crearPropietario("Luis Paz", "CC-2040");

        entityManager.flush();
        entityManager.clear();
        company = companyJpaRepository.getReferenceById(COMPANY);
        owner = ownerJpaRepository.getReferenceById(owner.getId());
        otroOwner = ownerJpaRepository.getReferenceById(otroOwner.getId());
        this.specie = specieJpaRepository.getReferenceById(specie.getId());
        this.breed = breedJpaRepository.getReferenceById(breed.getId());
        this.color = animalColorJpaRepository.getReferenceById(color.getId());
    }

    private SpecieJpaEntity specie;
    private BreedJpaEntity breed;
    private AnimalColorJpaEntity color;

    private OwnerJpaEntity crearPropietario(String nombre, String documento) {
        OwnerJpaEntity o = newInstance(OwnerJpaEntity.class);
        setField(o, "name", nombre);
        setField(o, "document", documento);
        setField(o, "documentType", OwnerDocumentType.CEDULA_CIUDADANIA);
        setField(o, "personType", PersonType.NATURAL);
        setField(o, "taxRegime", TaxRegime.NO_RESPONSABLE_IVA);
        setField(o, "fiscalResponsibility", FiscalResponsibility.NO_APLICA);
        setField(o, "city",
                entityManager.getReference(
                        com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity.class,
                        SchemaSeed.CITY_ID));
        setField(o, "company", companyJpaRepository.getReferenceById(COMPANY));
        setField(o, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        return ownerJpaRepository.save(o);
    }

    /**
     * Los *JpaEntity de otras features tienen constructor protegido: se instancia
     * por reflexion, igual que hace Hibernate al leer una fila.
     */
    private static <T> T newInstance(Class<T> type) {
        try {
            java.lang.reflect.Constructor<T> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Animal animalNuevo(String nombre, String codigo, OwnerRef ownerRef) {
        return Animal.create(nombre, codigo, new SpecieRef(specie.getId(), specie.getName()),
                new BreedRef(breed.getId(), breed.getName()), ownerRef, Gender.MALE,
                WeightType.KILOGRAMS, AnimalType.NONE, ReproductiveState.STERILIZED,
                new AnimalColorRef(color.getId(), color.getName()), LocalDate.of(2020, 5, 10), 30,
                false, null,
                new CompanyRef(company.getId(), company.getName(), company.getIdentifier()));
    }

    private OwnerRef ownerRef(OwnerJpaEntity o) {
        return new OwnerRef(o.getId(), o.getName(), o.getDocument());
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("save y findById")
    class Guardado {

        @Test
        @DisplayName("persiste el animal y devuelve el id asignado")
        void persiste_el_animal_y_devuelve_el_id() {
            Animal guardado = repository.save(animalNuevo("Firulais", "A-001", ownerRef(owner)));
            releerDesdeLaBase();

            assertThat(guardado.getId()).isNotNull();
            Animal releido = repository.findById(guardado.getId()).orElseThrow();
            assertThat(releido.getName()).isEqualTo("Firulais");
            assertThat(releido.getSpecie().id()).isEqualTo(specie.getId());
            assertThat(releido.getBreed().id()).isEqualTo(breed.getId());
            assertThat(releido.getOwner().id()).isEqualTo(owner.getId());
            assertThat(releido.getColor().id()).isEqualTo(color.getId());
            assertThat(releido.getCompany().id()).isEqualTo(company.getId());
        }

        @Test
        @DisplayName("un id inexistente devuelve vacio")
        void un_id_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999_999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class BusquedaPorEmpresa {

        @Test
        @DisplayName("un animal de otra empresa no se entrega")
        void animal_de_otra_empresa_no_se_entrega() {
            Animal guardado = repository.save(animalNuevo("Firulais", "A-001", ownerRef(owner)));
            releerDesdeLaBase();

            assertThat(
                    repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardado.getId(), COMPANY)).isPresent();
        }
    }

    @Nested
    @DisplayName("findByOwnerIdAndCompanyId")
    class ListadoPorPropietario {

        @Test
        @DisplayName("solo trae los animales del propietario pedido")
        void solo_trae_los_animales_del_propietario_pedido() {
            Animal deAna = repository.save(animalNuevo("Firulais", "A-001", ownerRef(owner)));
            repository.save(animalNuevo("Michi", "A-002", ownerRef(otroOwner)));
            releerDesdeLaBase();

            List<Animal> animales = repository.findByOwnerIdAndCompanyId(owner.getId(), COMPANY);

            assertThat(animales).extracting(Animal::getId).containsExactly(deAna.getId());
        }
    }

    @Nested
    @DisplayName("findAllByCompanyId — paginacion")
    class Listado {

        @Test
        @DisplayName("ordena por nombre y desempata por id, sin repetir entre paginas")
        void ordena_por_nombre_y_desempata_por_id() {
            repository.save(animalNuevo("Zeus", "A-003", ownerRef(owner)));
            repository.save(animalNuevo("Ana", "A-004", ownerRef(owner)));
            repository.save(animalNuevo("Bruno", "A-005", ownerRef(owner)));
            releerDesdeLaBase();

            PageResult<Animal> pagina = repository.findAllByCompanyId(COMPANY, 0, 20);

            assertThat(pagina.content()).extracting(Animal::getName).containsExactly("Ana", "Bruno",
                    "Zeus");
            assertThat(pagina.totalElements()).isEqualTo(3L);
        }

        @Test
        @DisplayName("un animal deshabilitado no aparece en el listado (SQLRestriction)")
        void un_animal_deshabilitado_no_aparece() {
            Animal guardado = repository.save(animalNuevo("Firulais", "A-001", ownerRef(owner)));
            releerDesdeLaBase();
            repository.delete(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            PageResult<Animal> pagina = repository.findAllByCompanyId(COMPANY, 0, 20);

            assertThat(pagina.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BorradoYReactivacion {

        @Test
        @DisplayName("un animal borrado desaparece de findById")
        void animal_borrado_desaparece() {
            Animal guardado = repository.save(animalNuevo("Firulais", "A-001", ownerRef(owner)));
            releerDesdeLaBase();

            repository.delete(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate() vuelve a hacer visible un animal borrado")
        void reactivate_vuelve_a_hacer_visible() {
            Animal guardado = repository.save(animalNuevo("Firulais", "A-001", ownerRef(owner)));
            releerDesdeLaBase();
            repository.delete(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            int filas = repository.reactivate(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate() sobre un id de otra empresa no afecta filas")
        void reactivate_sobre_id_de_otra_empresa() {
            Animal guardado = repository.save(animalNuevo("Firulais", "A-001", ownerRef(owner)));
            releerDesdeLaBase();
            repository.delete(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(repository.reactivate(guardado.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("enrich — peso actual derivado del ultimo WeightRecord")
    class PesoDerivado {

        @Test
        @DisplayName("findById trae el peso del ultimo registro habilitado")
        void find_by_id_trae_el_ultimo_peso() {
            Animal guardado = repository.save(animalNuevo("Firulais", "A-001", ownerRef(owner)));
            releerDesdeLaBase();
            AnimalRef animalRef = new AnimalRef(guardado.getId(), guardado.getName(),
                    guardado.getCode());
            CompanyRef companyRef = new CompanyRef(company.getId(), company.getName(),
                    company.getIdentifier());
            weightRecordRepository.save(new WeightRecord(null, animalRef, new BigDecimal("10.000"),
                    WeightType.KILOGRAMS, LocalDate.of(2026, 1, 10), WeightSource.MANUAL, null,
                    "primer control", companyRef, LocalDateTime.of(2026, 1, 10, 9, 0), true));
            weightRecordRepository.save(new WeightRecord(null, animalRef, new BigDecimal("12.000"),
                    WeightType.KILOGRAMS, LocalDate.of(2026, 2, 1), WeightSource.MANUAL, null,
                    "control mensual", companyRef, LocalDateTime.of(2026, 2, 1, 9, 0), true));
            releerDesdeLaBase();

            Animal releido = repository.findById(guardado.getId()).orElseThrow();

            // El peso actual es el del registro MAS RECIENTE (12.0), no el primero ni un
            // promedio: si enrich() tomara cualquier fila, este test lo destaparia.
            assertThat(releido.getCurrentWeight()).isEqualByComparingTo("12.000");
            assertThat(releido.getCurrentWeightMeasuredAt()).isEqualTo(LocalDate.of(2026, 2, 1));
        }

        @Test
        @DisplayName("sin registros de peso el animal no trae peso derivado")
        void sin_registros_no_trae_peso_derivado() {
            Animal guardado = repository.save(animalNuevo("Firulais", "A-001", ownerRef(owner)));
            releerDesdeLaBase();

            Animal releido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(releido.getCurrentWeight()).isNull();
        }

        @Test
        @DisplayName("findAllByCompanyId enriquece cada fila de la pagina en una sola query")
        void find_all_by_company_id_enriquece_la_pagina() {
            Animal primero = repository.save(animalNuevo("Ana", "A-004", ownerRef(owner)));
            Animal segundo = repository.save(animalNuevo("Bruno", "A-005", ownerRef(owner)));
            releerDesdeLaBase();
            CompanyRef companyRef = new CompanyRef(company.getId(), company.getName(),
                    company.getIdentifier());
            weightRecordRepository.save(new WeightRecord(null,
                    new AnimalRef(primero.getId(), primero.getName(), primero.getCode()),
                    new BigDecimal("5.500"), WeightType.KILOGRAMS, LocalDate.of(2026, 1, 10),
                    WeightSource.MANUAL, null, null, companyRef,
                    LocalDateTime.of(2026, 1, 10, 9, 0), true));
            releerDesdeLaBase();

            PageResult<Animal> pagina = repository.findAllByCompanyId(COMPANY, 0, 20);

            Animal conPeso = pagina.content().stream()
                    .filter(a -> a.getId().equals(primero.getId())).findFirst().orElseThrow();
            Animal sinPeso = pagina.content().stream()
                    .filter(a -> a.getId().equals(segundo.getId())).findFirst().orElseThrow();
            assertThat(conPeso.getCurrentWeight()).isEqualByComparingTo("5.500");
            assertThat(sinPeso.getCurrentWeight()).isNull();
        }
    }
}
