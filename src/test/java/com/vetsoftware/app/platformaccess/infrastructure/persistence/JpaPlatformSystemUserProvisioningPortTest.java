package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaEntity;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaMapper;
import com.vetsoftware.app.systemuser.infrastructure.persistence.SystemUserJpaRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El adaptador que crea la cuenta con control total de la plataforma. Es el
 * único hecho irreversible de todo el flujo.
 *
 * <p>
 * Lo que se fija aquí son las dos decisiones que lo hacen seguro y que un
 * refactor podría deshacer sin que nada más lo notara:
 *
 * <ul>
 * <li><b>Las dos comprobaciones de unicidad miran también las cuentas
 * deshabilitadas.</b> El borrado de este proyecto es lógico: si
 * {@code emailTaken} solo viera las activas, deshabilitar un superadministrador
 * y volver a pedir acceso con su correo crearía una segunda fila que el
 * {@code UNIQUE (email)} rechaza — el alta muere con una violación de clave
 * dentro de la transacción, la invitación queda a medias y el error que ve la
 * persona no dice nada.</li>
 * <li><b>La cuenta se construye con {@code SystemUser.provision}</b>, que es
 * quien decide el estado inicial. Aquí no se aceptan permisos, ni roles, ni
 * banderas: el filtro de autenticación concede {@code ROLE_SYSTEM} a toda
 * cuenta de sistema sin mirar permisos, así que cualquier parámetro extra sería
 * una escalada.</li>
 * </ul>
 *
 * <p>
 * <b>El mapper es real y no un doble, a propósito.</b> Es una función pura sin
 * dependencias, y desde que el adaptador dejó de importar
 * {@code systemuser.domain.SystemUser} —cruce de dominios que el vertical
 * slicing prohíbe y que ArchUnit no podía ver— la construcción vive en
 * {@code SystemUserJpaMapper.toJpaProvisioned}. Con un doble en su sitio, las
 * invariantes del punto anterior dejarían de evaluarse y estos tests pasarían
 * sin comprobar nada. Lo que se captura, por eso, es la entidad que llega a
 * {@code save}: exactamente lo que se va a escribir en la base.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPlatformSystemUserProvisioningPort — el alta de la cuenta de plataforma")
class JpaPlatformSystemUserProvisioningPortTest {

    private static final LocalDateTime CREADA = LocalDateTime.of(2026, 3, 14, 12, 0);

    @Mock
    private SystemUserJpaRepository systemUserJpaRepository;

    private final SystemUserJpaMapper systemUserJpaMapper = new SystemUserJpaMapper();

    private JpaPlatformSystemUserProvisioningPort crearPuerto() {
        return new JpaPlatformSystemUserProvisioningPort(systemUserJpaRepository,
                systemUserJpaMapper);
    }

