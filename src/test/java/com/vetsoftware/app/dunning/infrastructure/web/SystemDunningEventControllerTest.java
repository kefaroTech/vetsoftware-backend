package com.vetsoftware.app.dunning.infrastructure.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.port.in.ListAllDunningEventsUseCase;
import com.vetsoftware.app.dunning.testsupport.DunningEventMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemDunningEventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemDunningEventController - contrato HTTP cross-tenant")
class SystemDunningEventControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ListAllDunningEventsUseCase listUseCase;

    @Test
    @DisplayName("expone tenant, contrato y propaga filtro y pagina")
    void expone_tenant_contrato_y_paginacion() throws Exception {
        DunningEventDto event = DunningEventDto.from(DunningEventMother.recordatorio());
        when(listUseCase.listAll(DunningEventMother.EMPRESA, 3, 4))
                .thenReturn(PageResult.of(List.of(event), 3, 4, 17));

        mockMvc.perform(get("/system/dunning-events")
                .param("companyId", DunningEventMother.EMPRESA.toString()).param("page", "3")
                .param("pageSize", "4")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].companyId").value(DunningEventMother.EMPRESA))
                .andExpect(jsonPath("$.content[0].subscription.id").value(11))
                .andExpect(jsonPath("$.content[0].eventType").value("REMINDER_SENT"))
                .andExpect(jsonPath("$.page").value(3)).andExpect(jsonPath("$.pageSize").value(4))
                .andExpect(jsonPath("$.totalElements").value(17));

        verify(listUseCase).listAll(DunningEventMother.EMPRESA, 3, 4);
    }
}
