package itch.twp.reportes.servicio.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import itch.twp.reportes.client.IncidenciaClient;
import itch.twp.reportes.dto.IncidenciaDTO;
import itch.twp.reportes.dto.ReporteItemDto;
import itch.twp.reportes.servicio.ReporteServicio;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class ReporteServicioImp implements ReporteServicio {

    private final IncidenciaClient incidenciaClient;
 
    private final Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD);
    private final Font subtituloFont = new Font(Font.HELVETICA, 14, Font.BOLD);
    private final Font textoFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
    private final Font cursivaFont = new Font(Font.HELVETICA, 12, Font.ITALIC);
    private final Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

    // ==========================================
    // 1. CONFIGURACIÓN DEL ENCABEZADO (LAYOUT)
    // ==========================================
    
    private Document iniciarDocumento(ByteArrayOutputStream baos, String tituloReporte, String inicio, String fin) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Tabla de encabezado (Logo - Texto - Logo)
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1.2f, 3f, 1.2f});

        // Logo Izquierdo (Chilpancingo)
        headerTable.addCell(createImageCell("images/LogoChilpancingo.png", Element.ALIGN_LEFT));

        // Textos Centrales
        PdfPCell centroCell = new PdfPCell();
        centroCell.setBorder(Rectangle.NO_BORDER);
        centroCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph pCentro = new Paragraph();
        pCentro.add(new Chunk("ESTADO DE GUERRERO\n", new Font(Font.HELVETICA, 16, Font.BOLD)));
        pCentro.add(new Chunk("Ayuntamiento de Chilpancingo", new Font(Font.HELVETICA, 12, Font.NORMAL)));
        pCentro.setAlignment(Element.ALIGN_CENTER);
        centroCell.addElement(pCentro);
        headerTable.addCell(centroCell);

        // Logo Derecho (Renace)
        headerTable.addCell(createImageCell("images/LogoRenace.png", Element.ALIGN_RIGHT));

        document.add(headerTable);
        document.add(new Paragraph("\n"));

        // Título del reporte y fecha
        Paragraph pTitulo = new Paragraph(tituloReporte, tituloFont);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        document.add(pTitulo);
        
        if (inicio != null && !inicio.isEmpty()) {
            String periodo = (fin != null && !fin.isEmpty() && !inicio.equals(fin)) 
                             ? "Periodo: " + inicio + " al " + fin 
                             : "Fecha específica: " + inicio;
            Paragraph pPeriodo = new Paragraph(periodo, boldFont);
            pPeriodo.setAlignment(Element.ALIGN_CENTER);
            document.add(pPeriodo);
        }
        
        Paragraph pFecha = new Paragraph("Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), cursivaFont);
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        document.add(pFecha);
        
        document.add(new Paragraph("\n"));
        return document;
    }

    // ==========================================
    // 2. MÉTODOS DE GENERACIÓN DE REPORTES
    // ==========================================

    @Override
    public byte[] generarReporteColoniasPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // SE AÑADIÓ inicio y fin
            Document document = iniciarDocumento(baos, "Análisis de Situación por Colonia", inicio, fin);
            document.add(new Paragraph("Distribución geográfica de reportes ciudadanos y su estado de atención.\n", textoFont));
            
            insertarGrafico(document, "colonias.png");
            
            if (todas.isEmpty()) {
                document.add(new Paragraph("No hay incidencias registradas para el análisis.", textoFont));
            } else {
                for (IncidenciaDTO i : todas) {
                    String colonia = (i.getColonia() != null) ? i.getColonia() : (i.getLocalidad() != null ? i.getLocalidad() : "Ubicación no especificada");
                    
                    String asunto = (i.getTitulo() != null) ? i.getTitulo() : i.getNombreTipo();
                    
                    agregarBloqueDialogo(document, 
                        "Ciudadano (Zona: " + colonia + ")", 
                        "Se reporta: " + asunto + ".",
                        "Sistema: Reporte canalizado bajo el estatus [" + i.getNombreEstadoActual() + "]. " + (i.getDescripcion() != null ? "Nota: " + i.getDescripcion() : ""));
                }
            }
            
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de colonias", e);
        }
    }

    @Override
    public byte[] generarReporteTiposIncidenciaPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // SE AÑADIÓ inicio y fin
            Document document = iniciarDocumento(baos, "Clasificación de Reportes Ciudadanos", inicio, fin);
            document.add(new Paragraph("Resumen Narrativo de Tipos de Incidencias:\n", subtituloFont));
            
            insertarGrafico(document, "tipos.png");
            
            todas.stream()
                .collect(Collectors.groupingBy(i -> i.getNombreTipo() != null ? i.getNombreTipo() : "Tipo General"))
                .forEach((tipo, lista) -> {
                    try {
                        document.add(new Paragraph("Sobre los reportes clasificados como '" + tipo + "':", boldFont));
                        document.add(new Paragraph("Se han contabilizado un total de " + lista.size() + " casos. El sistema los ha registrado y canalizado a las áreas correspondientes para su pronta atención.", textoFont));
                        document.add(new Paragraph("\n"));
                    } catch (DocumentException e) { e.printStackTrace(); }
                });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de tipos", e);
        }
    }

    @Override
    public byte[] generarReporteEstatusYPromedioPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // SE AÑADIÓ inicio y fin
            Document document = iniciarDocumento(baos, "Reporte de Eficiencia y Estatus", inicio, fin);
            document.add(new Paragraph("Estado actual del seguimiento de atención ciudadana.\n", textoFont));
            
            insertarGrafico(document, "estatus.png");
            
            int total = todas.size();
            document.add(new Paragraph("Se han procesado un total de " + total + " incidencias en el sistema.", boldFont));
            document.add(new Paragraph("\n"));

            todas.stream()
                .collect(Collectors.groupingBy(i -> i.getNombreEstadoActual() != null ? i.getNombreEstadoActual() : "Estado Desconocido"))
                .forEach((estado, lista) -> {
                    try {
                        double porcentaje = (lista.size() * 100.0) / (total == 0 ? 1 : total);
                        document.add(new Paragraph("Estatus: [" + estado.toUpperCase() + "] - Representa el " + String.format("%.1f%%", porcentaje) + " del total.", boldFont));
                        document.add(new Paragraph("Actualmente hay " + lista.size() + " reportes en esta fase de atención.", textoFont));
                        document.add(new Paragraph("\n"));
                    } catch (DocumentException e) { e.printStackTrace(); }
                });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de estatus", e);
        }
    }
    
    @Override
    public byte[] generarReporteDepartamentosPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // SE AÑADIÓ inicio y fin
            Document document = iniciarDocumento(baos, "Distribución por Departamento", inicio, fin);
            document.add(new Paragraph("Carga de trabajo y canalización institucional.\n", textoFont));
            
            todas.stream()
                .filter(i -> i.getDepartamentoId() != null)
                .collect(Collectors.groupingBy(i -> i.getNombreDepartamento() != null ? i.getNombreDepartamento() : "Departamento #" + i.getDepartamentoId()))
                .forEach((depto, lista) -> {
                    try {
                        agregarBloqueDialogo(document, 
                            "Auditoría del Sistema", 
                            "Se ha verificado la carga operativa para el departamento de " + depto + ".",
                            "Respuesta Institucional: Tienen asignadas " + lista.size() + " incidencias para su resolución y seguimiento.");
                    } catch (DocumentException e) { e.printStackTrace(); }
                });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de departamentos", e);
        }
    }
    
    @Override
    public byte[] generarReportePersonalPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // SE AÑADIÓ inicio y fin
            Document document = iniciarDocumento(baos, "Desempeño del Personal Operativo", inicio, fin);
            document.add(new Paragraph("Registro de atención brindada por el personal del Ayuntamiento.\n", textoFont));
            
            todas.stream()
                .filter(i -> i.getPersonalId() != null)
                .collect(Collectors.groupingBy(i -> i.getNombrePersonal() != null ? i.getNombrePersonal() : "Personal #" + i.getPersonalId()))
                .forEach((personal, lista) -> {
                    try {
                        agregarBloqueDialogo(document, 
                            "Registro Operativo", 
                            "El servidor público " + personal + " ha estado activo en el sistema.",
                            "Métricas: Ha intervenido en la gestión de " + lista.size() + " reportes ciudadanos.");
                    } catch (DocumentException e) { e.printStackTrace(); }
                });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de personal", e);
        }
    }
    
    @Override
    public byte[] generarReporteUsuariosPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // SE AÑADIÓ inicio y fin
            Document document = iniciarDocumento(baos, "Participación Ciudadana", inicio, fin);
            document.add(new Paragraph("Métricas de ciudadanos que utilizan activamente la plataforma.\n", textoFont));
            
            todas.stream()
                .filter(i -> i.getUsuarioId() != null)
                .collect(Collectors.groupingBy(i -> i.getNombreUsuario() != null ? i.getNombreUsuario() : "Ciudadano Anónimo"))
                .entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size())) // Ordenar por los que más reportan
                .forEach(entry -> {
                    try {
                        document.add(new Paragraph("El usuario " + entry.getKey() + " ha contribuido reportando " + entry.getValue().size() + " incidencias en su comunidad.", textoFont));
                        document.add(new Paragraph("\n"));
                    } catch (DocumentException e) { e.printStackTrace(); }
                });

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de usuarios", e);
        }
    }
    
    @Override
    public byte[] generarReporteClimaPdf(String inicio, String fin) {
        List<IncidenciaDTO> todas = obtenerIncidencias(inicio, fin);
        
        long conAlerta = todas.stream().filter(i -> Boolean.TRUE.equals(i.getClimaAlerta())).count();
        long sinAlerta = todas.stream().filter(i -> i.getClimaAlerta() == null || !i.getClimaAlerta()).count();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // SE AÑADIÓ inicio y fin
            Document document = iniciarDocumento(baos, "Impacto de Condiciones Climáticas", inicio, fin);
            document.add(new Paragraph("Análisis de correlación entre reportes y alertas meteorológicas en la ciudad.\n", textoFont));
            
            agregarBloqueDialogo(document, 
                "Centro Meteorológico", 
                "Se detectaron condiciones climáticas adversas o alertas vigentes durante la recepción de reportes.",
                "Sistema de Incidencias: Durante estos periodos críticos, se recibieron y procesaron " + conAlerta + " reportes ciudadanos.");
                
            agregarBloqueDialogo(document, 
                "Operación Normal", 
                "Condiciones climáticas estables sin alertas meteorológicas.",
                "Sistema de Incidencias: En condiciones normales se han procesado " + sinAlerta + " reportes.");

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de clima", e);
        }
    }

    @Override
    public byte[] generarPdfPrueba() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // SE AÑADIÓ null, null porque es de prueba
            Document document = iniciarDocumento(baos, "Documento de Prueba y Calibración", null, null);
            
            document.add(new Paragraph("Este es un PDF de calibración para verificar que el diseño y el layout institucional funcionen correctamente en el servidor.\n", textoFont));
            
            agregarBloqueDialogo(document, 
                "Usuario de Pruebas", 
                "Solicito verificar el formato de diálogo y la carga de imágenes.",
                "Sistema: Los componentes visuales, incluyendo el encabezado de 'Estado de Guerrero' y la lógica de renderizado, operan de forma correcta.");
            
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de prueba", e);
        }
    }

    // ==========================================
    // 3. UTILIDADES Y MÉTODOS AUXILIARES
    // ==========================================

    private void agregarBloqueDialogo(Document document, String emisor, String mensaje, String respuesta) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(95);
        table.setSpacingBefore(10f);

        // Celda Emisor (Gris claro)
        PdfPCell cellEmisor = new PdfPCell(new Phrase(emisor + ": " + mensaje, textoFont));
        cellEmisor.setBackgroundColor(new java.awt.Color(245, 245, 245));
        cellEmisor.setPadding(8);
        cellEmisor.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.RIGHT);
        table.addCell(cellEmisor);

        // Celda Respuesta (Azul muy claro)
        PdfPCell cellRespuesta = new PdfPCell(new Phrase(respuesta, cursivaFont));
        cellRespuesta.setBackgroundColor(new java.awt.Color(230, 240, 255));
        cellRespuesta.setPadding(8);
        cellRespuesta.setBorder(Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT);
        table.addCell(cellRespuesta);

        document.add(table);
    }

    private PdfPCell createImageCell(String path, int alignment) {
        try {
            InputStream is = getLogoStream(path);
            if (is != null) {
                Image img = Image.getInstance(is.readAllBytes());
                img.scaleToFit(70, 70);
                PdfPCell cell = new PdfPCell(img);
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setHorizontalAlignment(alignment);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                return cell;
            }
        } catch (Exception e) { e.printStackTrace(); }
        PdfPCell empty = new PdfPCell();
        empty.setBorder(Rectangle.NO_BORDER);
        return empty;
    }

    private InputStream getLogoStream(String filename) {
        String[] paths = {
            filename,
            "static/" + filename,
            "static/images/" + filename,
            "META-INF/resources/" + filename,
            "META-INF/resources/static/" + filename
        };
        
        for (String path : paths) {
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is != null) {
                return is;
            }
        }
        return null;
    }

    private List<IncidenciaDTO> obtenerIncidencias(String inicio, String fin) {
        try {
            // Si mandan rango (inicio y fin)
            if (inicio != null && !inicio.isEmpty() && fin != null && !fin.isEmpty()) {
                return incidenciaClient.buscarPorFecha(inicio, fin);
            } 
            // Si mandan FECHA EXACTA (solo inicio)
            else if (inicio != null && !inicio.isEmpty()) {
                return incidenciaClient.buscarPorFecha(inicio, inicio);
            }
            // Si no mandan nada, traemos todo el histórico
            return incidenciaClient.listarParaEstadisticas();
        } catch (Exception e) {
            System.err.println("Advertencia: Falló la conexión con el MS de Incidencias.");
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    protected void insertarImagen(Document document, String imagePath, float width) {
        try {
            ClassPathResource resource = new ClassPathResource(imagePath);
            if (resource.exists()) {
                Image image = Image.getInstance(resource.getURL());
                image.scaleToFit(width, 200); // Mantiene proporción, max altura 200
                image.setAlignment(Image.ALIGN_CENTER);
                document.add(image);
            }
        } catch (IOException e) {
            System.err.println("No se encontró la imagen: " + imagePath);
        } catch (Exception e) {
            System.err.println("Error al insertar imagen: " + e.getMessage());
        }
    }

    protected void insertarGrafico(Document document, String nombreGrafico) {
        insertarImagen(document, "static/images/graficos/" + nombreGrafico, 400);
    }

    private void imprimirLista(Document document, String titulo, List<ReporteItemDto> datos) throws Exception {
        document.add(new Paragraph(titulo, subtituloFont));
        for (ReporteItemDto item : datos) {
            document.add(new Paragraph(" • " + item.getNombre() + " : " + item.getCantidad(), textoFont));
        }
    }
    
    @Override
    public byte[] generarReporteDetalladoIncidenciaPdf(Integer id) {
        try {
            IncidenciaDTO i = incidenciaClient.obtenerPorId(id);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                // SE AÑADIÓ null, null porque es para un solo reporte
                Document document = iniciarDocumento(baos, "Detalle Técnico de Incidencia #" + id, null, null);
                
                // Sección de Información General
                document.add(new Paragraph("Información General", subtituloFont));
                document.add(new Paragraph("Título: " + i.getTitulo(), textoFont));
                document.add(new Paragraph("Tipo: " + i.getNombreTipo(), textoFont));
                document.add(new Paragraph("Estado Actual: " + i.getNombreEstadoActual(), textoFont));
                document.add(new Paragraph("Fecha de Reporte: " + i.getFechaReporte(), textoFont));
                document.add(new Paragraph("\n"));

                // Sección de Ubicación
                document.add(new Paragraph("Ubicación del Incidente", subtituloFont));
                document.add(new Paragraph("Calle: " + i.getCalle(), textoFont));
                document.add(new Paragraph("Colonia: " + i.getColonia(), textoFont));
                document.add(new Paragraph("Localidad: " + i.getLocalidad(), textoFont));
                document.add(new Paragraph("\n"));

                // Sección de Asignación
                document.add(new Paragraph("Responsables y Seguimiento", subtituloFont));
                document.add(new Paragraph("Departamento: " + i.getNombreDepartamento(), textoFont));
                document.add(new Paragraph("Personal Asignado: " + i.getNombrePersonal(), textoFont));
                document.add(new Paragraph("Usuario que Reportó: " + i.getNombreUsuario(), textoFont));
                document.add(new Paragraph("\n"));

                // Sección Climática
                if (Boolean.TRUE.equals(i.getClimaAlerta())) {
                    document.add(new Paragraph("Condiciones Climáticas Especiales", subtituloFont));
                    document.add(new Paragraph("Alerta Activa: SÍ", textoFont));
                    document.add(new Paragraph("Observaciones: " + i.getObservacionesClima(), textoFont));
                }

                document.close();
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte detallado", e);
        }
    }
    
    @Override
    public byte[] generarReportePorPeriodoPdf(String inicio, String fin) {
        try {
            List<IncidenciaDTO> filtradas = incidenciaClient.buscarPorFecha(inicio, fin);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                // SE AÑADIÓ inicio y fin para que muestre el rango
                Document document = iniciarDocumento(baos, "Reporte de Incidencias General", inicio, fin);
                
                if (filtradas.isEmpty()) {
                    document.add(new Paragraph("No se encontraron incidencias en el periodo seleccionado.", textoFont));
                } else {
                    for (IncidenciaDTO i : filtradas) {
                        agregarBloqueDialogo(document, 
                            "Reporte #" + i.getId() + " - " + i.getColonia(),
                            i.getTitulo(),
                            "Estatus: " + i.getNombreEstadoActual() + ". Atendido por: " + i.getNombreDepartamento());
                    }
                }
                document.close();
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte por periodo", e);
        }
    }
}