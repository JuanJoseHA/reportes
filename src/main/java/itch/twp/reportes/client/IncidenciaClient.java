package itch.twp.reportes.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import itch.twp.reportes.dto.IncidenciaDTO;

@FeignClient(name = "incidencia-service", url = "http://26.116.60.216:8082", fallbackFactory = IncidenciaClientFallback.class)
public interface IncidenciaClient {
    @GetMapping("/api/incidencias/estadisticas")
    List<IncidenciaDTO> listarParaEstadisticas();

 // En IncidenciaClient.java
    @GetMapping("/api/incidencias/{id}")
    IncidenciaDTO obtenerPorId(@PathVariable("id") Integer id);

    @GetMapping("/api/incidencias/buscar/fecha")
    List<IncidenciaDTO> buscarPorFecha(
        @RequestParam("inicio") String inicio, 
        @RequestParam("fin") String fin
    );
}