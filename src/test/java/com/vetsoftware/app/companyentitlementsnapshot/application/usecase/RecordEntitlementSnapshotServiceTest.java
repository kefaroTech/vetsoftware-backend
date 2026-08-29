package com.vetsoftware.app.companyentitlementsnapshot.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companyentitlementsnapshot.application.command.RecordEntitlementSnapshotCommand;
import com.vetsoftware.app.companyentitlementsnapshot.application.dto.CompanyEntitlementSnapshotDto;
import com.vetsoftware.app.companyentitlementsnapshot.application.port.out.CompanyEntitlementSnapshotRepository;
import com.vetsoftware.app.companyentitlementsnapshot.domain.CompanyEntitlementSnapshot;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotActor;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotTriggerReason;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La foto de un recalculo de permisos: el registro con el que se le responde a
 * un cliente que reclama por un acceso de hace medio año.
 *
 * <p>
 * <b>Este servicio no tenia ningun test</b>, y es el destino de
 * {@code CompanyEntitlementSnapshotAdapter}, que tampoco lo tenia: el cable y
 * su destino sin red a la vez.
 *
 * <p>
 * <b>El reloj se inyecta y va fijo.</b> El servicio ignora deliberadamente
 * cualquier instante que venga en el comando y sella con
 * {@code LocalDateTime.now(clock)}; sin {@code Clock.fixed} eso no se puede
 * afirmar, solo suponer. Comprobado que sabe fallar: al cambiar el
 * {@code LocalDateTime.now(clock)} del servicio por
 * {@code LocalDateTime.now()}, {@code sella_la_foto_con_el_reloj_inyectado} se
 * pone rojo.
 *
 * <p>
 * <b>Lo que no se cubre y por que.</b> No hay caso para «el repositorio
 * devuelve {@code null}»: es un puerto cuyo contrato es devolver el agregado
 * persistido, y montar ese caso probaria el doble, no el codigo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecordEntitlementSnapshotService — la foto del recalculo")
class RecordEntitlementSnapshotServiceTest {

