package com.vetsoftware.app.publishadminpermissions.infrastructure.orchestration;

import static org.mockito.Mockito.verify;

import com.vetsoftware.app.publishadminpermissions.application.port.in.PublishAdminPermissionsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPermissionPublisherAdapter — puente hacia el caso de uso de publicacion")
class AdminPermissionPublisherAdapterTest {

    @Mock
    private PublishAdminPermissionsUseCase publishUseCase;
    @InjectMocks
    private AdminPermissionPublisherAdapter adapter;

    @Test
    @DisplayName("delega la publicacion en el caso de uso")
    void delega_la_publicacion_en_el_caso_de_uso() {
        adapter.publish();

        verify(publishUseCase).execute();
    }
}
