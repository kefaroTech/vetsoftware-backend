package com.vetsoftware.app.aiproposal.testsupport;

import com.vetsoftware.app.aiproposal.domain.PackOffer;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.domain.SellableItem;
import com.vetsoftware.app.aiproposal.domain.SellableItemKind;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El catalogo comercial <b>real</b>, el de las semillas 308 / 309 / 310 / 380.
 *
 * <p>
 * &#9940; <b>No es el mismo que {@link SellableCatalogMother}, y la diferencia
 * importa.</b> Aquel es un catalogo de laboratorio: seis modulos, codigos
 * abreviados ({@code VACCINATION} en vez de {@code VACCINATION_DEWORMING}) y
 * una cadena de dependencias inventada -{@code CLINICAL_HISTORY} exige
 * {@code SCHEDULING}- para tener tres saltos que recorrer. Sirve para probar el
 * motor; <b>no sirve para afirmar que propuesta recibe una clinica</b>, porque
 * en el catalogo real ese arco es un {@code RECOMMENDS} y el motor tiene
 * prohibido seguirlo. Un golden set escrito contra el catalogo de laboratorio
 * fijaria una propuesta que ningun prospecto va a recibir nunca.
 *
 * <p>
 * <b>Que se copia de las semillas, y de cual:</b>
 *
 * <ul>
 * <li><b>308</b>: los codigos, cuales son modulos y cuales capacidades, quien
 * es {@code is_core}, quien se puede contratar por autoservicio y los dias de
 * prueba (30 en los modulos clinicos; 14 en los cuatro de la caja y el
 * inventario; cero en {@code ELECTRONIC_INVOICING}, que es
 * {@code NEVER_FREE}).</li>
 * <li><b>309</b>: los nueve arcos {@code REQUIRES} y los componentes de los
 * tres paquetes. Los cuatro {@code RECOMMENDS} <b>no</b> se copian: el arco no
 * existe en {@link SellableCatalog} a proposito, para que el cierre no pueda
 * seguirlo ni el dia que alguien toque el bucle.</li>
 * <li><b>310</b>: los importes mensuales del tarifario 2026 y el IVA del
 * 19&nbsp;%.</li>
 * <li><b>380</b>: el decimo arco, {@code GROOMING REQUIRES SERVICES}, que es
 * una de las dos reglas de negocio que el golden set fija.</li>
 * </ul>
 *
 * <p>
 * <b>Los importes de los {@code EXTRA_*} salen de una escalera por tramos</b>
 * ({@code EXTRA_USER} vale 12.000 hasta el octavo y 9.000 a partir del noveno):
 * aqui se toma el primer tramo, y nada lo afirma —esos articulos entran al
 * golden set solo por su veredicto {@code NOT_SELF_SERVICE}—.
 */
public final class CatalogoComercial2026 {

    public static final String COP = "COP";

    private static final BigDecimal IVA = new BigDecimal("19.00");

    /** Los dias de prueba de los modulos clinicos y de estetica. */
    private static final int PRUEBA_LARGA = 30;

    /** Los de la caja, el inventario, las compras y las cuentas por cobrar. */
    private static final int PRUEBA_CORTA = 14;

    private static final int SIN_PRUEBA = 0;

    private CatalogoComercial2026() {
    }

