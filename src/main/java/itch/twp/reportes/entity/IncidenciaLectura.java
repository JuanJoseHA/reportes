package itch.twp.reportes.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "incidencias")
public class IncidenciaLectura {
    @Id
    private Long id;
    
    // No necesitamos mapear las demás columnas porque usaremos un Query Nativo
}