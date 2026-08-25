package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaEntity;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El interruptor {@code platform.access-request.open}, que decide si el
 * formulario público de alta de superadministradores existe.
 *
 * <p>
 * <b>Es el único control de admisión del flujo entero.</b> Los seis endpoints
 * son anónimos y su desenlace es una cuenta con control total de la plataforma;
 * cuando este interruptor dice que no, el endpoint responde 404 y no hay
 * solicitud, ni token, ni correo al aprobador. De ahí que el criterio sea
 * <b>cerrado por defecto</b>: si la fila no existe, si su valor es otro, o si
 * el texto no es exactamente {@code true}, la respuesta es cerrado. Abrir por
 * error de configuración sería abrir la puerta de la plataforma.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPlatformAccessSwitchPort — el interruptor del formulario público")
class JpaPlatformAccessSwitchPortTest {

    @Mock
    private SystemConfigurationJpaRepository systemConfigurationJpaRepository;

    private JpaPlatformAccessSwitchPort crearPuerto() {
        return new JpaPlatformAccessSwitchPort(systemConfigurationJpaRepository);
    }

    /**
     * La entidad JPA se dobla —y no se construye— porque su constructor sin
     * argumentos es {@code protected}, que es lo que exige la convención del repo
     * para toda {@code @Entity}. No es una entidad de dominio: no tiene invariantes
     * que un doble pudiera saltarse, solo el par nombre/valor que aquí se lee.
     */
    private void dadoElValor(String valor) {
        SystemConfigurationJpaEntity fila = org.mockito.Mockito
                .mock(SystemConfigurationJpaEntity.class);
        when(fila.getValue()).thenReturn(valor);
        when(systemConfigurationJpaRepository
                .findByPropertyName(JpaPlatformAccessSwitchPort.PROPERTY_NAME))
                .thenReturn(Optional.of(fila));
    }

    @Nested
    @DisplayName("abierto")
    class Abierto {

        @ParameterizedTest
        @ValueSource(strings = {"true", "TRUE", "True", " true ", "\ttrue\n"})
        @DisplayName("solo el texto true, en cualquier caja y con espacios, abre el formulario")
        void solo_el_texto_true_abre_el_formulario(String valor) {
            dadoElValor(valor);

            assertThat(crearPuerto().isOpen()).isTrue();
        }

        @Test
        @DisplayName("consulta la propiedad por su nombre exacto")
        void consulta_la_propiedad_por_su_nombre_exacto() {
            dadoElValor("true");

            crearPuerto().isOpen();

            // Un nombre distinto devolveria vacio y cerraria el formulario en
            // silencio: el sintoma seria «nadie puede pedir acceso» sin causa
            // visible.
            verify(systemConfigurationJpaRepository)
                    .findByPropertyName("platform.access-request.open");
        }
    }

    @Nested
    @DisplayName("cerrado — por defecto y ante cualquier duda")
    class Cerrado {

        @Test
        @DisplayName("si la propiedad no existe, el formulario esta cerrado")
        void sin_propiedad_esta_cerrado() {
            when(systemConfigurationJpaRepository
                    .findByPropertyName(JpaPlatformAccessSwitchPort.PROPERTY_NAME))
                    .thenReturn(Optional.empty());

            // Cerrado por defecto: una base recien migrada no puede acunar
            // superadministradores hasta que alguien lo decida por escrito.
            assertThat(crearPuerto().isOpen()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"false", "FALSE", "0", "1", "yes", "si", "on", "enabled", "", "   ",
                "truthy", "true false"})
        @DisplayName("cualquier otro texto cierra el formulario, incluidos los que 'parecen' un si")
        void cualquier_otro_texto_cierra_el_formulario(String valor) {
            dadoElValor(valor);

            assertThat(crearPuerto().isOpen()).isFalse();
        }

        @Test
        @DisplayName("un valor nulo en la fila cierra el formulario en vez de reventar")
        void un_valor_nulo_cierra_el_formulario() {
            dadoElValor(null);

            // La columna es NOT NULL, pero el mapeo no lo garantiza en memoria: un
            // NullPointerException aqui saldria como 500 en un endpoint publico.
            assertThat(crearPuerto().isOpen()).isFalse();
        }
    }
}
