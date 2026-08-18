package com.vetsoftware.app.prescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository;
import com.vetsoftware.app.animal.infrastructure.persistence.WeightRecordJpaRepository.LatestWeightProjection;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaEntity;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.prescription.application.dto.PrescriptionSignalment;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@code AnimalJpaEntity}/{@code OwnerJpaEntity}/{@code CompanyJpaEntity} y
 * compañía pertenecen a otras features y son filas sin invariantes propias: se
 * mockean para ejercitar cada rama del ensamblado de la fórmula sin levantar
 * base real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPrescriptionReportQueryPort — carga de la fórmula")
class JpaPrescriptionReportQueryPortTest {

    private static final LocalDate HOY = LocalDate.of(2026, 8, 17);

    @Mock
    private AnimalJpaRepository animalJpaRepository;
    @Mock
    private WeightRecordJpaRepository weightRecordJpaRepository;

    @Mock
    private AnimalJpaEntity animalEntity;
    @Mock
    private OwnerJpaEntity ownerEntity;
    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private CityJpaEntity cityEntity;
    @Mock
    private SpecieJpaEntity specieEntity;
    @Mock
    private BreedJpaEntity breedEntity;
    @Mock
    private AnimalColorJpaEntity colorEntity;
    @Mock
    private LatestWeightProjection weightProjection;

    @InjectMocks
    private JpaPrescriptionReportQueryPort port;

