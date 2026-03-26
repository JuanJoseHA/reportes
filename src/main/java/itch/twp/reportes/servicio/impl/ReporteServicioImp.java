package itch.twp.reportes.servicio.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import itch.twp.reportes.dto.ReporteItemDto;
import itch.twp.reportes.repositorio.ReporteRepositorio;
import itch.twp.reportes.servicio.ReporteServicio;
import lombok.AllArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@AllArgsConstructor 
public class ReporteServicioImp implements ReporteServicio {

    private ReporteRepositorio reporteRepositorio;

    // FUENTES GLOBALES PARA EL DISEÑO
    private final Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD);
    private final Font subtituloFont = new Font(Font.HELVETICA, 14, Font.BOLD);
    private final Font textoFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
    private final Font cursivaFont = new Font(Font.HELVETICA, 12, Font.ITALIC);

    // =========================================================================
    // 1. REPORTE DE COLONIAS (FOCOS ROJOS)
    // =========================================================================
    @Override
    public byte[] generarReporteColoniasPdf() {
        List<ReporteItemDto> datos = reporteRepositorio.obtenerIncidenciasPorColonia();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Análisis Geográfico de Incidencias");

            // NARRATIVA DINÁMICA
            document.add(new Paragraph("El presente documento detalla la distribución geográfica de los reportes ciudadanos " +
                    "en la ciudad de Chilpancingo. Analizar estas métricas permite a las autoridades identificar " +
                    "los 'focos rojos' para priorizar el envío de cuadrillas y la asignación de presupuesto urbano.", textoFont));
            document.add(new Paragraph("\n"));

            if (!datos.isEmpty()) {
                ReporteItemDto top1 = datos.get(0);
                document.add(new Paragraph("Hallazgo Principal: Se observa que la colonia '" + top1.getNombre() + 
                        "' concentra la mayor cantidad de reportes en la ciudad, acumulando un total de " + 
                        top1.getCantidad() + " incidencias hasta la fecha.", cursivaFont));
                document.add(new Paragraph("\n"));
            }

            imprimirLista(document, "Desglose por Colonia:", datos);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de colonias", e);
        }
    }

    // =========================================================================
    // 2. REPORTE DE TIPOS DE PROBLEMAS
    // =========================================================================
    @Override
    public byte[] generarReporteTiposIncidenciaPdf() {
        List<ReporteItemDto> datos = reporteRepositorio.obtenerTopTiposIncidencias();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Reporte de Problemas Urbanos Frecuentes");

            document.add(new Paragraph("Este informe expone las categorías de incidencias más recurrentes reportadas " +
                    "por los ciudadanos. Comprender el tipo de problema predominante es fundamental para la correcta " +
                    "adquisición de materiales operativos (asfalto, luminarias, tuberías) y la capacitación del personal.", textoFont));
            document.add(new Paragraph("\n"));

            if (!datos.isEmpty()) {
                document.add(new Paragraph("Hallazgo Principal: El problema urbano más urgente de atender actualmente es: " + 
                        datos.get(0).getNombre() + " (representando " + datos.get(0).getCantidad() + " reportes activos).", cursivaFont));
                document.add(new Paragraph("\n"));
            }

            imprimirLista(document, "Categorías Reportadas:", datos);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de tipos", e);
        }
    }

    // =========================================================================
    // 3. REPORTE DE ESTATUS Y TIEMPOS DE RESPUESTA
    // =========================================================================
    @Override
    public byte[] generarReporteEstatusYPromedioPdf() {
        List<ReporteItemDto> datos = reporteRepositorio.obtenerEstatusGlobal();
        Double tiempoPromedio = reporteRepositorio.obtenerTiempoPromedioResolucion();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Métricas de Eficiencia y Transparencia");

            document.add(new Paragraph("Este reporte refleja el nivel de atención y respuesta del H. Ayuntamiento " +
                    "frente a las demandas ciudadanas. Muestra el estado actual de los tickets " +
                    "y evalúa la velocidad de resolución de las cuadrillas.", textoFont));
            document.add(new Paragraph("\n"));

            // DATO DINÁMICO DE TIEMPO
            document.add(new Paragraph("INDICADOR DE RENDIMIENTO (KPI):", subtituloFont));
            document.add(new Paragraph("El tiempo promedio que tarda el Ayuntamiento en resolver una incidencia " +
                    "desde que el ciudadano la reporta es de: " + Math.round(tiempoPromedio) + " horas.", cursivaFont));
            document.add(new Paragraph("\n"));

            imprimirLista(document, "Estado Actual de los Reportes (Volumen):", datos);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de estatus", e);
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES (Para no repetir código de diseño)
    // =========================================================================
    private Document iniciarDocumento(ByteArrayOutputStream baos, String tituloReporte) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();

        document.add(new Paragraph(tituloReporte, tituloFont));
        document.add(new Paragraph("H. Ayuntamiento de Chilpancingo - Sistema de Incidencias", new Font(Font.HELVETICA, 12, Font.BOLDITALIC)));
        document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), textoFont));
        document.add(new Paragraph("-----------------------------------------------------------------------------------------"));
        document.add(new Paragraph("\n"));
        return document;
    }

    private void imprimirLista(Document document, String titulo, List<ReporteItemDto> datos) throws Exception {
        document.add(new Paragraph(titulo, subtituloFont));
        if (datos.isEmpty()) {
            document.add(new Paragraph("   Sin datos registrados.", textoFont));
        } else {
            for (ReporteItemDto item : datos) {
                document.add(new Paragraph("   • " + item.getNombre() + " : " + item.getCantidad() + " incidencias", textoFont));
            }
        }
    }
}