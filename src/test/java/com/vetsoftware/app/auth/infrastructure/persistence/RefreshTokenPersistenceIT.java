package com.vetsoftware.app.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository.NewRefreshToken;
import com.vetsoftware.app.auth.application.port.out.RefreshTokenRepository.StoredRefreshToken;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del refresh token contra MySQL real.
 *
 * <p>
 * Tres cosas de este adaptador solo existen en la base, y un doble del
 * repositorio no puede falsearlas porque responderia lo que el propio test le
 * hubiera dicho:
 *
 * <ul>
 * <li><b>El unico global de {@code token_hash}</b>, sin {@code company_id}. Es
 * lo correcto —el hash es la identidad del token en toda la plataforma— y es
 * exactamente lo que impide que dos sesiones compartan credencial.
 * <li><b>El {@code AND r.revoked = false} de {@code revokeById}.</b> Da la
 * idempotencia del sellado: {@code revoked_at} se escribe una sola vez. Y no es
 * cosmetico —lo dice el javadoc de {@code StoredRefreshToken}—: la deteccion de
 * reuso decide por la <em>antiguedad</em> de esa marca si lo que ve es una
 * carrera entre pestañas o un robo. Si la segunda llamada la refrescara, un
 * token robado hace dos dias pareceria recien revocado y pasaria por carrera.
 * <li><b>El par exacto de {@code revokeAllForSubject}.</b> Sujeto Y tipo: un
 * empleado y un usuario de sistema pueden compartir id numerico, asi que
 * revocar solo por {@code subject_id} tumbaria las sesiones de un tercero.
 * </ul>
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaRefreshTokenRepository — ciclo de vida del refresh token contra MySQL real")
class RefreshTokenPersistenceIT extends AbstractDataJpaTest {

    private static final String HASH_UNO = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String HASH_DOS = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
    private static final String HASH_TRES = "1111111111111111222222222222222233333333333333334444444444444444";
    private static final String HASH_CUATRO = "aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbccccccccccccccccdddddddddddddddd";

    private static final String EMPLEADO = "EMPLOYEE";
    private static final String SISTEMA = "SYSTEM_USER";
    private static final Long SUJETO = SchemaSeed.EMPLOYEE_ID;
    private static final Long OTRO_SUJETO = SchemaSeed.OTRO_EMPLOYEE_ID;

    private static final LocalDateTime EXPIRA = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

    /** Instante reconocible: ninguna ejecucion del reloj puede producirlo. */
    private static final String SELLO_ANTIGUO = "2020-01-01 00:00:00";

    @Autowired
    private JpaRefreshTokenRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    private void vaciarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    private void guardar(String hash, Long subjectId, String subjectType) {
        repository.save(new NewRefreshToken(hash, subjectId, subjectType, 1L, EXPIRA));
        vaciarContexto();
    }

    private StoredRefreshToken leer(String hash) {
        return repository.findByHash(hash).orElseThrow();
    }

    /**
     * Devuelve {@code revoked_at} formateado, o {@code "NULO"}. Se lee como texto a
     * proposito: el tipo Java que devuelve un {@code DATETIME} en consulta nativa
     * depende del driver, y aqui lo que importa es el instante exacto.
     */
    private String selloDeRevocacion(Long id) {
        return (String) entityManager
                .createNativeQuery("SELECT COALESCE(DATE_FORMAT(revoked_at,"
                        + " '%Y-%m-%d %H:%i:%s'), 'NULO') FROM refresh_tokens WHERE id = :id")
                .setParameter("id", id).getSingleResult();
    }

    private void fijarSelloAntiguo(Long id) {
        entityManager
                .createNativeQuery("UPDATE refresh_tokens SET revoked_at = :sello WHERE id = :id")
                .setParameter("sello", SELLO_ANTIGUO).setParameter("id", id).executeUpdate();
        vaciarContexto();
    }

    private long contar(String condicion, Long id) {
        return ((Number) entityManager
                .createNativeQuery(
                        "SELECT COUNT(*) FROM refresh_tokens WHERE id = :id AND " + condicion)
                .setParameter("id", id).getSingleResult()).longValue();
    }

    @Nested
    @DisplayName("Guardado")
    class Guardado {

