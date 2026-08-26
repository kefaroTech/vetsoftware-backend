package com.vetsoftware.app.vaccinationtype.application.port.out;

import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import java.util.List;
import java.util.Optional;

public interface VaccinationTypeRepository {
    VaccinationType save(VaccinationType vaccinationType);

    Optional<VaccinationType> findById(Long id);

    /** Lectura: la fila propia de la empresa o cualquiera de las generales. */
    Optional<VaccinationType> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Escritura: SOLO la fila propia de la empresa. Las generales quedan fuera a
     * propósito — editarlas, borrarlas o reactivarlas las cambiaría para todos los
     * tenants.
     */
    Optional<VaccinationType> findOwnedByIdAndCompanyId(Long id, Long companyId);

    List<VaccinationType> findAll();

    List<VaccinationType> findAllAvailableForCompany(Long companyId);

    void delete(Long id);

    /**
     * La fila del MISMO ámbito que ocupa ese nombre, incluidas las DESHABILITADAS.
     * El ámbito es la empresa cuando llega {@code companyId} y el catálogo de
     * plataforma cuando llega {@code null}.
     *
     * <p>
     * Ve las deshabilitadas a propósito: el índice único de la base solo cubre las
     * filas activas, así que una fila dada de baja NO ocupa el nombre y la
     * respuesta correcta del alta es reactivarla, no insertar otra ni fallar. Sin
     * este finder el alta chocaba contra un nombre que la usuaria no ve en el
     * listado, y el 409 hablaba de un conflicto con algo que para ella no existe.
     *
     * <p>
     * La igualdad la decide la base con la collation de la columna
     * ({@code utf8mb4_0900_ai_ci}): insensible a acentos y a caja, el mismo
     * criterio del índice único. Comparar en Java diría que «Antirrabica» está
     * libre y la base lo rechazaría después.
     */
    Optional<VaccinationType> findByNameAndCompanyIdIncludingDisabled(String name, Long companyId);

    /**
     * ¿Hay otra fila ACTIVA del mismo ámbito con ese nombre? Excluye la fila que se
     * está editando, que evidentemente ya lo lleva. Solo mira las activas porque
     * son las únicas que el índice único cuenta.
     */
    boolean existsActiveByNameAndCompanyIdExcludingId(String name, Long companyId, Long id);

    /**
     * Reactiva la fila deshabilitada y le aplica el nombre y la descripción de la
     * petición: la fila vuelve con lo que la usuaria acaba de escribir, no con lo
     * que tenía el día que se dio de baja. Devuelve las filas afectadas.
     */
    int reactivateWithDetails(Long id, Long companyId, String name, String description);
}
