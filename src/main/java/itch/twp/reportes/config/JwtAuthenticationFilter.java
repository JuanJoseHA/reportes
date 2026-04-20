package itch.twp.reportes.config;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import itch.twp.reportes.client.FeignClientInterceptor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private String secretKey = "VGhpcyBpcyBhIHN1cGVyIHNlY3JldCBrZXkgZm9yIEpXVCBhdXRoZW50aWNhdGlvbiAyMDI2";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                
                Claims claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String email = claims.getSubject();
                String rol = claims.get("rol", String.class);

                if (email != null) {
                    FeignClientInterceptor.setCurrentToken("Bearer " + token);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            email, token, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rol)));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                FeignClientInterceptor.clearCurrentToken();
                SecurityContextHolder.clearContext();
            }
        } else {
            FeignClientInterceptor.clearCurrentToken();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            FeignClientInterceptor.clearCurrentToken();
        }
    }
}