package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

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
import com.vetsoftware.app.medicament.domain.Medicament;
import com.vetsoftware.app.medicament.infrastructure.persistence.JpaMedicamentRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentRef;
import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import com.vetsoftware.app.prescription.domain.Prescription;
import com.vetsoftware.app.prescription.infrastructure.persistence.JpaPrescriptionRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaRepository;
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
 * Rodaja de persistencia de {@link JpaMedicamentPrescriptionRepository} contra
 * MySQL real: ejercita el {@code getReferenceById} de las dos asociaciones
 * (receta, medicamento) y el {@code @SQLRestriction} de soft-delete, que ningun
 * test en memoria puede ver.
 *
 * <p>
 * Requiere sembrar una receta real (que a su vez cuelga de animal, consulta y
 * empresa) y un medicamento real: se reusan los adaptadores de las features
 * {@code prescription} y {@code medicament} para no duplicar sus invariantes
 * dentro de un test que solo quiere una linea de receta.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaMedicamentPrescriptionRepository — lineas de receta contra MySQL real")
class MedicamentPrescriptionPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;

    @Autowired
    private JpaMedicamentPrescriptionRepository repository;
    @Autowired
    private JpaPrescriptionRepository prescriptionRepository;
    @Autowired
    private JpaMedicamentRepository medicamentRepository;
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

    private PrescriptionRef receta;
    private MedicamentRef medicamento;

    @BeforeEach
    void sembrarLaRecetaYElMedicamento() {
        SchemaSeed.seed(entityManager);
        Prescription prescripcionGuardada = sembrarPrescripcionEnEmpresa(COMPANY, "A-001",
                "123456");
        receta = new PrescriptionRef(prescripcionGuardada.getId(), prescripcionGuardada.getDate());

        Medicament medicamentoGuardado = medicamentRepository
                .save(Medicament.create("Amoxicilina 500mg", "Antibiotico", null, true));
        releerDesdeLaBase();
        medicamento = new MedicamentRef(medicamentoGuardado.getId(), medicamentoGuardado.getName());
    }

    /** Instancia por reflexion, igual que hace Hibernate al leer una fila. */
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

    /**
     * Siembra la cadena completa (especie, raza, color, dueno, animal, consulta y
     * receta) para una empresa dada y devuelve la receta ya persistida.
     */
    private Prescription sembrarPrescripcionEnEmpresa(Long companyId, String codigoAnimal,
            String documentoDueno) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(companyId);

        // Los nombres de especie/raza/color son unicos en todo el schema (no por
        // empresa): se sufijan con el companyId para poder sembrar dos cadenas
        // completas (una por tenant) dentro del mismo test sin chocar.
        SpecieJpaEntity specie = newInstance(SpecieJpaEntity.class);
        setField(specie, "name", "Perro-" + companyId);
        setField(specie, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        specie = specieJpaRepository.save(specie);

        BreedJpaEntity breed = newInstance(BreedJpaEntity.class);
        setField(breed, "name", "Labrador-" + companyId);
        setField(breed, "specie", specie);
        setField(breed, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        breed = breedJpaRepository.save(breed);

        AnimalColorJpaEntity color = newInstance(AnimalColorJpaEntity.class);
        setField(color, "name", "Negro-" + companyId);
        setField(color, "specie", specie);
        setField(color, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        color = animalColorJpaRepository.save(color);

        OwnerJpaEntity owner = newInstance(OwnerJpaEntity.class);
        setField(owner, "name", "Juan Perez");
        setField(owner, "document", documentoDueno);
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

        AnimalJpaEntity animal = newInstance(AnimalJpaEntity.class);
        setField(animal, "name", "Firulais");
        setField(animal, "code", codigoAnimal);
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
        setField(consultationType, "name", "Consulta general-" + companyId);
        setField(consultationType, "description", "Consulta general de rutina");
        setField(consultationType, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        consultationType = consultationTypeJpaRepository.save(consultationType);

        ConsultationJpaEntity consultation = newInstance(ConsultationJpaEntity.class);
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
        animal = animalJpaRepository.getReferenceById(animal.getId());
        consultation = consultationJpaRepository.getReferenceById(consultation.getId());
        company = companyJpaRepository.getReferenceById(companyId);

        Prescription prescripcion = Prescription.create(LocalDate.of(2026, 1, 10), "Otitis externa",
                "Control en 7 dias",
                new com.vetsoftware.app.prescription.domain.AnimalRef(animal.getId(), "Firulais",
                        codigoAnimal),
                new com.vetsoftware.app.prescription.domain.ConsultationRef(consultation.getId(),
                        LocalDate.of(2026, 1, 10)),
                new com.vetsoftware.app.prescription.domain.CompanyRef(company.getId(),
                        company.getName(), company.getIdentifier()));
        Prescription guardada = prescriptionRepository.save(prescripcion);
        releerDesdeLaBase();
        return guardada;
    }

    private MedicamentPrescription linea() {
        return MedicamentPrescription.create(medicamento, "Tableta", 2.0,
                "Cada 12 horas por 7 dias", "Con alimento", receta);
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("save")
    class Guardado {

        @Test
        @DisplayName("persiste la linea y devuelve el id asignado")
        void persiste_la_linea_y_devuelve_el_id() {
            MedicamentPrescription guardada = repository.save(linea());
            releerDesdeLaBase();

            assertThat(guardada.getId()).isNotNull();
            MedicamentPrescription releida = repository.findAll(0, 20).content().stream()
                    .filter(l -> l.getId().equals(guardada.getId())).findFirst().orElseThrow();
            assertThat(releida.getPresentation()).isEqualTo("Tableta");
            assertThat(releida.getQuantity()).isEqualTo(2.0);
            assertThat(releida.getPosology()).isEqualTo("Cada 12 horas por 7 dias");
            assertThat(releida.getObservation()).isEqualTo("Con alimento");
            assertThat(releida.getMedicamentId()).isEqualTo(medicamento.id());
            assertThat(releida.getName()).isEqualTo(medicamento.name());
            assertThat(releida.getPrescription().id()).isEqualTo(receta.id());
        }
    }

    @Nested
    @DisplayName("findAll")
    class Listado {

        @Test
        @DisplayName("la mas reciente va primero y no repite entre paginas")
        void la_mas_reciente_va_primero() {
            MedicamentPrescription primera = repository.save(linea());
            releerDesdeLaBase();
            MedicamentPrescription segunda = repository.save(MedicamentPrescription
                    .create(medicamento, "Ampolla", 1.0, "Una vez al dia", null, receta));
            releerDesdeLaBase();

            PageResult<MedicamentPrescription> pagina = repository.findAll(0, 20);

            assertThat(pagina.content()).extracting(MedicamentPrescription::getId)
                    .containsExactly(segunda.getId(), primera.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
        }
    }
}
