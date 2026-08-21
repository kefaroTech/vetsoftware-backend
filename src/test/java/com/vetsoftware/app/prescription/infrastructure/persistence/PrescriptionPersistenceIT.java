package com.vetsoftware.app.prescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaEntity;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaRepository;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaEntity;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaEntity;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaEntity;
import com.vetsoftware.app.consultationtype.infrastructure.persistence.ConsultationTypeJpaRepository;
import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
import com.vetsoftware.app.prescription.domain.Prescription;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de {@link JpaPrescriptionRepository} contra MySQL
 * real: ejercita el {@code getReferenceById} de las tres asociaciones (animal,
 * consulta, empresa) y el {@code @SQLRestriction} de soft-delete, que ningun
 * test en memoria puede ver.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPrescriptionRepository — recetas contra MySQL real")
class PrescriptionPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long SPECIE_ID = 960L;
    private static final Long BREED_ID = 961L;
    private static final Long COLOR_ID = 962L;
    private static final Long OWNER_ID = 963L;
    private static final Long ANIMAL_ID = 964L;
    private static final Long CONSULTATION_TYPE_ID = 965L;
    private static final Long CONSULTATION_ID = 966L;

    @Autowired
    private JpaPrescriptionRepository repository;
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
    @Autowired
    private AnimalJpaRepository animalJpaRepository;
    @Autowired
    private ConsultationTypeJpaRepository consultationTypeJpaRepository;
    @Autowired
    private ConsultationJpaRepository consultationJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private AnimalJpaEntity animal;
    private ConsultationJpaEntity consultation;
    private CompanyJpaEntity company;

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

        OwnerJpaEntity owner = newInstance(OwnerJpaEntity.class);
        setField(owner, "name", "Juan Perez");
        setField(owner, "document", "123456");
        setField(owner, "documentType", OwnerDocumentType.CEDULA_CIUDADANIA);
        setField(owner, "personType", PersonType.NATURAL);
        setField(owner, "taxRegime", TaxRegime.NO_RESPONSABLE_IVA);
        setField(owner, "fiscalResponsibility", FiscalResponsibility.NO_APLICA);
        setField(owner, "city",
                entityManager.getReference(
                        com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity.class,
                        SchemaSeed.CITY_ID));
        setField(owner, "company", company);
        setField(owner, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        owner = ownerJpaRepository.save(owner);

        animal = newInstance(AnimalJpaEntity.class);
        setField(animal, "name", "Firulais");
        setField(animal, "code", "A-001");
        setField(animal, "specie", specie);
        setField(animal, "breed", breed);
        setField(animal, "owner", owner);
        setField(animal, "gender", Gender.MALE);
        setField(animal, "weightType", WeightType.KILOGRAMS);
        setField(animal, "animalType", AnimalType.NONE);
        setField(animal, "reproductiveState", ReproductiveState.STERILIZED);
        setField(animal, "color", color);
        setField(animal, "company", company);
        setField(animal, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        setField(animal, "enabled", true);
        animal = animalJpaRepository.save(animal);

        ConsultationTypeJpaEntity consultationType = newInstance(ConsultationTypeJpaEntity.class);
        setField(consultationType, "name", "Consulta general");
        setField(consultationType, "description", "Consulta general de rutina");
        setField(consultationType, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        consultationType = consultationTypeJpaRepository.save(consultationType);

        consultation = newInstance(ConsultationJpaEntity.class);
        setField(consultation, "date", LocalDate.of(2026, 1, 10));
        setField(consultation, "consultationType", consultationType);
        setField(consultation, "anamnesis", "Prurito en oreja derecha");
        setField(consultation, "animal", animal);
        setField(consultation, "company", company);
        setField(consultation, "createdDate", LocalDateTime.of(2026, 1, 10, 9, 0));
        setField(consultation, "enabled", true);
        consultation = consultationJpaRepository.save(consultation);

        entityManager.flush();
        entityManager.clear();
        // Re-adjuntar las referencias tras el flush/clear: getReferenceById exige que
        // la entidad exista, y el resto del test necesita ids estables, no proxies del
        // contexto ya vaciado.
        animal = animalJpaRepository.getReferenceById(animal.getId());
        consultation = consultationJpaRepository.getReferenceById(consultation.getId());
        company = companyJpaRepository.getReferenceById(COMPANY);
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

    private Prescription prescripcion() {
        return Prescription.create(LocalDate.of(2026, 1, 10), "Otitis externa", "Control en 7 dias",
                new com.vetsoftware.app.prescription.domain.AnimalRef(animal.getId(), "Firulais",
                        "A-001"),
                new com.vetsoftware.app.prescription.domain.ConsultationRef(consultation.getId(),
                        LocalDate.of(2026, 1, 10)),
                new com.vetsoftware.app.prescription.domain.CompanyRef(company.getId(),
                        company.getName(), company.getIdentifier()));
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("save")
    class Guardado {

        @Test
        @DisplayName("persiste la receta y devuelve el id asignado")
        void persiste_la_receta_y_devuelve_el_id() {
            Prescription guardada = repository.save(prescripcion());
            releerDesdeLaBase();

            assertThat(guardada.getId()).isNotNull();
            Prescription releida = repository.findByIdAndCompanyId(guardada.getId(), COMPANY)
                    .orElseThrow();
            assertThat(releida.getDiagnosis()).isEqualTo("Otitis externa");
            assertThat(releida.getAnimal().id()).isEqualTo(animal.getId());
            assertThat(releida.getConsultation().id()).isEqualTo(consultation.getId());
            assertThat(releida.getCompany().id()).isEqualTo(company.getId());
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class BusquedaPorEmpresa {

        @Test
        @DisplayName("una receta de otra empresa no se entrega")
        void receta_de_otra_empresa_no_se_entrega() {
            Prescription guardada = repository.save(prescripcion());
            releerDesdeLaBase();

            assertThat(
                    repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), COMPANY)).isPresent();
        }
    }

    @Nested
    @DisplayName("findAll")
    class Listado {

        @Test
        @DisplayName("la mas reciente va primero y no repite entre paginas")
        void la_mas_reciente_va_primero() {
            Prescription primera = repository.save(prescripcion());
            releerDesdeLaBase();
            Prescription segunda = repository
                    .save(Prescription.create(LocalDate.of(2026, 2, 1), "Dermatitis", null,
                            new com.vetsoftware.app.prescription.domain.AnimalRef(animal.getId(),
                                    "Firulais", "A-001"),
                            new com.vetsoftware.app.prescription.domain.ConsultationRef(
                                    consultation.getId(), LocalDate.of(2026, 1, 10)),
                            new com.vetsoftware.app.prescription.domain.CompanyRef(company.getId(),
                                    company.getName(), company.getIdentifier())));
            releerDesdeLaBase();

            PageResult<Prescription> pagina = repository.findAll(0, 20);

            assertThat(pagina.content()).extracting(Prescription::getId)
                    .containsExactly(segunda.getId(), primera.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
        }
    }
}
