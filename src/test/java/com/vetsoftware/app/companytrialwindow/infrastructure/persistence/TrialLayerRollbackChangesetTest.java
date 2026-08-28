package com.vetsoftware.app.companytrialwindow.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * <b>R-TRIAL-23 comprobada sobre el changeset, no sobre una intencion.</b>
 *
 * <p>
 * La regla dice que la marcha atras del despliegue <strong>no borra</strong>
 * {@code company_trial_windows} ni {@code company_trial_grants}. No es una
 * preferencia de estilo: esas dos tablas son el registro de lo que cada empresa
 * ya probo, y son lo unico que impide que un articulo se regale dos veces.
 * Echar atras el despliegue con un {@code dropTable} dentro del
 * {@code rollback} le devuelve <em>a todos los clientes a la vez</em> el
 * derecho a probarlo todo otra vez, y no queda ni rastro de que ocurrio: las
 * filas simplemente ya no estan.
 *
 * <p>
 * <strong>Se lee el XML a proposito.</strong> Un test que llamara a Liquibase
 * comprobaria que el rollback corre, no <em>que</em> hace; y uno que afirmara
 * sobre una constante de Java no comprobaria nada del despliegue real. El
 * fichero es el artefacto que se despliega, asi que el fichero es lo que hay
 * que mirar. Es tambien la unica forma de que esto falle si alguien vuelve a
 * meter el {@code dropTable} dentro de un ano.
 *
 * <p>
 * <strong>Por que esta prueba se leia antes por fichero y ahora por
 * changeset.</strong> Cuando cada tabla vivia en un unico changeset con el
 * rollback vacio, «el fichero no nombra la tabla en su marcha atras» y «la
 * marcha atras no borra la tabla» eran la misma frase, y la prueba usaba la
 * primera como atajo de la segunda. Al partirse cada changeset en dos
 * -estructura con id original y rollback vacio, mas enlace con solo las claves
 * foraneas salientes- ese atajo dejo de valer: el rollback del changeset de
 * enlace nombra la tabla en un {@code dropForeignKeyConstraint}, que suelta una
 * clave y no toca ni una fila. El atajo daba rojo sobre un cambio correcto, asi
 * que se sustituye por la regla literal, que ademas es mas dificil de burlar:
 * <em>ningun</em> rollback de <em>ningun</em> changeset de la carpeta de
 * migraciones puede borrar esas dos tablas, y las dos declaraciones de
 * estructura tienen que seguir llevando su rollback explicitamente vacio, que
 * es el mecanismo concreto que impide a Liquibase auto-generar el
 * {@code dropTable} del {@code createTable}.
 */
@DisplayName("Capa de prueba — la marcha atras del despliegue no reabre el abuso")
class TrialLayerRollbackChangesetTest {

    private static final Path MIGRACIONES = Path.of("src/main/resources/db/changelog/migrations");

    private static final String FICHERO_VENTANAS = "301_create_company_trial_windows.xml";
    private static final String FICHERO_CONCESIONES = "302_create_company_trial_grants.xml";

    private static final Pattern BLOQUE_ROLLBACK = Pattern
            .compile("<rollback\\s*/>|<rollback[^>]*>(.*?)</rollback>", Pattern.DOTALL);

    /**
     * Los comentarios se quitan <strong>antes</strong> de buscar. El propio
     * changeset explica en un comentario por que su marcha atras esta vacia, y ese
     * texto nombra las etiquetas de las que habla: sin quitarlos, la prueba leeria
     * la explicacion como si fuera codigo y se rompería la proxima vez que alguien
     * escriba un comentario honesto.
     */
    private static final Pattern COMENTARIO = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    private static String leer(String fichero) {
        try {
            return Files.readString(MIGRACIONES.resolve(fichero), StandardCharsets.UTF_8);
        } catch (IOException noSePudoLeer) {
            throw new UncheckedIOException(
                    "No se pudo leer el changeset " + fichero + ": esta prueba vigila el fichero"
                            + " que se despliega, asi que sin fichero no hay nada que vigilar",
                    noSePudoLeer);
        }
    }

    private static String sinComentarios(String xml) {
        return COMENTARIO.matcher(xml).replaceAll("");
    }

    /**
     * El cuerpo del changeset con ese id exacto, ya sin comentarios.
     *
     * <p>
     * <strong>Revienta si el id no esta</strong>, en vez de devolver cadena vacia.
     * Es la diferencia entre una prueba y un adorno: si manana alguien renombra o
     * fusiona el changeset, esto tiene que ponerse rojo y obligar a mirar, no
     * quedarse en verde afirmando sobre la nada.
     */
    private static String changeSet(String fichero, String id) {
        Matcher elChangeSet = Pattern
                .compile("<changeSet\\s+id=\"" + Pattern.quote(id) + "\"[^>]*>(.*?)</changeSet>",
                        Pattern.DOTALL)
                .matcher(sinComentarios(leer(fichero)));
        if (!elChangeSet.find()) {
            throw new AssertionError("El changeset " + id + " ya no existe en " + fichero
                    + ". R-TRIAL-23 se defiende sobre changesets concretos: si este se renombro"
                    + " o se fusiono, hay que revisar a mano que la regla siga cumpliendose y"
                    + " actualizar esta prueba, no borrarla.");
        }
        return elChangeSet.group(1);
    }

