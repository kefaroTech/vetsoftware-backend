package com.vetsoftware.app.publishadminpermissions.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.publishadminpermissions.application.dto.PublishAdminPermissionsDto;
import com.vetsoftware.app.publishadminpermissions.application.port.in.PublishAdminPermissionsUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del controller: ruta, codigo de estado y forma del JSON. El caso
 * de uso se dobla — aqui no se prueba la logica de publicacion, se prueba el
 * contrato que ve el front.
 */
@WebMvcTest(AdminPermissionPublishController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("AdminPermissionPublishController — contrato HTTP")
class AdminPermissionPublishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublishAdminPermissionsUseCase publishAdminPermissionsUseCase;

    @Test
    @DisplayName("POST /admin/admin-permissions/publish responde 200 con los contadores de la publicacion")
    void post_responde_200_con_los_contadores() throws Exception {
        when(publishAdminPermissionsUseCase.execute())
                .thenReturn(new PublishAdminPermissionsDto(3, 2, 5, 4));

        mockMvc.perform(post("/admin/admin-permissions/publish")).andExpect(status().isOk())
                .andExpect(jsonPath("$.companiesProcessed").value(3))
                .andExpect(jsonPath("$.companiesUpdated").value(2))
                .andExpect(jsonPath("$.permissionsCreated").value(5))
                .andExpect(jsonPath("$.rolePermissionsCreated").value(4));
    }
}
