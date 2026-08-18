package com.vetsoftware.app.specie.testsupport;

import com.vetsoftware.app.specie.application.command.CreateSpecieCommand;
import com.vetsoftware.app.specie.application.command.UpdateSpecieCommand;
import com.vetsoftware.app.specie.domain.Specie;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo specie.
 *
 * <p>
 * Se construyen con el constructor publico y no con {@code Specie.create(...)}:
 * el factory pone {@code LocalDateTime.now()} y haria no deterministas las
 * aserciones sobre {@code createdDate}.
 */
public final class SpecieMother {

    public static final Long SPECIE_ID = 100L;

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private SpecieMother() {
    }

    /** Especie habilitada. El caso por defecto. */
    public static Specie perro() {
        return perro(SPECIE_ID);
    }

    public static Specie perro(Long id) {
        return new Specie(id, "Perro", CREADO, true);
    }

    public static Specie deshabilitada() {
        return new Specie(SPECIE_ID, "Perro", CREADO, false);
    }

    /** Comando de creacion coherente con las fixtures de arriba. */
    public static CreateSpecieCommand comandoCrear() {
        return new CreateSpecieCommand("Perro");
    }

    /** Comando de actualizacion que cambia el nombre. */
    public static UpdateSpecieCommand comandoActualizar() {
        return new UpdateSpecieCommand(SPECIE_ID, "Gato");
    }
}