    private static final Long EMPRESA_ID = 900L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 28, 3, 15, 30);
    private static final String PAYLOAD = "{\"formatVersion\":1,\"entitlements\":[]}";

    private final Clock reloj = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private CompanyEntitlementSnapshotRepository repository;

    private RecordEntitlementSnapshotService service;

    private RecordEntitlementSnapshotService servicio() {
        if (service == null) {
            service = new RecordEntitlementSnapshotService(repository, reloj);
        }
        return service;
    }

    @Nested
    @DisplayName("Escritura de la foto")
    class Escritura {

        /**
         * {@code ArgumentCaptor} y no {@code verify(repository).append(any())}: lo que
         * hay que afirmar es <b>que</b> foto se guarda, no que se guardo alguna.
         */
        @Test
        @DisplayName("sella la foto con el reloj inyectado y no con el instante del comando")
        void sella_la_foto_con_el_reloj_inyectado() {
            when(repository.append(any())).thenAnswer(llamada -> llamada.getArgument(0));

            servicio().execute(unaFoto(SnapshotTriggerReason.MANUAL, null));

            ArgumentCaptor<CompanyEntitlementSnapshot> guardada = ArgumentCaptor
                    .forClass(CompanyEntitlementSnapshot.class);
            verify(repository).append(guardada.capture());
            assertThat(guardada.getValue().getRecalculatedAt()).isEqualTo(AHORA);
            assertThat(guardada.getValue().getCreatedDate()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("guarda la empresa, el actor, el motivo, el documento y su version")
        void guarda_la_empresa_el_actor_el_motivo_el_documento_y_su_version() {
            when(repository.append(any())).thenAnswer(llamada -> llamada.getArgument(0));

            servicio().execute(unaFoto(SnapshotTriggerReason.DUNNING, null));

            ArgumentCaptor<CompanyEntitlementSnapshot> guardada = ArgumentCaptor
                    .forClass(CompanyEntitlementSnapshot.class);
            verify(repository).append(guardada.capture());
            CompanyEntitlementSnapshot foto = guardada.getValue();
            assertThat(foto.getId()).isNull();
            assertThat(foto.getCompanyId()).isEqualTo(EMPRESA_ID);
            assertThat(foto.getActor()).isEqualTo(SnapshotActor.automatedProcess());
            assertThat(foto.getTriggerReason()).isEqualTo(SnapshotTriggerReason.DUNNING);
            assertThat(foto.getAmendmentId()).isNull();
            assertThat(foto.getPayload()).isEqualTo(PAYLOAD);
            assertThat(foto.getPayloadFormatVersion()).isEqualTo(1);
        }

        /**
         * El DTO es lo que se devuelve al llamador; el actor viaja descompuesto en tres
         * componentes y una foto de proceso tiene que salir con los dos ids a nulo y la
         * marca de proceso en cierto. Cruzarlos haria que la auditoria atribuyera a un
         * empleado un recalculo que disparo un job.
         */
        @Test
        @DisplayName("el DTO devuelto descompone el actor sin cruzar los tres componentes")
        void el_dto_devuelto_descompone_el_actor() {
            when(repository.append(any()))
                    .thenAnswer(llamada -> conId(llamada.getArgument(0), 4242L));

            CompanyEntitlementSnapshotDto dto = servicio()
                    .execute(unaFoto(SnapshotTriggerReason.REPAIR, null));

            assertThat(dto.id()).isEqualTo(4242L);
            assertThat(dto.companyId()).isEqualTo(EMPRESA_ID);
            assertThat(dto.recalculatedAt()).isEqualTo(AHORA);
            assertThat(dto.actorEmployeeId()).isNull();
            assertThat(dto.actorSystemUserId()).isNull();
            assertThat(dto.actorIsProcess()).isTrue();
            assertThat(dto.triggerReason()).isEqualTo(SnapshotTriggerReason.REPAIR);
            assertThat(dto.amendmentId()).isNull();
            assertThat(dto.payload()).isEqualTo(PAYLOAD);
            assertThat(dto.payloadFormatVersion()).isEqualTo(1);
        }

        /**
         * Una foto firmada por un empleado —el tenant reparando sus propios permisos—
         * lleva el id del empleado y la marca de proceso en falso. Es el otro extremo
         * del caso anterior y lo que hace que el par de tests no pueda pasar con los
         * componentes cruzados.
         */
        @Test
        @DisplayName("una foto firmada por un empleado no se marca como proceso")
        void una_foto_firmada_por_un_empleado_no_se_marca_como_proceso() {
            when(repository.append(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyEntitlementSnapshotDto dto = servicio().execute(
                    new RecordEntitlementSnapshotCommand(EMPRESA_ID, SnapshotActor.employee(940L),
                            SnapshotTriggerReason.MANUAL, null, PAYLOAD, 1));

            assertThat(dto.actorEmployeeId()).isEqualTo(940L);
            assertThat(dto.actorSystemUserId()).isNull();
            assertThat(dto.actorIsProcess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Lo que no se puede fotografiar")
    class Validaciones {

        /**
         * <b>Este es el caso que destapa el defecto latente del adaptador.</b> El
         * dominio exige que una foto {@code CONTRACT_AMENDMENT} nombre el otrosi que la
         * causo, y aqui se comprueba que asi es. Lo que ese contrato revela es que
         * {@code CompanyEntitlementSnapshotAdapter} pasa {@code amendmentId = null}
         * <em>siempre</em>: el dia que alguien recalcule por un otrosi, esta excepcion
         * tumbara el recalculo entero, que va en la misma transaccion. Ver
         * {@code CompanyEntitlementSnapshotAdapterTest#el_otrosi_no_viaja_por_este_puerto}.
         */
        @Test
        @DisplayName("una foto por otrosi sin el otrosi no se escribe")
        void una_foto_por_otrosi_sin_el_otrosi_no_se_escribe() {
            assertThatThrownBy(() -> servicio()
                    .execute(unaFoto(SnapshotTriggerReason.CONTRACT_AMENDMENT, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must name the amendment");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una foto por otrosi con su otrosi si se escribe")
        void una_foto_por_otrosi_con_su_otrosi_si_se_escribe() {
            when(repository.append(any())).thenAnswer(llamada -> llamada.getArgument(0));

            CompanyEntitlementSnapshotDto dto = servicio()
                    .execute(unaFoto(SnapshotTriggerReason.CONTRACT_AMENDMENT, 7777L));

            assertThat(dto.amendmentId()).isEqualTo(7777L);
        }

        /**
         * Los otros cuatro motivos no exigen otrosi. Se recorren con
         * {@code @EnumSource} y una exclusion explicita para que anadir una constante
         * nueva al enumerado obligue a decidir de que lado cae, en vez de quedarse
         * fuera de la red en silencio.
         */
        @ParameterizedTest(name = "{0}")
        @EnumSource(value = SnapshotTriggerReason.class, names = "CONTRACT_AMENDMENT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("los motivos que no son un otrosi se escriben sin nombrar ninguno")
        void los_motivos_que_no_son_un_otrosi_se_escriben_sin_nombrar_ninguno(
                SnapshotTriggerReason motivo) {
            when(repository.append(any())).thenAnswer(llamada -> llamada.getArgument(0));

            assertThat(servicio().execute(unaFoto(motivo, null)).triggerReason()).isEqualTo(motivo);
        }

        /**
         * <b>Un documento vacio no prueba nada</b>, y guardarlo seria peor que no tener
         * foto: en la reclamacion de dentro de seis meses nadie sabria si la empresa no
         * tenia permisos o si la foto salio mal.
         */
        @Test
        @DisplayName("una foto con el documento en blanco no se escribe")
        void una_foto_con_el_documento_en_blanco_no_se_escribe() {
            assertThatThrownBy(
                    () -> servicio().execute(new RecordEntitlementSnapshotCommand(EMPRESA_ID,
                            SnapshotActor.automatedProcess(), SnapshotTriggerReason.MANUAL, null,
                            "   ", 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("payload is required");

            verifyNoInteractions(repository);
        }

        /**
         * <b>La version del formato empieza en 1.</b> Un cero se leeria como «sin
         * version» y las consultas sobre fotos viejas no sabrian por que rama entrar:
         * devolverian vacio en silencio, que es exactamente lo que R-ENT-15 evita.
         */
        @Test
        @DisplayName("una foto sin version de formato no se escribe")
        void una_foto_sin_version_de_formato_no_se_escribe() {
            assertThatThrownBy(
                    () -> servicio().execute(new RecordEntitlementSnapshotCommand(EMPRESA_ID,
                            SnapshotActor.automatedProcess(), SnapshotTriggerReason.MANUAL, null,
                            PAYLOAD, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("payload format version must be at least 1");

            verifyNoInteractions(repository);
        }
    }

    private static RecordEntitlementSnapshotCommand unaFoto(SnapshotTriggerReason motivo,
            Long otrosi) {
        return new RecordEntitlementSnapshotCommand(EMPRESA_ID, SnapshotActor.automatedProcess(),
                motivo, otrosi, PAYLOAD, 1);
    }

    private static CompanyEntitlementSnapshot conId(CompanyEntitlementSnapshot foto, Long id) {
        return new CompanyEntitlementSnapshot(id, foto.getCompanyId(), foto.getRecalculatedAt(),
                foto.getActor(), foto.getTriggerReason(), foto.getAmendmentId(), foto.getPayload(),
                foto.getPayloadFormatVersion(), foto.getCreatedDate());
    }
}
