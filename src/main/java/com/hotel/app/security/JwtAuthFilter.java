package com.hotel.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtil.validateJwtToken(jwt)) {
                String username = jwtUtil.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Se verifica en CADA peticion: deshabilitar la cuenta o forzar cierre surte efecto de inmediato
                if (!userDetails.isAccountNonLocked()) {
                    reject(response, request, "ACCOUNT_DISABLED",
                            "Tu cuenta fue deshabilitada por el administrador. Contacta con el hotel.");
                    return;
                }
                // Comparacion exacta de version (no depende de la hora de emision, que el JWT guarda en segundos)
                if (userDetails instanceof UserDetailsImpl impl
                        && jwtUtil.getTokenVersionFromJwtToken(jwt) != impl.getTokenVersion()) {
                    reject(response, request, "SESSION_REVOKED",
                            "Tu sesión fue cerrada por el administrador. Inicia sesión nuevamente.");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("No se puede establecer la autenticación del usuario: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, HttpServletRequest request, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("status", 401);
        body.put("error", code);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        mapper.writeValue(response.getOutputStream(), body);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
