package itch.twp.reportes.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import itch.twp.reportes.dto.IncidenciaDTO;
import java.util.Collections;
import java.util.List;

@Component
public class IncidenciaClientFallback implements FallbackFactory<IncidenciaClient> {

    private static final Logger log = LoggerFactory.getLogger(IncidenciaClientFallback.class);

    @Override
    public IncidenciaClient create(Throwable cause) {
        log.error("Error al conectar con el servicio de incidencias: {}", cause.getMessage());
        return new IncidenciaClient() {
            @Override
            public List<IncidenciaDTO> listarParaEstadisticas() {
                log.warn("Returning empty list due to fallback - servicio incidencias no disponible");
                return Collections.emptyList();
            }
        };
    }
}