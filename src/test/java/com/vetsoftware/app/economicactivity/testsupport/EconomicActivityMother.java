package com.vetsoftware.app.economicactivity.testsupport;

import com.vetsoftware.app.economicactivity.application.command.CreateEconomicActivityCommand;
import com.vetsoftware.app.economicactivity.application.command.UpdateEconomicActivityCommand;
import com.vetsoftware.app.economicactivity.domain.EconomicActivity;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo economicactivity: catalogo plano sin jerarquia ni FK
 * cruzada, a diferencia de otras features.
 *
 * <p>
 * Se construye con el constructor publico y no con
 * {@code EconomicActivity.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class EconomicActivityMother {

    public static final Long ECONOMIC_ACTIVITY_ID = 70L;
    public static final String CODIGO = "0111";
    public static final String NOMBRE = "Cultivo de cereales";
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private EconomicActivityMother() {
    }

    /** Actividad nueva (sin id), habilitada. El caso por defecto para insertar. */
    public static EconomicActivity nueva() {
        return nueva(CODIGO, NOMBRE);
    }

    public static EconomicActivity nueva(String code, String name) {
        return new EconomicActivity(null, code, name, CREADO, null, true);
    }

    /** Actividad ya persistida, habilitada. */
    public static EconomicActivity existente() {
        return existente(ECONOMIC_ACTIVITY_ID);
    }

    public static EconomicActivity existente(Long id) {
        return new EconomicActivity(id, CODIGO, NOMBRE, CREADO, null, true);
    }

    public static EconomicActivity deshabilitada() {
        return new EconomicActivity(ECONOMIC_ACTIVITY_ID, CODIGO, NOMBRE, CREADO, null, false);
    }

    public static CreateEconomicActivityCommand comandoCrear() {
        return new CreateEconomicActivityCommand(CODIGO, NOMBRE);
    }

    public static UpdateEconomicActivityCommand comandoActualizar() {
        return comandoActualizar(ECONOMIC_ACTIVITY_ID);
    }

    public static UpdateEconomicActivityCommand comandoActualizar(Long id) {
        return new UpdateEconomicActivityCommand(id, "0112", "Cultivo de hortalizas");
    }
}
