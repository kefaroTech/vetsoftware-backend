package com.vetsoftware.app.entitlement.infrastructure.persistence;

/**
 * Par (empresa, submodulo) concedido. Es la proyeccion que devuelve la variante
 * en lote de la consulta de permisos, para que quien resuelve varias empresas
 * de una vez --la consola de plataforma, la publicacion de permisos de admin--
 * no dispare una consulta por empresa.
 */
public interface CompanySubModuleGrantView {

    Long getCompanyId();

    Long getSubModuleId();
}
