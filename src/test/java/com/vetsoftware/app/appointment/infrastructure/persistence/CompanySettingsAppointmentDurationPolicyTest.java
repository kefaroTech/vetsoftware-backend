package com.vetsoftware.app.appointment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.companysettings.infrastructure.persistence.CompanySettingJpaEntity;
import com.vetsoftware.app.companysettings.infrastructure.persistence.CompanySettingJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Duracion por defecto de las citas leida de {@code company_settings} (BE-17).
 *
 * <p>
 * <b>Lo que de verdad se prueba aqui es el parseo defensivo.</b> La columna
 * {@code value} es un {@code VARCHAR(255)} de texto libre que se escribe por el
 * PUT generico de ajustes: no hay validacion en la escritura, asi que un admin
 * puede dejar «treinta», «-5», «0» o «999999» y quien lo paga es el
 * agendamiento. Cada uno de esos valores tiene que caer al respaldo de 30
 * minutos en vez de tumbar la creacion de citas de toda la empresa.
 *
 * <p>
 * <b>Por que se mockea la entidad JPA.</b> {@code CompanySettingJpaEntity}
 * declara su constructor sin argumentos como {@code protected} —lo exige JPA— y
 * vive en otro paquete raiz, asi que este test no puede instanciarla. Solo se
 * lee un accesor de ella, de modo que un doble no oculta ninguna invariante: la
 * logica bajo prueba es la del parseo, no la de la entidad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanySettingsAppointmentDurationPolicy — el ajuste de empresa y su parseo")
class CompanySettingsAppointmentDurationPolicyTest {

    private static final String CLAVE = "appointment.default_duration_minutes";
    private static final Long EMPRESA = 9L;
    private static final int RESPALDO = 30;

    @Mock
    private CompanySettingJpaRepository companySettingJpaRepository;
    @Mock
    private CompanySettingJpaEntity ajuste;
    @InjectMocks
    private CompanySettingsAppointmentDurationPolicy policy;

    private void ajusteConValor(String value) {
        when(ajuste.getValue()).thenReturn(value);
        when(companySettingJpaRepository.findByCompanyIdAndPropertyName(EMPRESA, CLAVE))
                .thenReturn(Optional.of(ajuste));
    }

    @Nested
    @DisplayName("Camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("devuelve los minutos configurados por la empresa")
        void devuelve_los_minutos_configurados() {
            ajusteConValor("45");

            assertThat(policy.defaultDurationMinutes(EMPRESA)).isEqualTo(45);
        }

        @Test
        @DisplayName("tolera los espacios alrededor del numero")
        void tolera_los_espacios_alrededor_del_numero() {
            ajusteConValor("  60  ");

            assertThat(policy.defaultDurationMinutes(EMPRESA)).isEqualTo(60);
        }

        @Test
        @DisplayName("acepta los extremos validos: 1 minuto y el techo del dominio")
        void acepta_los_extremos_validos() {
            ajusteConValor(String.valueOf(Appointment.MAX_DURATION_MINUTES));

            assertThat(policy.defaultDurationMinutes(EMPRESA))
                    .isEqualTo(Appointment.MAX_DURATION_MINUTES);
        }

        @Test
        @DisplayName("un minuto es un ajuste valido, por raro que sea")
        void un_minuto_es_un_ajuste_valido() {
            ajusteConValor("1");

            assertThat(policy.defaultDurationMinutes(EMPRESA)).isOne();
        }
    }

    @Nested
    @DisplayName("Ausencia de ajuste")
    class SinAjuste {

        @Test
        @DisplayName("sin fila en company_settings devuelve el respaldo de 30 minutos")
        void sin_fila_devuelve_el_respaldo() {
            when(companySettingJpaRepository.findByCompanyIdAndPropertyName(EMPRESA, CLAVE))
                    .thenReturn(Optional.empty());

            assertThat(policy.defaultDurationMinutes(EMPRESA)).isEqualTo(RESPALDO);
        }

        @Test
        @DisplayName("con companyId nulo devuelve el respaldo sin consultar la base")
        void con_company_id_nulo_devuelve_el_respaldo_sin_consultar() {
            assertThat(policy.defaultDurationMinutes(null)).isEqualTo(RESPALDO);

            verifyNoInteractions(companySettingJpaRepository);
        }
    }

    /**
     * El PUT de ajustes no valida el contenido: todos estos valores son escribibles
     * hoy desde la consola de administracion de la empresa.
     */
    @Nested
    @DisplayName("Parseo defensivo del texto libre")
    class ParseoDefensivo {

        @ParameterizedTest(name = "value = \"{0}\"")
        @ValueSource(strings = {"treinta", "30 minutos", "45m", "abc", "3.5", "30,5", "1e2", "--30",
                "+", "0x1E"})
        @DisplayName("un valor no numerico cae al respaldo en vez de tumbar el agendamiento")
        void un_valor_no_numerico_cae_al_respaldo(String valor) {
            ajusteConValor(valor);

            assertThat(policy.defaultDurationMinutes(EMPRESA)).isEqualTo(RESPALDO);
        }

        @ParameterizedTest(name = "value = \"{0}\"")
        @ValueSource(strings = {"0", "-5", "-1", "-720"})
        @DisplayName("un valor no positivo cae al respaldo: una cita de duracion cero o negativa no existe")
        void un_valor_no_positivo_cae_al_respaldo(String valor) {
            ajusteConValor(valor);

            // Un cero aqui haria que endAt == startAt y el intervalo semiabierto
            // [inicio, inicio) no se cruzaria jamas con nada: el bloqueo desapareceria.
            assertThat(policy.defaultDurationMinutes(EMPRESA)).isEqualTo(RESPALDO);
        }

        @ParameterizedTest(name = "value = \"{0}\"")
        @ValueSource(strings = {"721", "999999", "2147483647"})
        @DisplayName("un valor por encima del techo del dominio cae al respaldo")
        void un_valor_por_encima_del_techo_cae_al_respaldo(String valor) {
            ajusteConValor(valor);

            // La consulta de solapes acota su ventana con MAX_DURATION_MINUTES; aceptar
            // un default mayor abriria un rango que el indice ya no poda.
            assertThat(policy.defaultDurationMinutes(EMPRESA)).isEqualTo(RESPALDO);
        }

        @Test
        @DisplayName("un desbordamiento de int no revienta: cae al respaldo")
        void un_desbordamiento_de_int_cae_al_respaldo() {
            ajusteConValor("99999999999999");

            assertThat(policy.defaultDurationMinutes(EMPRESA)).isEqualTo(RESPALDO);
        }

        @ParameterizedTest(name = "value = \"{0}\"")
        @ValueSource(strings = {"", "   ", "\t"})
        @DisplayName("un valor vacio o en blanco cae al respaldo")
        void un_valor_en_blanco_cae_al_respaldo(String valor) {
            ajusteConValor(valor);

            assertThat(policy.defaultDurationMinutes(EMPRESA)).isEqualTo(RESPALDO);
        }

        @Test
        @DisplayName("un valor nulo en la columna cae al respaldo")
        void un_valor_nulo_cae_al_respaldo() {
            ajusteConValor(null);

            assertThat(policy.defaultDurationMinutes(EMPRESA)).isEqualTo(RESPALDO);
        }
    }
}
