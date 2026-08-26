package com.vetsoftware.app.consultationtype.testsupport;

import com.vetsoftware.app.consultationtype.application.command.CreateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.command.UpdateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo consultationtype.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code ConsultationType.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 *
 * <p>
 * <b>NO uses esta mother en un test que toque la base real.</b> Su
 * {@code NOMBRE} coincide —bajo la collation {@code utf8mb4_0900_ai_ci},
 * insensible a acentos y a caja— con una fila de la semilla de catálogo, y toda
 * fila que guarda un test en esta tabla es global ({@code company_id = NULL},
 * mismo {@code owner_scope} que la semilla): el INSERT chocaría contra el
 * índice único y el test fallaría al montarse, con un error que apunta a la
 * constraint y no al descuido. Para una rodaja de persistencia, construye el
 * fixture con un nombre propio con sufijo de prueba, como hace
 * {@code ConsultationTypePersistenceIT}. Aquí no molesta porque estos fixtures
 * solo alimentan dobles, que nunca llegan al motor.
 */
public final class ConsultationTypeMother {

    public static final Long ID = 1L;
    public static final String NOMBRE = "Consulta general";
    public static final String DESCRIPCION = "Consulta veterinaria general de rutina";
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private ConsultationTypeMother() {
    }

    /** Tipo de consulta habilitado. El caso por defecto. */
    public static ConsultationType consultaGeneral() {
        return consultaGeneral(ID);
    }

    public static ConsultationType consultaGeneral(Long id) {
        return new ConsultationType(id, NOMBRE, DESCRIPCION, CREADO, null, true);
    }

    public static ConsultationType deshabilitada() {
        return new ConsultationType(ID, NOMBRE, DESCRIPCION, CREADO, null, false);
    }

    /**
     * Tipo dado de baja con un id propio. La rama de reactivacion del alta necesita
     * afirmar sobre ESE id: es el que viaja a {@code reactivateWithDetails}.
     */
    public static ConsultationType deshabilitada(Long id) {
        return new ConsultationType(id, NOMBRE, DESCRIPCION, CREADO, null, false);
    }

    public static CreateConsultationTypeCommand comandoCrear() {
        return new CreateConsultationTypeCommand(NOMBRE, DESCRIPCION);
    }

    /**
     * Alta que reutiliza el nombre de arriba con una descripcion distinta. Es la
     * forma del alta que se topa con una fila dada de baja: mismo nombre —por eso
     * la encuentra— y descripcion nueva, que es lo unico que permite distinguir si
     * la reactivacion aplico los datos del comando o dejo los viejos.
     */
    public static CreateConsultationTypeCommand comandoCrearCon(String descripcion) {
        return new CreateConsultationTypeCommand(NOMBRE, descripcion);
    }

    public static UpdateConsultationTypeCommand comandoActualizar() {
        return new UpdateConsultationTypeCommand(ID, "Consulta especializada",
                "Consulta con especialista de referencia");
    }
}
