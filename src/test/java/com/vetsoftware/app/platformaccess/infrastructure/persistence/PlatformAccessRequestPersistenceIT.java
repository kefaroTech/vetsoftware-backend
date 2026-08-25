package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessDecision;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rodaja de persistencia de la solicitud contra MySQL real.
 *
 * <p>
 * <b>Lo que se comprueba aquí no existe en el código Java.</b> Los dos
 * {@code UPDATE} condicionales son la única barrera contra la fuerza bruta y
 * contra la doble decisión, y su {@code WHERE} <i>es</i> la invariante: con un
 * doble del repositorio, «incrementa solo si queda margen» y «decide solo si
 * sigue siendo decidible» darían verde aunque el {@code WHERE} estuviera vacío.
 * Los ocho {@code CHECK} de la tabla tampoco existen en Java: rechazan filas
 * que el dominio jamás construiría, pero que un {@code UPDATE} futuro sí podría
 * escribir.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPlatformAccessRequestRepository — solicitud de acceso contra MySQL real")
class PlatformAccessRequestPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 3, 14, 9, 0);
    private static final LocalDateTime EXPIRA = CREADA.plusHours(72);
    private static final String MOTIVO = "Necesito acceso para operar la plataforma";

    @Autowired
    private JpaPlatformAccessRequestRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private static String hashDe(String semilla) {
        // 64 caracteres hex, que es lo que exige la columna y el dominio.
        return String.format("%064x", semilla.hashCode() & 0xffffffffL);
    }

    private PlatformAccessRequest emitir(String email, String semillaDeToken) {
        return repository.save(PlatformAccessRequest.issue("Ana Ramirez", email, MOTIVO,
                hashDe(semillaDeToken), "$2a$10$hash-bcrypt-de-prueba", 5, CREADA, EXPIRA));
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * <b>Por qué los casos del contador corren SIN la transacción del test.</b>
     * {@code registerFailedAttempt} está declarado {@code REQUIRES_NEW} —es su
     * razón de ser: el incremento tiene que sobrevivir al rollback del 422/429—,
     * así que abre una conexión propia. Si la fila la insertó la transacción del
     * test y esa transacción sigue abierta, el {@code INSERT} mantiene el bloqueo
     * exclusivo de la fila y el {@code UPDATE} independiente espera a un dueño que
     * no va a soltar nada hasta que el test acabe: {@code Lock wait timeout
     * exceeded} a los 50 segundos, cuatro veces.
     *
     * <p>
     * <b>No es un defecto de producción</b>, y por eso se arregla el test y no el
     * código: el caso de uso llega a este {@code UPDATE} después de un
     * {@code SELECT} de lectura consistente, que bajo REPEATABLE READ no toma
     * ningún bloqueo. Quien sí toma el bloqueo es {@code applyDecision}, y ese solo
     * corre en el camino en el que el código acertó, donde no hay intento que
     * contar. Que el contador sobrevive de verdad al rollback lo demuestra
     * {@code PlatformAccessAttemptCounterIT} sobre la aplicación entera.
     *
     * <p>
     * Consecuencia para estos casos: cada {@code save} y cada {@code UPDATE}
     * confirman por su cuenta, así que la fila queda escrita de verdad. De ahí el
     * correo propio de cada uno —para no perturbar las consultas por
     * {@code ana@vetrina.co} de los demás— y el {@code @Sql} de limpieza.
     */
    private void gastarIntentos(Long id, int veces) {
        java.util.stream.IntStream.range(0, veces)
                .forEach(i -> repository.registerFailedAttempt(id));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y buscar por hash del token conserva cada campo")
        void guardar_y_buscar_por_hash_conserva_cada_campo() {
            PlatformAccessRequest guardada = emitir("ana@vetrina.co", "token-1");
            releerDesdeLaBase();

            PlatformAccessRequest leida = repository.findByApprovalTokenHash(hashDe("token-1"))
                    .orElseThrow();

            assertThat(leida.getId()).isEqualTo(guardada.getId());
            assertThat(leida.getFullName()).isEqualTo("Ana Ramirez");
            assertThat(leida.getEmail()).isEqualTo("ana@vetrina.co");
            assertThat(leida.getReason()).isEqualTo(MOTIVO);
            assertThat(leida.getVerificationAttempts()).isZero();
            assertThat(leida.getMaxAttempts()).isEqualTo(5);
            assertThat(leida.getExpiresAt()).isEqualTo(EXPIRA);
            assertThat(leida.getCreatedDate()).isEqualTo(CREADA);
            assertThat(leida.getDecision()).isNull();
            assertThat(leida.getDecidedAt()).isNull();
        }

        @Test
        @DisplayName("el unique del token impide dos solicitudes con el mismo hash")
        void el_unique_del_token_impide_duplicados() {
            emitir("ana@vetrina.co", "token-repetido");
            releerDesdeLaBase();

            // Sin este indice, dos solicitudes podrian responder al mismo enlace y
            // la aprobacion de una decidiria sobre la otra.
            assertThatThrownBy(() -> {
                emitir("otra@vetrina.co", "token-repetido");
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("un hash que no existe no se encuentra")
        void un_hash_que_no_existe_no_se_encuentra() {
            assertThat(repository.findByApprovalTokenHash(hashDe("nunca-emitido"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("findLivePendingByEmail — idempotencia del formulario")
    class SolicitudViva {

        @Test
        @DisplayName("encuentra la solicitud pendiente de ese correo")
        void encuentra_la_solicitud_pendiente() {
            emitir("ana@vetrina.co", "token-vivo");
            releerDesdeLaBase();

            assertThat(repository.findLivePendingByEmail("ana@vetrina.co", CREADA.plusHours(1)))
                    .isPresent();
        }

        @Test
        @DisplayName("no encuentra la de otro correo")
        void no_encuentra_la_de_otro_correo() {
            emitir("ana@vetrina.co", "token-de-ana");
            releerDesdeLaBase();

            assertThat(repository.findLivePendingByEmail("otro@vetrina.co", CREADA.plusHours(1)))
                    .isEmpty();
        }

        @Test
        @DisplayName("una solicitud caducada NO cuenta como viva: quien caduco puede volver a pedir")
        void una_solicitud_caducada_no_cuenta_como_viva() {
            emitir("ana@vetrina.co", "token-caducado");
            releerDesdeLaBase();

            // Es justo el motivo por el que esto no es un indice unico parcial: uno
            // sobre «sin decidir» bloquearia para siempre a quien pidio acceso,
            // caduco y vuelve a pedirlo.
            assertThat(repository.findLivePendingByEmail("ana@vetrina.co", EXPIRA.plusMinutes(1)))
                    .isEmpty();
        }

        @Test
        @DisplayName("una solicitud ya decidida NO cuenta como viva")
        void una_solicitud_ya_decidida_no_cuenta_como_viva() {
            PlatformAccessRequest guardada = emitir("ana@vetrina.co", "token-decidido");
            repository.applyDecision(guardada.getId(), PlatformAccessDecision.APPROVED,
                    CREADA.plusHours(1));
            releerDesdeLaBase();

            assertThat(repository.findLivePendingByEmail("ana@vetrina.co", CREADA.plusHours(2)))
                    .isEmpty();
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @Sql(statements = "DELETE FROM platform_access_requests WHERE email = 'bloqueada-viva@vetrina.co'", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        @DisplayName("una solicitud bloqueada NO cuenta como viva")
        void una_solicitud_bloqueada_no_cuenta_como_viva() {
            PlatformAccessRequest guardada = emitir("bloqueada-viva@vetrina.co", "token-bloqueado");
            gastarIntentos(guardada.getId(), 5);

            assertThat(repository.findLivePendingByEmail("bloqueada-viva@vetrina.co",
                    CREADA.plusHours(1))).isEmpty();
        }
    }

    @Nested
    @DisplayName("registerFailedAttempt — el contador que frena la fuerza bruta")
    class ContadorDeIntentos {

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @Sql(statements = "DELETE FROM platform_access_requests WHERE email = 'contador-uno@vetrina.co'", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        @DisplayName("incrementa el contador y mueve la version en el mismo UPDATE")
        void incrementa_el_contador_y_mueve_la_version() {
            PlatformAccessRequest guardada = emitir("contador-uno@vetrina.co", "token-intentos");
            Long versionInicial = guardada.getVersion();

            assertThat(repository.registerFailedAttempt(guardada.getId())).isEqualTo(1);

            PlatformAccessRequest releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getVerificationAttempts()).isEqualTo(1);
            // Sin este movimiento, un save cargado antes del UPDATE reescribiria la
            // fila con su valor viejo y su WHERE version = ? casaria igual.
            assertThat(releida.getVersion()).isGreaterThan(versionInicial);
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @Sql(statements = "DELETE FROM platform_access_requests WHERE email = 'contador-agotado@vetrina.co'", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        @DisplayName("al quinto intento la solicitud queda bloqueada y el sexto no afecta ninguna fila")
        void el_sexto_intento_no_afecta_ninguna_fila() {
            PlatformAccessRequest guardada = emitir("contador-agotado@vetrina.co", "token-agotado");

            gastarIntentos(guardada.getId(), 5);

            // rowcount 0 es lo que el servicio traduce a 429. Si el WHERE perdiera su
            // «AND verification_attempts < max_attempts», el contador crecería sin
            // techo y no habría bloqueo: fuerza bruta libre sobre 10^6 combinaciones.
            assertThat(repository.registerFailedAttempt(guardada.getId())).isZero();

            PlatformAccessRequest releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getVerificationAttempts()).isEqualTo(5);
            assertThat(releida.isBlocked()).isTrue();
            assertThat(releida.remainingAttempts()).isZero();
        }
    }

    @Nested
    @DisplayName("applyDecision — la decision es el UPDATE, no el if")
    class Decision {

        @Test
        @DisplayName("aprueba una solicitud pendiente y escribe el par decision/decided_at")
        void aprueba_una_solicitud_pendiente() {
            PlatformAccessRequest guardada = emitir("ana@vetrina.co", "token-aprobable");
            LocalDateTime decidida = CREADA.plusHours(2);

            assertThat(repository.applyDecision(guardada.getId(), PlatformAccessDecision.APPROVED,
                    decidida)).isEqualTo(1);
            releerDesdeLaBase();

            PlatformAccessRequest releida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(releida.getDecision()).isEqualTo(PlatformAccessDecision.APPROVED);
            assertThat(releida.getDecidedAt()).isEqualTo(decidida);
            assertThat(releida.isDecided()).isTrue();
        }

        @Test
        @DisplayName("una segunda decision no afecta ninguna fila: aprobar y rechazar no se pisan")
        void una_segunda_decision_no_afecta_ninguna_fila() {
            PlatformAccessRequest guardada = emitir("ana@vetrina.co", "token-carrera");
            repository.applyDecision(guardada.getId(), PlatformAccessDecision.APPROVED,
                    CREADA.plusHours(2));
            releerDesdeLaBase();

            // Dos pestanas del mismo correo pueden llegar a la vez. rowcount 0 es lo
            // que impide que la segunda reescriba la decision de la primera.
            assertThat(repository.applyDecision(guardada.getId(), PlatformAccessDecision.REJECTED,
                    CREADA.plusHours(3))).isZero();
            releerDesdeLaBase();

            assertThat(repository.findById(guardada.getId()).orElseThrow().getDecision())
                    .isEqualTo(PlatformAccessDecision.APPROVED);
        }

        @Test
        @DisplayName("no decide una solicitud caducada")
        void no_decide_una_solicitud_caducada() {
            PlatformAccessRequest guardada = emitir("ana@vetrina.co", "token-vencido");
            releerDesdeLaBase();

            assertThat(repository.applyDecision(guardada.getId(), PlatformAccessDecision.APPROVED,
                    EXPIRA.plusMinutes(1))).isZero();
        }

        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @Sql(statements = "DELETE FROM platform_access_requests WHERE email = 'decision-quemada@vetrina.co'", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        @DisplayName("no decide una solicitud bloqueada: el bloqueo gana a todo")
        void no_decide_una_solicitud_bloqueada() {
            PlatformAccessRequest guardada = emitir("decision-quemada@vetrina.co", "token-quemado");
            gastarIntentos(guardada.getId(), 5);

            assertThat(repository.applyDecision(guardada.getId(), PlatformAccessDecision.APPROVED,
                    CREADA.plusHours(1))).isZero();
        }
    }

    @Nested
    @DisplayName("los CHECK de la tabla, que no existen en Java")
    class ConstraintsDeLaBase {

        @Test
        @DisplayName("rechaza un motivo de menos de 20 caracteres aunque el UPDATE lo intente")
        void rechaza_un_motivo_demasiado_corto() {
            PlatformAccessRequest guardada = emitir("ana@vetrina.co", "token-check-motivo");
            releerDesdeLaBase();

            assertThatThrownBy(() -> {
                entityManager.createNativeQuery(
                        "UPDATE platform_access_requests SET reason = 'corto' WHERE id = ?1")
                        .setParameter(1, guardada.getId()).executeUpdate();
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("rechaza una decision que no sea APPROVED ni REJECTED")
        void rechaza_una_decision_desconocida() {
            PlatformAccessRequest guardada = emitir("ana@vetrina.co", "token-check-decision");
            releerDesdeLaBase();

            assertThatThrownBy(() -> {
                entityManager.createNativeQuery("""
                        UPDATE platform_access_requests
                        SET decision = 'MAYBE', decided_at = NOW(6)
                        WHERE id = ?1
                        """).setParameter(1, guardada.getId()).executeUpdate();
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("rechaza una decision sin fecha: el par nulo tiene que cerrar")
        void rechaza_una_decision_sin_fecha() {
            PlatformAccessRequest guardada = emitir("ana@vetrina.co", "token-check-par");
            releerDesdeLaBase();

            assertThatThrownBy(() -> {
                entityManager.createNativeQuery("""
                        UPDATE platform_access_requests
                        SET decision = 'APPROVED'
                        WHERE id = ?1
                        """).setParameter(1, guardada.getId()).executeUpdate();
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }
    }
}
