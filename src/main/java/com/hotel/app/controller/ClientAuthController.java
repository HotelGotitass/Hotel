package com.hotel.app.controller;

import com.hotel.app.dto.request.ClientRegisterRequest;
import com.hotel.app.dto.request.VerifyOtpRequest;
import com.hotel.app.dto.response.ApiResponse;
import com.hotel.app.service.ClientAuthService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/client/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    // Cache simple en memoria para los buckets
    // RNF-06: Al estar en memoria, si hay varias instancias, el limite es por instancia (N * 5 peticiones/min).
    private final Map<String, Bucket> ipCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> emailCache = new ConcurrentHashMap<>();

    private Bucket resolveBucket(Map<String, Bucket> cache, String key, int capacity, Duration duration) {
        return cache.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(capacity, duration)))
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody ClientRegisterRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String email = request.getEmail().toLowerCase();
        
        // Limites: 5 registros/min por IP, 3 registros/min por correo
        Bucket ipBucket = resolveBucket(ipCache, ip, 5, Duration.ofMinutes(1));
        Bucket emailBucket = resolveBucket(emailCache, email, 3, Duration.ofMinutes(1));

        if (ipBucket.tryConsume(1) && emailBucket.tryConsume(1)) {
            // Nota: La integracion real de CAPTCHA se presentara como consulta al usuario.
            clientAuthService.registerClient(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Registro exitoso. Verifique su correo.", null));
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>(false, "Demasiadas peticiones. Intente mas tarde.", null));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String email = request.getEmail().toLowerCase();
        
        // Limites: 10 intentos/min por IP, 5 intentos/min por correo
        Bucket ipBucket = resolveBucket(ipCache, ip + "-verify", 10, Duration.ofMinutes(1));
        Bucket emailBucket = resolveBucket(emailCache, email + "-verify", 5, Duration.ofMinutes(1));

        if (ipBucket.tryConsume(1) && emailBucket.tryConsume(1)) {
            clientAuthService.verifyOtp(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Cuenta verificada con exito.", null));
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>(false, "Demasiados intentos de verificacion. Intente mas tarde.", null));
        }
    }
}
