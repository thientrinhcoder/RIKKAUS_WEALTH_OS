package com.rikkaus.wealth.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test for the meta resource.
 *
 * <p>The clock arrives as a {@code @MockitoBean} rather than a competing {@code @Bean} so that no
 * bean-definition conflict is possible regardless of what {@code @WebMvcTest} decides to include in
 * the slice.
 */
@WebMvcTest(MetaController.class)
class MetaControllerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-27T07:42:00Z");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private Clock clock;

    @BeforeEach
    void fixTheClock() {
        given(clock.instant()).willReturn(FIXED_INSTANT);
    }

    @Test
    void returnsApiMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/meta"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.apiVersion").value("v1"))
                .andExpect(jsonPath("$.application").value("rikkaus-wealth-api"))
                // An exact assertion rather than a tolerance, which also pins the serialization
                // format: an ISO-8601 string, never an epoch number.
                .andExpect(jsonPath("$.serverTime").value("2026-09-27T07:42:00Z"));
    }

    @Test
    void reportsABuildVersion() throws Exception {
        mockMvc.perform(get("/api/v1/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildVersion").isNotEmpty());
    }

    @Test
    void reportsAnUnknownBuildVersionWhenBuildInfoIsAbsent() {
        // Running from an IDE without the Maven build-info goal leaves no BuildProperties bean.
        // A genuinely empty provider from a bare bean factory reproduces that, rather than a stub
        // that only asserts the branch the production classpath already takes.
        var emptyBeanFactory = new DefaultListableBeanFactory();
        var controller =
                new MetaController(
                        clock,
                        "rikkaus-wealth-api",
                        emptyBeanFactory.getBeanProvider(BuildProperties.class));

        assertThat(controller.meta().buildVersion()).isEqualTo("unknown");
    }

    @Test
    void isNotReachableWithoutTheVersionedPrefix() throws Exception {
        // Pins the prefix wiring rather than the controller method alone: if @RequestMapping ever
        // lost ApiPaths.V1, the test above would still pass against a bare /meta.
        mockMvc.perform(get("/meta")).andExpect(status().isNotFound());
    }
}
