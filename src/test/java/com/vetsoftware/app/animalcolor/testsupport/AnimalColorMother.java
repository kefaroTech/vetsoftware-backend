package com.vetsoftware.app.animalcolor.testsupport;

import com.vetsoftware.app.animalcolor.application.command.CreateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.command.UpdateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.domain.AnimalColor;
import com.vetsoftware.app.animalcolor.domain.SpecieRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo animalcolor.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code AnimalColor.create(...)}: el factory pone {@code LocalDateTime.now()}
 * y haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class AnimalColorMother {

    public static final Long ANIMAL_COLOR_ID = 100L;

    public static final SpecieRef PERRO = new SpecieRef(1L, "Perro");
    public static final SpecieRef GATO = new SpecieRef(2L, "Gato");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private AnimalColorMother() {
    }

    /** Color habilitado de la especie Perro. El caso por defecto. */
    public static AnimalColor negro() {
        return negro(ANIMAL_COLOR_ID);
    }

    public static AnimalColor negro(Long id) {
        return new AnimalColor(id, "Negro", PERRO, CREADO, true);
    }

    public static AnimalColor deshabilitado() {
        return new AnimalColor(ANIMAL_COLOR_ID, "Negro", PERRO, CREADO, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateAnimalColorCommand comandoCrear() {
        return new CreateAnimalColorCommand("Negro", PERRO.id());
    }

    /** Comando de actualizacion que cambia nombre y especie. */
    public static UpdateAnimalColorCommand comandoActualizar() {
        return new UpdateAnimalColorCommand(ANIMAL_COLOR_ID, "Blanco", GATO.id());
    }
}
