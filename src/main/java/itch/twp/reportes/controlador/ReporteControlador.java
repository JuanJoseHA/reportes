package itch.twp.reportes.controlador;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import itch.twp.reportes.client.FeignClientInterceptor;
import itch.twp.reportes.servicio.ReporteServicio;
import lombok.AllArgsConstructor;

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
    
    @GetMapping("/departamentos/descargar-pdf")
    public ResponseEntity<byte[]> descargarDepartamentosPdf() {
        byte[] pdfBytes = reporteServicio.generarReporteDepartamentosPdf();
        return construirRespuestaPdf(pdfBytes, "reporte-departamentos.pdf");
    }
    
    @GetMapping("/personal/descargar-pdf")
    public ResponseEntity<byte[]> descargarPersonalPdf() {
        byte[] pdfBytes = reporteServicio.generarReportePersonalPdf();
        return construirRespuestaPdf(pdfBytes, "reporte-personal.pdf");
    }
    
    @GetMapping("/usuarios/descargar-pdf")
    public ResponseEntity<byte[]> descargarUsuariosPdf() {
        byte[] pdfBytes = reporteServicio.generarReporteUsuariosPdf();
        return construirRespuestaPdf(pdfBytes, "reporte-usuarios.pdf");
    }
    
    @GetMapping("/clima/descargar-pdf")
    public ResponseEntity<byte[]> descargarClimaPdf() {
        byte[] pdfBytes = reporteServicio.generarReporteClimaPdf();
        return construirRespuestaPdf(pdfBytes, "reporte-clima.pdf");
    }

    @GetMapping("/test-pdf-layout")
    public ResponseEntity<byte[]> testPdfLayout() {
        return construirRespuestaPdf(reporteServicio.generarPdfPrueba(), "test-layout.pdf");
    }

    @GetMapping("/diagnostico")
    public ResponseEntity<String> diagnosticoIncidencias() {
        try {
            reporteServicio.generarReporteColoniasPdf();
            return ResponseEntity.ok("Diagnóstico completado.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }



    private ResponseEntity<byte[]> construirRespuestaPdf(byte[] pdfBytes, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}