    /** Boilerplate comun a toda fila: nombre, ficha clinica, tutor y clinica. */
    @BeforeEach
    void datosComunesDeLaFila() {
        // lenient: algunos tests (p.ej. color null) no llegan a leer todos estos
        // getters, pero son la parte estable de la fila en el resto de escenarios.
        // lenient: el escenario "animal no encontrado" nunca llega a toSignalment, asi
        // que este id no se llega a leer en ese caso.
        lenient().when(animalEntity.getId()).thenReturn(PrescriptionMother.ANIMAL_ID);
        lenient().when(animalEntity.getName()).thenReturn("Firulais");
        lenient().when(animalEntity.getCode()).thenReturn("A-001");
        lenient().when(animalEntity.getSpecie()).thenReturn(specieEntity);
        lenient().when(specieEntity.getName()).thenReturn("Perro");
        lenient().when(animalEntity.getBreed()).thenReturn(breedEntity);
        lenient().when(breedEntity.getName()).thenReturn("Labrador");
        lenient().when(animalEntity.getOwner()).thenReturn(ownerEntity);
        lenient().when(animalEntity.getCompany()).thenReturn(companyEntity);
        lenient().when(ownerEntity.getName()).thenReturn("Juan Perez");
        lenient().when(ownerEntity.getDocument()).thenReturn("123456");
        lenient().when(ownerEntity.getPhone()).thenReturn("3111111111");
        lenient().when(ownerEntity.getEmail()).thenReturn("juan@test.local");
        lenient().when(ownerEntity.getAddress()).thenReturn("Calle 2");
        lenient().when(companyEntity.getName()).thenReturn("Veterinaria Test");
        lenient().when(companyEntity.getIdentifier()).thenReturn("900123456");
        lenient().when(companyEntity.getAddress()).thenReturn("Calle 1");
        lenient().when(companyEntity.getContactNumber()).thenReturn("3000000000");
        lenient().when(companyEntity.getCity()).thenReturn(cityEntity);
        lenient().when(cityEntity.getName()).thenReturn("Medellin");
        lenient().when(animalEntity.getGender()).thenReturn(Gender.MALE);
        lenient().when(animalEntity.getColor()).thenReturn(colorEntity);
        lenient().when(colorEntity.getName()).thenReturn("Negro");
        lenient().when(animalEntity.getBod()).thenReturn(HOY.minusYears(3));
        lenient().when(animalJpaRepository.findByIdAndCompany_Id(PrescriptionMother.ANIMAL_ID,
                PrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(animalEntity));
        lenient().when(weightRecordJpaRepository.findLatestByAnimalId(PrescriptionMother.ANIMAL_ID,
                PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());
    }

    private PrescriptionSignalment cargar() {
        return port.loadByAnimal(PrescriptionMother.ANIMAL_ID, PrescriptionMother.COMPANY_ID)
                .orElseThrow();
    }

    @Nested
    @DisplayName("animal no encontrado")
    class NoEncontrado {

        @Test
        @DisplayName("devuelve vacio sin ensamblar nada")
        void devuelve_vacio_sin_ensamblar_nada() {
            when(animalJpaRepository.findByIdAndCompany_Id(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThat(
                    port.loadByAnimal(PrescriptionMother.ANIMAL_ID, PrescriptionMother.COMPANY_ID))
                    .isEmpty();

            verifyNoInteractions(weightRecordJpaRepository);
        }
    }

    @Nested
    @DisplayName("clinica, paciente y tutor")
    class DatosBase {

        @Test
        @DisplayName("copia clinica, paciente y tutor tal cual llegan de las filas")
        void copia_clinica_paciente_y_tutor() {
            PrescriptionSignalment signalment = cargar();

            assertThat(signalment.clinicName()).isEqualTo("Veterinaria Test");
            assertThat(signalment.clinicIdentifier()).isEqualTo("900123456");
            assertThat(signalment.clinicAddress()).isEqualTo("Calle 1");
            assertThat(signalment.clinicPhone()).isEqualTo("3000000000");
            assertThat(signalment.clinicCity()).isEqualTo("Medellin");
            assertThat(signalment.patientName()).isEqualTo("Firulais");
            assertThat(signalment.patientCode()).isEqualTo("A-001");
            assertThat(signalment.patientSpecies()).isEqualTo("Perro");
            assertThat(signalment.patientBreed()).isEqualTo("Labrador");
            assertThat(signalment.ownerName()).isEqualTo("Juan Perez");
            assertThat(signalment.ownerDocument()).isEqualTo("123456");
            assertThat(signalment.ownerPhone()).isEqualTo("3111111111");
            assertThat(signalment.ownerEmail()).isEqualTo("juan@test.local");
            assertThat(signalment.ownerAddress()).isEqualTo("Calle 2");
        }

        @Test
        @DisplayName("una clinica sin ciudad deja clinicCity en null")
        void clinica_sin_ciudad_deja_clinic_city_null() {
            when(companyEntity.getCity()).thenReturn(null);

            assertThat(cargar().clinicCity()).isNull();
        }

        @Test
        @DisplayName("un animal sin color deja patientColor en null")
        void animal_sin_color_deja_patient_color_null() {
            when(animalEntity.getColor()).thenReturn(null);

            assertThat(cargar().patientColor()).isNull();
        }

        @Test
        @DisplayName("un color asignado se traduce al nombre")
        void color_asignado_se_traduce_al_nombre() {
            assertThat(cargar().patientColor()).isEqualTo("Negro");
        }
    }

    @Nested
    @DisplayName("sexo — genderLabel")
    class Sexo {

        @Test
        @DisplayName("MALE se traduce a Macho")
        void male_se_traduce_a_macho() {
            when(animalEntity.getGender()).thenReturn(Gender.MALE);

            assertThat(cargar().patientSex()).isEqualTo("Macho");
        }

        @Test
        @DisplayName("FEMALE se traduce a Hembra")
        void female_se_traduce_a_hembra() {
            when(animalEntity.getGender()).thenReturn(Gender.FEMALE);

            assertThat(cargar().patientSex()).isEqualTo("Hembra");
        }

        @Test
        @DisplayName("sin sexo registrado queda en null")
        void sin_sexo_registrado_queda_en_null() {
            when(animalEntity.getGender()).thenReturn(null);

            assertThat(cargar().patientSex()).isNull();
        }
    }

    @Nested
    @DisplayName("edad — ageLabel")
    class Edad {

        @Test
        @DisplayName("sin fecha de nacimiento queda en null")
        void sin_fecha_de_nacimiento_queda_en_null() {
            when(animalEntity.getBod()).thenReturn(null);

            assertThat(cargar().patientAge()).isNull();
        }

        @Test
        @DisplayName("una fecha de nacimiento futura queda en null")
        void fecha_de_nacimiento_futura_queda_en_null() {
            when(animalEntity.getBod()).thenReturn(LocalDate.now().plusDays(1));

            assertThat(cargar().patientAge()).isNull();
        }

        @Test
        @DisplayName("un año exacto sin meses no menciona meses")
        void un_anio_exacto_sin_meses() {
            when(animalEntity.getBod()).thenReturn(LocalDate.now().minusYears(2));

            assertThat(cargar().patientAge()).isEqualTo("2 años");
        }

        @Test
        @DisplayName("exactamente un año usa el singular")
        void exactamente_un_anio_usa_singular() {
            when(animalEntity.getBod()).thenReturn(LocalDate.now().minusYears(1));

            assertThat(cargar().patientAge()).isEqualTo("1 año");
        }

        @Test
        @DisplayName("exactamente un mes usa el singular")
        void exactamente_un_mes_usa_singular() {
            when(animalEntity.getBod()).thenReturn(LocalDate.now().minusMonths(1));

            assertThat(cargar().patientAge()).isEqualTo("1 mes");
        }

        @Test
        @DisplayName("exactamente un dia usa el singular")
        void exactamente_un_dia_usa_singular() {
            when(animalEntity.getBod()).thenReturn(LocalDate.now().minusDays(1));

            assertThat(cargar().patientAge()).isEqualTo("1 día");
        }

        @Test
        @DisplayName("años con meses combina los dos, con singular/plural correcto")
        void anios_con_meses_combina_los_dos() {
            when(animalEntity.getBod()).thenReturn(LocalDate.now().minusYears(1).minusMonths(1));

            assertThat(cargar().patientAge()).contains("1 año").contains("1 mes")
                    .doesNotContain("meses");
        }

        @Test
        @DisplayName("menos de un año pero al menos un mes solo muestra meses")
        void menos_de_un_anio_muestra_solo_meses() {
            when(animalEntity.getBod()).thenReturn(LocalDate.now().minusMonths(3));

            assertThat(cargar().patientAge()).isEqualTo("3 meses");
        }

        @Test
        @DisplayName("menos de un mes muestra dias")
        void menos_de_un_mes_muestra_dias() {
            when(animalEntity.getBod()).thenReturn(LocalDate.now().minusDays(5));

            assertThat(cargar().patientAge()).isEqualTo("5 días");
        }
    }

    @Nested
    @DisplayName("peso — formatWeight")
    class Peso {

        @Test
        @DisplayName("sin registro de peso queda en null, sin formatear nada")
        void sin_registro_de_peso_queda_null() {
            assertThat(cargar().patientWeight()).isNull();
        }

        @ParameterizedTest(name = "{0} {1} -> {2}")
        @CsvSource({"12.50,KILOGRAMS,12.5 kg", "500,GRAMS,500 g", "10,POUNDS,10 lb", "7,OTRA,7"})
        @DisplayName("cada unidad conocida agrega su sufijo; una desconocida no agrega nada")
        void formatea_segun_la_unidad(String valor, String unidad, String esperado) {
            when(weightRecordJpaRepository.findLatestByAnimalId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(weightProjection));
            when(weightProjection.getValue()).thenReturn(new BigDecimal(valor));
            when(weightProjection.getUnit()).thenReturn(unidad);

            assertThat(cargar().patientWeight()).isEqualTo(esperado);
        }

        @Test
        @DisplayName("sin unidad registrada tampoco agrega sufijo")
        void sin_unidad_no_agrega_sufijo() {
            when(weightRecordJpaRepository.findLatestByAnimalId(PrescriptionMother.ANIMAL_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(weightProjection));
            when(weightProjection.getValue()).thenReturn(new BigDecimal("8.00"));
            when(weightProjection.getUnit()).thenReturn(null);

            assertThat(cargar().patientWeight()).isEqualTo("8");
        }
    }
}
