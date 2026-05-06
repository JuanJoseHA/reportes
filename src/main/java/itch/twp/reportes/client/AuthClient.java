package itch.twp.reportes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import itch.twp.reportes.dto.UsuarioDetalleDTO;

@FeignClient(name = "SERVICIO-AUTH", url = "http://26.87.230.97:8088")
public interface AuthClient {

    @GetMapping("/api/auth/usuario/{id}")
    UsuarioDetalleDTO obtenerUsuarioPorId(@PathVariable("id") Long id);
    
}

