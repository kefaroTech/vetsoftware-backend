package com.vetsoftware.app.medicament.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.medicament.domain.Medicament;
import java.util.List;
import java.util.Optional;

public interface MedicamentRepository {
    Medicament save(Medicament medicament);

    Optional<Medicament> findById(Long id);

    /**
     * Solo el medicamento PROPIO de la empresa. Es el que valida las escrituras: un
     * general es de la plataforma y ningun tenant puede editarlo, borrarlo ni
     * reactivarlo. Se queda con el nombre canonico justamente porque es el que
     * cualquier {@code Update/Delete/Reactivate} debe usar.
     */
    Optional<Medicament> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Disponible para la empresa: los generales de la plataforma MAS los suyos. Es
     * la vista de lectura y de receta; no autoriza ninguna escritura.
     */
    Optional<Medicament> findAvailableByIdAndCompanyId(Long id, Long companyId);

    /**
     * Catalogo COMPLETO de la plataforma —globales y privados de todas las
     * empresas—: no filtra por empresa. Pagina porque de otro modo trae la tabla
     * entera; su uso esta restringido a ROLE_SYSTEM.
     *
     * @param q
     *            subcadena a buscar en el nombre, o {@code null} para no filtrar.
     *            Nulo devuelve exactamente lo que devolvia antes de existir la
     *            busqueda.
     */
    PageResult<Medicament> findAll(String q, int page, int pageSize);

    /**
     * El catalogo GLOBAL de la plataforma: solo las filas sin empresa. Es lo que
     * administra la consola, frente a {@link #findAll(String, int, int)}, que
     * devuelve ademas los privados de cada empresa para dar contexto. Pagina por el
     * mismo motivo que aquel.
     *
     * @param q
     *            subcadena a buscar en el nombre, o {@code null} para no filtrar.
     */
    PageResult<Medicament> findAllGlobal(String q, int page, int pageSize);

    List<Medicament> findAllAvailableForCompany(Long companyId);

    List<Medicament> findAllDisabledForCompany(Long companyId);

    /**
     * Los globales PAUSADOS, que el {@code @SQLRestriction} esconde del catalogo
     * activo. Va aparte de {@link #findAllDisabledForCompany(Long)} y no como un
     * parametro nulable porque el filtro tiene que ser {@code company_id IS NULL}:
     * {@code company_id = NULL} no casa nunca en SQL y el listado saldria siempre
     * vacio, dejando los globales pausados sin ninguna pantalla desde la que
     * reactivarlos.
     */
    List<Medicament> findAllDisabledGlobal();

    void delete(Long id);

    int reactivate(Long id, Long companyId);

    /**
     * Reactiva un global. Gemela de {@link #reactivate(Long, Long)} y separada por
     * la misma razon que el listado de arriba: acotar por «no tiene empresa» exige
     * {@code IS NULL}, y pasar {@code null} a la acotada afecta cero filas, que el
     * servicio traduce a un 404 sobre una fila que existe. Devuelve las filas
     * afectadas.
     */
    int reactivateGlobal(Long id);

    /**
     * La fila del MISMO ambito que ocupa ese nombre, incluidas las DESHABILITADAS.
     * El ambito es la empresa cuando llega {@code companyId} y el vademecum de
     * plataforma cuando llega {@code null}.
     *
     * <p>
     * Ve las deshabilitadas a proposito: el indice unico de la base solo cubre las
     * filas activas, asi que una pausada NO ocupa el nombre y la respuesta correcta
     * del alta es reactivarla, no insertar otra ni fallar. Sin este finder el alta
     * chocaba contra un nombre que la clinica no ve en su catalogo activo.
     *
     * <p>
     * La igualdad la decide la base con la collation de la columna
     * ({@code utf8mb4_0900_ai_ci}): insensible a acentos y a caja, el mismo
     * criterio del indice unico. Comparar en Java diria que «Amoxicilina» esta
     * libre frente a «amoxicilina» y la base lo rechazaria despues.
     */
    Optional<Medicament> findByNameAndCompanyIdIncludingDisabled(String name, Long companyId);

    /**
     * Cierto si otra fila ACTIVA del mismo ambito ya lleva ese nombre. Excluye la
     * fila que se esta editando. Solo mira las activas porque son las unicas que el
     * indice unico cuenta.
     */
    boolean existsActiveByNameAndCompanyIdExcludingId(String name, Long companyId, Long id);

    /**
     * Reactiva la fila pausada y le aplica el nombre y la descripcion de la
     * peticion: vuelve con lo que la usuaria acaba de escribir, no con lo que tenia
     * el dia que se pauso. Devuelve las filas afectadas.
     */
    int reactivateWithDetails(Long id, Long companyId, String name, String description);
}
