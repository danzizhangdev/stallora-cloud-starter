package io.github.danzizhangdev.stallora.common;

import io.github.danzizhangdev.stallora.common.error.BusinessException;
import io.github.danzizhangdev.stallora.common.error.ErrorCode;
import io.github.danzizhangdev.stallora.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    enum TestCodes implements ErrorCode {
        TEAPOT;

        public String code() {
            return "TST-0418";
        }

        public String defaultMessage() {
            return "I am a teapot";
        }

        public int httpStatus() {
            return 418;
        }
    }

    @RestController
    static class BoomController {
        @GetMapping("/boom/business")
        String business() {
            throw new BusinessException(TestCodes.TEAPOT);
        }

        @GetMapping("/boom/unexpected")
        String unexpected() {
            throw new IllegalStateException("nope");
        }
    }

    MockMvc mvc = MockMvcBuilders.standaloneSetup(new BoomController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void businessException_mapsStatusAndCode() throws Exception {
        mvc.perform(get("/boom/business"))
           .andExpect(status().is(418))
           .andExpect(jsonPath("$.code").value("TST-0418"))
           .andExpect(jsonPath("$.message").value("I am a teapot"));
    }

    @Test
    void unexpectedException_maps500WithoutLeakingDetails() throws Exception {
        mvc.perform(get("/boom/unexpected"))
           .andExpect(status().is(500))
           .andExpect(jsonPath("$.code").value("CMN-0500"))
           .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}
