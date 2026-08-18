package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository.LatestWeightProjection;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaEntity;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaEntity;
import com.vetsoftware.app.clinicalhistory.application.dto.AnimalReportInfo;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Nota de construcción de mocks.</b> Los helpers {@code animal(...)} y
 * {@code peso(...)} se llaman SIEMPRE asignados a una variable local antes de
 * pasarlos a {@code when(repo.metodo(...)).thenReturn(...)}. Anidarlos dentro
 * del propio {@code thenReturn(...)} deja a Mockito con la stub externa a
 * medias mientras el helper abre las suyas, y revienta con
 * {@code UnfinishedStubbingException} — no es un detalle de estilo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAnimalReportQueryPort — ficha del animal para el reporte PDF")
class JpaAnimalReportQueryPortTest {

    private static final Long ANIMAL_ID = 100L;
    private static final Long COMPANY_ID = 9L;
    private static final Long OTRA_COMPANY_ID = 77L;

    @Mock
    private AnimalJpaRepository animalJpaRepository;
    @Mock
    private WeightRecordJpaRepository weightRecordJpaRepository;
    @InjectMocks
    private JpaAnimalReportQueryPort port;

    private static AnimalJpaEntity animal(Gender gender, ReproductiveState estado, Long companyId,
            LocalDate nacimiento, AnimalColorJpaEntity color) {
        AnimalJpaEntity a = mock(AnimalJpaEntity.class);
        when(a.getId()).thenReturn(ANIMAL_ID);
        when(a.getName()).thenReturn("Firulais");
        when(a.getCode()).thenReturn("A-001");
        SpecieJpaEntity specie = mock(SpecieJpaEntity.class);
        when(specie.getName()).thenReturn("Perro");
        when(a.getSpecie()).thenReturn(specie);
        BreedJpaEntity breed = mock(BreedJpaEntity.class);
        when(breed.getName()).thenReturn("Labrador");
        when(a.getBreed()).thenReturn(breed);
        when(a.getColor()).thenReturn(color);
        when(a.getGender()).thenReturn(gender);
        when(a.getReproductiveState()).thenReturn(estado);
        when(a.getBod()).thenReturn(nacimiento);
        when(a.isDeceased()).thenReturn(false);
        when(a.getDeceasedDate()).thenReturn(null);
        OwnerJpaEntity owner = mock(OwnerJpaEntity.class);
        when(owner.getName()).thenReturn("Ana Ruiz");
        when(owner.getDocument()).thenReturn("CC-1020");
        when(owner.getPhone()).thenReturn("3001234567");
        when(owner.getEmail()).thenReturn("ana@correo.com");
        when(owner.getAddress()).thenReturn("Calle 1");
        when(a.getOwner()).thenReturn(owner);
        CompanyJpaEntity company = mock(CompanyJpaEntity.class);
        when(company.getId()).thenReturn(companyId);
        when(company.getName()).thenReturn("Clinica Norte");
        when(company.getIdentifier()).thenReturn("NIT-900");
        when(company.getAddress()).thenReturn("Calle Central");
        when(company.getContactNumber()).thenReturn("6011234567");
        when(a.getCompany()).thenReturn(company);
        return a;
    }

    private static AnimalColorJpaEntity color(String nombre) {
        AnimalColorJpaEntity c = mock(AnimalColorJpaEntity.class);
        when(c.getName()).thenReturn(nombre);
        return c;
    }

