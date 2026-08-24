package com.vetsoftware.app.pricelist.application.usecase;

import static com.vetsoftware.app.pricelist.testsupport.PriceListMother.CREADA_EL;
import static com.vetsoftware.app.pricelist.testsupport.PriceListMother.DESDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.pricelist.application.command.CreatePriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.out.PriceListRepository;
import com.vetsoftware.app.pricelist.domain.PriceList;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePriceListService")
class CreatePriceListServiceTest {

    @Mock
    private PriceListRepository repository;

    private final Clock clock = Clock.fixed(CREADA_EL.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private CreatePriceListService service() {
        return new CreatePriceListService(repository, clock);
    }

    private static CreatePriceListCommand comandoValido() {
        return new CreatePriceListCommand("LISTA-2026-01", "Tarifa 2026", "COP", DESDE, null);
    }

    @Test
    @DisplayName("guarda un borrador sin firma y sellado con el reloj inyectado")
    void guarda_un_borrador_sin_firma() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PriceListDto dto = service().execute(comandoValido());

        ArgumentCaptor<PriceList> guardada = ArgumentCaptor.forClass(PriceList.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getStatus()).isEqualTo(PriceListStatus.DRAFT);
        assertThat(guardada.getValue().getPublishedAt()).isNull();
        assertThat(guardada.getValue().getPublishedBySystemUserId()).isNull();
        assertThat(guardada.getValue().getCreatedDate()).isEqualTo(CREADA_EL);
        assertThat(dto.code()).isEqualTo("LISTA-2026-01");
        assertThat(dto.status()).isEqualTo(PriceListStatus.DRAFT);
    }

    @Test
    @DisplayName("no toca el repositorio si el comando viola una invariante del dominio")
    void no_toca_el_repositorio_con_datos_invalidos() {
        CreatePriceListCommand invalido = new CreatePriceListCommand("LISTA", "Tarifa", "cop",
                DESDE, null);

        assertThatThrownBy(() -> service().execute(invalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency must be uppercase");

        verifyNoInteractions(repository);
    }
}
