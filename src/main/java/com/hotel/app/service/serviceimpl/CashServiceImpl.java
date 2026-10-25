package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.CashMovementRequest;
import com.hotel.app.dto.request.CloseCashRequest;
import com.hotel.app.dto.response.CashMovementResponse;
import com.hotel.app.dto.response.CashSessionResponse;
import com.hotel.app.dto.response.PaymentResponse;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ConflictException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.model.entity.*;
import com.hotel.app.model.enums.*;
import com.hotel.app.repository.*;
import com.hotel.app.service.CashService;
import com.hotel.app.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashServiceImpl implements CashService {

    // Los pagos en linea entran por el webhook de Mercado Pago, no por la caja fisica
    private static final Set<PaymentMethod> ONLINE_METHODS = Set.of(PaymentMethod.MERCADO_PAGO, PaymentMethod.ONLINE_GATEWAY);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ConsumptionRepository consumptionRepository;
    private final PaymentService paymentService;

    @Override
    @Transactional(readOnly = true)
    public Optional<CashSessionResponse> getCurrentSession() {
        return sessionRepository.findFirstByStatus(CashSessionStatus.OPEN).map(this::toDetail);
    }

    @Override
    @Transactional
    public CashSessionResponse openSession(BigDecimal openingAmount, String user) {
        sessionRepository.findFirstByStatus(CashSessionStatus.OPEN).ifPresent(open -> {
            throw new ConflictException("Ya hay una caja abierta por " + open.getOpenedBy()
                    + " desde " + open.getOpenedAt().format(TIME) + ". Ciérrela antes de abrir otra.");
        });
        CashSession session = sessionRepository.save(CashSession.builder()
                .status(CashSessionStatus.OPEN)
                .openedBy(user)
                .openedAt(LocalDateTime.now())
                .openingAmount(money(openingAmount))
                .build());
        log.info("Caja {} abierta por {} con S/ {}", session.getId(), user, session.getOpeningAmount());
        return toDetail(session);
    }

    @Override
    @Transactional
    public CashSessionResponse addMovement(CashMovementRequest request, String user) {
        CashSession session = lockOpenSession();
        PaymentMethod method = request.getMethod() != null ? request.getMethod() : PaymentMethod.CASH;
        if (ONLINE_METHODS.contains(method)) {
            throw new BusinessException("Los pagos en línea no se registran en la caja");
        }
        BigDecimal amount = money(request.getAmount());
        if (request.getType() == CashMovementType.EXPENSE && method == PaymentMethod.CASH
                && amount.compareTo(expectedCash(session)) > 0) {
            throw new BusinessException("No hay suficiente efectivo en caja (disponible: S/ " + expectedCash(session) + ")");
        }
        movementRepository.save(CashMovement.builder()
                .session(session)
                .type(request.getType())
                .method(method)
                .amount(amount)
                .concept(request.getConcept().trim())
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build());
        return toDetail(session);
    }

    @Override
    @Transactional
    public CashSessionResponse closeSession(CloseCashRequest request, String user, boolean isAdmin) {
        CashSession session = lockOpenSession();
        if (!isAdmin && !session.getOpenedBy().equalsIgnoreCase(user)) {
            throw new BusinessException("Solo quien abrió la caja (" + session.getOpenedBy() + ") o un administrador puede cerrarla");
        }
        BigDecimal expected = expectedCash(session);
        BigDecimal counted = money(request.getCountedAmount());
        session.setExpectedCash(expected);
        session.setCountedAmount(counted);
        session.setDifference(counted.subtract(expected));
        session.setNotes(request.getNotes());
        session.setClosedBy(user);
        session.setClosedAt(LocalDateTime.now());
        session.setStatus(CashSessionStatus.CLOSED);
        log.info("Caja {} cerrada por {}: esperado S/ {}, contado S/ {}, diferencia S/ {}",
                session.getId(), user, expected, counted, session.getDifference());
        return toDetail(sessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashSessionResponse> getHistory() {
        return sessionRepository.findTop30ByStatusOrderByOpenedAtDesc(CashSessionStatus.CLOSED).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CashSessionResponse getSession(Long id) {
        return toDetail(sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada")));
    }

    @Override
    @Transactional
    public PaymentResponse registerReservationPayment(Long reservationId, BigDecimal rawAmount, PaymentMethod method,
                                                      String reference, String user) {
        if (method == null || ONLINE_METHODS.contains(method)) {
            throw new BusinessException("Los pagos con Mercado Pago se registran automáticamente; elija otro medio de pago");
        }
        CashSession session = sessionRepository.findByStatusForUpdate(CashSessionStatus.OPEN)
                .orElseThrow(() -> new BusinessException("Abra la caja antes de registrar cobros"));
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("No se puede cobrar una reserva cancelada");
        }

        BigDecimal amount = money(rawAmount);
        BigDecimal paid = paymentService.getAmountPaid(reservationId);
        Optional<Invoice> invoice = invoiceRepository.findFirstByReservationIdAndStatusNotOrderByIdDesc(reservationId, InvoiceStatus.VOID);
        // Con comprobante emitido el total incluye consumos; sin el, estadia + consumos registrados
        BigDecimal totalDue = invoice.map(Invoice::getTotalAmount).orElseGet(() -> totalDue(reservation));
        BigDecimal balance = totalDue.subtract(paid);
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La reserva no tiene saldo pendiente");
        }
        if (amount.compareTo(balance) > 0) {
            throw new BusinessException("El monto excede el saldo pendiente (S/ " + money(balance) + ")");
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .reservation(reservation)
                .amount(amount)
                .paymentMethod(method)
                .reference(reference != null && !reference.isBlank() ? reference.trim() : null)
                .build());
        movementRepository.save(CashMovement.builder()
                .session(session)
                .type(CashMovementType.INCOME)
                .method(method)
                .amount(amount)
                .concept("Cobro reserva " + reservation.getBookingCode())
                .payment(payment)
                .reservation(reservation)
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build());

        BigDecimal newPaid = paid.add(amount);
        // Mismas reglas que el webhook: el anticipo confirma la reserva
        if (reservation.getStatus() == ReservationStatus.PENDING
                && newPaid.compareTo(paymentService.getDepositAmount(reservation)) >= 0) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setExpiresAt(null);
            reservationRepository.save(reservation);
        }
        invoice.filter(inv -> inv.getStatus() == InvoiceStatus.PENDING && newPaid.compareTo(inv.getTotalAmount()) >= 0)
                .ifPresent(inv -> {
                    inv.setStatus(InvoiceStatus.PAID);
                    invoiceRepository.save(inv);
                });

        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .method(method.name())
                .reference(payment.getReference())
                .createdAt(payment.getCreatedAt())
                .reservationStatus(reservation.getStatus().name())
                .amountPaid(newPaid)
                .balanceDue(totalDue.subtract(newPaid).max(BigDecimal.ZERO))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getReservationPayments(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId).stream()
                .map(p -> PaymentResponse.builder()
                        .id(p.getId())
                        .amount(p.getAmount())
                        .method(p.getPaymentMethod().name())
                        .reference(p.getReference())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void registerCashRefund(Long reservationId, BigDecimal rawAmount, String user) {
        CashSession session = sessionRepository.findByStatusForUpdate(CashSessionStatus.OPEN)
                .orElseThrow(() -> new BusinessException("Abra la caja para devolver el dinero en efectivo"));
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        BigDecimal amount = money(rawAmount);
        BigDecimal available = expectedCash(session);
        if (amount.compareTo(available) > 0) {
            throw new BusinessException("No hay suficiente efectivo en caja para el reembolso (disponible: S/ " + available + ")");
        }
        movementRepository.save(CashMovement.builder()
                .session(session)
                .type(CashMovementType.EXPENSE)
                .method(PaymentMethod.CASH)
                .amount(amount)
                .concept("Reembolso reserva " + reservation.getBookingCode())
                .reservation(reservation)
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ------------------------------------------------------------------

    private CashSession lockOpenSession() {
        return sessionRepository.findByStatusForUpdate(CashSessionStatus.OPEN)
                .orElseThrow(() -> new BusinessException("No hay una caja abierta"));
    }

    private BigDecimal totalDue(Reservation reservation) {
        BigDecimal consumptions = consumptionRepository.findByReservationId(reservation.getId()).stream()
                .map(Consumption::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return reservation.getTotalPrice().add(consumptions);
    }

    /** Efectivo esperado = fondo inicial + ingresos en efectivo - egresos en efectivo. */
    private BigDecimal expectedCash(CashSession session) {
        BigDecimal total = session.getOpeningAmount();
        for (CashMovement m : movementRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())) {
            if (m.getMethod() != PaymentMethod.CASH) continue;
            total = m.getType() == CashMovementType.INCOME ? total.add(m.getAmount()) : total.subtract(m.getAmount());
        }
        return money(total);
    }

    private CashSessionResponse toSummary(CashSession s) {
        return base(s).build();
    }

    private CashSessionResponse toDetail(CashSession s) {
        List<CashMovement> movements = movementRepository.findBySessionIdOrderByCreatedAtAsc(s.getId());
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        for (CashMovement m : movements) {
            if (m.getType() == CashMovementType.INCOME) {
                income = income.add(m.getAmount());
                byMethod.merge(m.getMethod().name(), m.getAmount(), BigDecimal::add);
            } else {
                expense = expense.add(m.getAmount());
            }
        }
        return base(s)
                .expectedCash(s.getStatus() == CashSessionStatus.OPEN ? expectedCash(s) : s.getExpectedCash())
                .totalIncome(money(income))
                .totalExpense(money(expense))
                .incomeByMethod(byMethod)
                .movements(movements.stream().map(m -> CashMovementResponse.builder()
                        .id(m.getId())
                        .type(m.getType().name())
                        .method(m.getMethod().name())
                        .amount(m.getAmount())
                        .concept(m.getConcept())
                        .bookingCode(m.getReservation() != null ? m.getReservation().getBookingCode() : null)
                        .createdBy(m.getCreatedBy())
                        .createdAt(m.getCreatedAt())
                        .build()).toList())
                .build();
    }

    private CashSessionResponse.CashSessionResponseBuilder base(CashSession s) {
        return CashSessionResponse.builder()
                .id(s.getId())
                .status(s.getStatus().name())
                .openedBy(s.getOpenedBy())
                .openedAt(s.getOpenedAt())
                .openingAmount(s.getOpeningAmount())
                .closedBy(s.getClosedBy())
                .closedAt(s.getClosedAt())
                .countedAmount(s.getCountedAmount())
                .difference(s.getDifference())
                .expectedCash(s.getExpectedCash())
                .notes(s.getNotes());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
