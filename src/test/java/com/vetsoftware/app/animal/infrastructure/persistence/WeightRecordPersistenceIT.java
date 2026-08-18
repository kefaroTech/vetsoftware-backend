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
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
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
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
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
 * Rodaja de persistencia de {@link JpaWeightRecordRepository} contra MySQL
 * real: el orden de la serie temporal (mas reciente primero, desempatado por
 * id) y el scoping por animal y empresa a la vez, que ningun test con dobles
 * puede ver.
 */
@Import({JpaWeightRecordRepository.class, WeightRecordJpaMapper.class, JpaAnimalRepository.class,
        AnimalJpaMapper.class})
@DisplayName("JpaWeightRecordRepository — serie de peso contra MySQL real")
class WeightRecordPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;

    @Autowired
    private JpaWeightRecordRepository repository;
    @Autowired
    private JpaAnimalRepository animalRepository;
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

    private CompanyRef companyRef;
    private AnimalRef firulais;
    private AnimalRef michi;

    @BeforeEach
    void sembrarUnAnimalPorEmpresa() {
        SchemaSeed.seed(entityManager);
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(COMPANY);

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

        OwnerJpaEntity owner = newInstance(OwnerJpaEntity.class);
        setField(owner, "name", "Ana Ruiz");
        setField(owner, "document", "CC-1020");
        setField(owner, "documentType", OwnerDocumentType.CEDULA_CIUDADANIA);
        setField(owner, "personType", PersonType.NATURAL);
        setField(owner, "taxRegime", TaxRegime.NO_RESPONSABLE_IVA);
        setField(owner, "fiscalResponsibility", FiscalResponsibility.NO_APLICA);
        setField(owner, "city",
                entityManager.getReference(CityJpaEntity.class, SchemaSeed.CITY_ID));
        setField(owner, "company", company);
        setField(owner, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        owner = ownerJpaRepository.save(owner);

        SpecieRef specieRef = new SpecieRef(specie.getId(), specie.getName());
        BreedRef breedRef = new BreedRef(breed.getId(), breed.getName());
        OwnerRef ownerRef = new OwnerRef(owner.getId(), owner.getName(), owner.getDocument());
        AnimalColorRef colorRef = new AnimalColorRef(color.getId(), color.getName());
        CompanyRef companyEntityRef = new CompanyRef(company.getId(), company.getName(),
                company.getIdentifier());

        Animal firulaisDomain = Animal.create("Firulais", "A-001", specieRef, breedRef, ownerRef,
                Gender.MALE, WeightType.KILOGRAMS, AnimalType.NONE, ReproductiveState.STERILIZED,
                colorRef, LocalDate.of(2020, 5, 10), 30, false, null, companyEntityRef);
        Animal savedFirulais = animalRepository.save(firulaisDomain);

        Animal michiDomain = Animal.create("Michi", "A-002", specieRef, breedRef, ownerRef,
                Gender.FEMALE, WeightType.KILOGRAMS, AnimalType.NONE, ReproductiveState.STERILIZED,
                colorRef, LocalDate.of(2021, 3, 1), 10, false, null, companyEntityRef);
        Animal savedMichi = animalRepository.save(michiDomain);

        entityManager.flush();
        entityManager.clear();
        companyRef = new CompanyRef(company.getId(), company.getName(), company.getIdentifier());
        firulais = new AnimalRef(savedFirulais.getId(), "Firulais", "A-001");
        michi = new AnimalRef(savedMichi.getId(), "Michi", "A-002");
    }

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

    private WeightRecord registro(AnimalRef animal, BigDecimal valor, LocalDate medidoEl) {
        return WeightRecord.create(animal, valor, WeightType.KILOGRAMS, medidoEl,
                WeightSource.MANUAL, null, "control", companyRef);
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("save")
    class Guardado {

        @Test
        @DisplayName("persiste el registro y devuelve el id asignado")
        void persiste_el_registro_y_devuelve_el_id() {
            WeightRecord guardado = repository
                    .save(registro(firulais, new BigDecimal("10.500"), LocalDate.of(2026, 1, 10)));

            assertThat(guardado.getId()).isNotNull();
            assertThat(guardado.getValue()).isEqualByComparingTo("10.500");
            assertThat(guardado.getAnimal().id()).isEqualTo(firulais.id());
        }
    }

    @Nested
    @DisplayName("findByAnimalIdAndCompanyId — serie temporal")
    class SerieTemporal {

        @Test
        @DisplayName("el mas reciente va primero y desempata por id con la misma fecha")
        void el_mas_reciente_va_primero() {
            WeightRecord primero = repository
                    .save(registro(firulais, new BigDecimal("10.000"), LocalDate.of(2026, 1, 10)));
            WeightRecord segundo = repository
                    .save(registro(firulais, new BigDecimal("11.000"), LocalDate.of(2026, 2, 1)));
            WeightRecord empatado = repository
                    .save(registro(firulais, new BigDecimal("11.500"), LocalDate.of(2026, 2, 1)));
            releerDesdeLaBase();

            List<WeightRecord> serie = repository.findByAnimalIdAndCompanyId(firulais.id(),
                    COMPANY);

            assertThat(serie).extracting(WeightRecord::getId).containsExactly(empatado.getId(),
                    segundo.getId(), primero.getId());
        }

        @Test
        @DisplayName("no mezcla la serie de otro animal de la misma empresa")
        void no_mezcla_la_serie_de_otro_animal() {
            repository
                    .save(registro(firulais, new BigDecimal("10.000"), LocalDate.of(2026, 1, 10)));
            WeightRecord deMichi = repository
                    .save(registro(michi, new BigDecimal("4.000"), LocalDate.of(2026, 1, 10)));
            releerDesdeLaBase();

            List<WeightRecord> serieDeMichi = repository.findByAnimalIdAndCompanyId(michi.id(),
                    COMPANY);

            assertThat(serieDeMichi).extracting(WeightRecord::getId)
                    .containsExactly(deMichi.getId());
        }
    }

    @Nested
    @DisplayName("findLatestByAnimalIdAndCompanyId")
    class UltimoRegistro {

        @Test
        @DisplayName("trae el ultimo registro, no el primero que se guardo")
        void trae_el_ultimo_registro() {
            repository
                    .save(registro(firulais, new BigDecimal("10.000"), LocalDate.of(2026, 1, 10)));
            WeightRecord ultimo = repository
                    .save(registro(firulais, new BigDecimal("12.000"), LocalDate.of(2026, 3, 1)));
            releerDesdeLaBase();

            assertThat(repository.findLatestByAnimalIdAndCompanyId(firulais.id(), COMPANY))
                    .map(WeightRecord::getId).contains(ultimo.getId());
        }

        @Test
        @DisplayName("sin registros devuelve vacio")
        void sin_registros_devuelve_vacio() {
            assertThat(repository.findLatestByAnimalIdAndCompanyId(firulais.id(), COMPANY))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndAnimalIdAndCompanyId")
    class BusquedaPorAnimalYEmpresa {

        @Test
        @DisplayName("un registro de otro animal no se entrega aunque sea de la misma empresa")
        void registro_de_otro_animal_no_se_entrega() {
            WeightRecord guardado = repository
                    .save(registro(firulais, new BigDecimal("10.000"), LocalDate.of(2026, 1, 10)));
            releerDesdeLaBase();

            assertThat(repository.findByIdAndAnimalIdAndCompanyId(guardado.getId(), michi.id(),
                    COMPANY)).isEmpty();
            assertThat(repository.findByIdAndAnimalIdAndCompanyId(guardado.getId(), firulais.id(),
                    COMPANY)).isPresent();
        }
    }

    @Nested
    @DisplayName("delete")
    class Borrado {

        @Test
        @DisplayName("un registro borrado desaparece de la serie (SQLRestriction)")
        void un_registro_borrado_desaparece_de_la_serie() {
            WeightRecord guardado = repository
                    .save(registro(firulais, new BigDecimal("10.000"), LocalDate.of(2026, 1, 10)));
            releerDesdeLaBase();

            repository.delete(guardado.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(repository.findByAnimalIdAndCompanyId(firulais.id(), COMPANY)).isEmpty();
        }

        @Test
        @DisplayName("borrar con el id de otra empresa no afecta el registro")
        void borrar_con_id_de_otra_empresa_no_afecta() {
            WeightRecord guardado = repository
                    .save(registro(firulais, new BigDecimal("10.000"), LocalDate.of(2026, 1, 10)));
            releerDesdeLaBase();

            repository.delete(guardado.getId(), SchemaSeed.OTRA_COMPANY_ID);
            releerDesdeLaBase();

            assertThat(repository.findByAnimalIdAndCompanyId(firulais.id(), COMPANY)).hasSize(1);
        }
    }
}
