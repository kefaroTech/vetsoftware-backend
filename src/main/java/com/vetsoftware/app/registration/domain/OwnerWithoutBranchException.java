package com.vetsoftware.app.registration.domain;

/**
 * El dueño de la empresa recien creada no quedo atado a ninguna sede, asi que
 * el alta se cancela entera.
 *
 * <p>
 * Es la tercera de la familia de {@link PlatformCatalogNotConfiguredException}
 * y {@link PlatformRoleCatalogNotConfiguredException}, y existe por el mismo
 * motivo palabra por palabra: <strong>el alta no puede devolver 201 entregando
 * una cuenta que falla tres pantallas despues</strong>. Aquellas cubren el
 * contrato y los roles; esta cubre la <em>sede</em>, que era el hueco que
 * quedaba.
 *
 * <p>
 * <b>El defecto que la justifica (#510).</b> El alta creaba la sede "Principal"
 * y creaba al dueño, y nadie escribia la fila de {@code employee_branches} que
 * las une. Las dos piezas por separado se leian perfectas —la sede existe, el
 * empleado existe, los roles estan— y el defecto vivia en la distancia entre
 * ellas. {@code Authz.currentBranchIds()} sale de {@code employee_branches} y
 * de ningun sitio mas, asi que para el dueño era el conjunto <em>vacio</em>: la
 * primera vez que intentaba invitar a un segundo empleado,
 * {@code requireAssignableBranches} rechazaba <b>la unica sede que existia</b>
 * con un <b>403 BRANCH_NOT_ALLOWED</b>. Un 403 que no menciona sedes rotas sino
 * permisos, de modo que quien lo investiga se va a {@code base_permissions} —
 * como paso en #506— y no encuentra nada mal, porque el problema estaba tres
 * pasos antes.
 *
 * <p>
 * <b>Por que 500 y no 503</b>, al reves que sus dos hermanas. Aquellas
 * denuncian un <em>despliegue incompleto</em>: faltan filas de catalogo, el
 * mensaje dice cuales sembrar y el siguiente registro volvera a fallar igual
 * hasta que un humano las siembre. Esta no: sus dos entradas —la sede y el
 * empleado— las acaba de crear el propio alta en esta misma transaccion. Si la
 * atadura no aparece, no falta ningun dato de plataforma que nadie pueda
 * sembrar; es el codigo del alta el que dejo de cuadrar consigo mismo. Mismo
 * criterio que los autochequeos de integridad de una cotizacion: no hay nada
 * que el cliente pueda reintentar y pide que un humano mire.
 *
 * <p>
 * GlobalExceptionHandler: <strong>500</strong>,
 * {@code REGISTRATION_OWNER_WITHOUT_BRANCH}.
 */
public class OwnerWithoutBranchException extends RuntimeException {

    private static final String QUE_PASO = """
            El dueño de la empresa '%s' no quedo asignado a ninguna sede, asi que el alta se \
            cancela entera (no queda ninguna empresa a medias). La sede "Principal" y el \
            empleado se acababan de crear en esta misma transaccion y la fila que los une en \
            employee_branches no aparece al releerla. \
            Sin esa fila Authz.currentBranchIds() devuelve el conjunto vacio para el dueño —es \
            su unica fuente— y la cuenta nace inservible de una forma que NO se ve al recibir \
            el 201: la empresa se lista, el dueño entra, y el primer POST /employees muere con \
            403 BRANCH_NOT_ALLOWED rechazando la unica sede que existe. \
            No es un catalogo sin sembrar: no hay nada que un operador pueda insertar para que \
            el siguiente registro funcione. Es un defecto del propio alta —revisa \
            SetEmployeeBranchesAdapter y el INSERT … SELECT de \
            EmployeeBranchJpaRepository.insert, que no produce ninguna fila, y sin error, si el \
            empleado o la sede no son de la empresa—. Razonado en la incidencia #510.""";

    public OwnerWithoutBranchException(String companyName) {
        super(QUE_PASO.formatted(companyName));
    }
}
