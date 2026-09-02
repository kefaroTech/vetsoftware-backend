package com.vetsoftware.app.subscription.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Las unidades de capacidad sin las cuales una empresa recien nacida no puede
 * existir, y que por tanto <strong>tiene que otorgar su contrato
 * inicial</strong>.
 *
 * <p>
 * <strong>Por que son exactamente estas dos.</strong> El alta de una empresa
 * crea, en su propia transaccion, la sede «Principal» —invariante del modelo:
 * toda empresa tiene al menos una sede— y el empleado dueño. Las dos cosas
 * consumen capacidad ({@code BRANCH} y {@code USER}), asi que un contrato
 * inicial que no las conceda produce una empresa que no puede terminar de
 * nacer. No es una preferencia comercial: es la definicion de inquilino
 * existente.
 *
 * <p>
 * <strong>Por que aqui SI hay una lista escrita a mano, y en el resto del slice
 * ya no.</strong> El changeset 333 saco la lista cerrada de unidades del
 * catalogo comercial: {@code capacity_unit} es hoy el codigo del eje y admite
 * los ocho que {@code limit_dimensions} tenga sembrados, porque vender un eje
 * nuevo tiene que ser insertar una fila (#655). Esta lista es otra cosa y por
 * eso no se movio: no dice <em>que se puede vender</em> —eso lo decide el
 * catalogo— sino <em>que consume el propio alta de una empresa</em>, que es un
 * hecho del codigo de {@code company} y no un dato de configuracion. El dia que
 * el alta cree algo mas, la linea que hay que cambiar es la de aqui, y tiene
 * que ser visible en el diff.
 *
 * <p>
 * <strong>Por que no estan los demas ejes.</strong> {@code TERMINAL},
 * {@code STORAGE_GB}, {@code ANIMAL}, {@code OWNER}, {@code APPOINTMENT} e
 * {@code INVOICE} no los consume el alta, y exigirlos aqui le negaria el
 * registro a una plataforma que no venda terminales de caja —o que no quiera
 * regalar mascotas—. Lo que se compra despues se contrata despues.
 *
 * <p>
 * Este es el sitio donde vive la regla de producto «no puede existir jamas un
 * estado de corte total de acceso» en la dimension de las <em>cantidades</em>.
 * Su gemela en la dimension de las <em>pantallas</em> es
 * {@code ContractStatus.maxAccessLevel()}, cuyo minimo es {@code READ_ONLY}: ni
 * la cancelacion ni la mora bajan de ahi. La capacidad no tenia suelo ninguno,
 * y esa asimetria es la que dejaba nacer empresas con cero sedes y cero
 * usuarios.
 *
 * <p>
 * <strong>El suelo se contrata, no se regala.</strong> Lo que esta clase
 * declara es que estas unidades han de venir del catalogo —de un
 * {@code catalog_items} con {@code item_type = 'CAPACITY'} e
 * {@code structural_minimum = TRUE}—, no que el codigo las invente. Un techo
 * que no sale de ninguna linea de contrato seria el unico numero del modelo sin
 * origen auditable, y el modelo ya tiene nombre y mecanismo para las
 * concesiones que no derivan de un contrato
 * ({@code EntitlementSource.MANUAL_GRANT}) precisamente para que dejen
 * constancia.
 */
public final class StructuralCapacityMinimum {

    /**
     * Los codigos de eje del minimo, en el orden en que se enumeran al fallar.
     * Lista y no conjunto: el orden es parte del mensaje de error y un
     * {@code Set.of} no lo garantiza.
     */
    private static final List<String> UNITS = List.of("BRANCH", "USER");

    private StructuralCapacityMinimum() {
    }

    /**
     * Las unidades del minimo que <strong>no</strong> cubre el conjunto recibido.
     * Vacio significa que el catalogo alcanza para firmar un contrato inicial
     * operable.
     *
     * <p>
     * La comparacion es exacta y no ignora mayusculas, igual que la colacion
     * {@code ascii_bin} de {@code limit_dimensions.code} que el 332 dejo puesta: un
     * catalogo sembrado con {@code 'user'} en minusculas no cubre el minimo, y
     * decir lo contrario aqui solo aplazaria el fallo hasta la clave foranea.
     */
    public static Set<String> missingFrom(Set<String> granted) {
        Set<String> missing = new LinkedHashSet<>(UNITS);
        if (granted != null) {
            missing.removeAll(granted);
        }
        return missing;
    }
}
