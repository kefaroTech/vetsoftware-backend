package com.vetsoftware.app.membershipsubmodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membershipsubmodule.application.command.UpdateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipQueryPort;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import com.vetsoftware.app.membershipsubmodule.testsupport.MembershipSubModuleMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMembershipSubModuleService")
class UpdateMembershipSubModuleServiceTest {

    @Mock
    private MembershipSubModuleRepository repository;
    @Mock
    private MembershipQueryPort membershipQueryPort;
    @Mock
    private SubModuleQueryPort subModuleQueryPort;

    private UpdateMembershipSubModuleService service;

    @BeforeEach
    void crearServicio() {
        service = new UpdateMembershipSubModuleService(repository, membershipQueryPort,
                subModuleQueryPort);
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("resuelve membership y subModule y actualiza la relacion existente")
        void resuelve_las_referencias_y_actualiza_la_relacion_existente() {
            MembershipSubModule existente = MembershipSubModuleMother.activa();
            UpdateMembershipSubModuleCommand command = MembershipSubModuleMother
                    .comandoActualizar();
            when(repository.findById(command.id())).thenReturn(Optional.of(existente));
            when(membershipQueryPort.findById(command.membershipId()))
                    .thenReturn(Optional.of(MembershipSubModuleMother.OTRO_PLAN));
            when(subModuleQueryPort.findById(command.subModuleId()))
                    .thenReturn(Optional.of(MembershipSubModuleMother.INVENTARIO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MembershipSubModuleDto dto = service.execute(command);

            ArgumentCaptor<MembershipSubModule> guardado = ArgumentCaptor
                    .forClass(MembershipSubModule.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getMembership())
                    .isEqualTo(MembershipSubModuleMother.OTRO_PLAN);
            assertThat(guardado.getValue().getSubModule())
                    .isEqualTo(MembershipSubModuleMother.INVENTARIO);
            assertThat(dto.membership().id())
                    .isEqualTo(MembershipSubModuleMother.OTRO_MEMBERSHIP_ID);
            assertThat(dto.subModule().id())
                    .isEqualTo(MembershipSubModuleMother.OTRO_SUB_MODULE_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no toca los puertos ni persiste si la relacion no existe")
        void no_toca_los_puertos_ni_persiste_si_la_relacion_no_existe() {
            UpdateMembershipSubModuleCommand command = MembershipSubModuleMother
                    .comandoActualizar();
            when(repository.findById(command.id())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(MembershipSubModuleNotFoundException.class)
                    .hasMessageContaining("MembershipSubModule not found: " + command.id());

            verifyNoInteractions(membershipQueryPort, subModuleQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no persiste si la membresia no existe")
        void no_persiste_si_la_membresia_no_existe() {
            MembershipSubModule existente = MembershipSubModuleMother.activa();
            UpdateMembershipSubModuleCommand command = MembershipSubModuleMother
                    .comandoActualizar();
            when(repository.findById(command.id())).thenReturn(Optional.of(existente));
            when(membershipQueryPort.findById(command.membershipId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Membership not found: " + command.membershipId());

            verifyNoInteractions(subModuleQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no persiste si el submodulo no existe")
        void no_persiste_si_el_sub_modulo_no_existe() {
            MembershipSubModule existente = MembershipSubModuleMother.activa();
            UpdateMembershipSubModuleCommand command = MembershipSubModuleMother
                    .comandoActualizar();
            when(repository.findById(command.id())).thenReturn(Optional.of(existente));
            when(membershipQueryPort.findById(command.membershipId()))
                    .thenReturn(Optional.of(MembershipSubModuleMother.OTRO_PLAN));
            when(subModuleQueryPort.findById(command.subModuleId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SubModule not found: " + command.subModuleId());

            verify(repository, never()).save(any());
        }
    }
}
