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
        return new ConsultationType(id, NOMBRE, DESCRIPCION, CREADO, true);
    }

    public static ConsultationType deshabilitada() {
        return new ConsultationType(ID, NOMBRE, DESCRIPCION, CREADO, false);
    }

    public static CreateConsultationTypeCommand comandoCrear() {
        return new CreateConsultationTypeCommand(NOMBRE, DESCRIPCION);
    }

    public static UpdateConsultationTypeCommand comandoActualizar() {
        return new UpdateConsultationTypeCommand(ID, "Consulta especializada",
                "Consulta con especialista de referencia");
    }
}
