package itch.twp.reportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import itch.twp.reportes.dto.IncidenciaDTO;
import java.util.List;

@FeignClient(name = "incidencia-service", url = "http://26.116.60.216:8082", fallbackFactory = IncidenciaClientFallback.class)
public interface IncidenciaClient {
    @GetMapping("/api/incidencias/estadisticas")
    List<IncidenciaDTO> listarParaEstadisticas();
}