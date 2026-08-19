package com.vetsoftware.app.animal.testsupport;

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.command.UpdateAnimalCommand;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.domain.AnimalColorRef;
import com.vetsoftware.app.animal.domain.AnimalType;
import com.vetsoftware.app.animal.domain.BreedRef;
import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.OwnerRef;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.SpecieRef;
import com.vetsoftware.app.animal.domain.WeightType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo animal.
 *
 * <p>
 * Se construyen con el constructor publico y no con {@code Animal.create(...)}:
 * el factory pone {@code LocalDateTime.now()} y haria no deterministas las
 * aserciones sobre {@code createdDate}.
 */
public final class AnimalMother {

    public static final Long ANIMAL_ID = 100L;
    public static final Long COMPANY_ID = 9L;

    public static final SpecieRef PERRO = new SpecieRef(1L, "Perro");
    public static final BreedRef LABRADOR = new BreedRef(2L, "Labrador");
    public static final OwnerRef DUENO = new OwnerRef(3L, "Ana Ruiz", "CC-1020");
    public static final AnimalColorRef NEGRO = new AnimalColorRef(4L, "Negro");
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");

    public static final SpecieRef GATO = new SpecieRef(11L, "Gato");
    public static final BreedRef SIAMES = new BreedRef(12L, "Siames");
    public static final OwnerRef OTRO_DUENO = new OwnerRef(13L, "Luis Paz", "CC-2040");
    public static final AnimalColorRef BLANCO = new AnimalColorRef(14L, "Blanco");

    public static final LocalDate NACIMIENTO = LocalDate.of(2020, 5, 10);
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private AnimalMother() {
    }

    /** Perro vivo, habilitado, sin peso derivado. El caso por defecto. */
    public static Animal perroSano() {
        return perroSano(ANIMAL_ID);
    }

    public static Animal perroSano(Long id) {
        return new Animal(id, "Firulais", "A-001", PERRO, LABRADOR, DUENO, Gender.MALE,
                WeightType.KILOGRAMS, AnimalType.NONE, ReproductiveState.STERILIZED, NEGRO,
                NACIMIENTO, 30, false, null, CLINICA, CREADO, null, true);
    }

    public static Animal fallecido(LocalDate fecha) {
        return new Animal(ANIMAL_ID, "Firulais", "A-001", PERRO, LABRADOR, DUENO, Gender.MALE,
                WeightType.KILOGRAMS, AnimalType.NONE, ReproductiveState.STERILIZED, NEGRO,
                NACIMIENTO, 30, true, fecha, CLINICA, CREADO, null, true);
    }

    public static Animal deshabilitado() {
        return new Animal(ANIMAL_ID, "Firulais", "A-001", PERRO, LABRADOR, DUENO, Gender.MALE,
                WeightType.KILOGRAMS, AnimalType.NONE, ReproductiveState.STERILIZED, NEGRO,
                NACIMIENTO, 30, false, null, CLINICA, CREADO, null, false);
    }

    /** Perro con el peso actual ya hidratado por la capa de persistencia. */
    public static Animal conPesoDerivado(BigDecimal valor, WeightType unidad, LocalDate medidoEl) {
        Animal animal = perroSano();
        animal.applyCurrentWeight(valor, unidad, medidoEl);
        return animal;
    }

    /** Comando de creacion coherente con las refs de arriba. Sin peso inicial. */
    public static CreateAnimalCommand comandoCrear() {
        return comandoCrear(null);
    }

    public static CreateAnimalCommand comandoCrear(BigDecimal pesoInicial) {
        return new CreateAnimalCommand("Firulais", "A-001", PERRO.id(), LABRADOR.id(), DUENO.id(),
                Gender.MALE, WeightType.KILOGRAMS, AnimalType.NONE, ReproductiveState.STERILIZED,
                NEGRO.id(), NACIMIENTO, pesoInicial, 30, false, null, COMPANY_ID);
    }

    /** Comando de actualizacion que cambia todas las referencias y el nombre. */
    public static UpdateAnimalCommand comandoActualizar() {
        return new UpdateAnimalCommand(ANIMAL_ID, "Michi", "A-002", GATO.id(), SIAMES.id(),
                OTRO_DUENO.id(), Gender.FEMALE, WeightType.GRAMS, AnimalType.SUPPORT,
                ReproductiveState.NO_STERILIZED, BLANCO.id(), LocalDate.of(2021, 3, 1), null, 12,
                false, null, COMPANY_ID);
    }
}
