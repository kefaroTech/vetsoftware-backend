package com.vetsoftware.app.diagnosticimaging.infrastructure.persistence;

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
import com.vetsoftware.app.diagnosticimaging.domain.AnimalRef;
import com.vetsoftware.app.diagnosticimaging.domain.CompanyRef;
import com.vetsoftware.app.diagnosticimaging.domain.ConsultationRef;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingTypeRef;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaEntity;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence.DiagnosticImagingTypeJpaRepository;
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
 * Rodaja de persistencia de {@link JpaDiagnosticImagingRepository} contra MySQL
 * real: ejercita el {@code getReferenceById} de las cuatro asociaciones (tipo,
 * animal, consulta, empresa), el {@code @SQLRestriction} de soft-delete y el
 * {@code @EntityGraph} + filtro de texto del listado por animal, que ningun
 * test en memoria puede ver.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaDiagnosticImagingRepository — imagenes diagnosticas contra MySQL real")
class DiagnosticImagingPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long SPECIE_ID = 970L;
    private static final Long BREED_ID = 971L;
    private static final Long COLOR_ID = 972L;
    private static final Long OWNER_ID = 973L;
    private static final Long ANIMAL_ID = 974L;
    private static final Long CONSULTATION_TYPE_ID = 975L;
    private static final Long CONSULTATION_ID = 976L;
    private static final Long DIAGNOSTIC_IMAGING_TYPE_ID = 977L;

    @Autowired
    private JpaDiagnosticImagingRepository repository;
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
    @Autowired
    private DiagnosticImagingTypeJpaRepository diagnosticImagingTypeJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private AnimalJpaEntity animal;
    private ConsultationJpaEntity consultation;
    private CompanyJpaEntity company;
    private DiagnosticImagingTypeJpaEntity diagnosticImagingType;

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
        setField(consultation, "anamnesis", "Cojera pata trasera");
        setField(consultation, "animal", animal);
        setField(consultation, "company", company);
        setField(consultation, "createdDate", LocalDateTime.of(2026, 1, 10, 9, 0));
        setField(consultation, "enabled", true);
        consultation = consultationJpaRepository.save(consultation);

        diagnosticImagingType = newInstance(DiagnosticImagingTypeJpaEntity.class);
        setField(diagnosticImagingType, "name", "Radiografia " + DIAGNOSTIC_IMAGING_TYPE_ID);
        setField(diagnosticImagingType, "description", "Radiografia simple");
        setField(diagnosticImagingType, "general", true);
        setField(diagnosticImagingType, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        diagnosticImagingType = diagnosticImagingTypeJpaRepository.save(diagnosticImagingType);

        entityManager.flush();
        entityManager.clear();
        // Re-adjuntar las referencias tras el flush/clear: getReferenceById exige que
        // la entidad exista, y el resto del test necesita ids estables, no proxies del
        // contexto ya vaciado.
        animal = animalJpaRepository.getReferenceById(animal.getId());
        consultation = consultationJpaRepository.getReferenceById(consultation.getId());
        company = companyJpaRepository.getReferenceById(COMPANY);
        diagnosticImagingType = diagnosticImagingTypeJpaRepository
                .getReferenceById(diagnosticImagingType.getId());
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

    private DiagnosticImagingTypeRef tipoRef() {
        return new DiagnosticImagingTypeRef(diagnosticImagingType.getId(),
                diagnosticImagingType.getName());
    }

    private DiagnosticImaging imagenValida() {
        return DiagnosticImaging.create(LocalDate.of(2026, 1, 10), tipoRef(), "Cojera pata trasera",
                "Radiografia de cadera", "Displasia leve", "Control en 30 dias",
                new AnimalRef(animal.getId(), "Firulais", "A-001"),
                new ConsultationRef(consultation.getId(), LocalDate.of(2026, 1, 10)),
                new CompanyRef(company.getId(), company.getName(), company.getIdentifier()));
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("save")
    class Guardado {

        @Test
        @DisplayName("persiste la imagen y devuelve el id asignado")
        void persiste_la_imagen_y_devuelve_el_id() {
            DiagnosticImaging guardada = repository.save(imagenValida());
            releerDesdeLaBase();

            assertThat(guardada.getId()).isNotNull();
            DiagnosticImaging releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getDiagnosis()).isEqualTo("Displasia leve");
            assertThat(releida.getDiagnosticImagingType().id())
                    .isEqualTo(diagnosticImagingType.getId());
            assertThat(releida.getAnimal().id()).isEqualTo(animal.getId());
            assertThat(releida.getConsultation().id()).isEqualTo(consultation.getId());
            assertThat(releida.getCompany().id()).isEqualTo(company.getId());
        }

        @Test
        @DisplayName("una imagen sin consulta persiste con la asociacion en null")
        void una_imagen_sin_consulta_persiste_con_la_asociacion_en_null() {
            DiagnosticImaging sinConsulta = DiagnosticImaging.create(LocalDate.of(2026, 1, 10),
                    tipoRef(), "Cojera pata trasera", "Radiografia de cadera", "Displasia leve",
                    null, new AnimalRef(animal.getId(), "Firulais", "A-001"), null,
                    new CompanyRef(company.getId(), company.getName(), company.getIdentifier()));

            DiagnosticImaging guardada = repository.save(sinConsulta);
            releerDesdeLaBase();

            DiagnosticImaging releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getConsultation()).isNull();
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class BusquedaPorEmpresa {

        @Test
        @DisplayName("una imagen de otra empresa no se entrega")
        void imagen_de_otra_empresa_no_se_entrega() {
            DiagnosticImaging guardada = repository.save(imagenValida());
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
        @DisplayName("una imagen borrada desaparece de findById (SQLRestriction)")
        void imagen_borrada_desaparece() {
            DiagnosticImaging guardada = repository.save(imagenValida());
            releerDesdeLaBase();

            repository.delete(guardada.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardada.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate() vuelve a hacer visible una imagen borrada")
        void reactivate_vuelve_a_hacer_visible() {
            DiagnosticImaging guardada = repository.save(imagenValida());
            releerDesdeLaBase();
            repository.delete(guardada.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(guardada.getId(), COMPANY);
            releerDesdeLaBase();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(guardada.getId(), COMPANY)).isPresent();
        }

        @Test
        @DisplayName("reactivate() con el companyId de OTRA empresa no afecta ninguna fila")
        void reactivate_con_empresa_ajena_no_afecta_filas() {
            DiagnosticImaging guardada = repository.save(imagenValida());
            releerDesdeLaBase();
            repository.delete(guardada.getId());
            releerDesdeLaBase();

            int filas = repository.reactivate(guardada.getId(), SchemaSeed.OTRA_COMPANY_ID);
            releerDesdeLaBase();

            assertThat(filas).isZero();
            assertThat(repository.findById(guardada.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate() sobre un id inexistente no afecta filas")
        void reactivate_sobre_id_inexistente() {
            assertThat(repository.reactivate(999_999L, COMPANY)).isZero();
        }
    }

    @Nested
    @DisplayName("findAllByAnimalIdAndCompanyId")
    class ListadoPorAnimal {

        @Test
        @DisplayName("la mas reciente va primero y no repite entre paginas")
        void la_mas_reciente_va_primero_y_no_repite_entre_paginas() {
            DiagnosticImaging primera = repository.save(imagenValida());
            releerDesdeLaBase();
            DiagnosticImaging segunda = repository.save(DiagnosticImaging.create(
                    LocalDate.of(2026, 2, 1), tipoRef(), "Vomito recurrente", "Ecografia abdominal",
                    "Cuerpo extrano", "Cirugia sugerida",
                    new AnimalRef(animal.getId(), "Firulais", "A-001"), null,
                    new CompanyRef(company.getId(), company.getName(), company.getIdentifier())));
            releerDesdeLaBase();

            PageResult<DiagnosticImaging> pagina = repository
                    .findAllByAnimalIdAndCompanyId(animal.getId(), COMPANY, null, 0, 20);

            assertThat(pagina.content()).extracting(DiagnosticImaging::getId)
                    .containsExactly(segunda.getId(), primera.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("el filtro de texto busca en signos, tipo de estudio, diagnostico y observaciones")
        void el_filtro_de_texto_busca_en_los_campos_clinicos() {
            repository.save(imagenValida());
            releerDesdeLaBase();

            PageResult<DiagnosticImaging> coincide = repository
                    .findAllByAnimalIdAndCompanyId(animal.getId(), COMPANY, "displasia", 0, 20);
            PageResult<DiagnosticImaging> noCoincide = repository
                    .findAllByAnimalIdAndCompanyId(animal.getId(), COMPANY, "no-coincide", 0, 20);

            assertThat(coincide.content()).hasSize(1);
            assertThat(noCoincide.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class Tenancy {

        @Test
        @DisplayName("findById (sin acotar empresa) si encuentra una imagen ajena")
        void find_by_id_sin_acotar_empresa_encuentra_la_ajena() {
            DiagnosticImaging guardada = repository.save(imagenValida());
            releerDesdeLaBase();

            assertThat(repository.findById(guardada.getId())).map(DiagnosticImaging::getId)
                    .contains(guardada.getId());
        }
    }
}
