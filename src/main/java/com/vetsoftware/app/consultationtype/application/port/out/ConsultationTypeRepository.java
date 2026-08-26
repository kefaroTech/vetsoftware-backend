package com.vetsoftware.app.consultationtype.application.port.out;

import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import java.util.List;
import java.util.Optional;

public interface ConsultationTypeRepository {
    ConsultationType save(ConsultationType consultationType);

    Optional<ConsultationType> findById(Long id);

    List<ConsultationType> findAll();

    void delete(Long id);

    int reactivate(Long id);

    /**
     * La fila que ocupa ese nombre, incluidas las DESHABILITADAS. Es un catálogo
     * global: el ámbito es la tabla entera.
     *
     * <p>
     * Ve las deshabilitadas a propósito: el índice único de la base solo cubre las
     * activas, así que una fila dada de baja NO ocupa el nombre y la respuesta
     * correcta del alta es reactivarla. Sin este finder el alta chocaba contra un
     * nombre que el administrador no ve en el listado.
     *
     * <p>
     * La igualdad la decide la base con la collation de la columna
     * ({@code utf8mb4_0900_ai_ci}): insensible a acentos y a caja, el mismo
     * criterio del índice único.
     */
    Optional<ConsultationType> findByNameIncludingDisabled(String name);

    /**
     * ¿Hay otra fila ACTIVA con ese nombre? Excluye la que se está editando.
     */
    boolean existsActiveByNameExcludingId(String name, Long id);

    /**
     * Reactiva la fila deshabilitada y le aplica el nombre y la descripción de la
     * petición. Devuelve las filas afectadas.
     */
    int reactivateWithDetails(Long id, String name, String description);
}