    private static AnimalJpaEntity animalDeLaCompany() {
        return animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                LocalDate.of(2020, 5, 10), color("Negro"));
    }

    private static LatestWeightProjection peso(BigDecimal valor, String unidad,
            LocalDate medidoEl) {
        LatestWeightProjection p = mock(LatestWeightProjection.class);
        when(p.getValue()).thenReturn(valor);
        when(p.getUnit()).thenReturn(unidad);
        when(p.getMeasuredAt()).thenReturn(medidoEl);
        return p;
    }

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("mapea todos los campos del animal, dueño y empresa cuando es de la company")
        void mapea_todos_los_campos() {
            AnimalJpaEntity entidad = animalDeLaCompany();
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            LatestWeightProjection proyeccion = peso(new BigDecimal("12.50"), "KILOGRAMS",
                    LocalDate.of(2026, 8, 1));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(proyeccion));

            AnimalReportInfo info = port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow();

            assertThat(info.id()).isEqualTo(ANIMAL_ID);
            assertThat(info.name()).isEqualTo("Firulais");
            assertThat(info.code()).isEqualTo("A-001");
            assertThat(info.specieName()).isEqualTo("Perro");
            assertThat(info.breedName()).isEqualTo("Labrador");
            assertThat(info.colorName()).isEqualTo("Negro");
            assertThat(info.genderLabel()).isEqualTo("Macho");
            assertThat(info.reproductiveStateLabel()).isEqualTo("Esterilizado");
            assertThat(info.birthDate()).isEqualTo(LocalDate.of(2020, 5, 10));
            assertThat(info.currentWeight()).isEqualTo("12.5 kg");
            assertThat(info.weightMeasuredAt()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(info.deceased()).isFalse();
            assertThat(info.ownerName()).isEqualTo("Ana Ruiz");
            assertThat(info.ownerDocument()).isEqualTo("CC-1020");
            assertThat(info.ownerPhone()).isEqualTo("3001234567");
            assertThat(info.ownerEmail()).isEqualTo("ana@correo.com");
            assertThat(info.ownerAddress()).isEqualTo("Calle 1");
            assertThat(info.companyName()).isEqualTo("Clinica Norte");
            assertThat(info.companyIdentifier()).isEqualTo("NIT-900");
            assertThat(info.companyAddress()).isEqualTo("Calle Central");
            assertThat(info.companyPhone()).isEqualTo("6011234567");
        }

        @Test
        @DisplayName("sin peso registrado, currentWeight y weightMeasuredAt quedan null")
        void sin_peso_registrado_queda_null() {
            AnimalJpaEntity entidad = animalDeLaCompany();
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            AnimalReportInfo info = port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow();

            assertThat(info.currentWeight()).isNull();
            assertThat(info.weightMeasuredAt()).isNull();
        }

        @Test
        @DisplayName("sin color asignado, colorName queda null")
        void sin_color_queda_null() {
            AnimalJpaEntity sinColor = animal(Gender.FEMALE, ReproductiveState.UNKNOWN, COMPANY_ID,
                    LocalDate.of(2020, 5, 10), null);
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(sinColor));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            AnimalReportInfo info = port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow();

            assertThat(info.colorName()).isNull();
        }

        @Test
        @DisplayName("animal inexistente devuelve vacío y no consulta el peso")
        void animal_inexistente_devuelve_vacio() {
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID)).isEmpty();

            verifyNoInteractions(weightRecordJpaRepository);
        }

        @Test
        @DisplayName("animal de otra empresa devuelve vacío y no consulta el peso")
        void animal_de_otra_empresa_devuelve_vacio() {
            // El filtro por company corta ANTES del mapeo: el resto de getters del
            // animal (nombre, especie, dueño...) nunca se invoca, así que se stubea
            // solo lo que el filtro necesita para no dejar stubs sin usar (strict stubs).
            AnimalJpaEntity deOtraCompany = mock(AnimalJpaEntity.class);
            CompanyJpaEntity company = mock(CompanyJpaEntity.class);
            when(company.getId()).thenReturn(OTRA_COMPANY_ID);
            when(deOtraCompany.getCompany()).thenReturn(company);
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(deOtraCompany));

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID)).isEmpty();

            verifyNoInteractions(weightRecordJpaRepository);
        }
    }

    @Nested
    @DisplayName("genderLabel — recorre las dos ramas del enum más null")
    class GenderLabelTest {

        @ParameterizedTest
        @MethodSource("com.vetsoftware.app.clinicalhistory.infrastructure.persistence.JpaAnimalReportQueryPortTest#generos")
        @DisplayName("cada género trae su etiqueta")
        void cada_genero_trae_su_etiqueta(Gender genero, String etiqueta) {
            AnimalJpaEntity entidad = animal(genero, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.of(2020, 5, 10), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().genderLabel())
                    .isEqualTo(etiqueta);
        }

        @Test
        @DisplayName("género nulo produce etiqueta null")
        void genero_nulo_produce_etiqueta_null() {
            AnimalJpaEntity entidad = animal(null, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.of(2020, 5, 10), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().genderLabel())
                    .isNull();
        }
    }

    static java.util.List<Arguments> generos() {
        return java.util.List.of(Arguments.of(Gender.MALE, "Macho"),
                Arguments.of(Gender.FEMALE, "Hembra"));
    }

    @Nested
    @DisplayName("reproductiveStateLabel — recorre las tres ramas del enum más null")
    class ReproductiveStateLabelTest {

        @ParameterizedTest
        @MethodSource("com.vetsoftware.app.clinicalhistory.infrastructure.persistence.JpaAnimalReportQueryPortTest#estadosReproductivos")
        @DisplayName("cada estado reproductivo trae su etiqueta")
        void cada_estado_trae_su_etiqueta(ReproductiveState estado, String etiqueta) {
            AnimalJpaEntity entidad = animal(Gender.MALE, estado, COMPANY_ID,
                    LocalDate.of(2020, 5, 10), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow()
                    .reproductiveStateLabel()).isEqualTo(etiqueta);
        }
    }

    static java.util.List<Arguments> estadosReproductivos() {
        return java.util.List.of(Arguments.of(ReproductiveState.STERILIZED, "Esterilizado"),
                Arguments.of(ReproductiveState.NO_STERILIZED, "Entero (sin esterilizar)"),
                Arguments.of(ReproductiveState.UNKNOWN, "Sin determinar"));
    }

    @Nested
    @DisplayName("formatWeight — sufijo por unidad y recorte de ceros")
    class FormatWeightTest {

        @ParameterizedTest
        @MethodSource("com.vetsoftware.app.clinicalhistory.infrastructure.persistence.JpaAnimalReportQueryPortTest#unidadesDePeso")
        @DisplayName("cada unidad trae su sufijo")
        void cada_unidad_trae_su_sufijo(String unidad, String sufijoEsperado) {
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.of(2020, 5, 10), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            LatestWeightProjection proyeccion = peso(new BigDecimal("10.00"), unidad,
                    LocalDate.of(2026, 1, 1));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(proyeccion));

            assertThat(
                    port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().currentWeight())
                    .isEqualTo(sufijoEsperado);
        }
    }

    static java.util.List<Arguments> unidadesDePeso() {
        return java.util.List.of(Arguments.of("GRAMS", "10 g"), Arguments.of("POUNDS", "10 lb"),
                Arguments.of("KILOGRAMS", "10 kg"));
    }

    @Nested
    @DisplayName("formatWeight — ramas sin sufijo: unidad null y unidad desconocida")
    class FormatWeightSinSufijoTest {

        @Test
        @DisplayName("sin unidad registrada no agrega sufijo")
        void sin_unidad_no_agrega_sufijo() {
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.of(2020, 5, 10), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            LatestWeightProjection proyeccion = peso(new BigDecimal("10.00"), null,
                    LocalDate.of(2026, 1, 1));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(proyeccion));

            assertThat(
                    port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().currentWeight())
                    .isEqualTo("10");
        }

        @Test
        @DisplayName("una unidad desconocida tampoco agrega sufijo")
        void unidad_desconocida_no_agrega_sufijo() {
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.of(2020, 5, 10), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            LatestWeightProjection proyeccion = peso(new BigDecimal("10.00"), "OTRA",
                    LocalDate.of(2026, 1, 1));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(proyeccion));

            assertThat(
                    port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().currentWeight())
                    .isEqualTo("10");
        }

        @Test
        @DisplayName("un registro de peso sin valor numerico queda en null")
        void registro_de_peso_sin_valor_queda_null() {
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.of(2020, 5, 10), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            LatestWeightProjection proyeccion = peso(null, "KILOGRAMS", LocalDate.of(2026, 1, 1));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.of(proyeccion));

            assertThat(
                    port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().currentWeight())
                    .isNull();
        }
    }

    @Nested
    @DisplayName("ageLabel — años, meses, días y los casos límite")
    class AgeLabelTest {

        @Test
        @DisplayName("una fecha de nacimiento de años atrás produce una etiqueta en años")
        void anios_atras_produce_etiqueta_en_anios() {
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.of(2000, 1, 1), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().ageLabel())
                    .contains("año");
        }

        @Test
        @DisplayName("una cantidad exacta de años sin meses no menciona meses")
        void anios_exactos_sin_meses_no_menciona_meses() {
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.now().minusYears(4), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().ageLabel())
                    .isEqualTo("4 años").doesNotContain("mes");
        }

        @Test
        @DisplayName("exactamente un mes usa el singular")
        void exactamente_un_mes_usa_singular() {
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.now().minusMonths(1), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().ageLabel())
                    .isEqualTo("1 mes");
        }

        @Test
        @DisplayName("exactamente un dia usa el singular")
        void exactamente_un_dia_usa_singular() {
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    LocalDate.now().minusDays(1), color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().ageLabel())
                    .isEqualTo("1 día");
        }

        @Test
        @DisplayName("nacido hace pocos meses produce una etiqueta en meses")
        void hace_pocos_meses_produce_etiqueta_en_meses() {
            LocalDate haceTresMeses = LocalDate.now().minusMonths(3);
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    haceTresMeses, color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().ageLabel())
                    .contains("mes");
        }

        @Test
        @DisplayName("nacido hace pocos días produce una etiqueta en días")
        void hace_pocos_dias_produce_etiqueta_en_dias() {
            LocalDate haceCincoDias = LocalDate.now().minusDays(5);
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    haceCincoDias, color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().ageLabel())
                    .contains("día");
        }

        @Test
        @DisplayName("sin fecha de nacimiento la etiqueta queda null")
        void sin_fecha_de_nacimiento_queda_null() {
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    null, color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().ageLabel())
                    .isNull();
        }

        @Test
        @DisplayName("una fecha de nacimiento futura queda sin etiqueta")
        void fecha_de_nacimiento_futura_queda_sin_etiqueta() {
            LocalDate manana = LocalDate.now().plusDays(1);
            AnimalJpaEntity entidad = animal(Gender.MALE, ReproductiveState.STERILIZED, COMPANY_ID,
                    manana, color("Negro"));
            when(animalJpaRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(entidad));
            when(weightRecordJpaRepository.findLatestByAnimalId(ANIMAL_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(ANIMAL_ID, COMPANY_ID).orElseThrow().ageLabel())
                    .isNull();
        }
    }
}
