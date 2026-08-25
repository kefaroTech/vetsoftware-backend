package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.platformaccess.domain.PlatformAccessInvitation;
import com.vetsoftware.app.platformaccess.domain.PlatformAccessRequest;
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
 * Rodaja de persistencia de la invitación contra MySQL real.
 *
 * <p>
 * <b>Aquí vive la constraint más importante del modelo</b>, y no está en Java:
 * el índice único sobre la columna generada {@code consumed_request_id}.
 * Reemitir una invitación es un {@code INSERT}, así que una solicitud aprobada
 * puede tener varias invitaciones vivas a la vez; lo que impide que se
 * conviertan en <b>dos superadministradores</b> es que como mucho una llegue a
 * consumirse. Eso no se puede expresar con una lectura previa en Java —la
 * concurrencia se la come— y por tanto solo se puede probar contra la base.
 *
 * <p>
 * La entidad no declara {@code @ManyToOne} (son columnas planas, a propósito:
 * una asociación pondría este paquete a un salto de otra feature y encendería
 * las reglas de tenencia sobre toda la rodaja), pero el esquema real <b>sí</b>
 * trae FK a {@code system_users} con {@code ON DELETE RESTRICT}: hace falta
 * sembrar la cuenta antes de consumir, algo invisible leyendo solo la entidad
 * JPA.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPlatformAccessInvitationRepository — invitación de un solo uso contra MySQL real")
class PlatformAccessInvitationPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 3, 14, 9, 0);
    private static final LocalDateTime EXPIRA = CREADA.plusDays(7);
    private static final String MOTIVO = "Necesito acceso para operar la plataforma";

    @Autowired
    private JpaPlatformAccessInvitationRepository repository;

    @Autowired
    private JpaPlatformAccessRequestRepository requestRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    private static String hashDe(String semilla) {
        return String.format("%064x", semilla.hashCode() & 0xffffffffL);
    }

    private PlatformAccessRequest solicitudAprobada(String email, String semilla) {
        return requestRepository.save(PlatformAccessRequest.issue("Ana Ramirez", email, MOTIVO,
                hashDe(semilla), "$2a$10$hash-bcrypt-de-prueba", 5, CREADA, CREADA.plusHours(72)));
    }

    private PlatformAccessInvitation emitir(Long requestId, String semilla) {
        return repository
                .save(PlatformAccessInvitation.issue(requestId, hashDe(semilla), CREADA, EXPIRA));
    }

    private void releerDesdeLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Segunda cuenta de sistema, que {@code SchemaSeed} no siembra. Va por SQL
     * nativo para no depender del adaptador de otra feature dentro de esta rodaja.
     */
    private Long crearOtroUsuarioDeSistema() {
        Long id = SchemaSeed.SYSTEM_USER_ID + 1;
        entityManager.createNativeQuery("""
                INSERT INTO system_users (id, code, hash_password, created_date, enabled, version)
                VALUES (?1, 'SEED-SYSTEM-2', 'x', NOW(), true, 0)
                ON DUPLICATE KEY UPDATE id = id
                """).setParameter(1, id).executeUpdate();
        return id;
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y buscar por hash conserva cada campo y nace sin consumir")
        void guardar_y_buscar_por_hash_conserva_cada_campo() {
            PlatformAccessRequest solicitud = solicitudAprobada("ana@vetrina.co", "aprobacion-1");
            PlatformAccessInvitation guardada = emitir(solicitud.getId(), "invitacion-1");
            releerDesdeLaBase();

            PlatformAccessInvitation leida = repository.findByTokenHash(hashDe("invitacion-1"))
                    .orElseThrow();

            assertThat(leida.getId()).isEqualTo(guardada.getId());
            assertThat(leida.getAccessRequestId()).isEqualTo(solicitud.getId());
            assertThat(leida.getExpiresAt()).isEqualTo(EXPIRA);
            assertThat(leida.getCreatedDate()).isEqualTo(CREADA);
            assertThat(leida.getConsumedAt()).isNull();
            assertThat(leida.getSystemUserId()).isNull();
            assertThat(leida.isConsumed()).isFalse();
        }

        @Test
        @DisplayName("el unique del token impide dos invitaciones con el mismo hash")
        void el_unique_del_token_impide_duplicados() {
            PlatformAccessRequest solicitud = solicitudAprobada("ana@vetrina.co", "aprobacion-2");
            emitir(solicitud.getId(), "invitacion-repetida");
            releerDesdeLaBase();

            assertThatThrownBy(() -> {
                emitir(solicitud.getId(), "invitacion-repetida");
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("consume — el UPDATE condicional es la barrera del uso unico")
    class Consumo {

        @Test
        @DisplayName("consume la invitacion y la ata al usuario creado")
        void consume_y_ata_al_usuario_creado() {
            PlatformAccessRequest solicitud = solicitudAprobada("ana@vetrina.co", "aprobacion-3");
            PlatformAccessInvitation invitacion = emitir(solicitud.getId(), "invitacion-3");
            LocalDateTime consumida = CREADA.plusHours(5);

            assertThat(repository.consume(invitacion.getId(), SchemaSeed.SYSTEM_USER_ID, consumida))
                    .isEqualTo(1);
            releerDesdeLaBase();

            PlatformAccessInvitation releida = repository.findByTokenHash(hashDe("invitacion-3"))
                    .orElseThrow();
            assertThat(releida.getConsumedAt()).isEqualTo(consumida);
            assertThat(releida.getSystemUserId()).isEqualTo(SchemaSeed.SYSTEM_USER_ID);
            assertThat(releida.isConsumed()).isTrue();
        }

        @Test
        @DisplayName("un segundo consumo no afecta ninguna fila")
        void un_segundo_consumo_no_afecta_ninguna_fila() {
            PlatformAccessRequest solicitud = solicitudAprobada("ana@vetrina.co", "aprobacion-4");
            PlatformAccessInvitation invitacion = emitir(solicitud.getId(), "invitacion-4");
            repository.consume(invitacion.getId(), SchemaSeed.SYSTEM_USER_ID, CREADA.plusHours(5));
            releerDesdeLaBase();

            // rowcount 0 es lo que el servicio traduce a «invitacion ya consumida».
            // Si el WHERE perdiera su «AND consumed_at IS NULL», el mismo enlace
            // crearia una cuenta cada vez que se pulsara.
            assertThat(repository.consume(invitacion.getId(), SchemaSeed.SYSTEM_USER_ID,
                    CREADA.plusHours(6))).isZero();
        }
    }

    @Nested
    @DisplayName("uq_pai_consumed_request — una aprobacion, como mucho un superadministrador")
    class UnaSolaCuentaPorAprobacion {

        @Test
        @DisplayName("se pueden emitir varias invitaciones para la misma solicitud")
        void se_pueden_emitir_varias_invitaciones() {
            PlatformAccessRequest solicitud = solicitudAprobada("ana@vetrina.co", "aprobacion-5");

            emitir(solicitud.getId(), "invitacion-5a");
            emitir(solicitud.getId(), "invitacion-5b");
            releerDesdeLaBase();

            // Reemitir es un INSERT y no una reescritura del token_hash: eso es lo
            // que conserva el registro de que token se envio antes.
            assertThat(repository.findByTokenHash(hashDe("invitacion-5a"))).isPresent();
            assertThat(repository.findByTokenHash(hashDe("invitacion-5b"))).isPresent();
        }

        @Test
        @DisplayName("pero solo UNA puede consumirse: la segunda choca contra el indice unico")
        void solo_una_puede_consumirse() {
            PlatformAccessRequest solicitud = solicitudAprobada("ana@vetrina.co", "aprobacion-6");
            PlatformAccessInvitation primera = emitir(solicitud.getId(), "invitacion-6a");
            PlatformAccessInvitation segunda = emitir(solicitud.getId(), "invitacion-6b");
            releerDesdeLaBase();

            repository.consume(primera.getId(), SchemaSeed.SYSTEM_USER_ID, CREADA.plusHours(5));
            releerDesdeLaBase();

            // Una cuenta DISTINTA, para que lo que rompa sea uq_pai_consumed_request
            // —«una aprobacion, como mucho un usuario»— y no uq_pai_system_user, que
            // cubre la otra mitad («un usuario, como mucho una invitacion»).
            Long otraCuenta = crearOtroUsuarioDeSistema();

            // Esta es la constraint que impide que dos enlaces vivos de la misma
            // aprobacion produzcan DOS superadministradores. Sin ella, reemitir seria
            // duplicar el privilegio.
            assertThatThrownBy(() -> {
                repository.consume(segunda.getId(), otraCuenta, CREADA.plusHours(6));
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }
    }
}
