package itch.twp.reportes.servicio.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import itch.twp.reportes.client.IncidenciaClient;
import itch.twp.reportes.dto.IncidenciaDTO;
import itch.twp.reportes.dto.ReporteItemDto;
import itch.twp.reportes.servicio.ReporteServicio;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor 
public class ReporteServicioImp implements ReporteServicio {

    private final IncidenciaClient incidenciaClient;
 
    private final Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD);
    private final Font subtituloFont = new Font(Font.HELVETICA, 14, Font.BOLD);
    private final Font textoFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
    private final Font cursivaFont = new Font(Font.HELVETICA, 12, Font.ITALIC);

    @Override
    public byte[] generarReporteColoniasPdf() {
        List<IncidenciaDTO> todas;
        try {
            todas = incidenciaClient.listarParaEstadisticas();
        } catch (Exception e) {
            todas = Collections.emptyList();
        }

        List<ReporteItemDto> datos = todas.stream()
            .filter(i -> i.getColonia() != null && !i.getColonia().trim().isEmpty())
            .collect(Collectors.groupingBy(IncidenciaDTO::getColonia, Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
            .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
            .collect(Collectors.toList());

        if (datos.isEmpty()) {
            datos = todas.stream()
                .filter(i -> i.getLocalidad() != null && !i.getLocalidad().trim().isEmpty())
                .collect(Collectors.groupingBy(IncidenciaDTO::getLocalidad, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
                .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
                .collect(Collectors.toList());
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Análisis Geográfico de Incidencias");
            
            document.add(new Paragraph("Distribución geográfica de reportes ciudadanos.", textoFont));
            
            // Insertar gráfico si existe
            insertarGrafico(document, "colonias.png");
            
            if (datos.isEmpty()) {
                document.add(new Paragraph("No hay datos disponibles.", textoFont));
            } else {
                imprimirLista(document, "Desglose por Colonia:", datos);
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de colonias", e);
        }
    }

    @Override
    public byte[] generarReporteTiposIncidenciaPdf() {
        List<IncidenciaDTO> todas;
        try {
            todas = incidenciaClient.listarParaEstadisticas();
        } catch (Exception e) {
            todas = Collections.emptyList();
        }
        
        // Agrupar por tipoId
        List<ReporteItemDto> datos = todas.stream()
            .filter(i -> i.getTipoId() != null)
            .collect(Collectors.groupingBy(
                i -> i.getNombreTipo() != null ? i.getNombreTipo() : "Tipo #" + i.getTipoId(),
                Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
            .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
            .collect(Collectors.toList());

        // Calcular total
        int total = datos.stream().mapToInt(ReporteItemDto::getCantidad).sum();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Reporte de Tipos de Incidencias");
            
            document.add(new Paragraph("Resumen por tipo de reporte ciudadano.", textoFont));
            document.add(new Paragraph("Total de incidencias: " + total, cursivaFont));
            document.add(new Paragraph("\n"));
            
            // Insertar gráfico
            insertarGrafico(document, "tipos.png");
            
            if (datos.isEmpty()) {
                document.add(new Paragraph("No hay datos disponibles.", textoFont));
            } else {
                imprimirLista(document, "Desglose por Tipo:", datos);
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de tipos", e);
        }
    }

    @Override
    public byte[] generarReporteEstatusYPromedioPdf() {
        List<IncidenciaDTO> todas;
        try {
            todas = incidenciaClient.listarParaEstadisticas();
        } catch (Exception e) {
            todas = Collections.emptyList();
        }
        
        List<ReporteItemDto> datos = todas.stream()
            .filter(i -> i.getNombreEstadoActual() != null)
            .collect(Collectors.groupingBy(IncidenciaDTO::getNombreEstadoActual, Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
            .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
            .collect(Collectors.toList());
        
        // Calcular total
        int total = datos.stream().mapToInt(ReporteItemDto::getCantidad).sum();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Reporte de Estados y Eficiencia");
            
            document.add(new Paragraph("Estado actual de todos los reportes.", textoFont));
            document.add(new Paragraph("Total de incidencias: " + total, cursivaFont));
            document.add(new Paragraph("\n"));
            
            // Insertar gráfico
            insertarGrafico(document, "estatus.png");
            
            if (datos.isEmpty()) {
                document.add(new Paragraph("No hay datos disponibles.", textoFont));
            } else {
                imprimirLista(document, "Estado de Reportes:", datos);
                
                for (ReporteItemDto item : datos) {
                    double porcentaje = (item.getCantidad() * 100.0) / total;
                    document.add(new Paragraph(String.format("   %s: %.1f%%", item.getNombre(), porcentaje), cursivaFont));
                }
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de estatus", e);
        }
    }
    
    public byte[] generarReporteDepartamentosPdf() {
        List<IncidenciaDTO> todas;
        try {
            todas = incidenciaClient.listarParaEstadisticas();
        } catch (Exception e) {
            todas = Collections.emptyList();
        }
        
        List<ReporteItemDto> datos = todas.stream()
            .filter(i -> i.getDepartamentoId() != null)
            .collect(Collectors.groupingBy(
                i -> i.getNombreDepartamento() != null ? i.getNombreDepartamento() : "Depto #" + i.getDepartamentoId(),
                Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
            .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
            .collect(Collectors.toList());
        
        int total = datos.stream().mapToInt(ReporteItemDto::getCantidad).sum();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Reporte por Departamento");
            
            document.add(new Paragraph("Distribución de incidencias por departamento.", textoFont));
            document.add(new Paragraph("Total: " + total + " incidencias", cursivaFont));
            document.add(new Paragraph("\n"));
            
            if (datos.isEmpty()) {
                document.add(new Paragraph("No hay datos disponibles.", textoFont));
            } else {
                imprimirLista(document, "Incidencias por Departamento:", datos);
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de departamentos", e);
        }
    }
    
    public byte[] generarReportePersonalPdf() {
        List<IncidenciaDTO> todas;
        try {
            todas = incidenciaClient.listarParaEstadisticas();
        } catch (Exception e) {
            todas = Collections.emptyList();
        }
        
        List<ReporteItemDto> datos = todas.stream()
            .filter(i -> i.getPersonalId() != null)
            .collect(Collectors.groupingBy(
                i -> i.getNombrePersonal() != null ? i.getNombrePersonal() : "Personal #" + i.getPersonalId(),
                Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
            .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
            .collect(Collectors.toList());
        
        int total = datos.stream().mapToInt(ReporteItemDto::getCantidad).sum();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Reporte de Personal");
            
            document.add(new Paragraph("Incidencias atendidas por cada miembro del personal.", textoFont));
            document.add(new Paragraph("Total atendidas: " + total, cursivaFont));
            document.add(new Paragraph("\n"));
            
            if (datos.isEmpty()) {
                document.add(new Paragraph("No hay datos disponibles.", textoFont));
            } else {
                imprimirLista(document, "Por Personal:", datos);
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de personal", e);
        }
    }
    
    public byte[] generarReporteUsuariosPdf() {
        List<IncidenciaDTO> todas;
        try {
            todas = incidenciaClient.listarParaEstadisticas();
        } catch (Exception e) {
            todas = Collections.emptyList();
        }
        
        List<ReporteItemDto> datos = todas.stream()
            .filter(i -> i.getUsuarioId() != null)
            .collect(Collectors.groupingBy(
                i -> i.getNombreUsuario() != null ? i.getNombreUsuario() : "Usuario #" + i.getUsuarioId(),
                Collectors.counting()))
            .entrySet().stream()
            .map(e -> new ReporteItemDto(e.getKey(), e.getValue().intValue()))
            .sorted((a, b) -> b.getCantidad().compareTo(a.getCantidad()))
            .collect(Collectors.toList());

        
        int total = datos.stream().mapToInt(ReporteItemDto::getCantidad).sum();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Reporte de Reportadores");
            
            document.add(new Paragraph("Ciudadanos que reportan más incidencias.", textoFont));
            document.add(new Paragraph("Total de reportes: " + total, cursivaFont));
            document.add(new Paragraph("\n"));
            
            if (datos.isEmpty()) {
                document.add(new Paragraph("No hay datos disponibles.", textoFont));
            } else {
                imprimirLista(document, "Top Reportadores:", datos);
            }
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de usuarios", e);
        }
    }
    
    public byte[] generarReporteClimaPdf() {
        List<IncidenciaDTO> todas;
        try {
            todas = incidenciaClient.listarParaEstadisticas();
        } catch (Exception e) {
            todas = Collections.emptyList();
        }
        
        long conAlerta = todas.stream()
            .filter(i -> Boolean.TRUE.equals(i.getClimaAlerta()))
            .count();
        
        long sinAlerta = todas.stream()
            .filter(i -> i.getClimaAlerta() == null || !i.getClimaAlerta())
            .count();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = iniciarDocumento(baos, "Reporte de Condiciones Climáticas");
            
            document.add(new Paragraph("Incidencias reportadas bajo alertas climáticas.", textoFont));
            document.add(new Paragraph("\n"));
            
            document.add(new Paragraph("Con alerta climática: " + conAlerta, subtituloFont));
            document.add(new Paragraph("Sin alerta climática: " + sinAlerta, subtituloFont));
            document.add(new Paragraph("Total: " + (conAlerta + sinAlerta), cursivaFont));
            
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error en reporte de clima", e);
        }
    }

    private Document iniciarDocumento(ByteArrayOutputStream baos, String tituloReporte) throws Exception {
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();
        
        // Obtener tamaño de página para posicionar correctamente
        float pageHeight = document.getPageSize().getHeight();
        float pageWidth = document.getPageSize().getWidth();
        
        // Insertar logos directamente aquí
        try {
            // Logo izquierdo - probar múltiples rutas
            InputStream logoIzqStream = getLogoStream("images/LogoChilpancingo.png");
            if (logoIzqStream != null) {
                byte[] logoBytes = logoIzqStream.readAllBytes();
                Image imgIzq = Image.getInstance(logoBytes);
                imgIzq.scaleToFit(150, 100);
                // Posición absoluta desde abajo izquierda
                imgIzq.setAbsolutePosition(20, pageHeight - 60);
                document.add(imgIzq);
            }
            
            // Logo derecho
            InputStream logoDerStream = getLogoStream("images/LogoRenace.png");
            if (logoDerStream != null) {
                byte[] logoBytes = logoDerStream.readAllBytes();
                Image imgDer = Image.getInstance(logoBytes);
                imgDer.scaleToFit(150, 100);
                imgDer.setAbsolutePosition(pageWidth - 120, pageHeight - 60);
                document.add(imgDer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Agregar espacio para que el contenido empiece después de los logos
        document.add(new Paragraph("\n\n"));

        document.add(new Paragraph(tituloReporte, tituloFont));
        document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), textoFont));
        document.add(new Paragraph("\n"));
        return document;
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

    private void imprimirLista(Document document, String titulo, List<ReporteItemDto> datos) throws Exception {
        document.add(new Paragraph(titulo, subtituloFont));
        for (ReporteItemDto item : datos) {
            document.add(new Paragraph(" • " + item.getNombre() + " : " + item.getCantidad(), textoFont));
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

    @Override
    public byte[] generarPdfPrueba() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            float pageHeight = document.getPageSize().getHeight();
            float pageWidth = document.getPageSize().getWidth();

            try {
                InputStream logoIzqStream = getLogoStream("images/LogoChilpancingo.png");
                if (logoIzqStream != null) {
                    byte[] logoBytes = logoIzqStream.readAllBytes();
                    Image imgIzq = Image.getInstance(logoBytes);
                    imgIzq.scaleToFit(125, 75);
                    imgIzq.setAbsolutePosition(20, pageHeight - 80);
                    document.add(imgIzq);
                }

                InputStream logoDerStream = getLogoStream("images/LogoRenace.png");
                if (logoDerStream != null) {
                    byte[] logoBytes = logoDerStream.readAllBytes();
                    Image imgDer = Image.getInstance(logoBytes);
                    imgDer.scaleToFit(125, 75);
                    imgDer.setAbsolutePosition(pageWidth - 120, pageHeight - 80);
                    document.add(imgDer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            document.add(new Paragraph("\n\n\n"));

            document.add(new Paragraph("PDF de Prueba - Diseño de Layout", tituloFont));
            document.add(new Paragraph("Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), textoFont));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Este es un PDF de prueba para verificar el diseño y layout.", textoFont));
            document.add(new Paragraph("Los logos deberían aparecer en la parte superior.", textoFont));
            document.add(new Paragraph("Este texto debería comenzar después de los logos.", textoFont));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Datos de ejemplo:", subtituloFont));
            document.add(new Paragraph("• Colonia Centro: 25 incidencias", textoFont));
            document.add(new Paragraph("• Colonia Norte: 18 incidencias", textoFont));
            document.add(new Paragraph("• Colonia Sur: 32 incidencias", textoFont));
            document.add(new Paragraph("• Colonia Este: 12 incidencias", textoFont));
            document.add(new Paragraph("• Colonia Oeste: 8 incidencias", textoFont));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Total de incidencias de ejemplo: 95", cursivaFont));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de prueba", e);
        }
    }
}