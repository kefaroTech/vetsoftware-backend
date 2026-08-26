package com.vetsoftware.app.spatype.testsupport;

import com.vetsoftware.app.spatype.application.command.CreateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.command.UpdateSpaTypeCommand;
import com.vetsoftware.app.spatype.domain.SpaType;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo spatype.
 *
 * <p>
 * Se construyen con el constructor publico y no con
 * {@code SpaType.create(...)}: ese factory llama a {@code LocalDateTime.now()}
 * y haria no deterministas las aserciones sobre {@code createdDate}. Deuda ya
 * registrada del repo —igual que en {@code Animal.create}—, no excusa para
 * propagarla.
 *
 * <p>
 * {@code spa_types} es catalogo global de plataforma: no hay {@code CompanyRef}
 * ni id de empresa que sembrar, a diferencia de las mothers de los catalogos
 * por empresa.
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
 * {@code SpaTypePersistenceIT}. Aquí no molesta porque estos fixtures solo
 * alimentan dobles, que nunca llegan al motor.
 */
public final class SpaTypeMother {

    public static final Long ID = 1L;
    public static final String NOMBRE = "Bano medicado";
    public static final String DESCRIPCION = "Bano con champu medicado";
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 8, 23, 10, 0);

    private SpaTypeMother() {
    }

    /** Tipo de spa habilitado. El caso por defecto. */
    public static SpaType banoMedicado() {
        return banoMedicado(ID);
    }

    public static SpaType banoMedicado(Long id) {
        return new SpaType(id, NOMBRE, DESCRIPCION, CREADO, null, true);
    }

    public static SpaType deshabilitado() {
        return deshabilitado(ID);
    }

    /**
     * Tipo dado de baja con un id propio. La rama de reactivacion del alta necesita
     * afirmar sobre ESE id: es el que viaja a {@code reactivateWithDetails}.
     */
    public static SpaType deshabilitado(Long id) {
        return new SpaType(id, NOMBRE, DESCRIPCION, CREADO, null, false);
    }

    public static CreateSpaTypeCommand comandoCrear() {
        return new CreateSpaTypeCommand(NOMBRE, DESCRIPCION);
    }

    /**
     * Alta que reutiliza el nombre de arriba con una descripcion distinta. Es la
     * forma del alta que se topa con una fila dada de baja: mismo nombre —por eso
     * la encuentra— y descripcion nueva, que es lo unico que permite distinguir si
     * la reactivacion aplico los datos del comando o dejo los viejos.
     */
    public static CreateSpaTypeCommand comandoCrearCon(String descripcion) {
        return new CreateSpaTypeCommand(NOMBRE, descripcion);
    }

    public static UpdateSpaTypeCommand comandoActualizar() {
        return new UpdateSpaTypeCommand(ID, "Corte de pelo", "Corte y peinado");
    }
}
