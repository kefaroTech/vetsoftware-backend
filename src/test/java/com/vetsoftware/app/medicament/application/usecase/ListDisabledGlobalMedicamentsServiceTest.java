package com.vetsoftware.app.medicament.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.out.MedicamentRepository;
import com.vetsoftware.app.medicament.testsupport.MedicamentMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los globales PAUSADOS: la unica pantalla desde la que se reactivan.
 *
 * <p>
 * Que use {@code findAllDisabledGlobal()} y no
 * {@code findAllDisabledForCompany(null)} es justo el defecto que se cerro —el
 * segundo compara {@code company_id = NULL} y devuelve siempre vacio, dejando
 * un global pausado sin ninguna pantalla desde la que recuperarlo—. Aqui lo
 * garantiza el stub exacto mas STRICT_STUBS; que la consulta correcta sea de
 * verdad la que MySQL ejecuta lo prueba {@code MedicamentPersistenceIT}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListDisabledGlobalMedicamentsService")
class ListDisabledGlobalMedicamentsServiceTest {

    @Mock
    private MedicamentRepository repository;

    @InjectMocks
    private ListDisabledGlobalMedicamentsService service;

    @Test
    @DisplayName("mapea los globales pausados a DTOs, con enabled=false y sin empresa")
    void mapea_los_globales_pausados() {
        when(repository.findAllDisabledGlobal())
                .thenReturn(List.of(MedicamentMother.pausadoGeneral()));

        List<MedicamentDto> pausados = service.listDisabled();

        assertThat(pausados).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(MedicamentMother.MEDICAMENT_ID);
            assertThat(dto.name()).isEqualTo("Amoxicilina");
            // enabled=false es el dato entero de esta pantalla: si saliera true, el
            // front no tendria de que colgar el boton de reactivar.
            assertThat(dto.enabled()).isFalse();
            assertThat(dto.general()).isTrue();
            assertThat(dto.company()).isNull();
        });
    }

    @Test
    @DisplayName("sin globales pausados devuelve una lista vacia, no null")
    void sin_pausados_devuelve_lista_vacia() {
        when(repository.findAllDisabledGlobal()).thenReturn(List.of());

        assertThat(service.listDisabled()).isEmpty();
    }
}
