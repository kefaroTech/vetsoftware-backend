package com.vetsoftware.app.electronicdocument.infrastructure.orchestration;

import static org.mockito.Mockito.verify;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentOnCloseUseCase;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClosedAccountEmissionAdapter — conecta el cierre de cuenta con la emision F4")
class ClosedAccountEmissionAdapterTest {

    @Mock
    private EmitElectronicDocumentOnCloseUseCase emitOnClose;

    private ClosedAccountEmissionAdapter adapter;

    @BeforeEach
    void montar() {
        adapter = new ClosedAccountEmissionAdapter(emitOnClose);
    }

    @Nested
    @DisplayName("tipos validos — se preservan tal cual")
    class TiposValidos {

        @ParameterizedTest
        @ValueSource(strings = {"FE_VENTA", "DOC_EQUIV_POS"})
        @DisplayName("FE_VENTA y DOC_EQUIV_POS se preservan tal cual")
        void tipos_validos_se_preservan(String raw) {
            adapter.emitForClosedAccount(100L, 9L, raw, false);

            ArgumentCaptor<EmitElectronicDocumentCommand> captor = ArgumentCaptor
                    .forClass(EmitElectronicDocumentCommand.class);
            verify(emitOnClose).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().documentType())
                    .isEqualTo(ElectronicDocumentType.valueOf(raw));
        }
    }

    @Nested
    @DisplayName("tipos invalidos — degradan a DOC_EQUIV_POS")
    class TiposInvalidos {

        @Test
        @DisplayName("un tipo distinto de FE_VENTA/DOC_EQUIV_POS (NOTA_CREDITO) degrada a DOC_EQUIV_POS")
        void tipo_no_soportado_degrada_a_pos() {
            adapter.emitForClosedAccount(100L, 9L, "NOTA_CREDITO", false);

            ArgumentCaptor<EmitElectronicDocumentCommand> captor = ArgumentCaptor
                    .forClass(EmitElectronicDocumentCommand.class);
            verify(emitOnClose).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().documentType())
                    .isEqualTo(ElectronicDocumentType.DOC_EQUIV_POS);
        }

        @Test
        @DisplayName("un valor desconocido (no es del enum) degrada a DOC_EQUIV_POS en vez de lanzar")
        void valor_desconocido_degrada_a_pos_sin_lanzar() {
            adapter.emitForClosedAccount(100L, 9L, "ALGO_QUE_NO_EXISTE", true);

            ArgumentCaptor<EmitElectronicDocumentCommand> captor = ArgumentCaptor
                    .forClass(EmitElectronicDocumentCommand.class);
            verify(emitOnClose).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().documentType())
                    .isEqualTo(ElectronicDocumentType.DOC_EQUIV_POS);
        }

        @Test
        @DisplayName("un tipo null degrada a DOC_EQUIV_POS")
        void tipo_null_degrada_a_pos() {
            adapter.emitForClosedAccount(100L, 9L, null, false);

            ArgumentCaptor<EmitElectronicDocumentCommand> captor = ArgumentCaptor
                    .forClass(EmitElectronicDocumentCommand.class);
            verify(emitOnClose).execute(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue().documentType())
                    .isEqualTo(ElectronicDocumentType.DOC_EQUIV_POS);
        }
    }

    @Test
    @DisplayName("traslada openAccountId, companyId y finalConsumer tal cual")
    void traslada_los_demas_campos_tal_cual() {
        adapter.emitForClosedAccount(100L, 9L, "FE_VENTA", true);

        verify(emitOnClose).execute(
                new EmitElectronicDocumentCommand(100L, ElectronicDocumentType.FE_VENTA, 9L, true));
    }
}
