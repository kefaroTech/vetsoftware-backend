package com.vetsoftware.app.entitlement.infrastructure.orchestration;

import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.AHORA;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.COMPANY_ID;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.USUARIOS;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.contadorExistente;
import static com.vetsoftware.app.entitlement.testsupport.EntitlementMother.relojFijo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitevent.application.port.out.CompanyLimitEventRepository;
import com.vetsoftware.app.companylimitevent.application.usecase.RecordLimitEventService;
import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.LimitDenialPort;
import com.vetsoftware.app.entitlement.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.entitlement.application.port.out.OverageAllowancePort;
import com.vetsoftware.app.entitlement.application.port.out.OverageChargePort;
import com.vetsoftware.app.entitlement.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.entitlement.application.usecase.AdjustCompanyCapacityUsageService;
import com.vetsoftware.app.entitlement.domain.CompanyCapacityLimitExceededException;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>La costura</b> entre el contador de cupo de {@code entitlement} y la
 * bitacora de {@code companylimitevent}: el cable por el que viaja el portazo.
 *
 * <p>
 * <b>Es el gemelo de {@link OverageChargeAdapter} y hace lo contrario a
 * proposito.</b> Aquel no se traga ni una excepcion —si el cargo no se escribe,
 * el consumo por encima del techo tampoco puede quedar—. Este se las traga
 * todas, porque la negacion <em>ya estaba decidida</em> cuando llegamos aqui y
 * al usuario le tiene que llegar «se te acabo el cupo» y no un error de
 * servidor. Los dos asertos de excepcion de estas dos clases son inversos entre
 * si, y esa inversion es intencionada: no las armonices.
 *
 * <p>
 * <b>Por que se monta el caso de uso real y no un doble.</b> Con un
 * {@link RecordLimitEventUseCase} mockeado, el test solo comprobaria que se
 * llama a un doble: ni que el command se construye entero, ni que el hecho
 * supera las invariantes del dominio, ni que los cinco numeros llegan al sitio
 * que les toca. Aqui el adaptador habla con el {@link RecordLimitEventService}
 * de verdad y solo se dobla su puerto de salida, que es donde termina la
 * costura.
 *
 * <p>
 * <b>Los cinco parametros de {@code limitDenied} se pueden cruzar sin que nadie
 * lo note</b>: dos identificadores seguidos y tres cantidades seguidas. Los
 * cinco valores del fixture son distintos entre si y bien separados —71, 4409,
 * 200, 193, 31— justamente para que un cruce por el camino no pueda compensarse
 * solo. La rodaja del final usa en cambio los datos del
 * {@code EntitlementMother} (empresa 10, eje 41) porque alli el sujeto es el
 * llamador, no el mapeo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LimitDenialAdapter — el portazo se escribe, y escribirlo no puede tumbar la negacion")
class LimitDenialAdapterTest {

    private static final Long EMPRESA = 71L;
    private static final Long EJE = 4409L;
    private static final int TECHO = 200;
    private static final int USADO = 193;
    private static final int PEDIDO = 31;

    private static final String CENTINELA = PeriodKey.SENTINEL;

    @Mock
    private CompanyLimitEventRepository bitacora;

    @Mock
    private CompanyCapacityRepository contadores;
    @Mock
    private LimitDimensionQueryPort catalogoDeEjes;
    @Mock
    private SubscriptionQueryPort contratos;
    @Mock
    private OverageAllowancePort permisoDeExcedente;
    @Mock
    private OverageChargePort cargoDeExcedente;
    @Mock
    private LimitDenialPort portazoQueRevienta;

    @Captor
    private ArgumentCaptor<CompanyLimitEvent> escrito;

    private LimitDenialAdapter adaptador;

    /**
     * Cableado a mano y no con {@code @InjectMocks}: el reloj no es un puerto y
     * mockearlo daria un {@code Clock} que no sabe decir la hora. Va fijo, que es
     * lo unico que permite afirmar sobre el instante con el que nace el hecho.
     */
    @BeforeEach
    void montarLaCostura() {
        adaptador = new LimitDenialAdapter(new RecordLimitEventService(bitacora, relojFijo()));
    }

    @Nested
    @DisplayName("El hecho que queda escrito")
    class ElHechoQueQuedaEscrito {

