package itch.twp.reportes.servicio;

public interface ReporteServicio {
    byte[] generarReporteColoniasPdf();
    byte[] generarReporteTiposIncidenciaPdf();
    byte[] generarReporteEstatusYPromedioPdf();
    byte[] generarReporteDepartamentosPdf();
    byte[] generarReportePersonalPdf();
    byte[] generarReporteUsuariosPdf();
    byte[] generarReporteClimaPdf();
    byte[] generarPdfPrueba();
}

