package com.vetsoftware.app.membershipsubmodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.port.out.MembershipSubModuleRepository;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModule;
import com.vetsoftware.app.membershipsubmodule.domain.MembershipSubModuleNotFoundException;
import com.vetsoftware.app.membershipsubmodule.testsupport.MembershipSubModuleMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateMembershipSubModuleService")
class ReactivateMembershipSubModuleServiceTest {

    @Mock
    private MembershipSubModuleRepository repository;

    private ReactivateMembershipSubModuleService service;

    @BeforeEach
    void crearServicio() {
        service = new ReactivateMembershipSubModuleService(repository);
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactiva y devuelve la relacion releida")
        void reactiva_y_devuelve_la_relacion_releida() {
            MembershipSubModule reactivada = MembershipSubModuleMother.activa();
            when(repository.reactivate(MembershipSubModuleMother.RELATION_ID)).thenReturn(1);
            when(repository.findById(MembershipSubModuleMother.RELATION_ID))
                    .thenReturn(Optional.of(reactivada));

            MembershipSubModuleDto dto = service.execute(MembershipSubModuleMother.RELATION_ID);

            assertThat(dto.id()).isEqualTo(MembershipSubModuleMother.RELATION_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("ninguna fila afectada no vuelve a leer y lanza no encontrada")
        void ninguna_fila_afectada_no_vuelve_a_leer() {
            when(repository.reactivate(MembershipSubModuleMother.RELATION_ID)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(MembershipSubModuleMother.RELATION_ID))
                    .isInstanceOf(MembershipSubModuleNotFoundException.class)
                    .hasMessageContaining("MembershipSubModule not found: "
                            + MembershipSubModuleMother.RELATION_ID);

            verify(repository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("una fila reactivada pero ilocalizable tambien lanza no encontrada")
        void una_fila_reactivada_pero_ilocalizable_tambien_lanza_no_encontrada() {
            when(repository.reactivate(MembershipSubModuleMother.RELATION_ID)).thenReturn(1);
            when(repository.findById(MembershipSubModuleMother.RELATION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(MembershipSubModuleMother.RELATION_ID))
                    .isInstanceOf(MembershipSubModuleNotFoundException.class)
                    .hasMessageContaining("MembershipSubModule not found: "
                            + MembershipSubModuleMother.RELATION_ID);
        }
    }
}