    /**
     * La foto completa: catorce modulos vendibles a mano, las capacidades y los
     * tres paquetes.
     */
    public static SellableCatalog catalogo() {
        Map<String, SellableItem> items = new LinkedHashMap<>();
        anadir(items, nucleo());
        anadir(items,
                modulo("SCHEDULING", "Agenda de citas",
                        "Reserva de horas, agenda por profesional y confirmacion al cliente",
                        35_000, PRUEBA_LARGA));
        anadir(items, modulo("CLINICAL_HISTORY", "Historia clinica y consultas",
                "Expediente medico del paciente, diagnosticos y formulas", 49_000, PRUEBA_LARGA));
        anadir(items, modulo("VACCINATION_DEWORMING", "Vacunacion y desparasitacion",
                "Carne de vacunas, lotes y avisos de refuerzo", 25_000, PRUEBA_LARGA));
        anadir(items,
                modulo("HOSPITALIZATION", "Hospitalizacion",
                        "Evolucion por turnos, medicacion horaria y notas de enfermeria", 39_000,
                        PRUEBA_LARGA));
        anadir(items, modulo("SURGERY", "Cirugia",
                "Registro del procedimiento, anestesia y desenlace", 29_000, PRUEBA_LARGA));
        anadir(items,
                modulo("LAB_IMAGING", "Laboratorio e imagen",
                        "Resultados de examenes e imagenes dentro del expediente del paciente",
                        45_000, PRUEBA_LARGA));
        anadir(items, modulo("GROOMING", "Estetica, bano y guarderia",
                "Bano, peluqueria, guarderia y hotel para mascotas sanas", 29_000, PRUEBA_LARGA));
        anadir(items,
                modulo("SERVICES", "Servicios y tarifas",
                        "Lista de servicios con tarifas por tamano o raza, paquetes y promociones",
                        29_000, PRUEBA_LARGA));
        anadir(items, modulo("CASH_REGISTER", "Caja y punto de venta",
                "Registro de la venta, medios de pago y cierre de caja", 46_000, PRUEBA_CORTA));
        anadir(items,
                modulo("INVENTORY", "Inventario",
                        "Existencias, vencimientos y despacho de producto en mostrador", 39_000,
                        PRUEBA_CORTA));
        anadir(items,
                modulo("PURCHASES", "Compras a proveedores",
                        "Ordenes de compra, recepcion de mercancia y factura del proveedor", 29_000,
                        PRUEBA_CORTA));
        anadir(items,
                modulo("OPEN_ACCOUNTS", "Cuentas abiertas y cartera",
                        "Consumos que se cobran despues, credito y cartera por cliente", 25_000,
                        PRUEBA_CORTA));
        anadir(items, modulo("ELECTRONIC_INVOICING", "Facturacion electronica",
                "Factura de venta ante la DIAN", 59_000, SIN_PRUEBA));
        anadir(items, capacidadDelTerminal());
        anadir(items,
                extra("EXTRA_USER", "Usuario adicional", "Una persona mas en la cuenta", 12_000));
        anadir(items,
                extra("EXTRA_TERMINAL", "Terminal adicional", "Un punto de venta mas", 18_000));
        anadir(items, extra("EXTRA_STORAGE", "Almacenamiento adicional",
                "Un gigabyte mas de archivos clinicos", 1_200));

        return new SellableCatalog(items, requiere(), List.of(packSpa(), packClinic(), packFull()));
    }

    /**
     * Los diez arcos {@code REQUIRES}: los nueve del changeset 309 mas
     * {@code GROOMING &rarr; SERVICES} del 380.
     *
     * <p>
     * <b>Ni uno de los cuatro {@code RECOMMENDS} esta aqui</b>
     * ({@code INVENTORY &rarr; CASH_REGISTER},
     * {@code CLINICAL_HISTORY &rarr; SCHEDULING},
     * {@code VACCINATION_DEWORMING &rarr; SCHEDULING},
     * {@code GROOMING &rarr; SCHEDULING}): auto-anadirlos seria un upsell
     * disfrazado de requisito tecnico, y varios casos del golden set existen
     * justamente para fijar que no entran.
     */
    private static Map<String, List<String>> requiere() {
        Map<String, List<String>> arcos = new LinkedHashMap<>();
        arcos.put("ELECTRONIC_INVOICING", List.of("CASH_REGISTER"));
        arcos.put("CAPACITY_TERMINAL", List.of("CASH_REGISTER"));
        arcos.put("EXTRA_TERMINAL", List.of("CASH_REGISTER"));
        arcos.put("OPEN_ACCOUNTS", List.of("CASH_REGISTER"));
        arcos.put("PURCHASES", List.of("INVENTORY"));
        arcos.put("HOSPITALIZATION", List.of("CLINICAL_HISTORY"));
        arcos.put("SURGERY", List.of("CLINICAL_HISTORY"));
        arcos.put("LAB_IMAGING", List.of("CLINICAL_HISTORY"));
        arcos.put("EXTRA_STORAGE", List.of("LAB_IMAGING"));
        arcos.put("GROOMING", List.of("SERVICES"));
        return arcos;
    }

