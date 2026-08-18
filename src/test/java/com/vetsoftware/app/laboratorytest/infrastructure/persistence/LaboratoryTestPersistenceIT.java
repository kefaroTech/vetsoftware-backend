package com.vetsoftware.app.laboratorytest.infrastructure.persistence;

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
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.laboratorytest.application.command.SearchLaboratoryTestsCommand;
import com.vetsoftware.app.laboratorytest.domain.AnimalRef;
import com.vetsoftware.app.laboratorytest.domain.CompanyRef;
import com.vetsoftware.app.laboratorytest.domain.ConsultationRef;
import com.vetsoftware.app.laboratorytest.domain.EmployeeRef;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestTypeRef;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaEntity;
import com.vetsoftware.app.laboratorytesttype.infrastructure.persistence.LaboratoryTestTypeJpaRepository;
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
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
 * Rodaja de persistencia de {@link JpaLaboratoryTestRepository} contra MySQL
 * real: el {@code getReferenceById} de las cinco asociaciones (tipo, animal,
 * consulta, empresa, procesador), el {@code @SQLRestriction} de soft-delete, el
 * {@code @EntityGraph} del listado por animal y el {@code Specification}
 * dinamico de {@code search} — nada de eso lo ve un test con dobles.
 */
@Import({JpaLaboratoryTestRepository.class, LaboratoryTestJpaMapper.class})
@DisplayName("JpaLaboratoryTestRepository — muestras de laboratorio contra MySQL real")
class LaboratoryTestPersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long SPECIE_ID = 960L;
    private static final Long BREED_ID = 961L;
    private static final Long COLOR_ID = 962L;
    private static final Long OWNER_ID = 963L;
    private static final Long ANIMAL_ID = 964L;
    private static final Long CONSULTATION_TYPE_ID = 965L;
    private static final Long CONSULTATION_ID = 966L;
    private static final Long TEST_TYPE_ID = 967L;

    @Autowired
    private JpaLaboratoryTestRepository repository;
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
    private LaboratoryTestTypeJpaRepository testTypeJpaRepository;
    @Autowired
    private EmployeeJpaRepository employeeJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private AnimalJpaEntity animal;
    private ConsultationJpaEntity consultation;
    private CompanyJpaEntity company;
    private LaboratoryTestTypeJpaEntity testType;
    private EmployeeJpaEntity employee;

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
        setField(owner, "name", "Ana Ruiz");
        setField(owner, "document", "CC-1020");
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
        setField(consultationType, "name", "Consulta general " + CONSULTATION_TYPE_ID);
        setField(consultationType, "description", "Consulta general de rutina");
        setField(consultationType, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        consultationType = consultationTypeJpaRepository.save(consultationType);

        consultation = newInstance(ConsultationJpaEntity.class);
        setField(consultation, "date", LocalDate.of(2026, 3, 14));
        setField(consultation, "consultationType", consultationType);
        setField(consultation, "anamnesis", "Decaimiento y palidez");
        setField(consultation, "animal", animal);
        setField(consultation, "company", company);
        setField(consultation, "createdDate", LocalDateTime.of(2026, 3, 14, 9, 0));
        setField(consultation, "enabled", true);
        consultation = consultationJpaRepository.save(consultation);

        testType = newInstance(LaboratoryTestTypeJpaEntity.class);
        setField(testType, "name", "Hemograma " + TEST_TYPE_ID);
        setField(testType, "description", "Hemograma completo");
        setField(testType, "general", true);
        setField(testType, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        testType = testTypeJpaRepository.save(testType);

        employee = newInstance(EmployeeJpaEntity.class);
        setField(employee, "employeeCode", "EMP-LAB-" + TEST_TYPE_ID);
        setField(employee, "hashPassword", "x");
        setField(employee, "name", "Ana Bacteriologa");
        setField(employee, "email", "bacteriologa" + TEST_TYPE_ID + "@test.local");
        setField(employee, "company", company);
        setField(employee, "createdDate", LocalDateTime.of(2026, 1, 1, 0, 0));
        employee = employeeJpaRepository.save(employee);

        entityManager.flush();
        entityManager.clear();
        // Re-adjuntar tras el flush/clear: getReferenceById exige que la fila exista,
        // y el resto del test necesita ids estables, no proxies del contexto vaciado.
        animal = animalJpaRepository.getReferenceById(animal.getId());
        consultation = consultationJpaRepository.getReferenceById(consultation.getId());
        company = companyJpaRepository.getReferenceById(COMPANY);
        testType = testTypeJpaRepository.getReferenceById(testType.getId());
        employee = employeeJpaRepository.getReferenceById(employee.getId());
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

    private LaboratoryTestTypeRef tipoRef() {
        return new LaboratoryTestTypeRef(testType.getId(), testType.getName());
    }

    private AnimalRef animalRef() {
        return new AnimalRef(animal.getId(), "Firulais", "A-001");
    }

    private ConsultationRef consultationRef() {
        return new ConsultationRef(consultation.getId(), LocalDate.of(2026, 3, 14));
    }

    private CompanyRef companyRef() {
        return new CompanyRef(company.getId(), company.getName(), company.getIdentifier());
    }

    private EmployeeRef employeeRef() {
        return new EmployeeRef(employee.getId(), employee.getEmployeeCode(), employee.getName());
    }

    private LaboratoryTest muestraValida() {
        return LaboratoryTest.create(LocalDate.of(2026, 3, 15), tipoRef(), 1, "Sospecha de anemia",
                animalRef(), consultationRef(), companyRef(), SchemaSeed.BRANCH_ID);
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("save y findById")
    class Guardado {

        @Test
        @DisplayName("persiste la muestra con las cinco asociaciones resueltas")
        void persiste_la_muestra_con_las_asociaciones_resueltas() {
            LaboratoryTest guardada = repository.save(muestraValida());
            releerDesdeLaBase();

            assertThat(guardada.getId()).isNotNull();
            LaboratoryTest releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getDiagnosis()).isEqualTo("Sospecha de anemia");
            assertThat(releida.getTestType().id()).isEqualTo(testType.getId());
            assertThat(releida.getAnimal().id()).isEqualTo(animal.getId());
            assertThat(releida.getConsultation().id()).isEqualTo(consultation.getId());
            assertThat(releida.getCompany().id()).isEqualTo(company.getId());
            assertThat(releida.getBranchId()).isEqualTo(SchemaSeed.BRANCH_ID);
        }

        @Test
        @DisplayName("una muestra sin consulta ni procesador persiste con esas asociaciones en null")
        void una_muestra_sin_consulta_ni_procesador_persiste_en_null() {
            LaboratoryTest sinOpcionales = LaboratoryTest.create(LocalDate.of(2026, 3, 15),
                    tipoRef(), 1, null, animalRef(), null, companyRef(), SchemaSeed.BRANCH_ID);

            LaboratoryTest guardada = repository.save(sinOpcionales);
            releerDesdeLaBase();

            LaboratoryTest releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getConsultation()).isNull();
            assertThat(releida.getProcessedBy()).isNull();
        }

        @Test
        @DisplayName("una muestra con procesador asignado lo conserva tras releer")
        void una_muestra_con_procesador_lo_conserva() {
            // Constructor completo, no la factory `create`: esa solo admite un estado
            // inicial PENDING_*, y aqui se necesita una muestra ya validada y firmada.
            LaboratoryTest firmada = new LaboratoryTest(null, LocalDate.of(2026, 3, 15), tipoRef(),
                    1, "Anemia regenerativa", LaboratoryTestStatus.COMPLETED,
                    LaboratoryTestPriority.URGENTE, animalRef(), consultationRef(), companyRef(),
                    SchemaSeed.BRANCH_ID, employeeRef(), LocalDateTime.of(2026, 3, 16, 9, 0),
                    LocalDateTime.of(2026, 3, 15, 8, 0), true);

            LaboratoryTest guardada = repository.save(firmada);
            releerDesdeLaBase();

            LaboratoryTest releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getProcessedBy().id()).isEqualTo(employee.getId());
            assertThat(releida.getStatus()).isEqualTo(LaboratoryTestStatus.COMPLETED);
            assertThat(releida.getPrioridad()).isEqualTo(LaboratoryTestPriority.URGENTE);
        }

        @Test
        @DisplayName("un id inexistente devuelve vacio")
        void un_id_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999_999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll — usado por el listado SYSTEM")
    class ListadoGlobal {

        @Test
        @DisplayName("trae las muestras de todas las empresas, con sus asociaciones hidratadas")
        void trae_las_muestras_de_todas_las_empresas() {
            LaboratoryTest guardada = repository.save(muestraValida());
            releerDesdeLaBase();

            List<LaboratoryTest> todas = repository.findAll();

            assertThat(todas).extracting(LaboratoryTest::getId).contains(guardada.getId());
            assertThat(todas).filteredOn(m -> m.getId().equals(guardada.getId())).first()
                    .extracting(m -> m.getTestType().id()).isEqualTo(testType.getId());
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class BusquedaPorEmpresa {

        @Test
        @DisplayName("una muestra de otra empresa no se entrega")
        void muestra_de_otra_empresa_no_se_entrega() {
            LaboratoryTest guardada = repository.save(muestraValida());
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
        @DisplayName("una muestra borrada desaparece de findById (SQLRestriction)")
        void muestra_borrada_desaparece() {
            LaboratoryTest guardada = repository.save(muestraValida());
            releerDesdeLaBase();

            repository.delete(guardada.getId());
            releerDesdeLaBase();

            assertThat(repository.findById(guardada.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate() vuelve a hacer visible una muestra borrada")
        void reactivate_vuelve_a_hacer_visible() {
            LaboratoryTest guardada = repository.save(muestraValida());
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
            LaboratoryTest guardada = repository.save(muestraValida());
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
        @DisplayName("la mas reciente va primero y no repite entre paginas (orden por id desc)")
        void la_mas_reciente_va_primero() {
            LaboratoryTest primera = repository.save(muestraValida());
            releerDesdeLaBase();
            LaboratoryTest segunda = repository.save(LaboratoryTest.create(
                    LocalDate.of(2026, 3, 20), tipoRef(), 2, "Control de seguimiento", animalRef(),
                    null, companyRef(), SchemaSeed.BRANCH_ID));
            releerDesdeLaBase();

            PageResult<LaboratoryTest> pagina = repository
                    .findAllByAnimalIdAndCompanyId(animal.getId(), COMPANY, null, 0, 20);

            assertThat(pagina.content()).extracting(LaboratoryTest::getId)
                    .containsExactly(segunda.getId(), primera.getId());
            assertThat(pagina.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("el filtro de texto busca en el diagnostico")
        void el_filtro_de_texto_busca_en_el_diagnostico() {
            repository.save(muestraValida());
            releerDesdeLaBase();

            PageResult<LaboratoryTest> coincide = repository
                    .findAllByAnimalIdAndCompanyId(animal.getId(), COMPANY, "anemia", 0, 20);
            PageResult<LaboratoryTest> noCoincide = repository
                    .findAllByAnimalIdAndCompanyId(animal.getId(), COMPANY, "no-coincide", 0, 20);

            assertThat(coincide.content()).hasSize(1);
            assertThat(noCoincide.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("search — filtros dinamicos")
    class Busqueda {

        private SearchLaboratoryTestsCommand comando(List<LaboratoryTestStatus> statuses,
                Long animalId, Long testTypeId, LaboratoryTestPriority prioridad,
                LocalDate dateFrom, LocalDate dateTo, Long branchId) {
            return new SearchLaboratoryTestsCommand(COMPANY, branchId, statuses, animalId,
                    testTypeId, prioridad, dateFrom, dateTo, 0, 20);
        }

        @Test
        @DisplayName("sin filtros trae solo lo de la propia empresa")
        void sin_filtros_trae_solo_lo_de_la_propia_empresa() {
            repository.save(muestraValida());
            releerDesdeLaBase();

            PageResult<LaboratoryTest> resultado = repository
                    .search(comando(null, null, null, null, null, null, null));

            assertThat(resultado.content()).hasSize(1);
        }

        @Test
        @DisplayName("filtra por estado")
        void filtra_por_estado() {
            repository.save(muestraValida());
            releerDesdeLaBase();

            PageResult<LaboratoryTest> pendientes = repository
                    .search(comando(List.of(LaboratoryTestStatus.PENDING_COLLECTION), null, null,
                            null, null, null, null));
            PageResult<LaboratoryTest> completadas = repository.search(comando(
                    List.of(LaboratoryTestStatus.COMPLETED), null, null, null, null, null, null));

            assertThat(pendientes.content()).hasSize(1);
            assertThat(completadas.content()).isEmpty();
        }

        @Test
        @DisplayName("filtra por prioridad")
        void filtra_por_prioridad() {
            repository.save(muestraValida());
            releerDesdeLaBase();

            PageResult<LaboratoryTest> normales = repository.search(
                    comando(null, null, null, LaboratoryTestPriority.NORMAL, null, null, null));
            PageResult<LaboratoryTest> urgentes = repository.search(
                    comando(null, null, null, LaboratoryTestPriority.URGENTE, null, null, null));

            assertThat(normales.content()).hasSize(1);
            assertThat(urgentes.content()).isEmpty();
        }

        @Test
        @DisplayName("filtra por rango de fechas")
        void filtra_por_rango_de_fechas() {
            repository.save(muestraValida());
            releerDesdeLaBase();

            PageResult<LaboratoryTest> dentroDelRango = repository.search(comando(null, null, null,
                    null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), null));
            PageResult<LaboratoryTest> fueraDelRango = repository.search(comando(null, null, null,
                    null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), null));

            assertThat(dentroDelRango.content()).hasSize(1);
            assertThat(fueraDelRango.content()).isEmpty();
        }

        @Test
        @DisplayName("filtra por sede")
        void filtra_por_sede() {
            repository.save(muestraValida());
            releerDesdeLaBase();

            PageResult<LaboratoryTest> sedeCorrecta = repository
                    .search(comando(null, null, null, null, null, null, SchemaSeed.BRANCH_ID));
            PageResult<LaboratoryTest> otraSede = repository
                    .search(comando(null, null, null, null, null, null, SchemaSeed.OTRA_BRANCH_ID));

            assertThat(sedeCorrecta.content()).hasSize(1);
            assertThat(otraSede.content()).isEmpty();
        }

        @Test
        @DisplayName("una muestra de otra empresa nunca aparece, filtre lo que filtre")
        void una_muestra_de_otra_empresa_nunca_aparece() {
            repository.save(muestraValida());
            releerDesdeLaBase();

            SearchLaboratoryTestsCommand deOtraEmpresa = new SearchLaboratoryTestsCommand(
                    SchemaSeed.OTRA_COMPANY_ID, null, null, null, null, null, null, null, 0, 20);

            assertThat(repository.search(deOtraEmpresa).content()).isEmpty();
        }
    }
}
