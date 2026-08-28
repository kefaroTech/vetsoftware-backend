package com.vetsoftware.app.entitlement.application.usecase;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.CITAS;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.CITAS_POSTERIORES;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.FIRMA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.MASCOTAS;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.SUBSCRIPTION_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.TERMINALES_POSTERIORES;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.USUARIOS;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contadorExistente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.relojFijo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.LimitDenialPort;
import com.vetsoftware.app.entitlement.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.entitlement.application.port.out.OverageAllowancePort;
import com.vetsoftware.app.entitlement.application.port.out.OverageChargePort;
import com.vetsoftware.app.entitlement.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityLimitExceededException;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityNotFoundException;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityUnderflowException;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdjustCompanyCapacityUsageService — mover el consumo sin carreras")
class AdjustCompanyCapacityUsageServiceTest {

    private static final String CENTINELA = PeriodKey.SENTINEL;

    @Mock
    private CompanyCapacityRepository repository;
    @Mock
    private LimitDimensionQueryPort limitDimensionQueryPort;
    @Mock
    private SubscriptionQueryPort subscriptionQueryPort;
    @Mock
    private LimitDenialPort limitDenialPort;
    @Mock
    private OverageAllowancePort overageAllowancePort;
    @Mock
    private OverageChargePort overageChargePort;

    private AdjustCompanyCapacityUsageService service;

    /**
     * Cableado a mano y no con {@code @InjectMocks}: el reloj no es un puerto y
     * mockearlo daria un {@code Clock} que no sabe decir la hora. Va fijo, que es
     * lo unico que permite afirmar sobre la fecha con la que nace una fila.
     */
    @BeforeEach
    void cablearServicio() {
        service = new AdjustCompanyCapacityUsageService(repository, limitDimensionQueryPort,
                subscriptionQueryPort, limitDenialPort, overageAllowancePort, overageChargePort,
                relojFijo());
    }

