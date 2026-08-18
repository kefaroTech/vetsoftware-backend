package com.vetsoftware.app.passwordreset.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia del token de restablecimiento contra MySQL real. La
 * entidad JPA no declara {@code @ManyToOne} (son columnas planas), pero el
 * schema real SI trae FK a {@code employees}/{@code companies}
 * ({@code fk_password_reset_tokens_company} y su equivalente de empleado): hace
 * falta sembrar la cadena de {@link SchemaSeed} antes de insertar, algo
 * invisible leyendo solo la entidad JPA.
 *
 * <p>
 * Lo que se comprueba aqui no existe en el codigo Java: la unicidad de
 * {@code token_hash} la impone un indice de la base, y el UPDATE masivo de
 * {@code consumeActiveForEmployee} depende de que el WHERE realmente filtre por
 * {@code employee_id} — con un doble del repositorio esa fuga (invalidar
 * sesiones de restablecimiento de otro empleado) daria verde siempre.
 */
@Import({JpaPasswordResetTokenRepository.class, PasswordResetTokenJpaMapper.class})
@DisplayName("JpaPasswordResetTokenRepository — token de un solo uso contra MySQL real")
class PasswordResetTokenPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPLOYEE_ID = SchemaSeed.EMPLOYEE_ID;
    private static final Long OTRO_EMPLEADO_ID = SchemaSeed.OTRO_EMPLOYEE_ID;
    private static final Long COMPANY_ID = SchemaSeed.COMPANY_ID;

    @Autowired
    private JpaPasswordResetTokenRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private PasswordResetToken emitir(Long employeeId, String tokenHash, LocalDateTime expira) {
        return repository.save(PasswordResetToken.issue(employeeId, COMPANY_ID, tokenHash, expira));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y buscar por hash conserva cada campo")
        void guardar_y_buscar_por_hash_conserva_cada_campo() {
            LocalDateTime expira = LocalDateTime.now().plusHours(1).withNano(0);
            PasswordResetToken guardado = emitir(EMPLOYEE_ID, "hash-unico-1", expira);

            Optional<PasswordResetToken> leido = repository.findByTokenHash("hash-unico-1");

            assertThat(leido).isPresent();
            PasswordResetToken token = leido.orElseThrow();
            assertThat(token.getId()).isEqualTo(guardado.getId());
            assertThat(token.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(token.getCompanyId()).isEqualTo(COMPANY_ID);
            assertThat(token.getExpiresAt()).isEqualTo(expira);
            assertThat(token.getConsumedAt()).isNull();
        }

        @Test
        @DisplayName("un hash que no existe no se encuentra")
        void un_hash_que_no_existe_no_se_encuentra() {
            assertThat(repository.findByTokenHash("hash-inexistente-" + System.nanoTime()))
                    .isEmpty();
        }

        @Test
        @DisplayName("el unique de token_hash impide dos tokens con el mismo hash")
        void el_unique_de_token_hash_impide_duplicados() {
            emitir(EMPLOYEE_ID, "hash-duplicado", LocalDateTime.now().plusHours(1));

            assertThatThrownBy(() -> emitir(OTRO_EMPLEADO_ID, "hash-duplicado",
                    LocalDateTime.now().plusHours(1)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("consumir y volver a guardar persiste consumedAt")
        void consumir_y_volver_a_guardar_persiste_consumed_at() {
            PasswordResetToken guardado = emitir(EMPLOYEE_ID, "hash-a-consumir",
                    LocalDateTime.now().plusHours(1));
            LocalDateTime consumidoEn = LocalDateTime.now().withNano(0);
            guardado.consume(consumidoEn);

            repository.save(guardado);

            PasswordResetToken releido = repository.findByTokenHash("hash-a-consumir")
                    .orElseThrow();
            assertThat(releido.getConsumedAt()).isEqualTo(consumidoEn);
        }
    }

    @Nested
    @DisplayName("consumeActiveForEmployee — invalidacion masiva")
    class ConsumeActiveForEmployee {

        @Test
        @DisplayName("consume todos los tokens vivos del empleado, sin tocar los de otro empleado")
        void consume_todos_los_tokens_vivos_del_empleado() {
            emitir(EMPLOYEE_ID, "hash-vivo-1", LocalDateTime.now().plusHours(1));
            emitir(EMPLOYEE_ID, "hash-vivo-2", LocalDateTime.now().plusHours(2));
            emitir(OTRO_EMPLEADO_ID, "hash-de-otro-empleado", LocalDateTime.now().plusHours(1));

            repository.consumeActiveForEmployee(EMPLOYEE_ID, COMPANY_ID, LocalDateTime.now());

            assertThat(repository.findByTokenHash("hash-vivo-1").orElseThrow().getConsumedAt())
                    .isNotNull();
            assertThat(repository.findByTokenHash("hash-vivo-2").orElseThrow().getConsumedAt())
                    .isNotNull();
            // El token del OTRO empleado no puede quedar afectado: si el UPDATE masivo
            // perdiera el filtro por employeeId, invalidaria sesiones de restablecimiento
            // de cuentas ajenas — justo el tipo de fuga que un doble del repositorio no
            // ve nunca.
            assertThat(repository.findByTokenHash("hash-de-otro-empleado").orElseThrow()
                    .getConsumedAt()).isNull();
        }

        @Test
        @DisplayName("con la empresa de otro tenant no consume ningun token")
        void con_otra_empresa_no_consume_nada() {
            // El token es de COMPANY_ID. Con OTRA_COMPANY_ID el AND del UPDATE no casa
            // ninguna fila, asi que el token sigue vivo.
            emitir(EMPLOYEE_ID, "hash-de-mi-empresa", LocalDateTime.now().plusHours(1));

            repository.consumeActiveForEmployee(EMPLOYEE_ID, SchemaSeed.OTRA_COMPANY_ID,
                    LocalDateTime.now());

            assertThat(
                    repository.findByTokenHash("hash-de-mi-empresa").orElseThrow().getConsumedAt())
                    .isNull();
        }

        @Test
        @DisplayName("no reescribe un token que ya estaba consumido con una fecha distinta")
        void no_reescribe_un_token_ya_consumido() {
            PasswordResetToken guardado = emitir(EMPLOYEE_ID, "hash-ya-consumido",
                    LocalDateTime.now().plusHours(1));
            LocalDateTime primerConsumo = LocalDateTime.now().minusMinutes(30).withNano(0);
            guardado.consume(primerConsumo);
            repository.save(guardado);

            repository.consumeActiveForEmployee(EMPLOYEE_ID, COMPANY_ID, LocalDateTime.now());

            assertThat(
                    repository.findByTokenHash("hash-ya-consumido").orElseThrow().getConsumedAt())
                    .isEqualTo(primerConsumo);
        }
    }
}
