package com.vetsoftware.app.entitlement.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.companyentitlementsnapshot.application.command.RecordEntitlementSnapshotCommand;
import com.vetsoftware.app.companyentitlementsnapshot.application.port.in.RecordEntitlementSnapshotUseCase;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotActor;
import com.vetsoftware.app.entitlement.domain.AccessLevel;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.EntitlementSource;
import com.vetsoftware.app.entitlement.domain.PeriodKey;
import com.vetsoftware.app.entitlement.domain.SnapshotReason;
import com.vetsoftware.app.entitlement.domain.SubModuleRef;
import com.vetsoftware.app.entitlement.testsupport.EntitlementMother;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * El unico archivo que traduce un recalculo de permisos a la foto que se guarda
 * para siempre.
 *
 * <p>
 * <b>Era el peor agujero de los treinta y un adaptadores de
 * {@code infrastructure/orchestration}</b>: ni el cable ni su destino
 * ({@code RecordEntitlementSnapshotService}) tenian un solo test, y sin embargo
 * aqui se construye a mano un documento JSON campo a campo y se traduce un
 * enumerado a otro caso por caso.
 *
 * <p>
 * <b>El {@code ObjectMapper} va real, no mockeado.</b> No es un puerto: es el
 * serializador cuyo resultado <em>es</em> lo que se prueba. Con un doble, este
 * test afirmaria sobre una cadena que el propio test invento.
 *
 * <p>
 * <b>El {@code switch} se recorre entero con {@code @EnumSource} y se compara
 * por nombre.</b> El compilador ya obliga a que un valor nuevo de
 * {@code SnapshotReason} tenga su caso —es un {@code switch} de expresion sin
 * {@code default}—, asi que el riesgo que queda no es la rama que falta sino
 * <b>el caso mal mapeado</b>: cambiar {@code DUNNING -> TRIAL_EXPIRED} compila,
 * no avisa y deja la auditoria diciendo que el cliente perdio acceso por vencer
 * la prueba cuando lo perdio por no pagar. Comprobado que sabe fallar: al
 * cruzar esos dos casos en el adaptador, la ejecucion parametrizada se pone
 * roja en dos de los cinco valores.
 *
 * <p>
 * <b>Lo que este test NO cubre, y por que no se disfraza.</b> El
 * {@code catch (JacksonException)} de {@code payload(...)} es inalcanzable con
 * un {@code ObjectMapper} sano: el documento son mapas, listas, cadenas y
 * numeros. Montar un doble que lance para pintar esa linea de verde probaria el
 * doble, no el codigo — seria exactamente la prueba vacia que hay que evitar.
 * Queda declarado como hueco consciente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyEntitlementSnapshotAdapter — el documento de la foto de permisos")
class CompanyEntitlementSnapshotAdapterTest {

    private static final Long EMPRESA_ID = 900L;
    private static final LocalDateTime RECALCULADO_EN = LocalDateTime.of(2026, 8, 28, 3, 15, 30);
    private static final ObjectMapper LECTOR = new ObjectMapper();

    private static final SubModuleRef AGENDA = new SubModuleRef(11L, "SCHEDULING", "Agenda");
    private static final SubModuleRef FACTURACION = new SubModuleRef(12L, "BILLING",
            "Facturacion electronica");

    @Mock
    private RecordEntitlementSnapshotUseCase recordSnapshot;

    private CompanyEntitlementSnapshotAdapter adapter;

    @BeforeEach
    void montarElAdaptadorConUnSerializadorReal() {
        adapter = new CompanyEntitlementSnapshotAdapter(recordSnapshot, new ObjectMapper());
    }

    @Nested
    @DisplayName("La cabecera del documento")
    class Cabecera {

        /**
         * <b>La version del formato va DENTRO del documento y ademas en su propia
         * columna.</b> Sin la de dentro, renombrar una clave hace que las consultas
         * sobre fotos viejas devuelvan vacio en silencio y parezca una empresa que no
         * tenia permisos (R-ENT-15). Se afirman las dos.
         */
        @Test
        @DisplayName("la version del formato viaja dentro del documento y en el comando")
        void la_version_del_formato_viaja_dentro_del_documento_y_en_el_comando() {
            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL, List.of(), List.of());

