package co.edu.javeriana.husi.service.impl;

import co.edu.javeriana.husi.dto.response.DashboardResponse.ResidenteRevisar;
import co.edu.javeriana.husi.repository.DashboardRepository.ParCruzado;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Un residente puede tener varias rotaciones en el semestre, pero nunca dos a la vez: hay cruce
 * cuando dos rotaciones asignadas comparten al menos un día. Las consecutivas (una termina y al
 * día siguiente empieza la otra) son válidas, y el servicio o el programa no influyen.
 */
final class CrucesDashboard {

  static final String MOTIVO = "cruce";

  private CrucesDashboard() {}

  /** Un registro por residente, con todos sus cruces en el detalle, uno por línea. */
  static List<ResidenteRevisar> porResidente(List<ParCruzado> pares) {
    Map<Integer, ResidenteRevisar> residentes = new LinkedHashMap<>();
    for (ParCruzado par : pares) {
      residentes.merge(
        par.residenteId(),
        new ResidenteRevisar(
          par.residenteId(),
          par.residente(),
          par.programa(),
          MOTIVO,
          describir(par)
        ),
        (anterior, nuevo) ->
          new ResidenteRevisar(
            anterior.residenteId(),
            anterior.residente(),
            anterior.programa(),
            MOTIVO,
            anterior.detalle() + "\n" + nuevo.detalle()
          )
      );
    }
    return new ArrayList<>(residentes.values());
  }

  /** «Cadera (Alto Riesgo) y Columna (Alto Riesgo): del 2025-08-01 al 2025-09-30, 61 días». */
  static String describir(ParCruzado par) {
    long dias = ChronoUnit.DAYS.between(par.desde(), par.hasta()) + 1;
    return "%s (%s) y %s (%s): del %s al %s, %d %s".formatted(
      par.asignatura1(),
      par.servicio1(),
      par.asignatura2(),
      par.servicio2(),
      par.desde(),
      par.hasta(),
      dias,
      dias == 1 ? "día" : "días"
    );
  }
}
