package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.response.BrickPaymentResponse;
import com.hotel.app.dto.response.PaymentPreferenceResponse;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.integration.MercadoPagoClient;
import com.hotel.app.model.entity.Payment;
import com.hotel.app.model.entity.PaymentAttempt;
import com.hotel.app.model.entity.Refund;
import com.hotel.app.model.entity.Reservation;
import com.hotel.app.model.enums.PaymentMethod;
import com.hotel.app.model.enums.PaymentOption;
import com.hotel.app.model.enums.RefundReason;
import com.hotel.app.model.enums.RefundStatus;
import com.hotel.app.model.enums.ReservationStatus;
import com.hotel.app.repository.PaymentAttemptRepository;
import com.hotel.app.repository.PaymentRepository;
import com.hotel.app.repository.RefundRepository;
import com.hotel.app.repository.ReservationRepository;
import com.hotel.app.service.EmailService;
import com.hotel.app.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final DateTimeFormatter MP_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RefundRepository refundRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final EmailService emailService;

    @Value("${booking.deposit-percentage:50}")
    private BigDecimal depositPercentage;

    @Value("${mercadopago.currency:PEN}")
    private String currency;

    @Value("${mercadopago.statement-descriptor:HOTEL GOTITAS}")
    private String statementDescriptor;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Override
    @Transactional
    public PaymentPreferenceResponse createPreference(Long reservationId, String clientEmail, PaymentOption option) {
        Reservation res = findClientReservation(reservationId, clientEmail);
        BigDecimal amount = resolveAmount(res, option);

        Map<String, Object> body = buildPreferenceBody(res, amount, option, clientEmail);
        Map<String, Object> pref = mercadoPagoClient.createPreference(body, UUID.randomUUID().toString());
        String preferenceId = String.valueOf(pref.get("id"));

        paymentAttemptRepository.save(PaymentAttempt.builder()
                .reservation(res)
                .amount(amount)
                .status("PREFERENCE_CREATED")
                .paymentMethod(PaymentMethod.MERCADO_PAGO.name())
                .preferenceId(preferenceId)
                .build());

        log.info("Preferencia MP {} creada para reserva {} ({} {} {})", preferenceId, res.getBookingCode(), option, currency, amount);

        return PaymentPreferenceResponse.builder()
                .preferenceId(preferenceId)
                .initPoint((String) pref.get("init_point"))
                .sandboxInitPoint((String) pref.get("sandbox_init_point"))
                .bookingCode(res.getBookingCode())
                .amount(amount)
                .build();
    }

    @Override
    @Transactional
    public BrickPaymentResponse payWithBrick(Long reservationId, String clientEmail, PaymentOption option, Map<String, Object> formData) {
        Reservation res = findClientReservation(reservationId, clientEmail);
        BigDecimal amount = resolveAmount(res, option);

        Object token = formData.get("token");
        Object paymentMethodId = formData.get("payment_method_id");
        if (token == null || paymentMethodId == null) {
            throw new BusinessException("Datos de tarjeta incompletos. Vuelva a ingresarlos.");
        }

        // Solo se copian los campos del Brick que Mercado Pago necesita; el monto lo fija el backend
        Map<String, Object> payer = new HashMap<>();
        payer.put("email", clientEmail);
        if (formData.get("payer") instanceof Map<?, ?> brickPayer) {
            if (brickPayer.get("email") != null) payer.put("email", brickPayer.get("email"));
            if (brickPayer.get("identification") != null) payer.put("identification", brickPayer.get("identification"));
        }
        payer.put("first_name", res.getGuest().getFirstName());
        payer.put("last_name", res.getGuest().getLastName());

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", amount);
        body.put("token", token);
        body.put("payment_method_id", paymentMethodId);
        if (formData.get("issuer_id") != null) body.put("issuer_id", formData.get("issuer_id"));
        body.put("installments", 1);
        body.put("payer", payer);
        body.put("description", paymentConcept(option) + " reserva " + res.getBookingCode() + " - " + res.getRoomCategory().getName());
        body.put("external_reference", res.getBookingCode());
        body.put("statement_descriptor", statementDescriptor);
        body.put("metadata", Map.of("reservation_id", res.getId(), "option", option.name()));
        // Aprobado o rechazado al instante: la reserva expira en minutos y no puede quedar "en revision"
        body.put("binary_mode", true);
        if (backendUrl.startsWith("https://")) {
            body.put("notification_url", backendUrl + "/api/v1/payments/webhook");
        }

        Map<String, Object> mpPayment = mercadoPagoClient.createPayment(body, UUID.randomUUID().toString());
        String paymentId = String.valueOf(mpPayment.get("id"));
        String status = String.valueOf(mpPayment.get("status"));
        log.info("Pago Brick MP {} para reserva {} ({} {} {}): {}", paymentId, res.getBookingCode(), option, currency, amount, status);

        // Registrar ya mismo sin esperar el webhook (que en local sin https no llega); es idempotente
        processPaymentNotification(paymentId);

        return BrickPaymentResponse.builder()
                .paymentId(paymentId)
                .status(status)
                .statusDetail((String) mpPayment.get("status_detail"))
                .bookingCode(res.getBookingCode())
                .amount(amount)
                .build();
    }

    private Reservation findClientReservation(Long reservationId, String clientEmail) {
        Reservation res = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        if (res.getUser() == null || !res.getUser().getEmail().equals(clientEmail)) {
            throw new BusinessException("No tiene permiso para pagar esta reserva");
        }
        return res;
    }

    private BigDecimal resolveAmount(Reservation res, PaymentOption option) {
        BigDecimal paid = getAmountPaid(res.getId());
        BigDecimal balance = res.getTotalPrice().subtract(paid);
        BigDecimal amount = switch (res.getStatus()) {
            case PENDING -> {
                if (res.getExpiresAt() != null && res.getExpiresAt().isBefore(LocalDateTime.now())) {
                    throw new BusinessException("El tiempo para pagar esta reserva expiro. Realice una nueva reserva.");
                }
                if (option == PaymentOption.DEPOSIT) yield getDepositAmount(res).subtract(paid);
                if (option == PaymentOption.FULL) yield balance;
                throw new BusinessException("Para una reserva pendiente elija pagar el anticipo o el total");
            }
            case CONFIRMED -> {
                if (option != PaymentOption.BALANCE) {
                    throw new BusinessException("La reserva ya esta confirmada; solo puede pagar el saldo pendiente");
                }
                yield balance;
            }
            default -> throw new BusinessException("La reserva no admite pagos en su estado actual");
        };

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La reserva no tiene saldo pendiente");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String paymentConcept(PaymentOption option) {
        return switch (option) {
            case DEPOSIT -> "Anticipo";
            case FULL -> "Pago total";
            case BALANCE -> "Saldo";
        };
    }

    private Map<String, Object> buildPreferenceBody(Reservation res, BigDecimal amount, PaymentOption option, String clientEmail) {
        long nights = ChronoUnit.DAYS.between(res.getCheckInDate(), res.getCheckOutDate());

        Map<String, Object> item = new HashMap<>();
        item.put("id", res.getBookingCode());
        item.put("title", paymentConcept(option) + " reserva " + res.getBookingCode() + " - " + res.getRoomCategory().getName() + " (" + nights + " noches)");
        item.put("category_id", "travels");
        item.put("quantity", 1);
        item.put("currency_id", currency);
        item.put("unit_price", amount);

        Map<String, Object> payer = new HashMap<>();
        payer.put("email", clientEmail);
        payer.put("name", res.getGuest().getFirstName());
        payer.put("surname", res.getGuest().getLastName());

        String resultUrl = frontendUrl + "/payment/result";
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("payer", payer);
        body.put("external_reference", res.getBookingCode());
        body.put("statement_descriptor", statementDescriptor);
        body.put("back_urls", Map.of("success", resultUrl, "pending", resultUrl, "failure", resultUrl));
        body.put("metadata", Map.of("reservation_id", res.getId(), "option", option.name()));
        // Pagos en efectivo (PagoEfectivo / agentes) tardan dias y la reserva expira en minutos
        body.put("payment_methods", Map.of(
                "excluded_payment_types", List.of(Map.of("id", "ticket"), Map.of("id", "atm")),
                "installments", 1));

        // Mercado Pago rechaza auto_return y notification_url que no sean URLs publicas https
        if (frontendUrl.startsWith("https://")) {
            body.put("auto_return", "approved");
        }
        if (backendUrl.startsWith("https://")) {
            body.put("notification_url", backendUrl + "/api/v1/payments/webhook");
        } else {
            log.warn("BACKEND_URL no es https: Mercado Pago no enviara webhooks. Use un tunel (ngrok) en desarrollo.");
        }

        if (res.getStatus() == ReservationStatus.PENDING && res.getExpiresAt() != null) {
            ZoneId zone = ZoneId.systemDefault();
            body.put("expires", true);
            body.put("expiration_date_from", LocalDateTime.now().atZone(zone).format(MP_DATE));
            body.put("expiration_date_to", res.getExpiresAt().atZone(zone).format(MP_DATE));
        }
        return body;
    }

    @Override
    @Transactional
    public void processPaymentNotification(String paymentId) {
        Map<String, Object> mpPayment = mercadoPagoClient.getPayment(paymentId);

        String status = String.valueOf(mpPayment.get("status"));
        String bookingCode = (String) mpPayment.get("external_reference");
        String paidCurrency = (String) mpPayment.get("currency_id");
        BigDecimal amount = new BigDecimal(String.valueOf(mpPayment.get("transaction_amount")));

        if (bookingCode == null) {
            log.warn("Pago MP {} sin external_reference, se ignora", paymentId);
            return;
        }

        Reservation res = reservationRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada para el pago " + paymentId));

        paymentAttemptRepository.save(PaymentAttempt.builder()
                .reservation(res)
                .amount(amount)
                .status(status.toUpperCase())
                .paymentMethod(PaymentMethod.MERCADO_PAGO.name())
                .externalReference(paymentId)
                .build());

        if (!"approved".equals(status)) {
            log.info("Pago MP {} en estado {} para reserva {}", paymentId, status, bookingCode);
            return;
        }
        if (!currency.equals(paidCurrency)) {
            log.error("Pago MP {} con moneda inesperada {} (se esperaba {})", paymentId, paidCurrency, currency);
            return;
        }
        // Mercado Pago reintenta y envia payment.created + payment.updated: registrar una sola vez
        if (paymentRepository.existsByReference(paymentId)) {
            log.info("Pago MP {} ya registrado, notificacion duplicada", paymentId);
            return;
        }

        paymentRepository.save(Payment.builder()
                .reservation(res)
                .amount(amount)
                .paymentMethod(PaymentMethod.MERCADO_PAGO)
                .reference(paymentId)
                .build());

        if (res.getStatus() == ReservationStatus.CANCELLED) {
            // La reserva expiro mientras el cliente pagaba: el dinero se devuelve
            refundRepository.save(Refund.builder()
                    .reservation(res)
                    .amount(amount)
                    .reason(RefundReason.LATE_PAYMENT_NO_AVAILABILITY)
                    .status(RefundStatus.REQUESTED)
                    .build());
            log.warn("Pago MP {} aprobado para reserva cancelada {}. Reembolso solicitado.", paymentId, bookingCode);
            return;
        }

        if (res.getStatus() == ReservationStatus.PENDING) {
            BigDecimal totalPaid = getAmountPaid(res.getId());
            if (totalPaid.compareTo(getDepositAmount(res)) >= 0) {
                res.setStatus(ReservationStatus.CONFIRMED);
                res.setExpiresAt(null);
                reservationRepository.save(res);
                sendConfirmationEmail(res, totalPaid);
                log.info("Pago MP {} aprobado. Reserva {} confirmada.", paymentId, bookingCode);
            } else {
                log.warn("Pago MP {} aprobado pero no cubre el anticipo de la reserva {}", paymentId, bookingCode);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAmountPaid(Long reservationId) {
        return paymentRepository.sumAmountByReservationId(reservationId);
    }

    @Override
    public BigDecimal getDepositAmount(Reservation reservation) {
        return reservation.getTotalPrice()
                .multiply(depositPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private void sendConfirmationEmail(Reservation res, BigDecimal totalPaid) {
        String to = res.getUser() != null ? res.getUser().getEmail() : res.getGuest().getEmail();
        if (to == null) return;
        BigDecimal balance = res.getTotalPrice().subtract(totalPaid);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto">
                  <h2 style="margin-bottom:4px">Reserva confirmada</h2>
                  <p>Hola %s, recibimos tu pago. Tu reserva en Hotel Gotitas esta confirmada.</p>
                  <table style="width:100%%;border-collapse:collapse">
                    <tr><td>Codigo</td><td><b>%s</b></td></tr>
                    <tr><td>Habitacion</td><td>%s</td></tr>
                    <tr><td>Check-in</td><td>%s</td></tr>
                    <tr><td>Check-out</td><td>%s</td></tr>
                    <tr><td>Pagado</td><td>S/ %s</td></tr>
                    <tr><td>Saldo a pagar en el hotel</td><td>S/ %s</td></tr>
                  </table>
                  <p style="color:#666;font-size:12px">Presenta tu codigo de reserva en recepcion.</p>
                </div>
                """.formatted(res.getGuest().getFirstName(), res.getBookingCode(), res.getRoomCategory().getName(),
                res.getCheckInDate(), res.getCheckOutDate(), totalPaid.setScale(2, RoundingMode.HALF_UP),
                balance.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        try {
            emailService.sendHtmlEmail(to, "Reserva " + res.getBookingCode() + " confirmada - Hotel Gotitas", html);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de confirmacion de {}", res.getBookingCode(), e);
        }
    }
}
