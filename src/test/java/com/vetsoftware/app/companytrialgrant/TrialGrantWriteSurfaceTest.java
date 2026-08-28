package com.vetsoftware.app.companytrialgrant;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companytrialgrant.application.port.in.ConsumeTrialGrantUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.in.GrantTrialUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListCompanyTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListExpiredTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.out.CompanyTrialGrantRepository;
import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import com.vetsoftware.app.companytrialgrant.infrastructure.persistence.CompanyTrialGrantJpaRepository;
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
 * R-TRIAL-22 y R-TRIAL-30: <b>una prueba concedida no se desconcede.</b> No hay
 * borrado, no hay desactivación, y la tabla ni siquiera lleva {@code enabled},
 * así que tampoco existe la puerta de atrás del borrado lógico.
 *
 * <h2>Por qué esta clase existe y no basta el caso que había</h2>
 *
 * <p>
 * {@code CompanyTrialGrantTest} tenía un caso que recorría
 * {@code CompanyTrialGrant.class.getMethods()} y comprobaba que ninguno se
 * llamara {@code set…}, {@code delete…}, {@code disable…} ni {@code revoke…}.
 * La regla es del <em>slice</em>, no del agregado: nadie borra invocando un
 * método del agregado. Se borra desde el repositorio, desde una consulta
 * anotada, o exponiendo un caso de uso nuevo que un controlador publique. Las
 * tres cosas pasaban por debajo de aquella reflexión sin tocarla.
 *
 * <p>
 * Aquí se miran las tres superficies por las que puede entrar una escritura:
 * <ol>
 * <li><b>El agregado</b>, igual que antes.
 * <li><b>Los puertos</b> — el de salida y los de entrada. Un controlador solo
 * puede publicar lo que un {@code port/in} ofrece, así que cerrar el puerto
 * cierra el controlador que todavía no existe.
 * <li><b>Las consultas del repositorio Spring Data</b>, que es donde vive el
 * riesgo real: {@code JpaRepository} <em>hereda</em> {@code delete},
 * {@code deleteById} y {@code deleteAll}, así que preguntar por
 * {@code getMethods()} allí no diría nada. Lo que se afirma es lo que la
 * interfaz <em>declara</em>, y que ninguna {@code @Query} suya sea una
 * escritura.
 * </ol>
 */
@DisplayName("companytrialgrant — la superficie de escritura del slice no admite desconceder")
class TrialGrantWriteSurfaceTest {

    /** Los verbos con los que se borra o se desactiva algo. */
    private static final List<String> VERBOS_PROHIBIDOS = List.of("set", "delete", "remove",
            "disable", "revoke", "reset", "clear", "purge");

    private static boolean escribeOBorra(String nombre) {
        String minusculas = nombre.toLowerCase(Locale.ROOT);
        return VERBOS_PROHIBIDOS.stream().anyMatch(minusculas::startsWith);
    }

    @Test
    @DisplayName("el agregado no ofrece ninguna operación que borre o desactive una concesión")
    void el_agregado_no_ofrece_ninguna_operacion_que_borre_o_desactive() {
        assertThat(Arrays.stream(CompanyTrialGrant.class.getMethods()).map(Method::getName))
                .noneMatch(TrialGrantWriteSurfaceTest::escribeOBorra);
    }

    /**
     * El puerto de salida y los cuatro de entrada. Estos últimos son los que
     * cierran el camino del controlador: no se puede publicar por HTTP lo que
     * ningún caso de uso ofrece.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(classes = {CompanyTrialGrantRepository.class, GrantTrialUseCase.class,
            ConsumeTrialGrantUseCase.class, ListCompanyTrialGrantsUseCase.class,
            ListExpiredTrialGrantsUseCase.class})
    @DisplayName("ningún puerto del slice ofrece borrar ni desactivar una concesión")
    void ningun_puerto_del_slice_ofrece_borrar_ni_desactivar(Class<?> puerto) {
        assertThat(Arrays.stream(puerto.getDeclaredMethods()).map(Method::getName))
                .as("métodos declarados por %s", puerto.getSimpleName())
                .noneMatch(TrialGrantWriteSurfaceTest::escribeOBorra);
    }

    /**
     * <b>{@code getDeclaredMethods} y no {@code getMethods}</b>: la interfaz
     * extiende {@code JpaRepository}, que ya trae {@code delete},
     * {@code deleteById} y {@code deleteAll} heredados. Lo que la regla vigila es
     * lo que este repositorio añade por su cuenta.
     */
    @Test
    @DisplayName("el repositorio Spring Data no declara ninguna operación de borrado propia")
    void el_repositorio_no_declara_ninguna_operacion_de_borrado_propia() {
        assertThat(Arrays.stream(CompanyTrialGrantJpaRepository.class.getDeclaredMethods())
                .map(Method::getName)).noneMatch(TrialGrantWriteSurfaceTest::escribeOBorra);
    }

    /**
     * La vía que la reflexión sobre el agregado no podía ver de ninguna manera: una
     * sentencia escrita a mano. Una {@code @Query} de escritura necesita
     * {@code @Modifying} para funcionar, y su texto empieza por el verbo; se
     * comprueban las dos cosas, porque {@code @Modifying} es la señal declarativa y
     * el texto es lo que acaba ejecutando el motor —incluido el SQL nativo—.
     */
    @Test
    @DisplayName("ninguna @Query del repositorio es una escritura: ni anotada, ni por el verbo de"
            + " la sentencia")
    void ninguna_consulta_del_repositorio_es_una_escritura() {
        assertThat(CompanyTrialGrantJpaRepository.class.getDeclaredMethods())
                .as("consultas declaradas por el repositorio de concesiones").allSatisfy(metodo -> {
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
