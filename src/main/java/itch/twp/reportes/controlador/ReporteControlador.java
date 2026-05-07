package itch.twp.reportes.controlador;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import itch.twp.reportes.servicio.ReporteServicio;
import lombok.AllArgsConstructor;

@CrossOrigin(origins = "*")
@RestController
@AllArgsConstructor
@RequestMapping("/api/reportes")
public class ReporteControlador {

    private ReporteServicio reporteServicio;

    @GetMapping("/colonias/descargar-pdf")
    public ResponseEntity<byte[]> descargarColoniasPdf(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin) {
        byte[] pdfBytes = reporteServicio.generarReporteColoniasPdf(inicio, fin);
        return construirRespuestaPdf(pdfBytes, "reporte-colonias.pdf");
    }

    @GetMapping("/tipos/descargar-pdf")
    public ResponseEntity<byte[]> descargarTiposPdf(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin) {
        byte[] pdfBytes = reporteServicio.generarReporteTiposIncidenciaPdf(inicio, fin);
        return construirRespuestaPdf(pdfBytes, "reporte-tipos-frecuentes.pdf");
    }

    @GetMapping("/eficiencia/descargar-pdf")
    public ResponseEntity<byte[]> descargarEstatusPdf(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin) {
        byte[] pdfBytes = reporteServicio.generarReporteEstatusYPromedioPdf(inicio, fin);
        return construirRespuestaPdf(pdfBytes, "reporte-eficiencia-transparencia.pdf");
    }
    
    @GetMapping("/departamentos/descargar-pdf")
    public ResponseEntity<byte[]> descargarDepartamentosPdf(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin) {
        byte[] pdfBytes = reporteServicio.generarReporteDepartamentosPdf(inicio, fin);
        return construirRespuestaPdf(pdfBytes, "reporte-departamentos.pdf");
    }
    
    @GetMapping("/personal/descargar-pdf")
    public ResponseEntity<byte[]> descargarPersonalPdf(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin) {
        byte[] pdfBytes = reporteServicio.generarReportePersonalPdf(inicio, fin);
        return construirRespuestaPdf(pdfBytes, "reporte-personal.pdf");
    }
    
    @GetMapping("/usuarios/descargar-pdf")
    public ResponseEntity<byte[]> descargarUsuariosPdf(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin) {
        byte[] pdfBytes = reporteServicio.generarReporteUsuariosPdf(inicio, fin);
        return construirRespuestaPdf(pdfBytes, "reporte-usuarios.pdf");
    }
    
    @GetMapping("/clima/descargar-pdf")
    public ResponseEntity<byte[]> descargarClimaPdf(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin) {
        byte[] pdfBytes = reporteServicio.generarReporteClimaPdf(inicio, fin);
        return construirRespuestaPdf(pdfBytes, "reporte-clima.pdf");
    }

    @GetMapping("/test-pdf-layout")
    public ResponseEntity<byte[]> testPdfLayout() {
        return construirRespuestaPdf(reporteServicio.generarPdfPrueba(), "test-layout.pdf");
    }

    @GetMapping("/diagnostico")
    public ResponseEntity<String> diagnosticoIncidencias() {
        try {
            // Se envían valores nulos para forzar la búsqueda general
            reporteServicio.generarReporteColoniasPdf(null, null);
            return ResponseEntity.ok("Diagnóstico completado. Comunicación exitosa con Incidencias.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
    // --- NUEVOS ENDPOINTS DE FILTRADO Y DETALLE ---

    @GetMapping("/periodo")
    public ResponseEntity<byte[]> reportePorPeriodo(
            @RequestParam String inicio, 
            @RequestParam String fin) {
        byte[] pdfBytes = reporteServicio.generarReportePorPeriodoPdf(inicio, fin);
        return construirRespuestaPdf(pdfBytes, "reporte-periodo.pdf");
    }

    @GetMapping("/detalle/{id}")
    public ResponseEntity<byte[]> reporteDetallado(@PathVariable Integer id) {
        byte[] pdfBytes = reporteServicio.generarReporteDetalladoIncidenciaPdf(id);
        return construirRespuestaPdf(pdfBytes, "incidencia-detalle-" + id + ".pdf");
    }

    // --- MÉTODO DE UTILIDAD PARA RESPUESTAS ---

    private ResponseEntity<byte[]> construirRespuestaPdf(byte[] pdfBytes, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}