    private SystemUserJpaEntity capturarLaGuardada() {
        ArgumentCaptor<SystemUserJpaEntity> captor = ArgumentCaptor
                .forClass(SystemUserJpaEntity.class);
        verify(systemUserJpaRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("unicidad — mirando también lo deshabilitado")
    class Unicidad {

        @Test
        @DisplayName("emailTaken es cierto cuando hay al menos una cuenta, activa o no")
        void email_taken_es_cierto_con_al_menos_una_cuenta() {
            when(systemUserJpaRepository.countByEmailIncludingDisabled("ana@vetrina.co"))
                    .thenReturn(1L);

            assertThat(crearPuerto().emailTaken("ana@vetrina.co")).isTrue();
        }

        @Test
        @DisplayName("emailTaken es falso cuando no hay ninguna")
        void email_taken_es_falso_sin_cuentas() {
            when(systemUserJpaRepository.countByEmailIncludingDisabled("ana@vetrina.co"))
                    .thenReturn(0L);

            assertThat(crearPuerto().emailTaken("ana@vetrina.co")).isFalse();
        }

        @Test
        @DisplayName("codeTaken es cierto cuando el codigo ya existe, activo o no")
        void code_taken_es_cierto_con_el_codigo_existente() {
            when(systemUserJpaRepository.countByCodeIncludingDisabled("SYS-ANARAMIREZ"))
                    .thenReturn(2L);

            assertThat(crearPuerto().codeTaken("SYS-ANARAMIREZ")).isTrue();
        }

        @Test
        @DisplayName("codeTaken es falso cuando el codigo esta libre")
        void code_taken_es_falso_con_el_codigo_libre() {
            when(systemUserJpaRepository.countByCodeIncludingDisabled("SYS-ANARAMIREZ-2"))
                    .thenReturn(0L);

            assertThat(crearPuerto().codeTaken("SYS-ANARAMIREZ-2")).isFalse();
        }

        @Test
        @DisplayName("consultar la unicidad no escribe nada")
        void consultar_la_unicidad_no_escribe_nada() {
            when(systemUserJpaRepository.countByEmailIncludingDisabled("ana@vetrina.co"))
                    .thenReturn(0L);

            crearPuerto().emailTaken("ana@vetrina.co");

            verify(systemUserJpaRepository, never()).save(any());
            verify(systemUserJpaRepository).countByEmailIncludingDisabled("ana@vetrina.co");
        }
    }

    @Nested
    @DisplayName("provision — el hecho irreversible")
    class Provision {

        @Test
        @DisplayName("construye la cuenta con el codigo, el correo, el nombre y el hash recibidos")
        void construye_la_cuenta_con_los_datos_recibidos() {
            SystemUserJpaEntity guardada = mock(SystemUserJpaEntity.class);
            when(systemUserJpaRepository.save(any(SystemUserJpaEntity.class))).thenReturn(guardada);
            when(guardada.getId()).thenReturn(9001L);

            Long id = crearPuerto().provision("SYS-ANARAMIREZ", "ana@vetrina.co", "Ana Ramirez",
                    "$2a$12$hash", CREADA);

            SystemUserJpaEntity fila = capturarLaGuardada();
            assertThat(fila.getCode()).isEqualTo("SYS-ANARAMIREZ");
            assertThat(fila.getEmail()).isEqualTo("ana@vetrina.co");
            assertThat(fila.getFullName()).isEqualTo("Ana Ramirez");
            assertThat(fila.getHashPassword()).isEqualTo("$2a$12$hash");
            assertThat(fila.getCreatedDate()).isEqualTo(CREADA);
            assertThat(id).isEqualTo(9001L);
        }

        @Test
        @DisplayName("la cuenta nace habilitada, con authVersion 0 y sin un solo permiso")
        void la_cuenta_nace_con_el_estado_inicial_de_provision() {
            SystemUserJpaEntity guardada = mock(SystemUserJpaEntity.class);
            when(systemUserJpaRepository.save(any(SystemUserJpaEntity.class))).thenReturn(guardada);
            when(guardada.getId()).thenReturn(9001L);

            crearPuerto().provision("SYS-ANARAMIREZ", "ana@vetrina.co", "Ana Ramirez",
                    "$2a$12$hash", CREADA);

            // Lo decide SystemUser.provision, y sigue decidiendolo aunque el adaptador
            // ya no importe el dominio: la construccion vive en el mapper de la rodaja
            // dueña del modelo.
            SystemUserJpaEntity fila = capturarLaGuardada();
            assertThat(fila.isEnabled()).isTrue();
            assertThat(fila.getAuthVersion()).isZero();
        }

        @Test
        @DisplayName("la cuenta nace sin id: lo pone la base, nunca el llamador")
        void la_cuenta_nace_sin_id() {
            SystemUserJpaEntity guardada = mock(SystemUserJpaEntity.class);
            when(systemUserJpaRepository.save(any(SystemUserJpaEntity.class))).thenReturn(guardada);
            when(guardada.getId()).thenReturn(9001L);

            crearPuerto().provision("SYS-ANARAMIREZ", "ana@vetrina.co", "Ana Ramirez",
                    "$2a$12$hash", CREADA);

            assertThat(capturarLaGuardada().getId()).isNull();
        }

        @Test
        @DisplayName("devuelve el id de la fila GUARDADA, no el de la que se mando a guardar")
        void devuelve_el_id_de_la_fila_guardada() {
            SystemUserJpaEntity guardada = mock(SystemUserJpaEntity.class);
            when(systemUserJpaRepository.save(any(SystemUserJpaEntity.class))).thenReturn(guardada);
            when(guardada.getId()).thenReturn(9001L);

            // Ese id es lo que se escribe en platform_access_invitations.system_user_id
            // y lo unico que ata la cuenta creada con la solicitud que la autorizo.
            assertThat(crearPuerto().provision("SYS-ANARAMIREZ", "ana@vetrina.co", "Ana Ramirez",
                    "$2a$12$hash", CREADA)).isEqualTo(9001L);
        }

        @Test
        @DisplayName("un correo vacio no llega a la base: lo rechaza el dominio antes de mapear")
        void un_correo_vacio_no_llega_a_la_base() {
            assertThatThrownBy(() -> crearPuerto().provision("SYS-ANARAMIREZ", "  ", "Ana Ramirez",
                    "$2a$12$hash", CREADA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email is required");

            verifyNoInteractions(systemUserJpaRepository);
        }

        @Test
        @DisplayName("un nombre vacio tampoco llega a la base")
        void un_nombre_vacio_no_llega_a_la_base() {
            assertThatThrownBy(() -> crearPuerto().provision("SYS-ANARAMIREZ", "ana@vetrina.co",
                    null, "$2a$12$hash", CREADA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fullName is required");

            verifyNoInteractions(systemUserJpaRepository);
        }
    }
}
