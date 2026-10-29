package io.github.temporalrift.read.simulation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import io.github.temporalrift.read.ReadServiceIntegrationTest;

/** An ordinary deployment answers that the controls do not exist, whatever credential the caller holds. */
@ReadServiceIntegrationTest
class SimulationControlUnavailableIT {

    @Autowired
    MockMvc mvc;

    @Test
    void aControlTokenCannotReachAnOrdinaryDeployment() throws Exception {
        var control = jwt().authorities(new SimpleGrantedAuthority("SCOPE_simulation:control"));

        mvc.perform(delete("/internal/simulation/v1/execution")
                        .with(control)
                        .param("caseKey", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIMULATION_CONTROL_UNAVAILABLE"));
        mvc.perform(put("/internal/simulation/v1/execution")
                        .with(control)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SimulationFacts.context(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIMULATION_CONTROL_UNAVAILABLE"));
    }

    @Test
    void noCredentialIsReadBeforeAnsweringUnavailable() throws Exception {
        mvc.perform(get("/internal/simulation/v1/checkpoint").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIMULATION_CONTROL_UNAVAILABLE"));
    }
}
