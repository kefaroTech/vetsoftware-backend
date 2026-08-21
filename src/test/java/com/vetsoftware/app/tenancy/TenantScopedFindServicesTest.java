package com.vetsoftware.app.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.animal.application.port.in.FindLatestWeightRecordUseCase;
import com.vetsoftware.app.animal.application.port.out.WeightRecordRepository;
import com.vetsoftware.app.animal.application.usecase.FindLatestWeightRecordService;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.security.access.prepost.PreAuthorize;

class TenantScopedFindServicesTest {

    private static final String BASE_PACKAGE = "com.vetsoftware.app.";
    private static final Path SOURCE_ROOT = Path.of("src", "main", "java", "com", "vetsoftware",
            "app");
    private static final Long RESOURCE_ID = 42L;
    private static final Long COMPANY_ID = 7L;

    private static final List<FindCase> FIND_CASES = List.of(
            new FindCase("prescription", "Prescription"),
            // El catalogo de medicamentos mezcla generales de plataforma con propios de
            // la empresa, asi que su LECTURA acota con findAvailableByIdAndCompanyId (ve
            // ambos) y no con findByIdAndCompanyId, que es la vista de ESCRITURA y solo
            // alcanza lo propio. Sigue siendo tenant-scoped: lo de otra empresa no entra.
            // Ver FindMedicamentService y el @Nested "Disponibilidad" de
            // MedicamentPersistenceIT.
            new FindCase("medicament", "Medicament", "findAvailableByIdAndCompanyId"),
            new FindCase("hospitalization", "Hospitalization"),
            new FindCase("laboratorytest", "LaboratoryTest"),
            new FindCase("laboratorytesttype", "LaboratoryTestType"),
            new FindCase("surgery", "Surgery"), new FindCase("surgerytype", "SurgeryType"),
            new FindCase("vaccination", "Vaccination"),
            new FindCase("vaccinationtype", "VaccinationType"),
            new FindCase("deworming", "Deworming"),
            new FindCase("diagnosticimaging", "DiagnosticImaging"),
            new FindCase("diagnosticimagingtype", "DiagnosticImagingType"),
            new FindCase("spa", "Spa"), new FindCase("employee", "Employee"),
            new FindCase("numberingresolution", "NumberingResolution"),
            new FindCase("promotion", "Promotion"));

    @TestFactory
    Stream<DynamicTest> everyFindServiceRejectsResourcesOutsideTheCompanyScope() {
        return FIND_CASES.stream().map(findCase -> DynamicTest.dynamicTest(findCase.feature(),
                () -> verifyTenantScopedFind(findCase)));
    }

    @Test
    void latestWeightLookupIsTenantScopedToo() throws Exception {
        WeightRecordRepository repository = mock(WeightRecordRepository.class);
        FindLatestWeightRecordService service = new FindLatestWeightRecordService(repository);

        assertThat(catchThrowableOfType(() -> service.findLatest(RESOURCE_ID, COMPANY_ID),
                RuntimeException.class)).hasMessageContaining(String.valueOf(RESOURCE_ID));
        verify(repository).findLatestByAnimalIdAndCompanyId(RESOURCE_ID, COMPANY_ID);

        Method portMethod = FindLatestWeightRecordUseCase.class.getMethod("findLatest", Long.class,
                Long.class);
        assertCompanyGuard(portMethod);
    }

    private static void verifyTenantScopedFind(FindCase findCase) throws Exception {
        Class<?> serviceType = Class
                .forName(findCase.className("application.usecase.Find", "Service"));
        Constructor<?> constructor = serviceType.getConstructors()[0];
        Object[] dependencies = Arrays.stream(constructor.getParameterTypes())
                .map(TenantScopedFindServicesTest::mockDependency).toArray();
        Object service = constructor.newInstance(dependencies);

        Class<?> repositoryType = constructor.getParameterTypes()[0];
        Method repositoryMethod = repositoryType.getMethod(findCase.scopedFinder(), Long.class,
                Long.class);
        Method serviceMethod = serviceType.getMethod("findById", Long.class, Long.class);

        InvocationTargetException failure = catchThrowableOfType(
                () -> serviceMethod.invoke(service, RESOURCE_ID, COMPANY_ID),
                InvocationTargetException.class);
        assertThat(failure.getCause().getClass().getSimpleName()).endsWith("NotFoundException");
        repositoryMethod.invoke(verify(dependencies[0]), RESOURCE_ID, COMPANY_ID);

        Class<?> useCaseType = Class
                .forName(findCase.className("application.port.in.Find", "UseCase"));
        assertCompanyGuard(useCaseType.getMethod("findById", Long.class, Long.class));
        assertSourceArchitecture(findCase);
    }

    private static void assertCompanyGuard(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).as("Missing @PreAuthorize on %s", method).isNotNull();
        assertThat(preAuthorize.value()).contains("@authz.isMyCompany(#companyId)");
    }

    private static void assertSourceArchitecture(FindCase findCase) throws Exception {
        Path serviceFile = SOURCE_ROOT.resolve(Path.of(findCase.feature(), "application", "usecase",
                "Find" + findCase.name() + "Service.java"));
        String serviceSource = Files.readString(serviceFile);
        String compactServiceSource = serviceSource.replaceAll("\\s+", "");
        assertThat(compactServiceSource)
                .contains("repository." + findCase.scopedFinder() + "(id,companyId)");
        assertThat(compactServiceSource).doesNotContain("repository.findById(id)");

        Path controllerFile = SOURCE_ROOT.resolve(Path.of(findCase.feature(), "infrastructure",
                "web", findCase.name() + "Controller.java"));
        String compactControllerSource = Files.readString(controllerFile).replaceAll("\\s+", "");
        assertThat(compactControllerSource)
                .contains("findUseCase.findById(id,authz.currentCompanyId())");
    }

    @SuppressWarnings("unchecked")
    private static <T> T mockDependency(Class<T> type) {
        return (T) org.mockito.Mockito.mock(type);
    }

    /**
     * {@code scopedFinder} es el metodo del puerto de salida con el que ese
     * servicio acota por empresa. Casi siempre es {@code findByIdAndCompanyId}; una
     * feature cuyo catalogo mezcla filas globales con propias declara el suyo,
     * porque su lectura acotada tiene otro nombre y otra semantica.
     */
    private record FindCase(String feature, String name, String scopedFinder) {

        private static final String FINDER_POR_DEFECTO = "findByIdAndCompanyId";

        FindCase(String feature, String name) {
            this(feature, name, FINDER_POR_DEFECTO);
        }

        String className(String prefix, String suffix) {
            return BASE_PACKAGE + feature + '.' + prefix + name + suffix;
        }
    }
}
