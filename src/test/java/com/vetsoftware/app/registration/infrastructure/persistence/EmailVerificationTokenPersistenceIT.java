package com.vetsoftware.app.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.registration.domain.EmailVerificationToken;
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
 * Rodaja de persistencia del token de verificacion de correo contra MySQL real.
 *
 * <p>
 * <b>El caso que justifica la rodaja entera esta en
 * {@link FechaDeCreacionInmutable}.</b> {@code EmailVerificationTokenJpaMapper}
 * <em>no</em> copia {@code createdDate} al ir del dominio a la entidad —el
 * dominio ni siquiera tiene ese campo—, asi que al consumir el token la entidad
 * detachada llega con {@code createdDate = null}. Lo unico que impide que ese
 * {@code null} llegue a una columna {@code NOT NULL} es el
 * {@code updatable = false} de la anotacion, que deja la columna fuera de todo
 * {@code UPDATE}. El comentario de la entidad describe un defecto que ya
 * ocurrio; hasta ahora ninguna prueba lo fijaba, y quitar ese atributo "por
 * limpieza" volveria a romper la verificacion de correo de cada dueño que se
 * registra —el paso final del alta— sin que nada mas se queje.
 *
 * <p>
 * El unico de {@code token_hash} es global y tambien es deliberado: el hash es
 * la identidad del enlace que viaja en el correo.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaEmailVerificationTokenRepository — token de verificación contra MySQL real")
class EmailVerificationTokenPersistenceIT extends AbstractDataJpaTest {

    private static final String HASH_UNO = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String HASH_DOS = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
    private static final String HASH_INEXISTENTE = "9999999999999999888888888888888877777777777777776666666666666666";

    private static final LocalDateTime EXPIRA = LocalDateTime.of(2026, 12, 31, 23, 59, 59);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 24, 10, 30, 0);

    @Autowired
    private JpaEmailVerificationTokenRepository repository;

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

    private EmailVerificationToken emitir(String hash) {
        return EmailVerificationToken.issue(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID, hash,
                EXPIRA);
    }

    /**
     * Lee {@code created_date} como texto: el tipo Java que devuelve un
     * {@code DATETIME} en consulta nativa depende del driver, y lo que hay que
     * comparar es el instante exacto antes y despues del consumo.
     */
    private String fechaDeCreacion(Long id) {
        return (String) entityManager
                .createNativeQuery("SELECT COALESCE(DATE_FORMAT(created_date,"
                        + " '%Y-%m-%d %H:%i:%s'), 'NULO')"
                        + " FROM email_verification_tokens WHERE id = :id")
                .setParameter("id", id).getSingleResult();
    }

    @Nested
    @DisplayName("Ida y vuelta por el mapper")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el token y lo relee por su hash con todos sus campos")
        void guarda_el_token_y_lo_relee_por_su_hash() {
            EmailVerificationToken guardado = repository.save(emitir(HASH_UNO));
            vaciarContexto();

            assertThat(guardado.getId()).isNotNull();
            assertThat(repository.findByTokenHash(HASH_UNO)).get().satisfies(leido -> {
                assertThat(leido.getId()).isEqualTo(guardado.getId());
                assertThat(leido.getEmployeeId()).isEqualTo(SchemaSeed.EMPLOYEE_ID);
                assertThat(leido.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(leido.getTokenHash()).isEqualTo(HASH_UNO);
                assertThat(leido.getExpiresAt()).isEqualTo(EXPIRA);
                assertThat(leido.getConsumedAt()).isNull();
            });
        }

        @Test
        @DisplayName("un hash que nadie emitió devuelve vacío")
        void un_hash_que_nadie_emitio_devuelve_vacio() {
            repository.save(emitir(HASH_UNO));
            vaciarContexto();

            assertThat(repository.findByTokenHash(HASH_INEXISTENTE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("El hash es único en toda la plataforma")
    class HashUnicoGlobal {

        @Test
        @DisplayName("dos tokens con el mismo hash chocan contra el índice único")
        void dos_tokens_con_el_mismo_hash_chocan() {
            // El unico no lleva company_id, y es correcto: el hash viaja en el enlace del
            // correo y tiene que resolver a un solo alta. Dos filas iguales harian que el
            // mismo enlace verificara la cuenta equivocada.
            repository.save(emitir(HASH_UNO));
            vaciarContexto();

            assertThatThrownBy(() -> repository.save(emitir(HASH_UNO)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("hashes distintos para el mismo empleado conviven: se puede reenviar el correo")
        void hashes_distintos_para_el_mismo_empleado_conviven() {
            repository.save(emitir(HASH_UNO));
            repository.save(emitir(HASH_DOS));
            vaciarContexto();

            assertThat(repository.findByTokenHash(HASH_UNO)).isPresent();
            assertThat(repository.findByTokenHash(HASH_DOS)).isPresent();
        }
    }

    @Nested
    @DisplayName("La fecha de creación sobrevive al consumo")
    class FechaDeCreacionInmutable {

        @Test
        @DisplayName("consumir el token NO borra ni cambia su created_date, que el mapper no copia")
        void consumir_el_token_no_cambia_su_created_date() {
            // El defecto que esto fija, paso a paso: toJpa no copia createdDate, asi que
            // la entidad que llega al segundo save la trae a null. Si created_date no
            // fuera updatable = false, ese null entraria en el UPDATE contra una columna
            // NOT NULL y la verificacion de correo -el ultimo paso del auto-registro-
            // moriria con una violacion de integridad.
            EmailVerificationToken guardado = repository.save(emitir(HASH_UNO));
            vaciarContexto();
            Long id = guardado.getId();
            String creadoAlEmitir = fechaDeCreacion(id);

            EmailVerificationToken porConsumir = repository.findByTokenHash(HASH_UNO).orElseThrow();
            porConsumir.consume(AHORA);
            repository.save(porConsumir);
            vaciarContexto();

            assertThat(creadoAlEmitir).isNotEqualTo("NULO");
            assertThat(fechaDeCreacion(id)).isEqualTo(creadoAlEmitir);
        }

        @Test
        @DisplayName("y el consumo sí queda escrito: la fila recuerda cuándo se usó")
        void el_consumo_si_queda_escrito() {
            // La otra mitad: que created_date no se toque no puede lograrse a costa de
            // que el UPDATE no escriba nada. Sin consumed_at persistido, el token seria
            // reutilizable indefinidamente.
            repository.save(emitir(HASH_UNO));
            vaciarContexto();
            EmailVerificationToken porConsumir = repository.findByTokenHash(HASH_UNO).orElseThrow();
            porConsumir.consume(AHORA);

            repository.save(porConsumir);
            vaciarContexto();

            assertThat(repository.findByTokenHash(HASH_UNO)).get()
                    .satisfies(leido -> assertThat(leido.getConsumedAt()).isEqualTo(AHORA));
        }

        @Test
        @DisplayName("el @PrePersist sella la fecha aunque el dominio no la conozca")
        void el_pre_persist_sella_la_fecha() {
            EmailVerificationToken guardado = repository.save(emitir(HASH_UNO));
            vaciarContexto();

            assertThat(fechaDeCreacion(guardado.getId())).isNotEqualTo("NULO");
        }
    }
}