        @BeforeEach
        void laBitacoraDevuelveLoQueGuarda() {
            when(bitacora.append(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        }

        @Test
        @DisplayName("cada uno de los cinco numeros cae en su sitio: empresa 71, eje 4409, techo"
                + " 200, usado 193 y pedido 31")
        void cada_uno_de_los_cinco_numeros_cae_en_su_sitio() {
            adaptador.limitDenied(EMPRESA, EJE, TECHO, USADO, PEDIDO);

            assertThat(hechoEscrito()).satisfies(hecho -> {
                assertThat(hecho.getCompanyId()).isEqualTo(EMPRESA);
                assertThat(hecho.getLimitDimensionId()).isEqualTo(EJE);
                assertThat(hecho.getLimitQuantity()).isEqualTo(TECHO);
                assertThat(hecho.getUsedQuantity()).isEqualTo(USADO);
                assertThat(hecho.getRequestedDelta()).isEqualTo(PEDIDO);
                assertThat(hecho.getOccurredAt()).isEqualTo(AHORA);
            });
        }

        /**
         * Los tres valores fijos son los que hacen que la bitacora distinga un portazo
         * de cualquier otro hecho de cupo: sin ellos, «doce choques contra el techo
         * esta semana» deja de ser una consulta.
         */
        @Test
        @DisplayName("el hecho se marca como portazo del sistema: LIMIT_BLOCKED, techo de"
                + " suscripcion y proceso automatico, sin excepcion negociada ni motivo")
        void el_hecho_se_marca_como_portazo_del_sistema() {
            adaptador.limitDenied(EMPRESA, EJE, TECHO, USADO, PEDIDO);

            assertThat(hechoEscrito()).satisfies(hecho -> {
                assertThat(hecho.getEventType()).isEqualTo(LimitEventType.LIMIT_BLOCKED);
                assertThat(hecho.getLimitSource()).isEqualTo(LimitSource.SUBSCRIPTION);
                assertThat(hecho.getActor().process()).isTrue();
                assertThat(hecho.getActor().employeeId()).isNull();
                assertThat(hecho.getActor().systemUserId()).isNull();
                assertThat(hecho.getOverrideId()).isNull();
                assertThat(hecho.getReasonCode()).isNull();
                assertThat(hecho.getReason()).isNull();
            });
        }

        /**
         * La transaccion externa sigue viva mientras esta corre, asi que el hecho tiene
         * que ser corto y no puede tocar filas que la externa tenga bloqueadas. De ahi
         * que todos sus numeros lleguen ya resueltos y que este camino no consulte
         * nada: una lectura de mas aqui es un interbloqueo esperando fecha.
         */
        @Test
        @DisplayName("escribe un solo hecho y no consulta nada mas: la transaccion externa sigue"
                + " viva y con filas bloqueadas")
        void escribe_un_solo_hecho_y_no_consulta_nada_mas() {
            adaptador.limitDenied(EMPRESA, EJE, TECHO, USADO, PEDIDO);

            verify(bitacora).append(escrito.capture());
            verifyNoMoreInteractions(bitacora);
        }
    }

    /**
     * <b>El caso que justifica esta clase, y el aserto es el INVERSO del
     * gemelo.</b>
     *
     * <p>
     * En {@code OverageChargeAdapterTest} la excepcion <em>sale</em> y ese es el
     * aserto: si el cargo por excedente no se escribe, el consumo por encima del
     * techo tampoco puede quedar, porque consumir de mas y cobrarlo son un solo
     * hecho. Aqui la excepcion <em>no debe salir</em>: cuando llegamos a este
     * adaptador la negacion ya esta decidida y es correcta, asi que un fallo al
     * dejar constancia solo puede costar la prueba ante una reclamacion y la senal
     * de venta —nunca convertir un 409 legitimo en un 500—.
     *
     * <p>
     * Y ese {@code catch} no lo ejercitaba nadie:
     * {@code RecordLimitEventRollbackIT} importa {@link RecordLimitEventService}
     * directo, sin pasar por el adaptador. Quitarlo por «defensivo de mas» no ponia
     * rojo ni un test.
     */
    @Nested
    @DisplayName("Escribir el hecho no puede impedir la negacion")
    class EscribirElHechoNoPuedeImpedirLaNegacion {

        /**
         * El {@code catch} es de {@code RuntimeException} entera y no de una familia
         * concreta, y esa anchura es la que hay que sujetar. El javadoc de
         * {@link RecordLimitEventUseCase} nombra el caso de la
         * {@link AccessDeniedException} por su nombre: si el gate negara —porque el
         * empleado que topo con el techo no es administrador—, estrechar el
         * {@code catch} a los fallos de motor dejaria ese portazo devolviendo un 500.
         */
        static Stream<Arguments> fallosDeLaBitacora() {
            return Stream.of(
                    Arguments.of("el motor de la base",
                            new IllegalStateException("could not execute statement")),
                    Arguments.of("el gate que niega al empleado",
                            new AccessDeniedException("Access Denied")),
                    Arguments.of("un fallo inesperado del adaptador",
                            new NullPointerException("append devolvio null")));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("fallosDeLaBitacora")
        @DisplayName("un fallo al escribir el hecho no sale del adaptador: al cliente le sigue"
                + " llegando el portazo y no un error de servidor")
        void un_fallo_al_escribir_el_hecho_no_sale_del_adaptador(String forma,
                RuntimeException fallo) {
            when(bitacora.append(any())).thenThrow(fallo);

            assertThatCode(() -> adaptador.limitDenied(EMPRESA, EJE, TECHO, USADO, PEDIDO))
                    .doesNotThrowAnyException();
        }

        /**
         * La segunda forma de fallo, y es distinta de las de arriba: aqui el hecho ni
         * siquiera llega a escribirse porque el dominio lo rechaza al construirlo, asi
         * que la excepcion nace <em>dentro</em> del {@code try} y antes del
         * repositorio. La firma del puerto admite enteros negativos, de modo que un
         * error del llamador cae por aqui — y tampoco puede tumbar la negacion.
         */
        @Test
        @DisplayName("un hecho que el dominio rechaza tampoco sale, y no deja nada escrito a"
                + " medias")
        void un_hecho_que_el_dominio_rechaza_tampoco_sale() {
            assertThatCode(() -> adaptador.limitDenied(EMPRESA, EJE, -1, USADO, PEDIDO))
                    .doesNotThrowAnyException();

            verifyNoInteractions(bitacora);
        }

        /**
         * <b>Lo que si tiene que salir.</b> El {@code catch} es de
         * {@code RuntimeException} y no de {@code Throwable}, y la distincion es
         * deliberada: seguir adelante con la JVM muriendose para devolver un 409 cortes
         * es mentirle al cliente sobre un proceso que ya no puede escribir nada. Si
         * alguien ensancha el {@code catch}, este caso se pone rojo.
         */
        @Test
        @DisplayName("un Error no se traga: el catch es de RuntimeException, no de Throwable")
        void un_error_no_se_traga() {
            when(bitacora.append(any())).thenThrow(new OutOfMemoryError("Metaspace"));

            assertThatThrownBy(() -> adaptador.limitDenied(EMPRESA, EJE, TECHO, USADO, PEDIDO))
                    .isInstanceOf(OutOfMemoryError.class).hasMessageContaining("Metaspace");
        }
    }

    /**
     * La consecuencia de negocio, que es lo unico que el cliente ve. Los dos casos
     * van juntos a proposito: el segundo demuestra que el verde del primero lo pone
     * el {@code catch} del adaptador y no una red del llamador, que no la tiene.
     */
    @Nested
    @DisplayName("Lo que ve el cliente cuando la bitacora falla")
    class LoQueVeElCliente {

        private AdjustCompanyCapacityUsageService llamador(LimitDenialPort portazo) {
            return new AdjustCompanyCapacityUsageService(contadores, catalogoDeEjes, contratos,
                    portazo, permisoDeExcedente, cargoDeExcedente, relojFijo());
        }

        private AdjustCompanyCapacityUsageCommand altaQueNoCabe() {
            when(catalogoDeEjes.findByCode("USER")).thenReturn(Optional.of(USUARIOS));
            when(contadores.addUsage(COMPANY_ID, USUARIOS.id(), CENTINELA, 2)).thenReturn(0);
            when(contadores.findByCompanyIdAndDimension(COMPANY_ID, USUARIOS.id(), CENTINELA))
                    .thenReturn(Optional.of(contadorExistente(31L, USUARIOS, 5, 4)));
            return new AdjustCompanyCapacityUsageCommand(COMPANY_ID, "USER", 2);
        }

        @Test
        @DisplayName("con la bitacora reventada el cliente sigue viendo «se te acabo el cupo»,"
                + " no un error de servidor")
        void con_la_bitacora_reventada_el_cliente_sigue_viendo_que_se_le_acabo_el_cupo() {
            AdjustCompanyCapacityUsageCommand alta = altaQueNoCabe();
            when(bitacora.append(any()))
                    .thenThrow(new IllegalStateException("could not execute statement"));

            assertThatThrownBy(() -> llamador(adaptador).execute(alta))
                    .isInstanceOf(CompanyCapacityLimitExceededException.class)
                    .hasMessageContaining("limit 5").hasMessageContaining("used 4");
        }

        /**
         * <b>El llamador no tiene red propia, y por eso el caso de arriba no es
         * decorativo.</b> {@code AdjustCompanyCapacityUsageService} llama al puerto y
         * lanza acto seguido, sin envolver nada: contra un puerto que revienta, lo que
         * sale es el fallo de la bitacora y el 409 se pierde. La proteccion vive entera
         * en el {@code catch} del adaptador — quitarlo de alli es exactamente este
         * resultado en produccion.
         */
        @Test
        @DisplayName("el llamador no protege nada por su cuenta: contra un puerto que lanza, el"
                + " fallo de la bitacora se lleva por delante el 409")
        void el_llamador_no_protege_nada_por_su_cuenta() {
            AdjustCompanyCapacityUsageCommand alta = altaQueNoCabe();
            doThrow(new IllegalStateException("could not execute statement"))
                    .when(portazoQueRevienta).limitDenied(COMPANY_ID, USUARIOS.id(), 5, 4, 2);

            assertThatThrownBy(() -> llamador(portazoQueRevienta).execute(alta))
                    .isInstanceOf(IllegalStateException.class)
                    .isNotInstanceOf(CompanyCapacityLimitExceededException.class);
        }
    }

    /**
     * La afirmacion estructural del javadoc: el hecho sobrevive a la vuelta atras
     * porque el caso de uso de destino abre transaccion propia. No es un detalle de
     * implementacion — es la razon entera de que la bitacora no quede vacia justo
     * en los casos que existe para registrar, y no hay ningun test de
     * comportamiento en esta clase que lo note si desaparece.
     */
    @Nested
    @DisplayName("La transaccion propia que hace que el hecho sobreviva")
    class LaTransaccionPropia {

        @Test
        @DisplayName("el caso de uso de destino escribe en transaccion propia (REQUIRES_NEW): sin"
                + " eso el hecho se escribiria y se borraria en el mismo suspiro")
        void el_caso_de_uso_de_destino_escribe_en_transaccion_propia() throws Exception {
            Transactional propagacion = RecordLimitEventService.class
                    .getMethod("execute", RecordLimitEventCommand.class)
                    .getAnnotation(Transactional.class);

            assertThat(propagacion).isNotNull();
            assertThat(propagacion.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        }

        /**
         * La propagacion la aplica el proxy, asi que solo existe si la llamada cruza de
         * un bean a otro. Fundir el adaptador con el caso de uso —o hacer que se llame
         * a si mismo— esquivaria el {@code REQUIRES_NEW} <b>sin avisar y sin
         * fallar</b>, que es la peor forma de romperlo: todo seguiria verde y la
         * bitacora quedaria vacia.
         */
        @Test
        @DisplayName("la llamada cruza de un bean a otro: el adaptador no es el propio caso de"
                + " uso, porque una llamada interna esquivaria la propagacion en silencio")
        void la_llamada_cruza_de_un_bean_a_otro() {
            assertThat(RecordLimitEventUseCase.class.isAssignableFrom(LimitDenialAdapter.class))
                    .isFalse();
        }

        /**
         * Y depende del puerto que abre esa transaccion, no del repositorio: alcanzar
         * {@code CompanyLimitEventRepository} directamente escribiria el hecho dentro
         * de la transaccion que esta a punto de deshacerse.
         */
        @Test
        @DisplayName("el adaptador solo depende del puerto que abre la transaccion, nunca del"
                + " repositorio de la bitacora")
        void el_adaptador_solo_depende_del_puerto_que_abre_la_transaccion() {
            assertThat(LimitDenialAdapter.class.getDeclaredConstructors()).hasSize(1);
            assertThat(LimitDenialAdapter.class.getDeclaredConstructors()[0].getParameterTypes())
                    .containsExactly(RecordLimitEventUseCase.class);
        }
    }

    private CompanyLimitEvent hechoEscrito() {
        verify(bitacora).append(escrito.capture());
        return escrito.getValue();
    }
}
