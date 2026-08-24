package com.vetsoftware.app.subscription.domain;

import java.util.EnumSet;
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
 * <strong>Por que no estan las otras dos.</strong> {@code TERMINAL} y
 * {@code STORAGE_GB} no las consume el alta, y exigirlas aqui le negaria el
 * registro a una plataforma que no venda terminales de caja. Lo que se compra
 * despues se contrata despues.
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
 * {@code is_core = TRUE}—, no que el codigo las invente. Un techo que no sale
 * de ninguna linea de contrato seria el unico numero del modelo sin origen
 * auditable, y el modelo ya tiene nombre y mecanismo para las concesiones que
 * no derivan de un contrato ({@code EntitlementSource.MANUAL_GRANT})
 * precisamente para que dejen constancia.
 */
public final class StructuralCapacityMinimum {

    private static final Set<CapacityUnit> UNITS = EnumSet.of(CapacityUnit.BRANCH,
            CapacityUnit.USER);

    private StructuralCapacityMinimum() {
    }

    /**
     * Las unidades del minimo que <strong>no</strong> cubre el conjunto recibido.
     * Vacio significa que el catalogo alcanza para firmar un contrato inicial
     * operable.
     */
    public static Set<CapacityUnit> missingFrom(Set<CapacityUnit> granted) {
        EnumSet<CapacityUnit> missing = EnumSet.copyOf(UNITS);
        if (granted != null) {
            missing.removeAll(granted);
        }
        return missing;
    }
}
