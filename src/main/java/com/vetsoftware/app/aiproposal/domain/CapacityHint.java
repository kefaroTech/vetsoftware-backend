package com.vetsoftware.app.aiproposal.domain;

/**
 * Los tres enteros de capacidad que el modelo deduce del texto: cuanta gente,
 * cuantas sedes y cuantas cajas describio el prospecto.
 *
 * <p>
 * ⛔ <strong>SON DATO, NUNCA UNA LINEA COTIZADA</strong> (plan S2.3, regla 2).
 * La pantalla puede escribir "3 personas: 1 incluida; los usuarios adicionales
 * se ajustan al contratar" y nada mas. Convertirlos en linea exigiria cotizar
 * {@code EXTRA_USER}, {@code EXTRA_BRANCH} o {@code EXTRA_TERMINAL}, y
 * <strong>ninguno de los cuatro {@code EXTRA_*} es contratable por
 * autoservicio</strong>: la semilla 309 no mete ninguno en los tres packs, asi
 * que el gate de contratacion los rechaza. Una propuesta que los cotice muere
 * en el paso 6 con un {@code ARTICULO_NO_CONTRATABLE} indistinguible, despues
 * de que el prospecto se registro y verifico el correo.
 *
 * <p>
 * ⛔ <strong>Y los escribe el modelo, asi que son entrada no confiable.</strong>
 * Nada impide que devuelva 9.000 sedes porque el texto decia "atendemos a nueve
 * mil clientes"; con precio por tramos acumulativos eso seria una cifra enorme
 * pintada en una pantalla de compra. Se acotan aqui, en el constructor, que es
 * donde el {@code CLAUDE.md} pone las invariantes.
 */
public record CapacityHint(int staff, int branches, int terminals) {

    /**
     * Los topes de cordura del esquema de salida (anexo E §2: {@code usuarios}
     * 1-500, {@code sedes} 1-200, {@code cajas} 0-100).
     *
     * <p>
     * <strong>No son reglas de negocio</strong> —nada dice que una cadena no pueda
     * tener 200 sedes— sino el limite por encima del cual el numero solo puede
     * venir de que el modelo leyo mal, y pintarlo hace mas dano que omitirlo.
     *
     * <p>
     * ⚠️ <strong>Este parrafo decia que «el proveedor los hace cumplir con
     * {@code strict: true}» y eso nunca ha sido verdad de los rangos.</strong>
     * {@code strict} valida <em>forma</em> —que el campo este, que sea un entero,
     * que no sobre ninguno— y hoy si viaja, pero los rangos no se declaran como
     * palabras clave del esquema: van en la descripcion de cada campo, que el
     * modelo lee y ningun validador comprueba. Ver {@code ProposalOutputSchema},
     * que explica por que. Asi que <strong>estas tres lineas no son una segunda
     * comprobacion: son la unica</strong>, y ademas tienen que seguir corriendo
     * aunque el modo de salida estructurada baje a {@code PROMPT} y no haya esquema
     * ninguno.
     */
    private static final int MAX_USUARIOS = 500;

    private static final int MAX_SEDES = 200;

    private static final int MAX_CAJAS = 100;

    public CapacityHint {
        staff = acotar(staff, MAX_USUARIOS);
        branches = acotar(branches, MAX_SEDES);
        terminals = acotar(terminals, MAX_CAJAS);
    }

    /** Lo que se asume cuando el modelo no dijo nada o no se le pregunto. */
    public static CapacityHint desconocido() {
        return new CapacityHint(0, 0, 0);
    }

    /**
     * Fuera de rango se normaliza a cero —"no lo se"— y <strong>no al
     * techo</strong>: quedarse en 500 seria afirmar una cifra que nadie dijo, y el
     * front pinta esa cifra al lado de un precio.
     */
    private static int acotar(int valor, int maximo) {
        return valor < 0 || valor > maximo ? 0 : valor;
    }

    /** {@code true} si hay algo que ensenar en la nota de capacidades. */
    public boolean hayAlgoQueDecir() {
        return staff > 0 || branches > 0 || terminals > 0;
    }
}
