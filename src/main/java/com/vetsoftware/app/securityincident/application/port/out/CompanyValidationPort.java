package com.vetsoftware.app.securityincident.application.port.out;

/**
 * Solo comprueba que la clinica existe. No trae ni un campo suyo.
 *
 * <p>
 * Es el {@code YyyValidationPort} del CLAUDE.md y no un {@code YyyQueryPort}
 * porque esta rodaja no necesita <em>nada</em> de la empresa: la puente la
 * archiva por id y la respuesta no publica su nombre. Depender de la forma de
 * una entidad ajena para no usarla es como un cambio inocente alli rompe esto.
 *
 * <p>
 * Sin esto, dar de alta una clinica inexistente moriria mas tarde y como una
 * violacion de {@code fk_sic_company}, que es un error de integridad crudo en
 * vez del «esa empresa no existe» que corresponde.
 */
public interface CompanyValidationPort {

    boolean existsById(Long companyId);
}
