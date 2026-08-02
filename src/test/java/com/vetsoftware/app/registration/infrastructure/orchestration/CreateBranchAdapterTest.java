package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.branch.application.command.CreateBranchCommand;
import com.vetsoftware.app.branch.application.port.in.CreateBranchUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sembrado de la sede "Principal" durante el registro público. La invariante "toda empresa nace con
 * ≥1 sede" se cumple mapeando los datos de la empresa a un CreateBranchCommand, y ejecutando la
 * creación SIEMPRE dentro del contexto de sistema (ROLE_SYSTEM) — porque el registro es anónimo y
 * el use case exige autoridad.
 */
@ExtendWith(MockitoExtension.class)
class CreateBranchAdapterTest {

  @Mock private CreateBranchUseCase createBranchUseCase;
  @Mock private SystemAuthRunner systemAuthRunner;
  @InjectMocks private CreateBranchAdapter adapter;

  @Test
  void siembra_la_principal_con_los_datos_mapeados_bajo_contexto_de_sistema() {
    // Hacemos que el runner ejecute el Runnable que se le pasa (simula el envoltorio ROLE_SYSTEM).
    doAnswer(
            inv -> {
              ((Runnable) inv.getArgument(0)).run();
              return null;
            })
        .when(systemAuthRunner)
        .run(any(Runnable.class));

    adapter.create("Principal", "PRINCIPAL", "Cra 1 #2-3", "3001112233", 5L, 9L);

    ArgumentCaptor<CreateBranchCommand> captor = ArgumentCaptor.forClass(CreateBranchCommand.class);
    verify(createBranchUseCase).execute(captor.capture());
    CreateBranchCommand cmd = captor.getValue();
    assertThat(cmd.name()).isEqualTo("Principal");
    assertThat(cmd.code()).isEqualTo("PRINCIPAL");
    assertThat(cmd.address()).isEqualTo("Cra 1 #2-3");
    assertThat(cmd.phone()).isEqualTo("3001112233");
    assertThat(cmd.cityId()).isEqualTo(5L);
    assertThat(cmd.companyId()).isEqualTo(9L);
    verify(systemAuthRunner).run(any(Runnable.class));
  }

  @Test
  void la_creacion_ocurre_solo_dentro_del_system_auth_runner() {
    // Sin stubear el runner, el Runnable no corre ⇒ el use case NO debe invocarse fuera del
    // contexto.
    adapter.create("Principal", "PRINCIPAL", "addr", "phone", 5L, 9L);

    verify(createBranchUseCase, never()).execute(any());
    verify(systemAuthRunner).run(any(Runnable.class));
  }
}