            RecordEntitlementSnapshotCommand comando = comandoEmitido();
            assertThat(comando.payloadFormatVersion()).isEqualTo(1);
            assertThat(documento(comando).get("formatVersion").asInt()).isEqualTo(1);
        }

        @Test
        @DisplayName("firma la foto como proceso automatico y nombra la empresa")
        void firma_la_foto_como_proceso_automatico_y_nombra_la_empresa() {
            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL, List.of(), List.of());

            RecordEntitlementSnapshotCommand comando = comandoEmitido();
            assertThat(comando.companyId()).isEqualTo(EMPRESA_ID);
            assertThat(comando.actor()).isEqualTo(SnapshotActor.automatedProcess());
        }

        @Test
        @DisplayName("el instante del recalculo va dentro del documento, sin reloj propio")
        void el_instante_del_recalculo_va_dentro_del_documento() {
            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL, List.of(), List.of());

            assertThat(documento(comandoEmitido()).get("recalculatedAt").asString())
                    .isEqualTo(RECALCULADO_EN.toString());
        }

        /**
         * Un recalculo que deja a la empresa sin nada —contrato terminado— produce un
         * documento con las dos listas vacias, no un documento sin esas claves. Quien
         * lea la foto dentro de seis meses tiene que poder distinguir «no tenia
         * permisos» de «el documento estaba mal formado».
         */
        @Test
        @DisplayName("un recalculo sin permisos ni contadores deja las dos listas vacias")
        void un_recalculo_sin_permisos_ni_contadores_deja_las_dos_listas_vacias() {
            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL, List.of(), List.of());

            JsonNode documento = documento(comandoEmitido());
            assertThat(documento.get("entitlements").isArray()).isTrue();
            assertThat(documento.get("entitlements").size()).isZero();
            assertThat(documento.get("capacities").isArray()).isTrue();
            assertThat(documento.get("capacities").size()).isZero();
        }
    }

    @Nested
    @DisplayName("Las filas de permisos")
    class Permisos {

        /**
         * Los siete campos se afirman uno a uno y con valores distintos entre si: el
         * bucle los copia por nombre de clave, asi que un cruce entre
         * {@code subModuleId} y {@code subscriptionItemId} —los dos {@code Long}—
         * compila y no lo ve nadie.
         */
        @Test
        @DisplayName("cada permiso copia sus siete campos sin cruzar ninguno")
        void cada_permiso_copia_sus_siete_campos_sin_cruzar_ninguno() {
            CompanyEntitlement permiso = CompanyEntitlement.derived(EMPRESA_ID, AGENDA,
                    AccessLevel.READ_ONLY, EntitlementSource.SUBSCRIPTION, 970L, 8801L,
                    LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 12, 31, 23, 59),
                    RECALCULADO_EN);

            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL, List.of(permiso),
                    List.of());

            JsonNode fila = documento(comandoEmitido()).get("entitlements").get(0);
            assertThat(fila.get("subModuleId").asLong()).isEqualTo(11L);
            assertThat(fila.get("subModuleCode").asString()).isEqualTo("SCHEDULING");
            assertThat(fila.get("accessLevel").asString()).isEqualTo("READ_ONLY");
            assertThat(fila.get("source").asString()).isEqualTo("SUBSCRIPTION");
            assertThat(fila.get("subscriptionItemId").asLong()).isEqualTo(8801L);
            assertThat(fila.get("validFrom").asString())
                    .isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0).toString());
            assertThat(fila.get("validUntil").asString())
                    .isEqualTo(LocalDateTime.of(2026, 12, 31, 23, 59).toString());
        }

        /**
         * <b>La ventana abierta se escribe como nulo, no como ausencia.</b> Es la rama
         * del ternario que no ejecutaba nadie. Un permiso sin fecha de fin es el caso
         * normal —el modulo contratado y vigente—, y si la clave desapareciera del
         * documento, quien lea la foto no podria distinguirlo de un permiso al que se
         * le perdio la caducidad.
         */
        @Test
        @DisplayName("un permiso sin caducidad escribe la clave a nulo, no la omite")
        void un_permiso_sin_caducidad_escribe_la_clave_a_nulo() {
            CompanyEntitlement permiso = CompanyEntitlement.derived(EMPRESA_ID, FACTURACION,
                    AccessLevel.FULL, EntitlementSource.CORE, null, null,
                    LocalDateTime.of(2026, 1, 1, 0, 0), null, RECALCULADO_EN);

            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL, List.of(permiso),
                    List.of());

            JsonNode fila = documento(comandoEmitido()).get("entitlements").get(0);
            assertThat(fila.has("validUntil")).isTrue();
            assertThat(fila.get("validUntil").isNull()).isTrue();
            assertThat(fila.get("subscriptionItemId").isNull()).isTrue();
        }

        @Test
        @DisplayName("conserva el orden de los permisos que le da el recalculo")
        void conserva_el_orden_de_los_permisos_que_le_da_el_recalculo() {
            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL,
                    List.of(unPermisoDe(AGENDA), unPermisoDe(FACTURACION)), List.of());

            JsonNode filas = documento(comandoEmitido()).get("entitlements");
            assertThat(filas.get(0).get("subModuleCode").asString()).isEqualTo("SCHEDULING");
            assertThat(filas.get(1).get("subModuleCode").asString()).isEqualTo("BILLING");
        }
    }

    @Nested
    @DisplayName("Las filas de contadores")
    class Contadores {

        /**
         * Techo 7 y consumo 3: numeros distintos a proposito, porque son los dos
         * {@code int} contiguos del bucle y cruzarlos dejaria la foto diciendo que la
         * clinica tenia contratados tres usuarios y usaba siete.
         */
        @Test
        @DisplayName("cada contador copia sus cinco campos sin cruzar techo y consumo")
        void cada_contador_copia_sus_cinco_campos_sin_cruzar_techo_y_consumo() {
            CompanyCapacity contador = EntitlementMother.contadorExistente(5001L,
                    EntitlementMother.MASCOTAS, 7, 3);

            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL, List.of(),
                    List.of(contador));

            JsonNode fila = documento(comandoEmitido()).get("capacities").get(0);
            assertThat(fila.get("dimensionId").asLong()).isEqualTo(EntitlementMother.MASCOTAS.id());
            assertThat(fila.get("dimensionCode").asString()).isEqualTo("ANIMAL");
            assertThat(fila.get("periodKey").asString()).isEqualTo(PeriodKey.SENTINEL);
            assertThat(fila.get("limitQuantity").asInt()).isEqualTo(7);
            assertThat(fila.get("usedQuantity").asInt()).isEqualTo(3);
        }

        /**
         * <b>El eje de flujo lleva su periodo real, no el centinela.</b> Es lo que
         * permite que la foto de agosto y la de septiembre del mismo eje no se
         * confundan; escribir {@code ALLTIME} aqui haria que las dos parecieran el
         * mismo contador.
         */
        @Test
        @DisplayName("un contador de flujo escribe su clave de periodo real")
        void un_contador_de_flujo_escribe_su_clave_de_periodo_real() {
            CompanyCapacity contador = EntitlementMother.contadorDeFlujo(5002L,
                    EntitlementMother.CITAS, "2026-08", 500, 120);

            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL, List.of(),
                    List.of(contador));

            JsonNode fila = documento(comandoEmitido()).get("capacities").get(0);
            assertThat(fila.get("periodKey").asString()).isEqualTo("2026-08");
            assertThat(fila.get("limitQuantity").asInt()).isEqualTo(500);
            assertThat(fila.get("usedQuantity").asInt()).isEqualTo(120);
        }

        /**
         * Un contador con el techo agotado —o por encima, que R-LIMIT-38 permite tras
         * una bajada de plan— tiene que salir en la foto tal cual. Cortarlo a cero o al
         * techo borraria justamente la prueba de que el cliente estaba excedido.
         */
        @Test
        @DisplayName("un consumo por encima del techo se fotografia tal cual")
        void un_consumo_por_encima_del_techo_se_fotografia_tal_cual() {
            CompanyCapacity contador = EntitlementMother.contadorExistente(5003L,
                    EntitlementMother.USUARIOS, 3, 5);

            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.MANUAL, List.of(),
                    List.of(contador));

            JsonNode fila = documento(comandoEmitido()).get("capacities").get(0);
            assertThat(fila.get("limitQuantity").asInt()).isEqualTo(3);
            assertThat(fila.get("usedQuantity").asInt()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("La traduccion del motivo")
    class Motivo {

        /**
         * Los cinco valores, comparados por nombre. Un caso mal copiado —el unico fallo
         * que el compilador no ve— cae aqui.
         */
        @ParameterizedTest(name = "{0}")
        @EnumSource(SnapshotReason.class)
        @DisplayName("cada motivo del recalculo se traduce al del mismo nombre")
        void cada_motivo_del_recalculo_se_traduce_al_del_mismo_nombre(SnapshotReason motivo) {
            adapter.record(EMPRESA_ID, RECALCULADO_EN, motivo, List.of(), List.of());

            assertThat(comandoEmitido().triggerReason().name()).isEqualTo(motivo.name());
        }

        /**
         * <b>DEFECTO LATENTE, no contrato deseado.</b> Este adaptador escribe
         * {@code amendmentId = null} siempre, porque {@code EntitlementSnapshotPort} no
         * transporta el otrosi. Para los otros cuatro motivos da igual; para
         * {@code CONTRACT_AMENDMENT} no: el dominio exige que la foto nombre el otrosi
         * que la causo, asi que el comando que se arma aqui es <b>irrecibible</b> por
         * {@code RecordEntitlementSnapshotService} —ver
         * {@code RecordEntitlementSnapshotServiceTest#una_foto_por_otrosi_sin_el_otrosi_no_se_escribe}—
         * y, como la foto va en la misma transaccion que el recalculo, tumbaria el
         * recalculo entero: la clinica firma una ampliacion y se queda sin el modulo
         * que acaba de comprar.
         *
         * <p>
         * <b>Hoy no lo alcanza nadie</b>, y eso es el otro hallazgo: el unico llamador
         * de {@code CompanyEntitlementRecalculator.recalculate(companyId, reason)} pasa
         * siempre {@code MANUAL}, asi que los otros cuatro motivos no tienen camino de
         * produccion. Este caso deja el hecho escrito en el sitio donde se rompera, en
         * vez de dejarlo solo en un informe que caduca.
         */
        @Test
        @DisplayName("el otrosi no viaja por este puerto: la foto por otrosi nace sin el")
        void el_otrosi_no_viaja_por_este_puerto() {
            adapter.record(EMPRESA_ID, RECALCULADO_EN, SnapshotReason.CONTRACT_AMENDMENT, List.of(),
                    List.of());

            assertThat(comandoEmitido().amendmentId())
                    .describedAs("el puerto no transporta el otrosi, asi que el comando de"
                            + " CONTRACT_AMENDMENT nace invalido para el dominio de destino")
                    .isNull();
        }
    }

    private CompanyEntitlement unPermisoDe(SubModuleRef subModule) {
        return CompanyEntitlement.derived(EMPRESA_ID, subModule, AccessLevel.FULL,
                EntitlementSource.SUBSCRIPTION, 970L, 8801L, LocalDateTime.of(2026, 1, 1, 0, 0),
                null, RECALCULADO_EN);
    }

    private RecordEntitlementSnapshotCommand comandoEmitido() {
        ArgumentCaptor<RecordEntitlementSnapshotCommand> comando = ArgumentCaptor
                .forClass(RecordEntitlementSnapshotCommand.class);
        verify(recordSnapshot).execute(comando.capture());
        return comando.getValue();
    }

    private static JsonNode documento(RecordEntitlementSnapshotCommand comando) {
        return LECTOR.readTree(comando.payload());
    }
}
