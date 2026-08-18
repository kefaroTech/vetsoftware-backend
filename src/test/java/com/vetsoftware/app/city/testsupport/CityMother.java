package com.vetsoftware.app.city.testsupport;

import com.vetsoftware.app.city.application.command.CreateCityCommand;
import com.vetsoftware.app.city.application.command.UpdateCityCommand;
import com.vetsoftware.app.city.domain.City;
import com.vetsoftware.app.city.domain.StateRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo city.
 *
 * <p>
 * Las ciudades se construyen con el constructor publico y no con
 * {@code City.create(...)}: el factory pone {@code LocalDateTime.now()} y haria
 * no deterministas las aserciones sobre {@code createdDate}.
 */
public final class CityMother {

    public static final Long CITY_ID = 80L;
    public static final Long STATE_ID = 9L;
    public static final Long OTRO_STATE_ID = 99L;

    public static final StateRef ANTIOQUIA = new StateRef(STATE_ID, "Antioquia");
    public static final StateRef OTRO_ESTADO = new StateRef(OTRO_STATE_ID, "Cundinamarca");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private CityMother() {
    }

    /** Ciudad activa, habilitada. El caso por defecto. */
    public static City activa() {
        return activa(CITY_ID);
    }

    public static City activa(Long id) {
        return new City(id, "Medellin", ANTIOQUIA, "05001", CREADO, true);
    }

    public static City deshabilitada() {
        return new City(CITY_ID, "Medellin", ANTIOQUIA, "05001", CREADO, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateCityCommand comandoCrear() {
        return new CreateCityCommand("Medellin", STATE_ID, "05001");
    }

    /** Comando de actualizacion que mueve la ciudad a otro departamento. */
    public static UpdateCityCommand comandoActualizar() {
        return new UpdateCityCommand(CITY_ID, "Envigado", OTRO_STATE_ID, "05266");
    }
}
