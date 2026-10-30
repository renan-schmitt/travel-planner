package com.travelplanner.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Encaminha para o {@code index.html} da SPA (gerado em {@code frontend/dist} e servido como
 * recurso estático) as rotas do React Router, para que um refresh ou link direto funcione.
 *
 * <p>Ficam de fora: {@code /api/**}, {@code /oauth2/**}, {@code /login/**} (o callback do OAuth; a
 * página {@code /login} em si é da SPA), {@code /actuator/**} e qualquer caminho cujo último
 * segmento tenha extensão (arquivos estáticos). Controllers mais específicos têm prioridade sobre
 * este mapeamento genérico.
 */
@Controller
public class SpaForwardController {

  private static final Pattern BACKEND_PATHS =
      Pattern.compile("^/(api|oauth2|actuator)(/.*)?$|^/login/.*$");

  // O PathPatternParser só aceita `**` no início ou no fim; o prefixo é conferido no método.
  @GetMapping({"/", "/**/{last:[^.]*}"})
  String forward(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (BACKEND_PATHS.matcher(path).matches()) {
      throw new NotFoundException("Recurso não encontrado.");
    }
    return "forward:/index.html";
  }
}
