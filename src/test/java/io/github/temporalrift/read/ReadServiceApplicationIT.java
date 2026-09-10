package io.github.temporalrift.read;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;
import org.springframework.modulith.docs.Documenter.CanvasOptions;
import org.springframework.modulith.docs.Documenter.DiagramOptions;

@ReadServiceIntegrationTest
class ReadServiceApplicationIT {

    @Test
    void contextLoads() {
        var modules = ApplicationModules.of(ReadServiceApplication.class);

        modules.verify();
        assertThat(modules.getModuleByName("projection"))
                .as("projection module must be detected by Spring Modulith")
                .isPresent();
        assertThat(modules.getModuleByName("notification"))
                .as("notification module must be detected by Spring Modulith")
                .isPresent();

        new Documenter(modules)
                .writeModulesAsPlantUml(DiagramOptions.defaults())
                .writeIndividualModulesAsPlantUml(DiagramOptions.defaults())
                .writeModuleCanvases(CanvasOptions.defaults());
    }
}