    public static SellableItem nucleo() {
        return new SellableItem("CORE", "Nucleo: clientes, mascotas y cuenta",
                "Duenos y mascotas, sedes, empleados, roles y suscripcion", SellableItemKind.MODULE,
                true, true, true, PRUEBA_LARGA, new BigDecimal("69000.00"), IVA, COP);
    }

    /**
     * {@code CAPACITY_TERMINAL} cuelga de los tres paquetes, asi que si se puede
     * contratar por autoservicio, y vale cero: lo que hace es levantar el techo de
     * terminales, sin el cual {@code CASH_REGISTER} no puede abrir ni la primera
     * caja.
     */
    public static SellableItem capacidadDelTerminal() {
        return new SellableItem("CAPACITY_TERMINAL", "Terminal de caja incluida",
                "Un punto de venta habilitado", SellableItemKind.CAPACITY, false, true, true,
                PRUEBA_CORTA, new BigDecimal("0.00"), IVA, COP);
    }

    /**
     * Los {@code EXTRA_*} no cuelgan de ningun paquete, asi que el catalogo
     * publicado no los da por contratables a mano: el motor los rechaza con
     * {@code NOT_SELF_SERVICE}.
     */
    private static SellableItem extra(String code, String nombre, String descripcion, int precio) {
        return new SellableItem(code, nombre, descripcion, SellableItemKind.CAPACITY, false, true,
                false, SIN_PRUEBA, new BigDecimal(precio + ".00"), IVA, COP);
    }

    private static SellableItem modulo(String code, String nombre, String descripcion, int precio,
            int diasDePrueba) {
        return new SellableItem(code, nombre, descripcion, SellableItemKind.MODULE, false, true,
                true, diasDePrueba, new BigDecimal(precio + ".00"), IVA, COP);
    }

    public static PackOffer packSpa() {
        return new PackOffer("PACK_SPA", "Pack Spa", new BigDecimal("179000.00"), IVA, SIN_PRUEBA,
                Set.of("CORE", "SCHEDULING", "SERVICES", "GROOMING", "CASH_REGISTER",
                        "CAPACITY_TERMINAL"));
    }

    public static PackOffer packClinic() {
        return new PackOffer("PACK_CLINIC", "Pack Clinica", new BigDecimal("189000.00"), IVA,
                SIN_PRUEBA, Set.of("CORE", "SCHEDULING", "CLINICAL_HISTORY",
                        "VACCINATION_DEWORMING", "CASH_REGISTER", "CAPACITY_TERMINAL"));
    }

    public static PackOffer packFull() {
        return new PackOffer("PACK_FULL", "Pack Completo", new BigDecimal("449000.00"), IVA,
                SIN_PRUEBA,
                Set.of("CORE", "SCHEDULING", "CLINICAL_HISTORY", "VACCINATION_DEWORMING",
                        "HOSPITALIZATION", "SURGERY", "LAB_IMAGING", "GROOMING", "SERVICES",
                        "CASH_REGISTER", "CAPACITY_TERMINAL", "INVENTORY", "PURCHASES",
                        "OPEN_ACCOUNTS", "ELECTRONIC_INVOICING"));
    }

    private static void anadir(Map<String, SellableItem> items, SellableItem item) {
        items.put(item.code(), item);
    }
}
