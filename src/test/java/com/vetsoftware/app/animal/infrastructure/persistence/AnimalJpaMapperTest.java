package com.vetsoftware.app.animal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.animalcolor.infrastructure.persistence.AnimalColorJpaEntity;
import com.vetsoftware.app.breed.infrastructure.persistence.BreedJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.specie.infrastructure.persistence.SpecieJpaEntity;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El mapper es el unico punto que conoce dominio y entidad JPA a la vez, asi
 * que un campo cruzado aqui no lo detecta ninguna otra capa: compila, persiste
 * y solo se ve en pantalla.
 *
 * <p>
 * Las entidades JPA de otras features se mockean porque su constructor sin
 * argumentos es {@code protected} y no son instanciables desde este paquete. No
 * tienen logica: son portadores de datos, y mockearlas no oculta
 * comportamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnimalJpaMapper")
class AnimalJpaMapperTest {

    private final AnimalJpaMapper mapper = new AnimalJpaMapper();

    @Mock
    private SpecieJpaEntity specieEntity;
    @Mock
    private BreedJpaEntity breedEntity;
    @Mock
    private OwnerJpaEntity ownerEntity;
    @Mock
    private CompanyJpaEntity companyEntity;
    @Mock
    private AnimalColorJpaEntity colorEntity;

    private AnimalJpaEntity entidadCompleta() {
        AnimalJpaEntity entity = new AnimalJpaEntity();
        entity.setId(AnimalMother.ANIMAL_ID);
        entity.setName("Firulais");
        entity.setCode("A-001");
        entity.setGender(Gender.MALE);
        entity.setWeightType(WeightType.KILOGRAMS);
        entity.setAnimalType(AnimalType.NONE);
        entity.setReproductiveState(ReproductiveState.STERILIZED);
        entity.setBod(AnimalMother.NACIMIENTO);
        entity.setSize(30);
        entity.setDeceased(false);
        entity.setDeceasedDate(null);
        entity.setCreatedDate(AnimalMother.CREADO);
        entity.setEnabled(true);
        return entity;
    }

    @Nested
    @DisplayName("toJpa — dominio a entidad")
    class ToJpa {

