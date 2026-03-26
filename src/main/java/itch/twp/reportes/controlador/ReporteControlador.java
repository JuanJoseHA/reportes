package itch.twp.reportes.controlador;

import itch.twp.reportes.servicio.ReporteServicio;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/reportes")
public class ReporteControlador {

    private ReporteServicio reporteServicio;

    @GetMapping("/colonias/descargar-pdf")
    public ResponseEntity<byte[]> descargarColoniasPdf() {
        byte[] pdfBytes = reporteServicio.generarReporteColoniasPdf();
        return construirRespuestaPdf(pdfBytes, "reporte-colonias.pdf");
    }

    @GetMapping("/tipos/descargar-pdf")
    public ResponseEntity<byte[]> descargarTiposPdf() {
        byte[] pdfBytes = reporteServicio.generarReporteTiposIncidenciaPdf();
        return construirRespuestaPdf(pdfBytes, "reporte-tipos-frecuentes.pdf");
    }

    @GetMapping("/eficiencia/descargar-pdf")
    public ResponseEntity<byte[]> descargarEstatusPdf() {
        byte[] pdfBytes = reporteServicio.generarReporteEstatusYPromedioPdf();
        return construirRespuestaPdf(pdfBytes, "reporte-eficiencia-transparencia.pdf");
    }

    // Método auxiliar para no repetir la configuración de cabeceras de Spring HTTP
    private ResponseEntity<byte[]> construirRespuestaPdf(byte[] pdfBytes, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}