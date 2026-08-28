package com.vetsoftware.app.companytrialwindow;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companytrialwindow.application.port.in.CloseTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.in.FindCurrentTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.in.OpenTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.out.CompanyTrialWindowRepository;
import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindow;
import com.vetsoftware.app.companytrialwindow.infrastructure.persistence.CompanyTrialWindowJpaRepository;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * R-TRIAL-09 y R-TRIAL-10: <b>la ventana de prueba no se estira.</b> Una vez
 * abierta, sus días no se amplían y su cierre no se mueve — y esa promesa es la
 * que sostiene todo lo demás, porque el fin de la ventana está copiado dentro
 * de cada concesión y atado por clave foránea.
 *
 * <h2>Por qué esta clase existe y no basta el caso que había</h2>
 *
 * <p>
 * {@code CompanyTrialWindowTest} tenía un caso que recorría
 * {@code CompanyTrialWindow.class.getMethods()} buscando {@code set…},
 * {@code extend…} o {@code prolong…}. Nadie estira una ventana llamando a un
 * método del agregado: se estira con un {@code UPDATE} desde el repositorio,
 * con una consulta anotada, o publicando un caso de uso que un controlador
 * exponga. Las tres pasaban por debajo.
 *
 * <p>
 * Aquí se miran las tres superficies. La última —que ninguna {@code @Query} del
 * repositorio sea una escritura— es la que cierra el hueco de verdad: el
 * javadoc del repositorio ya <em>afirma</em> que no declara ninguna escritura
 * masiva, y esto es lo que convierte esa afirmación en algo que se rompe si
 * deja de ser cierta.
 *
 * <p>
 * <b>Ojo con la relación con el motor.</b> Que la ventana no se estire lo
 * sostiene además {@code fk_company_trial_grants_window} con su
 * {@code ON UPDATE RESTRICT}, y eso se prueba contra MySQL en
 * {@code CompanyTrialGrantPersistenceIT}. Esta clase cubre lo otro: que el
 * código no llegue siquiera a intentarlo, y que nadie publique una puerta para
 * hacerlo.
 */
@DisplayName("companytrialwindow — la superficie de escritura del slice no admite estirar la"
        + " ventana")
class TrialWindowWriteSurfaceTest {

    /** Los verbos con los que se alarga, se reabre o se pisa una ventana. */
    private static final List<String> VERBOS_PROHIBIDOS = List.of("set", "extend", "prolong",
            "enlarge", "reopen", "delete", "remove", "reset", "clear");

    private static boolean estiraOPisa(String nombre) {
        String minusculas = nombre.toLowerCase(Locale.ROOT);
        return VERBOS_PROHIBIDOS.stream().anyMatch(minusculas::startsWith);
    }

    @Test
    @DisplayName("el agregado no ofrece ninguna operación que amplíe los días de una ventana viva")
    void el_agregado_no_ofrece_ninguna_operacion_que_amplie_los_dias() {
        assertThat(Arrays.stream(CompanyTrialWindow.class.getMethods()).map(Method::getName))
                .noneMatch(TrialWindowWriteSurfaceTest::estiraOPisa);
    }

    /**
     * El puerto de salida y los tres de entrada. {@code CloseTrialWindowUseCase} es
     * la única mutación publicada del slice, y cerrar no es estirar: la ventana se
     * acorta o se queda como está, nunca crece.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(classes = {CompanyTrialWindowRepository.class, OpenTrialWindowUseCase.class,
            CloseTrialWindowUseCase.class, FindCurrentTrialWindowUseCase.class})
    @DisplayName("ningún puerto del slice ofrece ampliar ni reabrir una ventana")
    void ningun_puerto_del_slice_ofrece_ampliar_ni_reabrir(Class<?> puerto) {
        assertThat(Arrays.stream(puerto.getDeclaredMethods()).map(Method::getName))
                .as("métodos declarados por %s", puerto.getSimpleName())
                .noneMatch(TrialWindowWriteSurfaceTest::estiraOPisa);
    }

    /**
     * <b>{@code getDeclaredMethods} y no {@code getMethods}</b>: la interfaz
     * extiende {@code JpaRepository} y hereda su familia de borrado. Lo que se
     * vigila es lo que este repositorio añade.
     */
    @Test
    @DisplayName("el repositorio Spring Data no declara ninguna escritura propia")
    void el_repositorio_no_declara_ninguna_escritura_propia() {
        assertThat(Arrays.stream(CompanyTrialWindowJpaRepository.class.getDeclaredMethods())
                .map(Method::getName)).noneMatch(TrialWindowWriteSurfaceTest::estiraOPisa);
    }

    /**
     * La sentencia nativa, que es la vía que ninguna reflexión sobre el agregado
     * podía ver. Se comprueban las dos señales: {@code @Modifying}, que es lo que
     * Spring Data exige para que una {@code @Query} de escritura funcione, y el
     * verbo con el que arranca el texto, que es lo que acaba ejecutando el motor.
     */
    @Test
    @DisplayName("ninguna @Query del repositorio es una escritura: ni anotada, ni por el verbo de"
            + " la sentencia")
    void ninguna_consulta_del_repositorio_es_una_escritura() {
        assertThat(CompanyTrialWindowJpaRepository.class.getDeclaredMethods())
                .as("consultas declaradas por el repositorio de ventanas").allSatisfy(metodo -> {
                    assertThat(metodo.getAnnotation(Modifying.class))
                            .as("@Modifying en %s", metodo.getName()).isNull();
                    assertThat(sentenciaDe(metodo)).as("verbo de la @Query de %s", metodo.getName())
                            .doesNotStartWith("UPDATE").doesNotStartWith("DELETE")
                            .doesNotStartWith("INSERT");
                });
    }

    /** El texto de la {@code @Query}, normalizado; cadena vacía si no la lleva. */
    private static String sentenciaDe(Method metodo) {
        Query consulta = metodo.getAnnotation(Query.class);
        return consulta == null ? "" : consulta.value().strip().toUpperCase(Locale.ROOT);
    }
}
