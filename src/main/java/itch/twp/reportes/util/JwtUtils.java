package itch.twp.reportes.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;

@Component
public class JwtUtils {

    private static final String SECRET_KEY = "VGhpcyBpcyBhIHN1cGVyIHNlY3JldCBrZXkgZm9yIEpXVCBhdXRoZW50aWNhdGlvbiAyMDI2";

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void validateToken(String token) {
        // Si el token fue alterado o expiró, esto lanzará una excepción
        extractAllClaims(token);
    }
}