        @Test
        @DisplayName("copia cada campo escalar en su columna")
        void copia_cada_campo_escalar_en_su_columna() {
            Animal animal = AnimalMother.perroSano();

            AnimalJpaEntity entity = mapper.toJpa(animal, specieEntity, breedEntity, ownerEntity,
                    companyEntity, colorEntity);

            assertThat(entity.getId()).isEqualTo(AnimalMother.ANIMAL_ID);
            assertThat(entity.getName()).isEqualTo("Firulais");
            assertThat(entity.getCode()).isEqualTo("A-001");
            assertThat(entity.getGender()).isEqualTo(Gender.MALE);
            assertThat(entity.getWeightType()).isEqualTo(WeightType.KILOGRAMS);
            assertThat(entity.getAnimalType()).isEqualTo(AnimalType.NONE);
            assertThat(entity.getReproductiveState()).isEqualTo(ReproductiveState.STERILIZED);
            assertThat(entity.getBod()).isEqualTo(AnimalMother.NACIMIENTO);
            assertThat(entity.getSize()).isEqualTo(30);
            assertThat(entity.isDeceased()).isFalse();
            assertThat(entity.getDeceasedDate()).isNull();
            assertThat(entity.getCreatedDate()).isEqualTo(AnimalMother.CREADO);
            assertThat(entity.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("engancha cada asociacion en su slot")
        void engancha_cada_asociacion_en_su_slot() {
            AnimalJpaEntity entity = mapper.toJpa(AnimalMother.perroSano(), specieEntity,
                    breedEntity, ownerEntity, companyEntity, colorEntity);

            assertThat(entity.getSpecie()).isSameAs(specieEntity);
            assertThat(entity.getBreed()).isSameAs(breedEntity);
            assertThat(entity.getOwner()).isSameAs(ownerEntity);
            assertThat(entity.getCompany()).isSameAs(companyEntity);
            assertThat(entity.getColor()).isSameAs(colorEntity);
        }

        @Test
        @DisplayName("bod y deceasedDate no se cruzan — ambos son LocalDate")
        void bod_y_deceased_date_no_se_cruzan() {
            LocalDate fallecimiento = LocalDate.of(2026, 3, 20);
            Animal animal = AnimalMother.fallecido(fallecimiento);

            AnimalJpaEntity entity = mapper.toJpa(animal, specieEntity, breedEntity, ownerEntity,
                    companyEntity, colorEntity);

            assertThat(entity.getBod()).isEqualTo(AnimalMother.NACIMIENTO);
            assertThat(entity.getDeceasedDate()).isEqualTo(fallecimiento);
            assertThat(entity.isDeceased()).isTrue();
        }

        @Test
        @DisplayName("no arrastra el peso derivado: no es columna de esta tabla")
        void no_arrastra_el_peso_derivado() {
            Animal animal = AnimalMother.conPesoDerivado(new java.math.BigDecimal("12.50"),
                    WeightType.GRAMS, LocalDate.of(2026, 2, 1));

            AnimalJpaEntity entity = mapper.toJpa(animal, specieEntity, breedEntity, ownerEntity,
                    companyEntity, colorEntity);

            // weightType es la unidad PREFERIDA del animal, no la del ultimo registro.
            assertThat(entity.getWeightType()).isEqualTo(WeightType.KILOGRAMS);
        }
    }

    @Nested
    @DisplayName("toDomain con refs precargados — camino de escritura")
    class ToDomainConRefs {

        @Test
        @DisplayName("reconstruye el agregado sin tocar las asociaciones JPA")
        void reconstruye_el_agregado_sin_tocar_las_asociaciones() {
            // Este overload existe para no inicializar los proxies de getReferenceById:
            // si leyera entity.getSpecie(), Hibernate lanzaria un SELECT extra por save.
            Animal animal = mapper.toDomain(entidadCompleta(), AnimalMother.PERRO,
                    AnimalMother.LABRADOR, AnimalMother.DUENO, AnimalMother.CLINICA,
                    AnimalMother.NEGRO);

            assertThat(animal.getId()).isEqualTo(AnimalMother.ANIMAL_ID);
            assertThat(animal.getName()).isEqualTo("Firulais");
            assertThat(animal.getCode()).isEqualTo("A-001");
            assertThat(animal.getSpecie()).isEqualTo(AnimalMother.PERRO);
            assertThat(animal.getBreed()).isEqualTo(AnimalMother.LABRADOR);
            assertThat(animal.getOwner()).isEqualTo(AnimalMother.DUENO);
            assertThat(animal.getCompany()).isEqualTo(AnimalMother.CLINICA);
            assertThat(animal.getColor()).isEqualTo(AnimalMother.NEGRO);
            assertThat(animal.getGender()).isEqualTo(Gender.MALE);
            assertThat(animal.getBod()).isEqualTo(AnimalMother.NACIMIENTO);
            assertThat(animal.getSize()).isEqualTo(30);
            assertThat(animal.getCreatedDate()).isEqualTo(AnimalMother.CREADO);
            assertThat(animal.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("la ida y vuelta dominio → entidad → dominio no pierde nada")
        void la_ida_y_vuelta_no_pierde_nada() {
            Animal original = AnimalMother.perroSano();

            AnimalJpaEntity entity = mapper.toJpa(original, specieEntity, breedEntity, ownerEntity,
                    companyEntity, colorEntity);
            Animal vuelta = mapper.toDomain(entity, original.getSpecie(), original.getBreed(),
                    original.getOwner(), original.getCompany(), original.getColor());

            assertThat(vuelta).usingRecursiveComparison().isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toDomain desde las asociaciones — camino de lectura")
    class ToDomainDesdeAsociaciones {

        @Test
        @DisplayName("construye cada companion VO desde su propia asociacion")
        void construye_cada_companion_vo_desde_su_asociacion() {
            when(specieEntity.getId()).thenReturn(AnimalMother.PERRO.id());
            when(specieEntity.getName()).thenReturn(AnimalMother.PERRO.name());
            when(breedEntity.getId()).thenReturn(AnimalMother.LABRADOR.id());
            when(breedEntity.getName()).thenReturn(AnimalMother.LABRADOR.name());
            when(ownerEntity.getId()).thenReturn(AnimalMother.DUENO.id());
            when(ownerEntity.getName()).thenReturn(AnimalMother.DUENO.name());
            when(ownerEntity.getDocument()).thenReturn(AnimalMother.DUENO.document());
            when(companyEntity.getId()).thenReturn(AnimalMother.CLINICA.id());
            when(companyEntity.getName()).thenReturn(AnimalMother.CLINICA.name());
            when(companyEntity.getIdentifier()).thenReturn(AnimalMother.CLINICA.identifier());
            when(colorEntity.getId()).thenReturn(AnimalMother.NEGRO.id());
            when(colorEntity.getName()).thenReturn(AnimalMother.NEGRO.name());

            AnimalJpaEntity entity = entidadCompleta();
            entity.setSpecie(specieEntity);
            entity.setBreed(breedEntity);
            entity.setOwner(ownerEntity);
            entity.setCompany(companyEntity);
            entity.setColor(colorEntity);

            Animal animal = mapper.toDomain(entity);

            assertThat(animal.getSpecie()).isEqualTo(AnimalMother.PERRO);
            assertThat(animal.getBreed()).isEqualTo(AnimalMother.LABRADOR);
            assertThat(animal.getOwner()).isEqualTo(AnimalMother.DUENO);
            assertThat(animal.getCompany()).isEqualTo(AnimalMother.CLINICA);
            assertThat(animal.getColor()).isEqualTo(AnimalMother.NEGRO);
        }
    }
}
