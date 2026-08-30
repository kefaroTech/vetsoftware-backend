package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.port.out.ProposalRetentionPort;
import jakarta.persistence.Column;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * El adaptador delega, y en la supresion <b>ordena</b> y <b>deja
 * constancia</b>. Lo segundo y lo tercero es lo que hace falta probar.
 *
 * <p>
 * &#9940; <b>El orden de los tres pasos de la supresion es al reves que el del
 * barrido, y es lo unico que hace que funcione.</b> El paso que borra
 * {@code contact_email} destruye {@code contact_email_hash} —columna
 * {@code GENERATED ALWAYS ... STORED}—, que es por lo que buscan los otros dos.
 * Invertirlo deja los motivos del titular escritos, con el metodo devolviendo
 * "una fila suprimida" y el informe de cumplimiento diciendo que se le borro.
 * Es un defecto que ninguna revision humana ve leyendo las tres llamadas,
 * porque las tres se leen bien.
 *
 * <p>
 * &#9940; <b>Y la evidencia commitea con los borrados o no commitea.</b> Los
 * dos estados que hay que impedir no se ven en produccion hasta que alguien
 * pide el informe: un borrado sin prueba, y una prueba de un borrado que
 * revirtio. El segundo es peor, porque afirma por escrito ante la SIC algo que
 * no ocurrio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProposalRetentionPort — delegacion, orden y constancia de la supresion")
class JpaProposalRetentionPortTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 30, 3, 55);

    private static final LocalDateTime CORTE = LocalDateTime.of(2026, 6, 1, 0, 0);

    private static final Long ACTOR = 6L;

    @Mock
    private AiProposalRetentionJpaRepository jpaRepository;

    @Mock
    private AiProposalSuppressionRequestJpaRepository suppressionRepository;

    @InjectMocks
    private JpaProposalRetentionPort port;

    /**
     * La huella tal y como la calcularia MySQL: SHA-256 del correo en minusculas.
     */
    private static byte[] huellaEsperada(String correo) throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(correo.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private AiProposalSuppressionRequestJpaEntity constanciaEscrita() {
        ArgumentCaptor<AiProposalSuppressionRequestJpaEntity> fila = ArgumentCaptor.captor();
        verify(suppressionRepository).save(fila.capture());
        return fila.getValue();
    }

    @Nested
    @DisplayName("Barrido")
    class Barrido {

        @Test
        @DisplayName("cada paso pasa su corte y su lote tal cual y devuelve las filas movidas")
        void cada_paso_delega_con_sus_argumentos() {
            when(jpaRepository.anonymizeProposals(CORTE, AHORA, 50)).thenReturn(7);
            when(jpaRepository.redactTurns(50)).thenReturn(9);
            when(jpaRepository.redactLineReasons(AHORA, 50)).thenReturn(11);
            when(jpaRepository.purgeLines(CORTE, 50)).thenReturn(3);
            when(jpaRepository.purgeTurns(CORTE, 50)).thenReturn(2);
            when(jpaRepository.purgeProposals(CORTE, 50)).thenReturn(1);

            assertThat(port.anonymizeProposals(CORTE, AHORA, 50)).isEqualTo(7);
            assertThat(port.redactTurns(50)).isEqualTo(9);
            assertThat(port.redactLineReasons(AHORA, 50)).isEqualTo(11);
            assertThat(port.purgeLines(CORTE, 50)).isEqualTo(3);
            assertThat(port.purgeTurns(CORTE, 50)).isEqualTo(2);
            assertThat(port.purgeProposals(CORTE, 50)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Supresion dirigida")
    class SupresionDirigida {

        @Test
        @DisplayName("borra motivos y turnos ANTES que el correo, o el hash ya no existiria")
        void borra_los_motivos_antes_que_el_correo() {
            when(jpaRepository.suppressLinesByEmail("laura@vet.co", AHORA)).thenReturn(4);
            when(jpaRepository.suppressTurnsByEmail("laura@vet.co")).thenReturn(2);
            when(jpaRepository.suppressProposalsByEmail("laura@vet.co", AHORA)).thenReturn(1);

            ProposalRetentionPort.SuppressionResult resultado = port
                    .suppressByContactEmail("laura@vet.co", ACTOR, AHORA);

            var enOrden = inOrder(jpaRepository);
            enOrden.verify(jpaRepository).suppressLinesByEmail("laura@vet.co", AHORA);
            enOrden.verify(jpaRepository).suppressTurnsByEmail("laura@vet.co");
            enOrden.verify(jpaRepository).suppressProposalsByEmail("laura@vet.co", AHORA);

            assertThat(resultado.lines()).isEqualTo(4);
            assertThat(resultado.turns()).isEqualTo(2);
            assertThat(resultado.proposals()).isEqualTo(1);
            assertThat(resultado.total()).isEqualTo(7);
        }

        /**
         * Sin este corte, un correo vacio se convertiria en
         * {@code UNHEX(SHA2(LOWER(''), 256))}, que es un hash perfectamente valido: no
         * casaria con nada hoy, pero el metodo estaria preguntando a la base por una
         * cadena vacia en vez de negarse. Tampoco escribe constancia: no hay titular al
         * que dejarsela.
         */
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("un correo en blanco no llega a la base y devuelve el resultado vacio")
        void un_correo_en_blanco_no_llega_a_la_base(String correo) {
            ProposalRetentionPort.SuppressionResult resultado = port.suppressByContactEmail(correo,
                    ACTOR, AHORA);

            assertThat(resultado.total()).isZero();
            verifyNoInteractions(jpaRepository, suppressionRepository);
        }

        @Test
        @DisplayName("un correo que no esta devuelve ceros, no un fallo")
        void un_correo_que_no_esta_devuelve_ceros() {
            when(jpaRepository.suppressLinesByEmail(anyString(), any())).thenReturn(0);
            when(jpaRepository.suppressTurnsByEmail(anyString())).thenReturn(0);
            when(jpaRepository.suppressProposalsByEmail(anyString(), any())).thenReturn(0);

            assertThat(port.suppressByContactEmail("nadie@vet.co", ACTOR, AHORA).total()).isZero();
        }
    }

    /**
     * El aviso de privacidad promete el derecho de supresion, el endpoint borra, y
     * hasta hoy no quedaba constancia de que se hubiera atendido: el propio borrado
     * destruye el hash por el que se reconoce al titular. Estas pruebas son las que
     * impiden que esa constancia vuelva a faltar, se escriba de mentira o se
     * escriba fuera de la transaccion que la hace cierta.
     */
    @Nested
    @DisplayName("Constancia de la supresion")
    class Constancia {

        /**
         * &#9940; "Atendimos la peticion y no habia nada" es justo la respuesta que
         * pide el regulador. Escribir la fila solo cuando se movio algo dejaria sin
         * acuse precisamente el caso repetido —el borrado anterior ya se llevo el hash
         * por el que se busca— y ese silencio se lee igual que "nunca hubo peticion".
         */
        @Test
        @DisplayName("la fila se escribe tambien con los tres contadores a cero")
        void la_fila_se_escribe_tambien_con_los_contadores_a_cero() {
            port.suppressByContactEmail("nadie@vet.co", ACTOR, AHORA);

            AiProposalSuppressionRequestJpaEntity fila = constanciaEscrita();
            assertThat(fila.getProposalsSuppressed()).isZero();
            assertThat(fila.getTurnsSuppressed()).isZero();
            assertThat(fila.getLinesSuppressed()).isZero();
            assertThat(fila.getExecutedBySystemUserId()).isEqualTo(ACTOR);
            assertThat(fila.getExecutedAt()).isEqualTo(AHORA);
        }

        /**
         * &#9940; La huella tiene que ser la MISMA que calcula
         * {@code UNHEX(SHA2(LOWER(contact_email), 256))} en la columna generada. Si se
         * separan, la tabla se sigue llenando y ninguna peticion repetida vuelve a
         * emparejar con su predecesora: el fallo es mudo y solo se ve el dia del
         * informe.
         */
        @Test
        @DisplayName("la huella es SHA-256 del correo en minusculas, byte a byte")
        void la_huella_es_sha256_del_correo_en_minusculas() throws Exception {
            port.suppressByContactEmail("Laura@Vet.CO", ACTOR, AHORA);

            assertThat(constanciaEscrita().getSubjectEmailHash())
                    .isEqualTo(huellaEsperada("laura@vet.co"));
        }

        @Test
        @DisplayName("dos escrituras del mismo correo con distinta caja dan la misma huella")
        void la_caja_del_correo_no_cambia_la_huella() throws Exception {
            port.suppressByContactEmail("LAURA@VET.CO", ACTOR, AHORA);

            assertThat(constanciaEscrita().getSubjectEmailHash())
                    .isEqualTo(huellaEsperada("laura@vet.co"));
        }

        @Test
        @DisplayName("la constancia guarda el hecho y no el dato: ni un campo de texto")
        void la_constancia_no_guarda_el_correo_en_claro() {
            assertThat(AiProposalSuppressionRequestJpaEntity.class.getDeclaredFields())
                    .as("un campo de texto aqui reintroduciria el dato que se acaba de borrar")
                    .noneMatch(campo -> campo.getType() == String.class);
        }

        /**
         * Los dos ceros que sin esto se leen igual: "ya se le borro en julio" y "nunca
         * hubo nada suyo".
         */
        @Test
        @DisplayName("una peticion repetida se reconoce por la fecha de su predecesora")
        void una_peticion_repetida_se_reconoce_por_su_predecesora() throws Exception {
            LocalDateTime enJulio = LocalDateTime.of(2026, 7, 3, 9, 0);
            when(suppressionRepository.findLastExecutedAt(huellaEsperada("laura@vet.co")))
                    .thenReturn(Optional.of(enJulio));

            var resultado = port.suppressByContactEmail("laura@vet.co", ACTOR, AHORA);

            assertThat(resultado.previouslySuppressedAt()).isEqualTo(enJulio);
            assertThat(constanciaEscrita().getExecutedAt())
                    .as("la repetida deja su propia constancia, no reusa la de julio")
                    .isEqualTo(AHORA);
        }

        @Test
        @DisplayName("la primera peticion de un titular no tiene predecesora")
        void la_primera_peticion_no_tiene_predecesora() {
            var resultado = port.suppressByContactEmail("laura@vet.co", ACTOR, AHORA);

            assertThat(resultado.previouslySuppressedAt()).isNull();
            assertThat(constanciaEscrita().getExecutedAt()).isEqualTo(AHORA);
        }

        /**
         * &#9940; <b>La entidad y el changeset 392 son una sola pieza.</b> Un nombre de
         * columna que no case -o una columna de mas- no rompe ningun test de
         * comportamiento: rompe {@code ddl-auto: validate} al arrancar la aplicacion,
         * es decir en el sitio mas caro y mas lejos de la causa. Esta lista es la que
         * lo convierte en un fallo de segundos.
         *
         * <p>
         * {@code id} no aparece porque va por {@code @Id} sin {@code @Column}, y
         * {@code previously_suppressed_at} <b>no existe</b>: la fecha de la peticion
         * anterior se deriva con {@code max(executed_at)} sobre el indice
         * {@code (subject_email_hash, executed_at)}, que es justo para lo que el
         * changeset lo declara. Copiarla a una columna seria un segundo sitio donde
         * vive el mismo dato.
         */
        @Test
        @DisplayName("las columnas de la entidad son exactamente las del changeset 392")
        void las_columnas_son_exactamente_las_del_changeset_392() {
            assertThat(AiProposalSuppressionRequestJpaEntity.class.getDeclaredFields())
                    .filteredOn(campo -> campo.isAnnotationPresent(Column.class))
                    .extracting(campo -> campo.getAnnotation(Column.class).name())
                    .containsExactlyInAnyOrder("subject_email_hash", "executed_at",
                            "executed_by_system_user_id", "proposals_suppressed",
                            "turns_suppressed", "lines_suppressed", "created_date");
        }

        /**
         * &#9940; La lectura de la anterior va ANTES del {@code save}: despues, la fila
         * recien escrita seria su propia predecesora y toda peticion —incluida la
         * primera— saldria como repetida.
         */
        @Test
        @DisplayName("lee la peticion anterior antes de escribir la nueva, no despues")
        void lee_la_anterior_antes_de_escribir_la_nueva() {
            port.suppressByContactEmail("laura@vet.co", ACTOR, AHORA);

            var enOrden = inOrder(suppressionRepository, jpaRepository);
            enOrden.verify(suppressionRepository).findLastExecutedAt(any());
            enOrden.verify(jpaRepository).suppressProposalsByEmail(anyString(), any());
            enOrden.verify(suppressionRepository).save(any());
        }

        /**
         * &#9940; <b>La prueba que mas importa.</b> Si la escritura de la constancia
         * falla, lo que NO puede pasar es que el metodo devuelva un acuse: eso dejaria
         * los tres borrados commiteados sin nada que ensenar. La excepcion tiene que
         * subir para que el {@code @Transactional} de abajo se lleve los borrados por
         * delante. Un {@code try/catch} alrededor del {@code save} —el arreglo
         * "defensivo" natural— rompe exactamente esto.
         */
        @Test
        @DisplayName("si la constancia no se puede escribir, la excepcion sube: no hay acuse")
        void si_la_constancia_falla_la_excepcion_sube() {
            when(suppressionRepository.save(any()))
                    .thenThrow(new IllegalStateException("la tabla no acepta la fila"));

            assertThatThrownBy(() -> port.suppressByContactEmail("laura@vet.co", ACTOR, AHORA))
                    .as("tragarse esto deja tres borrados commiteados y cero evidencia")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("la tabla no acepta la fila");
        }

        /**
         * La otra mitad de la garantia, y la que un doble no puede observar: que los
         * borrados y la constancia comparten <b>una sola</b> transaccion. Un
         * {@code REQUIRES_NEW} aqui commitearia la evidencia aparte y volveria posible
         * el peor de los dos estados —una prueba de un borrado que despues revirtio—,
         * sin que ningun test de comportamiento lo notara.
         */
        @Test
        @DisplayName("la constancia y los borrados van en la misma transaccion, no en dos")
        void la_constancia_y_los_borrados_van_en_la_misma_transaccion() throws Exception {
            Method metodo = JpaProposalRetentionPort.class.getMethod("suppressByContactEmail",
                    String.class, Long.class, LocalDateTime.class);

            Transactional tx = metodo.getAnnotation(Transactional.class);

            assertThat(tx).as("sin @Transactional cada escritura commitea sola").isNotNull();
            assertThat(tx.propagation())
                    .as("REQUIRES_NEW commitearia la evidencia aparte de lo que evidencia")
                    .isEqualTo(Propagation.REQUIRED);
        }

        @Test
        @DisplayName("el actor llega tal cual a la fila: no lo inventa el adaptador")
        void el_actor_llega_tal_cual() {
            port.suppressByContactEmail("laura@vet.co", 99L, AHORA);

            assertThat(constanciaEscrita().getExecutedBySystemUserId()).isEqualTo(99L);
        }
    }

    @Nested
    @DisplayName("El resultado")
    class Resultado {

        @Test
        @DisplayName("el total es la suma de las tres tablas, no un contador aparte")
        void el_total_es_la_suma() {
            assertThat(new ProposalRetentionPort.SuppressionResult(1, 2, 3, null).total())
                    .isEqualTo(6);
            assertThat(new ProposalRetentionPort.SuppressionResult(0, 0, 0, null).total()).isZero();
        }

        /**
         * El desglose no es cosmetico: un unico total deja indistinguibles "ese correo
         * no esta" y "el paso de motivos no toco nada porque su subconsulta esta rota".
         */
        @Test
        @DisplayName("el desglose separa lo que un total unico confundiria")
        void el_desglose_separa_lo_que_el_total_confunde() {
            var soloCabecera = new ProposalRetentionPort.SuppressionResult(1, 0, 0, null);

            assertThat(soloCabecera.lines())
                    .as("cabecera borrada y cero motivos es exactamente el defecto de esta fase")
                    .isZero();
            assertThat(soloCabecera.total()).isEqualTo(1);
        }

        /**
         * La fecha de la peticion anterior no entra en el total: no es una fila movida,
         * es el dato que dice como hay que leer las que si lo son.
         */
        @Test
        @DisplayName("la fecha de la peticion anterior no altera el total")
        void la_fecha_anterior_no_altera_el_total() {
            var repetida = new ProposalRetentionPort.SuppressionResult(0, 0, 0,
                    LocalDateTime.of(2026, 7, 3, 9, 0));

            assertThat(repetida.total()).isZero();
            assertThat(repetida.previouslySuppressedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Argumentos del barrido")
    class ArgumentosDelBarrido {

        @Test
        @DisplayName("el lote no se reinterpreta por el camino")
        void el_lote_no_se_reinterpreta() {
            when(jpaRepository.redactTurns(anyInt())).thenReturn(0);

            port.redactTurns(123);

            verify(jpaRepository).redactTurns(123);
        }
    }
}
