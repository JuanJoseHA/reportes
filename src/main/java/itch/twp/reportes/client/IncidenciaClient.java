package itch.twp.reportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import itch.twp.reportes.dto.IncidenciaDTO;
import java.util.List;

@FeignClient(name = "servicio-incidencias", url = "http://192.168.212.117:8082")
public interface IncidenciaClient {

    @GetMapping("/api/incidencias/estadisticas")
    List<IncidenciaDTO> listarParaEstadisticas();
}