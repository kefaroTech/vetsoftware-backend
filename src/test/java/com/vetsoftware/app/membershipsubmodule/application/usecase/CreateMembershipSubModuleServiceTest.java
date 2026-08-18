package com.vetsoftware.app.membershipsubmodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
@DisplayName("CreateMembershipSubModuleService")
class CreateMembershipSubModuleServiceTest {

    @Mock
    private MembershipSubModuleRepository repository;
    @Mock
    private MembershipQueryPort membershipQueryPort;
    @Mock
    private SubModuleQueryPort subModuleQueryPort;

    private CreateMembershipSubModuleService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateMembershipSubModuleService(repository, membershipQueryPort,
                subModuleQueryPort);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("resuelve membership y subModule y persiste la relacion nueva")
        void resuelve_las_referencias_y_persiste_la_relacion_nueva() {
            when(membershipQueryPort.findById(MembershipSubModuleMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.PLAN_PREMIUM));
            when(subModuleQueryPort.findById(MembershipSubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.FACTURACION));
            when(repository.findDisabledIdByMembershipAndSubModule(
                    MembershipSubModuleMother.MEMBERSHIP_ID,
                    MembershipSubModuleMother.SUB_MODULE_ID)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(MembershipSubModuleMother.activa());

            MembershipSubModuleDto dto = service.execute(MembershipSubModuleMother.comandoCrear());

            ArgumentCaptor<MembershipSubModule> guardado = ArgumentCaptor
                    .forClass(MembershipSubModule.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getMembership())
                    .isEqualTo(MembershipSubModuleMother.PLAN_PREMIUM);
            assertThat(guardado.getValue().getSubModule())
                    .isEqualTo(MembershipSubModuleMother.FACTURACION);
            assertThat(guardado.getValue().getId()).isNull();
            assertThat(dto.id()).isEqualTo(MembershipSubModuleMother.RELATION_ID);
        }

        @Test
        @DisplayName("una combinacion previamente deshabilitada se reactiva en vez de duplicarse")
        void una_combinacion_deshabilitada_se_reactiva_en_vez_de_duplicarse() {
            when(membershipQueryPort.findById(MembershipSubModuleMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.PLAN_PREMIUM));
            when(subModuleQueryPort.findById(MembershipSubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.FACTURACION));
            when(repository.findDisabledIdByMembershipAndSubModule(
                    MembershipSubModuleMother.MEMBERSHIP_ID,
                    MembershipSubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.RELATION_ID));
            when(repository.findById(MembershipSubModuleMother.RELATION_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.activa()));

            MembershipSubModuleDto dto = service.execute(MembershipSubModuleMother.comandoCrear());

            verify(repository).reactivate(MembershipSubModuleMother.RELATION_ID);
            verify(repository, never()).save(any());
            assertThat(dto.id()).isEqualTo(MembershipSubModuleMother.RELATION_ID);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("una fila reactivada pero ilocalizable lanza no encontrada")
        void una_fila_reactivada_pero_ilocalizable_lanza_no_encontrada() {
            when(membershipQueryPort.findById(MembershipSubModuleMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.PLAN_PREMIUM));
            when(subModuleQueryPort.findById(MembershipSubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.FACTURACION));
            when(repository.findDisabledIdByMembershipAndSubModule(
                    MembershipSubModuleMother.MEMBERSHIP_ID,
                    MembershipSubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.RELATION_ID));
            when(repository.findById(MembershipSubModuleMother.RELATION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MembershipSubModuleMother.comandoCrear()))
                    .isInstanceOf(MembershipSubModuleNotFoundException.class)
                    .hasMessageContaining("MembershipSubModule not found: "
                            + MembershipSubModuleMother.RELATION_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("no consulta el submodulo ni persiste si la membresia no existe")
        void no_consulta_el_sub_modulo_ni_persiste_si_la_membresia_no_existe() {
            when(membershipQueryPort.findById(MembershipSubModuleMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MembershipSubModuleMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Membership not found: " + MembershipSubModuleMother.MEMBERSHIP_ID);

            verifyNoInteractions(subModuleQueryPort, repository);
        }

        @Test
        @DisplayName("no persiste si el submodulo no existe")
        void no_persiste_si_el_sub_modulo_no_existe() {
            when(membershipQueryPort.findById(MembershipSubModuleMother.MEMBERSHIP_ID))
                    .thenReturn(Optional.of(MembershipSubModuleMother.PLAN_PREMIUM));
            when(subModuleQueryPort.findById(MembershipSubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MembershipSubModuleMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "SubModule not found: " + MembershipSubModuleMother.SUB_MODULE_ID);

            verifyNoInteractions(repository);
        }
    }
}
