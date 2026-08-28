package com.vetsoftware.app.companyusageevent.domain;

/**
 * El eje de limite tal como lo necesita esta rodaja: su identificador y su
 * codigo, nada mas.
 *
 * <p>
 * Companion VO propio, no la entidad de dominio de {@code limitdimension}: el
 * {@code domain} de una feature nunca importa el {@code domain} de otra. Lo
 * resuelve {@code LimitDimensionQueryPort} en una sola consulta.
 *
 * <p>
 * <strong>Los dos campos viajan juntos porque la clave foranea es
 * compuesta.</strong> {@code fk_cue_dimension (limit_dimension_id,
 * limit_dimension_code) -> limit_dimensions(id, code)} copia el codigo del eje
 * a la fila del hecho y una clave compuesta impide que diverja —el mismo
 * mecanismo que {@code fk_company_capacities_dimension} (changeset 314)—. Traer
 * solo el {@code id} y escribir el codigo por nuestra cuenta seria inventarse
 * la mitad de una clave.
 */
public record LimitDimensionRef(Long id, String code) {

    public LimitDimensionRef {
        if (id == null) {
            throw new IllegalArgumentException("limit dimension id is required");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("limit dimension code is required");
        }
    }

    /**
     * La rama contable de este eje, o un fallo en voz alta si el eje es de
     * existencias. Ver {@link UsageBranch#ofDimensionCode(String)}.
     */
    public UsageBranch branch() {
        return UsageBranch.ofDimensionCode(code);
    }
}
