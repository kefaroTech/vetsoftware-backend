package com.vetsoftware.app.breed.testsupport;

import com.vetsoftware.app.breed.application.command.CreateBreedCommand;
import com.vetsoftware.app.breed.application.command.UpdateBreedCommand;
import com.vetsoftware.app.breed.domain.Breed;
import com.vetsoftware.app.breed.domain.SpecieRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo breed.
 *
 * <p>
 * Se construyen con el constructor publico y no con {@code Breed.create(...)}:
 * el factory pone {@code LocalDateTime.now()} y haria no deterministas las
 * aserciones sobre {@code createdDate}.
 */
public final class BreedMother {

    public static final Long BREED_ID = 100L;

    public static final SpecieRef PERRO = new SpecieRef(1L, "Perro");
    public static final SpecieRef GATO = new SpecieRef(2L, "Gato");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private BreedMother() {
    }

    /** Raza habilitada de la especie Perro. El caso por defecto. */
    public static Breed labrador() {
        return labrador(BREED_ID);
    }

    public static Breed labrador(Long id) {
        return new Breed(id, "Labrador", PERRO, CREADO, true);
    }

    public static Breed deshabilitado() {
        return new Breed(BREED_ID, "Labrador", PERRO, CREADO, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateBreedCommand comandoCrear() {
        return new CreateBreedCommand("Labrador", PERRO.id());
    }

    /** Comando de actualizacion que cambia nombre y especie. */
    public static UpdateBreedCommand comandoActualizar() {
        return new UpdateBreedCommand(BREED_ID, "Golden", GATO.id());
    }
}
