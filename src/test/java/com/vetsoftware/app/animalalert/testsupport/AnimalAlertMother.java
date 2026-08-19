package com.vetsoftware.app.animalalert.testsupport;

import com.vetsoftware.app.animalalert.application.command.CreateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.command.UpdateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.query.ListAnimalAlertsByAnimalQuery;
import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.animalalert.domain.AnimalRef;
import com.vetsoftware.app.animalalert.domain.CompanyRef;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo animalalert.
 *
 * <p>
 * Se construye con el constructor publico y no con
 * {@code AnimalAlert.create(...)}: el factory pone {@code LocalDateTime.now()}
 * y haria no deterministas las aserciones sobre {@code createdDate}.
 */
public final class AnimalAlertMother {

    public static final Long ALERT_ID = 500L;
    public static final Long ANIMAL_ID = 100L;
    public static final Long COMPANY_ID = 9L;

    public static final AnimalRef FIRULAIS = new AnimalRef(ANIMAL_ID, "Firulais", "A-001");
    public static final CompanyRef CLINICA = new CompanyRef(COMPANY_ID, "Clinica Norte", "NIT-900");

    public static final AnimalRef OTRO_ANIMAL = new AnimalRef(200L, "Michi", "A-002");
    public static final CompanyRef OTRA_CLINICA = new CompanyRef(19L, "Clinica Sur", "NIT-901");

    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private AnimalAlertMother() {
    }

    /** Alerta de alergia, habilitada. El caso por defecto. */
    public static AnimalAlert alergia() {
        return alergia(ALERT_ID);
    }

    public static AnimalAlert alergia(Long id) {
        return new AnimalAlert(id, FIRULAIS, CLINICA, AlertType.ALLERGY, "Alergia a la penicilina",
                AlertSeverity.HIGH, CREADO, null, true);
    }

    public static AnimalAlert deshabilitada() {
        return new AnimalAlert(ALERT_ID, FIRULAIS, CLINICA, AlertType.ALLERGY,
                "Alergia a la penicilina", AlertSeverity.HIGH, CREADO, null, false);
    }

    /** Comando de creacion coherente con las refs de arriba. */
    public static CreateAnimalAlertCommand comandoCrear() {
        return new CreateAnimalAlertCommand(ANIMAL_ID, AlertType.ALLERGY, "Alergia a la penicilina",
                AlertSeverity.HIGH, COMPANY_ID);
    }

    /** Comando de actualizacion que cambia tipo, descripcion y severidad. */
    public static UpdateAnimalAlertCommand comandoActualizar() {
        return new UpdateAnimalAlertCommand(ALERT_ID, AlertType.BEHAVIOR, "Agresivo con extranos",
                AlertSeverity.MEDIUM, COMPANY_ID);
    }

    public static ListAnimalAlertsByAnimalQuery consultaPorAnimal() {
        return new ListAnimalAlertsByAnimalQuery(ANIMAL_ID, COMPANY_ID);
    }
}
