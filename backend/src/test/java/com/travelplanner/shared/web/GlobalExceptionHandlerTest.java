package com.travelplanner.shared.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new TestController())
          .setControllerAdvice(new GlobalExceptionHandler())
          .build();

  @Test
  void validationErrorReturns400WithFieldList() throws Exception {
    mvc.perform(
            post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\", \"quantity\": 0}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors.length()").value(2))
        .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'quantity')]").exists());
  }

  @Test
  void notFoundReturns404() throws Exception {
    mvc.perform(get("/test/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Viagem não encontrada."));
  }

  @Test
  void forbiddenReturns403() throws Exception {
    mvc.perform(get("/test/forbidden"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void optimisticLockReturns409() throws Exception {
    mvc.perform(get("/test/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void unexpectedErrorReturns500WithoutInternals() throws Exception {
    mvc.perform(get("/test/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail").value("Erro interno."))
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("segredo interno"))));
  }

  record TestRequest(@NotBlank String name, @Positive int quantity) {}

  @RestController
  static class TestController {

    @PostMapping("/test/validate")
    void validate(@Valid @RequestBody TestRequest request) {}

    @GetMapping("/test/not-found")
    void notFound() {
      throw new NotFoundException("Viagem não encontrada.");
    }

    @GetMapping("/test/forbidden")
    void forbidden() {
      throw new ForbiddenException("Sem permissão.");
    }

    @GetMapping("/test/conflict")
    void conflict() {
      throw new OptimisticLockingFailureException("versão divergente");
    }

    @GetMapping("/test/boom")
    void boom() {
      throw new IllegalStateException("segredo interno");
    }
  }
}