        @Test
        @DisplayName("guarda y relee los siete campos del token almacenado")
        void guarda_y_relee_los_siete_campos() {
            repository.save(new NewRefreshToken(HASH_UNO, SUJETO, EMPLEADO, 5L, EXPIRA));
            vaciarContexto();

            assertThat(repository.findByHash(HASH_UNO)).get().satisfies(leido -> {
                assertThat(leido.id()).isNotNull();
                assertThat(leido.subjectId()).isEqualTo(SUJETO);
                assertThat(leido.subjectType()).isEqualTo(EMPLEADO);
                assertThat(leido.authVersion()).isEqualTo(5L);
                assertThat(leido.expiresAt()).isEqualTo(EXPIRA);
                assertThat(leido.revoked()).isFalse();
                assertThat(leido.revokedAt()).isNull();
            });
        }

        @Test
        @DisplayName("sella la fecha de creación, que es NOT NULL y el adaptador pone a mano")
        void sella_la_fecha_de_creacion() {
            // El adaptador escribe createdDate = LocalDateTime.now(). No se puede afirmar
            // el instante -no hay Clock inyectado, ver el informe-, pero si que la columna
            // NOT NULL queda escrita: sin esa linea el INSERT revienta.
            guardar(HASH_UNO, SUJETO, EMPLEADO);

            assertThat(contar("created_date IS NOT NULL", leer(HASH_UNO).id())).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("El hash es único en toda la plataforma")
    class HashUnicoGlobal {

        @Test
        @DisplayName("dos tokens con el mismo hash chocan contra el índice único")
        void dos_tokens_con_el_mismo_hash_chocan() {
            // Sin company_id en el indice, y es deliberado: el hash ES la credencial. Dos
            // filas con el mismo hash harian que presentar un token resolviera a dos
            // sesiones distintas y que revocar una dejara viva la otra.
            guardar(HASH_UNO, SUJETO, EMPLEADO);

            assertThatThrownBy(() -> repository
                    .save(new NewRefreshToken(HASH_UNO, OTRO_SUJETO, EMPLEADO, 1L, EXPIRA)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("dos sujetos distintos con hashes distintos conviven sin estorbarse")
        void dos_sujetos_distintos_con_hashes_distintos_conviven() {
            // La otra cara: el unico es del hash, no del sujeto. Un mismo empleado tiene
            // varias sesiones vivas -movil y escritorio- y ninguna desplaza a la otra.
            guardar(HASH_UNO, SUJETO, EMPLEADO);
            guardar(HASH_DOS, OTRO_SUJETO, EMPLEADO);
            guardar(HASH_TRES, SUJETO, EMPLEADO);

            assertThat(leer(HASH_UNO).subjectId()).isEqualTo(SUJETO);
            assertThat(leer(HASH_DOS).subjectId()).isEqualTo(OTRO_SUJETO);
            assertThat(leer(HASH_TRES).subjectId()).isEqualTo(SUJETO);
        }
    }

    @Nested
    @DisplayName("Lectura por hash bajo bloqueo pesimista")
    class LecturaPorHash {

        @Test
        @DisplayName("un token vivo llega sin revocar y sin sello de revocación")
        void un_token_vivo_llega_sin_revocar_y_sin_sello() {
            guardar(HASH_UNO, SUJETO, EMPLEADO);

            assertThat(repository.findByHash(HASH_UNO)).get().satisfies(leido -> {
                assertThat(leido.revoked()).isFalse();
                assertThat(leido.revokedAt()).isNull();
            });
        }

        @Test
        @DisplayName("un hash que nadie emitió devuelve vacío")
        void un_hash_que_nadie_emitio_devuelve_vacio() {
            assertThat(repository.findByHash(HASH_CUATRO)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Revocación individual e idempotencia del sello")
    class Revocacion {

        @Test
        @DisplayName("la primera revocación marca revoked y sella la fecha")
        void la_primera_revocacion_marca_y_sella() {
            guardar(HASH_UNO, SUJETO, EMPLEADO);
            Long id = leer(HASH_UNO).id();

            repository.revokeById(id);
            vaciarContexto();

            assertThat(repository.findByHash(HASH_UNO)).get().satisfies(leido -> {
                assertThat(leido.revoked()).isTrue();
                assertThat(leido.revokedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("la segunda revocación NO vuelve a mover la fecha: es lo que distingue una carrera de un robo")
        void la_segunda_revocacion_no_vuelve_a_mover_la_fecha() {
            // El sello se retrasa a un instante reconocible para que la segunda llamada
            // no pueda "coincidir por el reloj": dos UPDATE en el mismo segundo darian el
            // mismo CURRENT_TIMESTAMP y el test pasaria sin probar nada.
            //
            // Lo que fija: el AND r.revoked = false. Sin el, la segunda presentacion de
            // un token robado hace dos dias refrescaria revoked_at, la deteccion de reuso
            // lo leeria como recien revocado y lo tomaria por una carrera entre pestañas
            // -que se perdona- en vez de por el robo que es.
            guardar(HASH_UNO, SUJETO, EMPLEADO);
            Long id = leer(HASH_UNO).id();
            repository.revokeById(id);
            vaciarContexto();
            fijarSelloAntiguo(id);

            repository.revokeById(id);
            vaciarContexto();

            assertThat(selloDeRevocacion(id)).isEqualTo(SELLO_ANTIGUO);
            assertThat(contar("revoked = true", id)).isEqualTo(1L);
        }

        @Test
        @DisplayName("revocar un token no toca a los demás del mismo sujeto")
        void revocar_un_token_no_toca_a_los_demas() {
            guardar(HASH_UNO, SUJETO, EMPLEADO);
            guardar(HASH_DOS, SUJETO, EMPLEADO);
            Long id = leer(HASH_UNO).id();

            repository.revokeById(id);
            vaciarContexto();

            assertThat(leer(HASH_DOS).revoked()).isFalse();
            assertThat(leer(HASH_DOS).revokedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Revocación masiva por sujeto")
    class RevocacionMasiva {

        @Test
        @DisplayName("solo caen los tokens del par sujeto+tipo exacto: los demás siguen vivos")
        void solo_caen_los_del_par_exacto() {
            // Los cuatro cuadrantes. El id 940 puede ser a la vez un empleado y un
            // usuario de sistema -son secuencias independientes-, asi que revocar solo
            // por subject_id echaria de la plataforma a un tercero sin relacion.
            guardar(HASH_UNO, SUJETO, EMPLEADO);
            guardar(HASH_DOS, SUJETO, SISTEMA);
            guardar(HASH_TRES, OTRO_SUJETO, EMPLEADO);
            guardar(HASH_CUATRO, OTRO_SUJETO, SISTEMA);

            repository.revokeAllForSubject(SUJETO, EMPLEADO);
            vaciarContexto();

            assertThat(leer(HASH_UNO).revoked()).isTrue();
            assertThat(leer(HASH_DOS).revoked()).isFalse();
            assertThat(leer(HASH_TRES).revoked()).isFalse();
            assertThat(leer(HASH_CUATRO).revoked()).isFalse();
        }

        @Test
        @DisplayName("tumba TODAS las sesiones vivas del sujeto, no solo una")
        void tumba_todas_las_sesiones_vivas_del_sujeto() {
            guardar(HASH_UNO, SUJETO, EMPLEADO);
            guardar(HASH_DOS, SUJETO, EMPLEADO);
            guardar(HASH_TRES, SUJETO, EMPLEADO);

            repository.revokeAllForSubject(SUJETO, EMPLEADO);
            vaciarContexto();

            assertThat(leer(HASH_UNO).revokedAt()).isNotNull();
            assertThat(leer(HASH_DOS).revokedAt()).isNotNull();
            assertThat(leer(HASH_TRES).revokedAt()).isNotNull();
        }

        @Test
        @DisplayName("no vuelve a sellar lo que ya estaba revocado: el AND revoked = false también aplica aquí")
        void no_vuelve_a_sellar_lo_que_ya_estaba_revocado() {
            guardar(HASH_UNO, SUJETO, EMPLEADO);
            guardar(HASH_DOS, SUJETO, EMPLEADO);
            Long yaRevocado = leer(HASH_UNO).id();
            repository.revokeById(yaRevocado);
            vaciarContexto();
            fijarSelloAntiguo(yaRevocado);

            repository.revokeAllForSubject(SUJETO, EMPLEADO);
            vaciarContexto();

            assertThat(selloDeRevocacion(yaRevocado)).isEqualTo(SELLO_ANTIGUO);
            assertThat(leer(HASH_DOS).revoked()).isTrue();
        }
    }
}
