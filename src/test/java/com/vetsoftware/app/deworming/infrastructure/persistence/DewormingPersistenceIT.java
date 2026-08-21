package com.vetsoftware.app.deworming.infrastructure.persistence;

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
import com.vetsoftware.app.deworming.domain.AnimalRef;
import com.vetsoftware.app.deworming.domain.CompanyRef;
import com.vetsoftware.app.deworming.domain.ConsultationRef;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.domain.DewormingType;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de {@link JpaDewormingRepository} contra MySQL real:
 * ejercita el {@code getReferenceById} de las tres asociaciones (animal,
 * consulta, empresa), el {@code @SQLRestriction} de soft-delete y el filtro de
 * texto sobre producto/observaciones, que ningun test en memoria puede ver.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaDewormingRepository — desparasitaciones contra MySQL real")
class DewormingPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;

    @Autowired
    private JpaDewormingRepository repository;
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
        setField(consultation, "anamnesis", "Control de rutina");
        setField(consultation, "animal", animal);
        setField(consultation, "company", company);
        setField(consultation, "createdDate", LocalDateTime.of(2026, 1, 10, 9, 0));
        setField(consultation, "enabled", true);
        consultation = consultationJpaRepository.save(consultation);

        entityManager.flush();
        entityManager.clear();
        // Re-adjuntar las referencias tras el flush/clear: getReferenceById exige que
        // la
        // entidad exista, y el resto del test necesita ids estables, no proxies del
        // contexto
        // ya vaciado.
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

    private Deworming desparasitacion(ConsultationRef consultationRef) {
        return Deworming.create(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 1, 1),
                DewormingType.INTERNAL, "Drontal Plus", "1 tableta / 10kg",
                LocalDate.of(2026, 7, 1), "Sin reacciones adversas",
                new AnimalRef(animal.getId(), "Firulais", "A-001"), consultationRef,
                new CompanyRef(company.getId(), company.getName(), company.getIdentifier()));
    }

    private ConsultationRef consultationRef() {
        return new ConsultationRef(consultation.getId(), LocalDate.of(2026, 1, 10));
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("save")
    class Guardado {

        @Test
        @DisplayName("persiste la desparasitacion con su consulta y devuelve el id asignado")
        void persiste_la_desparasitacion_con_su_consulta() {
            Deworming guardada = repository.save(desparasitacion(consultationRef()));
            releerDesdeLaBase();

            assertThat(guardada.getId()).isNotNull();
            Deworming releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getProduct()).isEqualTo("Drontal Plus");
            assertThat(releida.getType()).isEqualTo(DewormingType.INTERNAL);
            assertThat(releida.getAnimal().id()).isEqualTo(animal.getId());
            assertThat(releida.getConsultation().id()).isEqualTo(consultation.getId());
            assertThat(releida.getCompany().id()).isEqualTo(company.getId());
        }

        @Test
        @DisplayName("persiste sin consulta asociada: consultation queda null")
        void persiste_sin_consulta_asociada() {
            Deworming guardada = repository.save(desparasitacion(null));
            releerDesdeLaBase();

            Deworming releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getConsultation()).isNull();
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class BusquedaPorEmpresa {

        @Test
        @DisplayName("una desparasitacion de otra empresa no se entrega")
        void desparasitacion_de_otra_empresa_no_se_entrega() {
            Deworming guardada = repository.save(desparasitacion(consultationRef()));
            releerDesdeLaBase();

            assertThat(
                    repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), COMPANY)).isPresent();
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BorradoYReactivacion {

        @Test
        @DisplayName("una desparasitacion borrada desaparece de findById (SQLRestriction)")
        void desparasitacion_borrada_desaparece() {
            Deworming guardada = repository.save(desparasitacion(consultationRef()));
            releerDesdeLaBase();

            repository.delete(guardada.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardada.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate() vuelve a hacer visible una desparasitacion borrada")
        void reactivate_vuelve_a_hacer_visible() {
            Deworming guardada = repository.save(desparasitacion(consultationRef()));
            releerDesdeLaBase();
            repository.delete(guardada.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(guardada.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardada.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivate() sobre un id inexistente no afecta filas")
        void reactivate_sobre_id_inexistente() {
            assertThat(repository.reactivate(999_999L, COMPANY)).isZero();
        }

        /**
         * BE-fix3: {@code DewormingJpaRepository.reactivate} ahora exige id + companyId
         * en el UPDATE nativo, igual que {@code AnimalJpaRepository.reactivate}. Antes
         * el UPDATE solo filtraba por id y reactivaba un registro de cualquier tenant
         * que conociera el id.
         */
        @Test
        @DisplayName("reactivate() con el companyId de otra empresa no reactiva nada y la fila sigue deshabilitada")
        void reactivate_con_company_id_de_otra_empresa_no_reactiva_nada() {
            Deworming guardada = repository.save(desparasitacion(consultationRef()));
            releerDesdeLaBase();
            repository.delete(guardada.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(guardada.getId(), SchemaSeed.OTRA_COMPANY_ID);
            releerDesdeLaBase();

            assertThat(filas).isZero();
            assertThat(repository.findById(guardada.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class ListadoGlobal {

        @Test
        @DisplayName("trae la desparasitacion recien creada, sin acotar por empresa")
        void trae_la_desparasitacion_recien_creada() {
            repository.save(desparasitacion(consultationRef()));
            releerDesdeLaBase();

            assertThat(repository.findAll()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAllByAnimalIdAndCompanyId")
    class ListadoPorAnimal {

        @Test
        @DisplayName("la mas reciente va primero y no repite entre paginas")
        void la_mas_reciente_va_primero() {
            Deworming primera = repository.save(desparasitacion(consultationRef()));
            releerDesdeLaBase();
            Deworming segunda = repository.save(Deworming.create(LocalDate.of(2026, 5, 1), null,
                    DewormingType.EXTERNAL, "Bravecto", "1 comprimido", null, null,
                    new AnimalRef(animal.getId(), "Firulais", "A-001"), null,
                    new CompanyRef(company.getId(), company.getName(), company.getIdentifier())));
            releerDesdeLaBase();

            PageResult<Deworming> pagina = repository.findAllByAnimalIdAndCompanyId(animal.getId(),
                    COMPANY, null, 0, 20);

            assertThat(pagina.content()).extracting(Deworming::getId)
                    .containsExactly(segunda.getId(), primera.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("el filtro de texto busca en producto y observaciones, sin distinguir mayusculas")
        void el_filtro_de_texto_busca_en_producto_y_observaciones() {
            repository.save(desparasitacion(consultationRef()));
            releerDesdeLaBase();

            assertThat(repository
                    .findAllByAnimalIdAndCompanyId(animal.getId(), COMPANY, "drontal", 0, 20)
                    .content()).hasSize(1);
            assertThat(repository
                    .findAllByAnimalIdAndCompanyId(animal.getId(), COMPANY, "REACCIONES", 0, 20)
                    .content()).hasSize(1);
            assertThat(repository
                    .findAllByAnimalIdAndCompanyId(animal.getId(), COMPANY, "inexistente", 0, 20)
                    .content()).isEmpty();
        }

        @Test
        @DisplayName("no trae desparasitaciones de otra empresa aunque sean del mismo animal")
        void no_trae_desparasitaciones_de_otra_empresa() {
            repository.save(desparasitacion(consultationRef()));
            releerDesdeLaBase();

            assertThat(repository.findAllByAnimalIdAndCompanyId(animal.getId(),
                    SchemaSeed.OTRA_COMPANY_ID, null, 0, 20).content()).isEmpty();
        }
    }
}
