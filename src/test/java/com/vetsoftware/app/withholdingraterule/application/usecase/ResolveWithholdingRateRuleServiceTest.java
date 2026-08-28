package com.vetsoftware.app.withholdingraterule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.application.port.out.WithholdingRateRuleRepository;
import com.vetsoftware.app.withholdingraterule.domain.NoEffectiveWithholdingRateRuleException;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import com.vetsoftware.app.withholdingraterule.testsupport.WithholdingRateRuleMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La consulta por la que existe la feature, y el sitio donde el fallo caro deja
 * de ser silencioso.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveWithholdingRateRuleService — la tarifa vigente para un supuesto")
class ResolveWithholdingRateRuleServiceTest {

    private static final Long EMPRESA_DEL_TOKEN = 77L;
    private static final LocalDate EL_DIA_DE_LA_FACTURA = LocalDate.of(2026, 6, 15);

    /**
     * Reloj congelado en un dia DISTINTO del de la factura. Los dos tienen que
     * diferir: con la misma fecha, un service que ignorara el {@code on} recibido y
     * usara siempre «hoy» pasaria todos los casos en verde.
     */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-09-02T10:15:00Z"),
            ZoneOffset.UTC);
    private static final LocalDate HOY = LocalDate.of(2026, 9, 2);

    @Mock
    private WithholdingRateRuleRepository repository;

    private ResolveWithholdingRateRuleService service;

    @BeforeEach
    void construirConRelojCongelado() {
        // Nada de @InjectMocks aqui: Mockito inyectaria un Clock mockeado, y un
        // mock de Clock devuelve null en instant(), asi que LocalDate.now(clock)
        // reventaria en el unico caso que interesa probar.
        service = new ResolveWithholdingRateRuleService(repository, RELOJ);
    }

    @Nested
    @DisplayName("Resolucion")
    class Resolucion {

        @Test
        @DisplayName("devuelve la tarifa de ICA con su 6,9 por mil intacto")
        void devuelve_la_tarifa_de_ica_con_su_por_mil_intacto() {
            when(repository.findEffective(WithholdingType.ICA, ServiceNature.CONSULTING,
                    WithholdingRateRuleMother.BOGOTA, EL_DIA_DE_LA_FACTURA))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.ica()));

            WithholdingRateRuleDto tarifa = service.resolve(WithholdingType.ICA,
                    ServiceNature.CONSULTING, WithholdingRateRuleMother.BOGOTA,
                    EL_DIA_DE_LA_FACTURA, EMPRESA_DEL_TOKEN);

            // De este numero sale cuanto se espera que retenga el cliente. Con dos
            // decimales se retendria casi un uno por ciento de menos por factura.
            assertThat(tarifa.ratePercent()).isEqualByComparingTo("0.69");
            assertThat(tarifa.ratePercent().scale()).isEqualTo(6);
            assertThat(tarifa.municipalityCode()).isEqualTo("11001");
        }

        @Test
        @DisplayName("una retencion nacional se consulta con el municipio en nulo")
        void una_retencion_nacional_se_consulta_con_el_municipio_en_nulo() {
            // El nulo llega hasta el adaptador, que es quien lo traduce al
            // centinela de municipality_key. El service no lo toca.
            when(repository.findEffective(WithholdingType.INCOME_TAX,
                    ServiceNature.TECHNICAL_SERVICE, null, EL_DIA_DE_LA_FACTURA))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.nacional()));

            WithholdingRateRuleDto tarifa = service.resolve(WithholdingType.INCOME_TAX,
                    ServiceNature.TECHNICAL_SERVICE, null, EL_DIA_DE_LA_FACTURA, EMPRESA_DEL_TOKEN);

            assertThat(tarifa.municipalityCode()).isNull();
            assertThat(tarifa.ratePercent()).isEqualByComparingTo("11.00");
        }

        @Test
        @DisplayName("traslada los cuatro criterios del supuesto al repositorio sin cruzarlos")
        void traslada_los_cuatro_criterios_sin_cruzarlos() {
            when(repository.findEffective(WithholdingType.VAT, ServiceNature.SOFTWARE_LICENSING,
                    WithholdingRateRuleMother.MEDELLIN, EL_DIA_DE_LA_FACTURA))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.nacional()));

            service.resolve(WithholdingType.VAT, ServiceNature.SOFTWARE_LICENSING,
                    WithholdingRateRuleMother.MEDELLIN, EL_DIA_DE_LA_FACTURA, EMPRESA_DEL_TOKEN);

            verify(repository).findEffective(WithholdingType.VAT, ServiceNature.SOFTWARE_LICENSING,
                    "05001", EL_DIA_DE_LA_FACTURA);
        }
    }

    @Nested
    @DisplayName("El dia por defecto")
    class ElDiaPorDefecto {

        @Test
        @DisplayName("sin fecha resuelve con el reloj inyectado, no con el del sistema")
        void sin_fecha_resuelve_con_el_reloj_inyectado() {
            // Que el dia por defecto lo ponga el caso de uso y no el controller no
            // es reparto de responsabilidades: un LocalDate.now() en la capa web es
            // una fecha que ningun test puede fijar —el caso pasaria o fallaria
            // segun el dia de ejecucion— y RELOJ_INYECTADO_EN_VEZ_DE_NOW rompe el
            // build por ello.
            when(repository.findEffective(WithholdingType.INCOME_TAX,
                    ServiceNature.TECHNICAL_SERVICE, null, HOY))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.nacional()));

            service.resolve(WithholdingType.INCOME_TAX, ServiceNature.TECHNICAL_SERVICE, null, null,
                    EMPRESA_DEL_TOKEN);

            verify(repository).findEffective(WithholdingType.INCOME_TAX,
                    ServiceNature.TECHNICAL_SERVICE, null, HOY);
        }

        @Test
        @DisplayName("con fecha usa la del hecho economico y NO la de hoy")
        void con_fecha_usa_la_del_hecho_economico() {
            // La otra mitad, y la que importa para la cartera: recalcular una
            // factura de junio con la tarifa de septiembre descuadra un saldo ya
            // cerrado. El reloj esta congelado en otro dia justamente para que un
            // service que ignorara el parametro se ponga rojo aqui.
            when(repository.findEffective(WithholdingType.INCOME_TAX,
                    ServiceNature.TECHNICAL_SERVICE, null, EL_DIA_DE_LA_FACTURA))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.nacional()));

            service.resolve(WithholdingType.INCOME_TAX, ServiceNature.TECHNICAL_SERVICE, null,
                    EL_DIA_DE_LA_FACTURA, EMPRESA_DEL_TOKEN);

            verify(repository).findEffective(WithholdingType.INCOME_TAX,
                    ServiceNature.TECHNICAL_SERVICE, null, EL_DIA_DE_LA_FACTURA);
        }

        @Test
        @DisplayName("sin tarifa para hoy, el mensaje del 404 nombra el dia del reloj")
        void sin_tarifa_para_hoy_el_mensaje_nombra_el_dia_del_reloj() {
            when(repository.findEffective(WithholdingType.VAT, ServiceNature.CONSULTING, null, HOY))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(WithholdingType.VAT, ServiceNature.CONSULTING,
                    null, null, EMPRESA_DEL_TOKEN))
                    .isInstanceOf(NoEffectiveWithholdingRateRuleException.class)
                    .hasMessageContaining("on 2026-09-02");
        }
    }

    @Nested
    @DisplayName("Cuando no hay tarifa")
    class CuandoNoHayTarifa {

        @Test
        @DisplayName("LANZA en vez de devolver vacio: el fallo caro no puede ser un cero silencioso")
        void lanza_en_vez_de_devolver_vacio() {
            // Este caso es la razon de ser de la excepcion. Si service_nature
            // divergiera entre catalog_items y esta tabla, la busqueda saldria
            // vacia, la retencion esperada seria cero y NO HABRIA ERROR: la
            // factura se emite, el cliente retiene igual y gira de menos, y nadie
            // se entera hasta cuadrar la cartera. Devolver un Optional vacio desde
            // aqui reproduciria exactamente esa forma de fallar.
            when(repository.findEffective(WithholdingType.ICA, ServiceNature.CONSULTING,
                    WithholdingRateRuleMother.MEDELLIN, EL_DIA_DE_LA_FACTURA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(WithholdingType.ICA, ServiceNature.CONSULTING,
                    WithholdingRateRuleMother.MEDELLIN, EL_DIA_DE_LA_FACTURA, EMPRESA_DEL_TOKEN))
                    .isInstanceOf(NoEffectiveWithholdingRateRuleException.class);
        }

        @Test
        @DisplayName("el mensaje nombra el supuesto entero para que se sepa que falta configurar")
        void el_mensaje_nombra_el_supuesto_entero() {
            when(repository.findEffective(WithholdingType.ICA, ServiceNature.CONSULTING,
                    WithholdingRateRuleMother.MEDELLIN, EL_DIA_DE_LA_FACTURA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(WithholdingType.ICA, ServiceNature.CONSULTING,
                    WithholdingRateRuleMother.MEDELLIN, EL_DIA_DE_LA_FACTURA, EMPRESA_DEL_TOKEN))
                    .hasMessage("No effective withholding rate rule for ICA/CONSULTING"
                            + " in municipality 05001 on 2026-06-15");
        }

        @Test
        @DisplayName("en una nacional el mensaje escribe el centinela y no la palabra null")
        void en_una_nacional_el_mensaje_escribe_el_centinela() {
            when(repository.findEffective(WithholdingType.VAT, ServiceNature.SOFTWARE_LICENSING,
                    null, EL_DIA_DE_LA_FACTURA)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.resolve(WithholdingType.VAT, ServiceNature.SOFTWARE_LICENSING,
                            null, EL_DIA_DE_LA_FACTURA, EMPRESA_DEL_TOKEN))
                    .hasMessageContaining("in municipality - on");
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la empresa autoriza en el puerto y NO llega al repositorio")
        void la_empresa_autoriza_y_no_llega_al_repositorio() {
            // La tarifa depende del supuesto fiscal y no del cliente. El companyId
            // va el ultimo en la firma justamente por eso: los cuatro primeros
            // describen que se consulta; el quinto, quien pregunta.
            when(repository.findEffective(WithholdingType.ICA, ServiceNature.CONSULTING,
                    WithholdingRateRuleMother.BOGOTA, EL_DIA_DE_LA_FACTURA))
                    .thenReturn(Optional.of(WithholdingRateRuleMother.ica()));

            service.resolve(WithholdingType.ICA, ServiceNature.CONSULTING,
                    WithholdingRateRuleMother.BOGOTA, EL_DIA_DE_LA_FACTURA, EMPRESA_DEL_TOKEN);

            verify(repository).findEffective(WithholdingType.ICA, ServiceNature.CONSULTING, "11001",
                    EL_DIA_DE_LA_FACTURA);
            verifyNoMoreInteractions(repository);
        }
    }
}
