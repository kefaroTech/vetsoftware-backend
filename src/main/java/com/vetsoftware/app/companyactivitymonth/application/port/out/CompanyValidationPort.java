package com.vetsoftware.app.companyactivitymonth.application.port.out;

/**
 * Comprueba que la clinica existe antes de escribirle un mes de actividad.
 *
 * <p>
 * <strong>Es un {@code ValidationPort} y no un {@code QueryPort} porque esta
 * feature no necesita ni un solo dato de la empresa</strong> —ni el nombre, ni
 * el identificador—: la fila guarda el {@code company_id} escalar y nada mas.
 * Traer un companion VO obligaria a mantener aqui una copia del agregado ajeno
 * para no usarla.
 *
 * <p>
 * Sin esta comprobacion, {@code fk_cam_company} rechazaria la fila igual, pero
 * como error de integridad en vez de como el «esa empresa no existe» que
 * corresponde.
 */
public interface CompanyValidationPort {

    boolean existsById(Long companyId);
}