    private static AdjustCompanyCapacityUsageCommand alta() {
        return new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "USER", 1);
    }

    @Nested
    @DisplayName("El eje sale del catálogo, no de una lista cerrada")
    class ElEjeSaleDelCatalogo {

        @BeforeEach
        void ejeSembrado() {
            when(limitDimensionQueryPort.findByCode("ANIMAL")).thenReturn(Optional.of(MASCOTAS));
        }

        /**
         * La razon de ser de #629. {@code ANIMAL} no existia como constante de Java:
         * con el enumerado de cuatro valores, contar mascotas exigia editar el enum, el
         * {@code CHECK} del esquema y desplegar. Esta prueba solo compila si nombrar un
         * eje es pasar su codigo.
         */
        @Test
        @DisplayName("empezar a contar un eje nuevo no exige tocar código")
        void empezar_a_contar_un_eje_nuevo_no_exige_tocar_codigo() {
            when(repository.addUsage(COMPANY_ID, MASCOTAS.id(), CENTINELA, 1)).thenReturn(1);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, MASCOTAS.id(), CENTINELA))
                    .thenReturn(Optional.of(contadorExistente(60L, MASCOTAS, 100, 41)));

            CompanyCapacityDto contador = service
                    .execute(new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "ANIMAL", 1));

            assertThat(contador.dimensionCode()).isEqualTo("ANIMAL");
            assertThat(contador.measureKind()).isEqualTo("CUMULATIVE");
        }
    }

    @Nested
    @DisplayName("La clave de periodo nunca va vacía (R-LIMIT-05)")
    class ClaveDePeriodo {

        /**
         * Un eje de flujo tiene un contador por periodo. Si el servidor se inventara
         * uno por defecto, el consumo se repartiria entre dos filas segun quien lo
         * escribiera, y ninguna de las dos diria la verdad.
         */
        @Test
        @DisplayName("un eje de flujo sin clave de periodo se rechaza en vez de inventarse una")
        void un_eje_de_flujo_sin_clave_de_periodo_se_rechaza() {
            when(limitDimensionQueryPort.findByCode("APPOINTMENT")).thenReturn(Optional.of(CITAS));

            assertThatThrownBy(() -> service
                    .execute(new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "APPOINTMENT", 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("FLOW dimension needs an explicit period key");

            verify(repository, never()).addUsage(org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("un eje que no es de flujo rechaza que le pasen un periodo")
        void un_eje_que_no_es_de_flujo_rechaza_que_le_pasen_periodo() {
            when(limitDimensionQueryPort.findByCode("USER")).thenReturn(Optional.of(USUARIOS));

            assertThatThrownBy(() -> service.execute(
                    new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "USER", "2026-03", 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not accept a period key");
        }

        @Test
        @DisplayName("un eje de flujo consume contra la fila de su propio periodo")
        void un_eje_de_flujo_consume_contra_la_fila_de_su_periodo() {
            when(limitDimensionQueryPort.findByCode("APPOINTMENT")).thenReturn(Optional.of(CITAS));
            when(repository.addUsage(COMPANY_ID, CITAS.id(), "2026-03", 1)).thenReturn(1);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, CITAS.id(), "2026-03"))
                    .thenReturn(Optional
                            .of(new CompanyCapacity(70L, COMPANY_ID, CITAS, PeriodKey.of("2026-03"),
                                    200, 12, SUBSCRIPTION_ID, AHORA, null, AHORA.minusDays(5))));

            CompanyCapacityDto contador = service.execute(
                    new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "APPOINTMENT", "2026-03", 1));

            assertThat(contador.periodKey()).isEqualTo("2026-03");
        }
    }

    @Nested
    @DisplayName("Cero filas afectadas: tres causas, tres mensajes")
    class CeroFilas {

        @BeforeEach
        void usuariosSembrado() {
            when(limitDimensionQueryPort.findByCode("USER")).thenReturn(Optional.of(USUARIOS));
        }

        @Test
        @DisplayName("devuelve el contador ya movido")
        void devuelve_el_contador_ya_movido() {
            when(repository.addUsage(COMPANY_ID, USUARIOS.id(), CENTINELA, 1)).thenReturn(1);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, USUARIOS.id(), CENTINELA))
                    .thenReturn(Optional
                            .of(new CompanyCapacity(31L, COMPANY_ID, USUARIOS, PeriodKey.sentinel(),
                                    5, 3, SUBSCRIPTION_ID, AHORA, null, AHORA.minusDays(90))));

            CompanyCapacityDto contador = service.execute(alta());

            assertThat(contador.usedQuantity()).isEqualTo(3);
            assertThat(contador.exhausted()).isFalse();
        }

        @Test
        @DisplayName("si la empresa no tiene contratado ese eje, lo dice con nombre y apellido")
        void sin_contador_lo_dice_con_nombre_y_apellido() {
            when(repository.addUsage(COMPANY_ID, USUARIOS.id(), CENTINELA, 1)).thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, USUARIOS.id(), CENTINELA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(alta()))
                    .isInstanceOf(CompanyCapacityNotFoundException.class)
                    .hasMessageContaining("USER");
        }

        @Test
        @DisplayName("un descuento que dejaria el consumo en negativo se rechaza")
        void un_descuento_que_dejaria_negativo_se_rechaza() {
            AdjustCompanyCapacityUsageCommand baja = new AdjustCompanyCapacityUsageCommand(
                    COMPANY_ID, "USER", -4);
            when(repository.addUsage(COMPANY_ID, USUARIOS.id(), CENTINELA, -4)).thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, USUARIOS.id(), CENTINELA))
                    .thenReturn(Optional.of(contadorExistente(31L, USUARIOS, 5, 1)));

            assertThatThrownBy(() -> service.execute(baja))
                    .isInstanceOf(CompanyCapacityUnderflowException.class)
                    .hasMessageContaining("below zero").hasMessageContaining("used 1")
                    .hasMessageContaining("requested delta -4");
        }

        @Test
        @DisplayName("un alta que supera el límite contratado informa límite, uso y delta")
        void un_alta_que_supera_el_limite_contratado_informa_el_detalle() {
            AdjustCompanyCapacityUsageCommand altaDeDos = new AdjustCompanyCapacityUsageCommand(
                    COMPANY_ID, "USER", 2);
            when(repository.addUsage(COMPANY_ID, USUARIOS.id(), CENTINELA, 2)).thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, USUARIOS.id(), CENTINELA))
                    .thenReturn(Optional.of(contadorExistente(31L, USUARIOS, 5, 4)));

            assertThatThrownBy(() -> service.execute(altaDeDos))
                    .isInstanceOf(CompanyCapacityLimitExceededException.class)
                    .hasMessageContaining("limit 5").hasMessageContaining("used 4")
                    .hasMessageContaining("requested delta 2");
        }

        /**
         * <b>El portazo se escribe, no solo se lanza.</b>
         *
         * <p>
         * Antes de esto, negar por cupo lanzaba la excepcion y no dejaba ni una fila:
         * el hecho existia en la bitacora como tabla, con su caso de uso escrito y su
         * prueba de integracion pasando, pero <em>nadie lo llamaba nunca</em>. La
         * consecuencia no es un log que falta: es que no hay prueba ninguna que
         * ensenarle a quien reclama que nunca se le aviso, y que la senal comercial mas
         * util del producto --quien choca contra su techo-- no se recoge.
         *
         * <p>
         * Se afirma que se escribe <strong>antes</strong> de lanzar, y con los tres
         * numeros del momento: techo, consumo y cuanto se pidio.
         */
        @Test
        @DisplayName("negar la creacion por cupo deja escrito el hecho con los tres numeros"
                + " del momento, y no solo lanza la excepcion")
        void negar_por_cupo_deja_escrito_el_hecho_y_no_solo_lanza_la_excepcion() {
            AdjustCompanyCapacityUsageCommand altaDeDos = new AdjustCompanyCapacityUsageCommand(
                    COMPANY_ID, "USER", 2);
            when(repository.addUsage(COMPANY_ID, USUARIOS.id(), CENTINELA, 2)).thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, USUARIOS.id(), CENTINELA))
                    .thenReturn(Optional.of(contadorExistente(31L, USUARIOS, 5, 4)));

            assertThatThrownBy(() -> service.execute(altaDeDos))
                    .isInstanceOf(CompanyCapacityLimitExceededException.class);

            verify(limitDenialPort).limitDenied(COMPANY_ID, USUARIOS.id(), 5, 4, 2);
        }

        /** Un alta que cabe no deja rastro en la bitacora: solo se registra el no. */
        @Test
        @DisplayName("un alta que cabe no escribe ningun portazo")
        void un_alta_que_cabe_no_escribe_ningun_portazo() {
            when(repository.addUsage(COMPANY_ID, USUARIOS.id(), CENTINELA, 1)).thenReturn(1);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, USUARIOS.id(), CENTINELA))
                    .thenReturn(Optional.of(contadorExistente(31L, USUARIOS, 5, 2)));

            service.execute(alta());

            verifyNoInteractions(limitDenialPort);
        }
    }

    @Nested
    @DisplayName("R-LIMIT-04 · el cupo de flujo se reinicia, no se cierra")
    class ReinicioDelCupoDeFlujo {

        private static final String MARZO = "2026-03";
        private static final String ABRIL = "2026-04";

        /**
         * El caso violador, tal como lo escribe el catalogo de reglas.
         *
         * <p>
         * Antes de esto, a las 00:00 del 1 de abril la clave pasaba a {@code 2026-04},
         * no habia fila, el {@code UPDATE} afectaba a cero filas y el servicio lo leia
         * como «no contratado» — <strong>indistinguible de haber topado con el
         * techo</strong>. La agenda quedaba bloqueada al 100 % hasta que alguien
         * recalculara a mano, y el recalculo tampoco la habria creado porque deriva del
         * contrato y el contrato no habla de periodos.
         *
         * <p>
         * «Sin que corra ningun proceso» es literal: no hay barrido que la cree. Nace
         * en el primer consumo del periodo, heredando el techo ya resuelto del periodo
         * anterior.
         */
        @Test
        @DisplayName("el 1 de abril nace la fila 2026-04 del cupo de citas sin que corra ningún proceso")
        void el_1_de_abril_nace_la_fila_2026_04_del_cupo_de_citas_sin_que_corra_ningun_proceso() {
            when(limitDimensionQueryPort.findByCode("APPOINTMENT")).thenReturn(Optional.of(CITAS));
            // Primer intento: la fila de abril todavia no existe.
            when(repository.addUsage(COMPANY_ID, CITAS.id(), ABRIL, 1)).thenReturn(0, 1);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, CITAS.id(), ABRIL))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(new CompanyCapacity(71L, COMPANY_ID, CITAS,
                            PeriodKey.of(ABRIL), 200, 1, SUBSCRIPTION_ID, AHORA, null, AHORA)));
            when(repository.openPeriod(COMPANY_ID, CITAS.id(), ABRIL, AHORA)).thenReturn(1);

            CompanyCapacityDto contador = service.execute(
                    new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "APPOINTMENT", ABRIL, 1));

            assertThat(contador.periodKey()).isEqualTo(ABRIL);
            assertThat(contador.limitQuantity()).isEqualTo(200);
            assertThat(contador.usedQuantity()).isEqualTo(1);
            assertThat(contador.uncapped()).isFalse();
            verify(repository).openPeriod(COMPANY_ID, CITAS.id(), ABRIL, AHORA);
        }

        /**
         * La otra mitad de la regla: el nacimiento se intenta <strong>una vez</strong>.
         * Si despues de nacer la fila el movimiento sigue sin entrar, la causa ya no es
         * la ausencia de fila sino el techo, y eso tiene que salir como tope alcanzado
         * y no como un bucle ni como «no contratado».
         */
        @Test
        @DisplayName("si el periodo nace y aun así se topa el techo, el error es el del tope")
        void el_periodo_nace_y_el_tope_se_informa_como_tope() {
            when(limitDimensionQueryPort.findByCode("APPOINTMENT")).thenReturn(Optional.of(CITAS));
            when(repository.addUsage(COMPANY_ID, CITAS.id(), ABRIL, 1)).thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, CITAS.id(), ABRIL))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(new CompanyCapacity(71L, COMPANY_ID, CITAS,
                            PeriodKey.of(ABRIL), 40, 40, SUBSCRIPTION_ID, AHORA, null, AHORA)));
            when(repository.openPeriod(COMPANY_ID, CITAS.id(), ABRIL, AHORA)).thenReturn(1);

            assertThatThrownBy(() -> service.execute(
                    new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "APPOINTMENT", ABRIL, 1)))
                    .isInstanceOf(CompanyCapacityLimitExceededException.class)
                    .hasMessageContaining("limit 40");

            verify(repository, org.mockito.Mockito.times(1)).openPeriod(COMPANY_ID, CITAS.id(),
                    ABRIL, AHORA);
        }

        /**
         * Sin periodo anterior no hay techo que heredar: la serie no existe todavia y
         * quien la abre es el recalculo. No se inventa una fila con techo cero, que
         * seria peor que el fallo original.
         */
        @Test
        @DisplayName("sin periodo anterior del que heredar, no se inventa una fila")
        void sin_periodo_anterior_no_se_inventa_una_fila() {
            when(limitDimensionQueryPort.findByCode("APPOINTMENT")).thenReturn(Optional.of(CITAS));
            when(repository.addUsage(COMPANY_ID, CITAS.id(), MARZO, 1)).thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, CITAS.id(), MARZO))
                    .thenReturn(Optional.empty());
            when(repository.openPeriod(COMPANY_ID, CITAS.id(), MARZO, AHORA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(
                    new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "APPOINTMENT", MARZO, 1)))
                    .isInstanceOf(CompanyCapacityNotFoundException.class);

            verify(repository, org.mockito.Mockito.times(1)).addUsage(COMPANY_ID, CITAS.id(), MARZO,
                    1);
        }

        /**
         * El contraste de la regla: las existencias no tienen periodo que reiniciar, y
         * dar de baja devuelve la plaza <em>en el acto</em> sobre la misma fila del
         * centinela. Un eje de existencias no puede acabar en el camino del nacimiento.
         */
        @Test
        @DisplayName("dar de baja un empleado libera su plaza en el acto")
        void dar_de_baja_un_empleado_libera_su_plaza_en_el_acto() {
            when(limitDimensionQueryPort.findByCode("USER")).thenReturn(Optional.of(USUARIOS));
            when(repository.addUsage(COMPANY_ID, USUARIOS.id(), CENTINELA, -1)).thenReturn(1);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, USUARIOS.id(), CENTINELA))
                    .thenReturn(Optional.of(contadorExistente(31L, USUARIOS, 5, 2)));

            CompanyCapacityDto contador = service
                    .execute(new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "USER", -1));

            assertThat(contador.periodKey()).isEqualTo(CENTINELA);
            assertThat(contador.usedQuantity()).isEqualTo(2);
            verify(repository, never()).openPeriod(org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyLong(),
                    org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("R-LIMIT-08 · sin fila no siempre significa techo cero (D-74)")
    class EjeNacidoDespuesDeLaFirma {

        /**
         * El caso violador que escribe el catalogo, palabra por palabra.
         *
         * <p>
         * Se añade el eje {@code APPOINTMENT} con un techo de fabrica. Las empresas que
         * firmaron antes no tienen fila de ese eje. Con la regla vieja eso se leia como
         * techo cero y las agendas dejaban de poder crear citas: ninguna de esas
         * empresas acepto ese limite, se lo aplico una fila de catalogo insertada
         * <em>despues</em> de su firma. El daño es proporcional al exito del producto.
         */
        @Test
        @DisplayName("añadir el eje APPOINTMENT en abril no deja bloqueadas las 40 agendas de contratos firmados en enero")
        void anadir_el_eje_APPOINTMENT_en_abril_no_deja_bloqueadas_las_40_agendas_de_contratos_firmados_en_enero() {
            when(limitDimensionQueryPort.findByCode("APPOINTMENT"))
                    .thenReturn(Optional.of(CITAS_POSTERIORES));
            when(repository.addUsage(COMPANY_ID, CITAS_POSTERIORES.id(), "2026-04", 1))
                    .thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, CITAS_POSTERIORES.id(),
                    "2026-04")).thenReturn(Optional.empty());
            when(repository.openPeriod(COMPANY_ID, CITAS_POSTERIORES.id(), "2026-04", AHORA))
                    .thenReturn(0);
            when(subscriptionQueryPort.findContractSignedOnByCompanyId(COMPANY_ID))
                    .thenReturn(Optional.of(FIRMA));

            CompanyCapacityDto contador = service.execute(
                    new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "APPOINTMENT", "2026-04", 1));

            assertThat(contador.uncapped()).isTrue();
            assertThat(contador.dimensionCode()).isEqualTo("APPOINTMENT");
            assertThat(contador.exhausted()).isFalse();
        }

        /**
         * Lo mismo sobre un eje de existencias: la regla es del eje, no del periodo.
         */
        @Test
        @DisplayName("un eje de existencias posterior a la firma tampoco pone techo")
        void un_eje_de_existencias_posterior_a_la_firma_tampoco_pone_techo() {
            when(limitDimensionQueryPort.findByCode("TERMINAL"))
                    .thenReturn(Optional.of(TERMINALES_POSTERIORES));
            when(repository.addUsage(COMPANY_ID, TERMINALES_POSTERIORES.id(), CENTINELA, 1))
                    .thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, TERMINALES_POSTERIORES.id(),
                    CENTINELA)).thenReturn(Optional.empty());
            when(subscriptionQueryPort.findContractSignedOnByCompanyId(COMPANY_ID))
                    .thenReturn(Optional.of(FIRMA));

            CompanyCapacityDto contador = service
                    .execute(new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "TERMINAL", 1));

            assertThat(contador.uncapped()).isTrue();
        }

        /**
         * La otra mitad de R-LIMIT-08, y la que impide que el arreglo se convierta en
         * una barra libre: un eje que <em>ya existia</em> cuando se firmo sigue
         * significando techo cero cuando no hay fila. Sin esta prueba, un error de
         * signo en la comparacion abriria todos los cupos del sistema sin que nada
         * fallara.
         */
        @Test
        @DisplayName("un eje existente sin fila para una empresa da techo cero, no ilimitado")
        void un_eje_existente_sin_fila_para_una_empresa_da_techo_cero_no_ilimitado() {
            when(limitDimensionQueryPort.findByCode("USER")).thenReturn(Optional.of(USUARIOS));
            when(repository.addUsage(COMPANY_ID, USUARIOS.id(), CENTINELA, 1)).thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, USUARIOS.id(), CENTINELA))
                    .thenReturn(Optional.empty());
            when(subscriptionQueryPort.findContractSignedOnByCompanyId(COMPANY_ID))
                    .thenReturn(Optional.of(FIRMA));

            assertThatThrownBy(() -> service.execute(alta()))
                    .isInstanceOf(CompanyCapacityNotFoundException.class)
                    .hasMessageContaining("limit zero, not unlimited");
        }

        /**
         * Sin contrato no hay firma a la que ampararse. La respuesta correcta sigue
         * siendo techo cero: lo contrario dejaria sin limite a una empresa sin
         * contrato, que es exactamente al reves de lo que interesa.
         */
        @Test
        @DisplayName("una empresa sin contrato no hereda el «sin techo» de D-74")
        void una_empresa_sin_contrato_no_hereda_el_sin_techo() {
            when(limitDimensionQueryPort.findByCode("TERMINAL"))
                    .thenReturn(Optional.of(TERMINALES_POSTERIORES));
            when(repository.addUsage(COMPANY_ID, TERMINALES_POSTERIORES.id(), CENTINELA, 1))
                    .thenReturn(0);
            when(repository.findByCompanyIdAndDimension(COMPANY_ID, TERMINALES_POSTERIORES.id(),
                    CENTINELA)).thenReturn(Optional.empty());
            when(subscriptionQueryPort.findContractSignedOnByCompanyId(COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "TERMINAL", 1)))
                    .isInstanceOf(CompanyCapacityNotFoundException.class);
        }
    }

    /**
     * Un codigo que no esta sembrado es un fallo de configuracion, no un cupo
     * agotado. Confundirlos convertiria una fila que falta en
     * {@code limit_dimensions} en el bloqueo silencioso de una funcion entera:
     * "techo cero" es exactamente lo que el resto del sistema entiende cuando no
     * hay contador.
     */
    @Test
    @DisplayName("un código de eje que no está en el catálogo no se lee como techo cero")
    void un_codigo_de_eje_desconocido_no_se_lee_como_techo_cero() {
        when(limitDimensionQueryPort.findByCode("MASCOTAS")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service
                .execute(new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "MASCOTAS", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown limit dimension code: MASCOTAS")
                .isNotInstanceOf(CompanyCapacityNotFoundException.class);
    }

    @Test
    @DisplayName("un movimiento de cero no llega ni al repositorio")
    void un_movimiento_de_cero_no_llega_al_repositorio() {
        assertThatThrownBy(() -> new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "USER", 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("delta");
    }
}
