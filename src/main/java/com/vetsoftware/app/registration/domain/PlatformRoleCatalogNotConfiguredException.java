package com.vetsoftware.app.registration.domain;

import java.util.List;

/**
 * La plataforma no tiene catalogo de roles base con el que dotar de
 * administrador a una empresa nueva, asi que el alta se cancela entera.
 *
 * <p>
 * Es la gemela de {@link PlatformCatalogNotConfiguredException} en la dimension
 * de los <strong>permisos</strong>, y existe por el mismo motivo palabra por
 * palabra: una empresa cuyo dueño nace sin un solo rol entra al sistema y no
 * puede hacer nada, sin ningun mensaje que lo explique; se investiga como un
 * problema de permisos del usuario —y aqui lo es literalmente— y hay que
 * borrarla a mano de la base. El catalogo comercial tenia guarda desde #364; el
 * de roles no la tuvo hasta #500, y por eso el alta devolvia
 * <strong>201</strong> entregando una cuenta inservible.
 *
 * <p>
 * Los dos mensajes son distintos porque son dos fallos distintos y se arreglan
 * en sitios distintos —misma razon por la que
 * {@code PlatformCatalogNotConfiguredForSubscriptionException} tiene dos
 * constructores—: sin ninguna fila hay que sembrar la tabla, y con filas pero
 * sin ninguna obligatoria hay que corregir la que ya existe.
 *
 * <p>
 * GlobalExceptionHandler: <strong>503</strong>,
 * {@code PLATFORM_ROLE_CATALOG_NOT_CONFIGURED}.
 */
public class PlatformRoleCatalogNotConfiguredException extends RuntimeException {

    private static final String TABLA_VACIA = """
            La plataforma no tiene ningun rol base configurado, asi que el dueño de la empresa \
            '%s' nacería sin un solo rol y el alta se cancela entera (no queda ninguna empresa a \
            medias). Falta el minimo estructural, en este orden: \
            (1) una fila en base_roles con code='ADMIN', mandatory=true y enabled=true, que es la \
            unica que el alta se auto-asigna al dueño; \
            (2) sus base_role_permissions: los seeds que atan permisos al rol ADMIN son \
            INSERT ... SELECT ... FROM base_roles br ... WHERE br.code = 'ADMIN', asi que con \
            base_roles vacia insertaron cero filas sin error y NO se reejecutan solos —ya estan \
            marcados en DATABASECHANGELOG—: hace falta un changeset de backfill posterior. \
            Sin (1) el dueño no recibe ningun rol; sin (2) recibe un rol sin un solo permiso, que \
            desde la pantalla se ve igual de roto. \
            Siembra el catalogo de roles (changesets 266 y 267) y reintenta el alta.""";

    private static final String SIN_OBLIGATORIOS = """
            La plataforma tiene roles base configurados %s pero ninguno con mandatory=true, asi \
            que el alta de la empresa '%s' crearia las plantillas de rol y no le asignaria \
            ninguna al dueño: quedaria una cuenta sin administrador. El alta se cancela entera \
            (no queda ninguna empresa a medias). \
            Falta marcar como obligatorio el rol que hereda el dueño: \
            UPDATE base_roles SET mandatory = TRUE WHERE code = 'ADMIN'. \
            Es un fallo distinto al de la tabla vacia y se arregla en otro sitio: alli falta el \
            INSERT, aqui la fila ya existe y lo que falta es la marca.""";

    /** No hay ninguna fila en {@code base_roles}. */
    public PlatformRoleCatalogNotConfiguredException(String companyName) {
        super(TABLA_VACIA.formatted(companyName));
    }

    /**
     * Hay roles base, pero ninguno obligatorio: el dueño se quedaria sin ninguno.
     *
     * @param companyName
     *            la empresa cuya alta se cancela
     * @param baseRoleCodes
     *            los codigos que si existen, para que quien lea el error vea que la
     *            tabla no esta vacia y no vuelva a sembrarla
     */
    public PlatformRoleCatalogNotConfiguredException(String companyName,
            List<String> baseRoleCodes) {
        super(SIN_OBLIGATORIOS.formatted(baseRoleCodes, companyName));
    }
}