    /**
     * Todo lo que hay dentro de los bloques {@code <rollback>} de ese trozo de XML,
     * concatenado. Un {@code <rollback/>} vacio aporta cadena vacia, que es
     * exactamente lo que se quiere.
     *
     * <p>
     * <strong>Revienta si no hay ni un bloque de rollback.</strong> Sin la
     * etiqueta, Liquibase auto-genera la marcha atras del {@code createTable}, que
     * es literalmente el {@code dropTable} que R-TRIAL-23 prohibe; un ausente no
     * puede leerse como un vacio inocente.
     */
    private static String marchaAtrasDe(String xml, String queEs) {
        Matcher bloques = BLOQUE_ROLLBACK.matcher(xml);
        StringBuilder marchaAtras = new StringBuilder();
        boolean huboAlguno = false;
        while (bloques.find()) {
            huboAlguno = true;
            marchaAtras.append(bloques.group(1) == null ? "" : bloques.group(1));
        }
        if (!huboAlguno) {
            throw new AssertionError("No hay ni un <rollback> en " + queEs
                    + ". Sin la etiqueta, Liquibase auto-genera el dropTable del createTable:"
                    + " la ausencia es precisamente el fallo que R-TRIAL-23 prohibe.");
        }
        return marchaAtras.toString();
    }

    /** Todos los rollbacks de todos los changesets del fichero, concatenados. */
    private static String marchaAtrasDelFichero(String fichero) {
        return marchaAtrasDe(sinComentarios(leer(fichero)), fichero);
    }

    @Nested
    @DisplayName("R-TRIAL-23 · el registro de lo ya probado no se destruye")
    class ElRegistroDeLoProbadoNoSeDestruye {

        /**
         * El caso violador, tal como lo escribe el catalogo de reglas: la regla
         * literal, sobre todos los changesets de los dos ficheros.
         */
        @Test
        @DisplayName("ninguna marcha atras de la capa I borra las dos tablas del registro")
        void ninguna_marcha_atras_de_la_capa_I_borra_las_dos_tablas_del_registro() {
            assertThat(marchaAtrasDelFichero(FICHERO_VENTANAS))
                    .as("un dropTable en el rollback de 301 devolveria a todos los clientes"
                            + " el derecho a probarlo todo otra vez")
                    .doesNotContain("dropTable");

            assertThat(marchaAtrasDelFichero(FICHERO_CONCESIONES))
                    .as("un dropTable en el rollback de 302 borraria el registro de que un"
                            + " articulo ya se regalo, que es la invariante entera de la tabla")
                    .doesNotContain("dropTable");
        }

        /**
         * El mecanismo concreto, no solo su efecto. Que el rollback este declarado
         * <em>y vacio</em> en el changeset que crea la tabla es lo unico que impide a
         * Liquibase auto-generar el {@code dropTable}. Comprobar solo la ausencia de la
         * cadena {@code dropTable} pasaria en verde el dia que alguien borrara la
         * etiqueta entera, que es el mismo desastre escrito de otra forma.
         */
        @Test
        @DisplayName("los changesets que crean las dos tablas declaran su rollback vacio")
        void los_changesets_que_crean_las_dos_tablas_declaran_su_rollback_vacio() {
            assertThat(
                    marchaAtrasDe(changeSet(FICHERO_VENTANAS, "301_create_company_trial_windows"),
                            "301_create_company_trial_windows"))
                    .as("la estructura de company_trial_windows no revierte nada: la tabla,"
                            + " sus indices y sus columnas sobreviven al rollback")
                    .isBlank();

            assertThat(
                    marchaAtrasDe(changeSet(FICHERO_CONCESIONES, "302_create_company_trial_grants"),
                            "302_create_company_trial_grants"))
                    .as("la estructura de company_trial_grants no revierte nada: es el"
                            + " registro PARA SIEMPRE de que ya se probo")
                    .isBlank();
        }

        /**
         * Que no borre las tablas no significa que no pueda revertir nada. El changeset
         * de enlace de 302 añade una clave foranea sobre {@code subscription_items},
         * que es una tabla de otro sitio: eso si se deshace, y dejarlo colgando
         * bloquearia el propio redespliegue.
         */
        @Test
        @DisplayName("el enlace de 302 si retira la clave foranea que puso sobre otra tabla")
        void el_enlace_de_302_si_retira_la_clave_foranea_sobre_otra_tabla() {
            assertThat(marchaAtrasDe(
                    changeSet(FICHERO_CONCESIONES, "302_link_company_trial_grants_fk"),
                    "302_link_company_trial_grants_fk")).contains("dropForeignKeyConstraint")
                    .contains("fk_subscription_items_trial_grant");
        }

        /**
         * El gemelo del anterior en 301: las dos claves foraneas salientes de la
         * ventana se sueltan, porque si no un rollback total no puede llegar hasta
         * {@code companies} ni {@code quotes}.
         */
        @Test
        @DisplayName("el enlace de 301 si retira las dos claves foraneas salientes de la ventana")
        void el_enlace_de_301_si_retira_las_dos_claves_foraneas_salientes() {
            assertThat(
                    marchaAtrasDe(changeSet(FICHERO_VENTANAS, "301_link_company_trial_windows_fk"),
                            "301_link_company_trial_windows_fk"))
                    .contains("dropForeignKeyConstraint")
                    .contains("fk_company_trial_windows_company")
                    .contains("fk_company_trial_windows_quote");
        }

        /**
         * La comprobacion de que la comprobacion sirve: si el patron de lectura
         * estuviera mal, los tests de arriba pasarian en verde sobre una cadena vacia y
         * no vigilarian nada.
         */
        @Test
        @DisplayName("los changesets existen y declaran su bloque de marcha atras")
        void los_changesets_existen_y_declaran_su_bloque_de_marcha_atras() {
            assertThat(leer(FICHERO_VENTANAS)).contains("company_trial_windows")
                    .contains("<rollback");
            assertThat(leer(FICHERO_CONCESIONES)).contains("company_trial_grants")
                    .contains("<rollback");
        }
    }
}
