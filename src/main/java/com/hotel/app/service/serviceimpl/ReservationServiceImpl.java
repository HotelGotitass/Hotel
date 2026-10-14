package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.ReservationRequest;
import com.hotel.app.dto.response.CategoryAvailabilityResponse;
import com.hotel.app.dto.response.ReservationResponse;
import com.hotel.app.model.entity.*;
import com.hotel.app.model.enums.*;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ConflictException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.GuestRepository;
import com.hotel.app.repository.ReservationRepository;
import com.hotel.app.repository.RoomCategoryRepository;
import com.hotel.app.repository.RoomRepository;
import com.hotel.app.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomCategoryRepository categoryRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final com.hotel.app.repository.UserRepository userRepository;
    private final com.hotel.app.repository.PaymentRepository paymentRepository;
    private final com.hotel.app.repository.RefundRepository refundRepository;

    private static final List<ReservationStatus> ACTIVE_STATUSES = Arrays.asList(
            ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN
    );

    @Override
    @Transactional(readOnly = true)
    public List<CategoryAvailabilityResponse> searchAvailability(LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut);

        List<RoomCategory> categories = categoryRepository.findAll();

        return categories.stream().map(category -> {
            long totalRooms = roomRepository.countByCategoryId(category.getId());
            long overlapping = reservationRepository.countOverlappingReservations(category.getId(), checkIn, checkOut, ACTIVE_STATUSES);
            long available = totalRooms - overlapping;

            return CategoryAvailabilityResponse.builder()
                    .categoryId(category.getId())
                    .name(category.getName())
                    .description(category.getDescription())
                    .basePrice(category.getBasePrice())
                    .maxCapacity(category.getMaxCapacity())
                    .imageUrl(category.getImageUrl())
                    .availableRooms(Math.max(0, available))
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        Guest guest = guestRepository.findById(request.getGuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest no encontrado"));
                
        RoomCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));


            long totalRooms = roomRepository.countByCategoryId(category.getId());
            long overlapping = reservationRepository.countOverlappingReservations(
                    category.getId(), request.getCheckInDate(), request.getCheckOutDate(), ACTIVE_STATUSES);
            long available = totalRooms - overlapping;

            if (available <= 0) {
                throw new ConflictException("No hay habitaciones disponibles para la categoria y fechas seleccionadas.");
            }

            long days = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
            BigDecimal totalPrice = BigDecimal.valueOf(category.getBasePrice()).multiply(BigDecimal.valueOf(days));

            Reservation reservation = Reservation.builder()
                    .guest(guest)
                    .roomCategory(category)
                    .checkInDate(request.getCheckInDate())
                    .checkOutDate(request.getCheckOutDate())
                    .status(ReservationStatus.CONFIRMED)
                    .totalPrice(totalPrice)
                    .notes(request.getNotes())
                    .build();

            reservation = reservationRepository.save(reservation);
            return mapToResponse(reservation);

    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationResponse assignRoomAndCheckIn(Long reservationId, Long roomId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        
        if (reservation.getStatus() != ReservationStatus.CONFIRMED && reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("Solo las reservas confirmadas o pendientes pueden hacer check-in");
        }
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Habitacion no encontrada"));
                
        if (!room.getCategory().getId().equals(reservation.getRoomCategory().getId())) {
            throw new BusinessException("La habitacion no pertenece a la categoria reservada");
        }
        
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new BusinessException("La habitacion seleccionada no esta disponible");
        }
        
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        
        reservation.setRoom(room);
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        
        return mapToResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservationByClient(Long reservationId, String clientEmail) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        if (!reservation.getUser().getEmail().equals(clientEmail)) {
            throw new BusinessException("No tiene permiso para cancelar esta reserva");
        }
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new BusinessException("No se puede cancelar una reserva despues del check-in. Acerquese a recepcion.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledBy(CancelledBy.CLIENT);
        reservation.setCancellationReason("Cancelado por el cliente desde el portal web");
        return mapToResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponse cancelReservationByAdmin(Long reservationId, RefundReason refundReason) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
                
        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new BusinessException("La reserva ya esta inactiva");
        }
        
        if (reservation.getRoom() != null && reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            Room room = reservation.getRoom();
            room.setStatus(RoomStatus.CLEANING);
            roomRepository.save(room);
        }
        
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledBy(CancelledBy.HOTEL);
        reservation.setCancellationReason("Cancelado por el hotel. Motivo: " + refundReason.name());
        
        java.math.BigDecimal totalPaid = paymentRepository.findByReservationId(reservation.getId())
                .stream()
                .map(Payment::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        if (totalPaid.compareTo(java.math.BigDecimal.ZERO) > 0) {
            Refund refund = Refund.builder()
                    .reservation(reservation)
                    .amount(totalPaid)
                    .reason(refundReason)
                    .status(RefundStatus.REQUESTED)
                    .build();
            refundRepository.save(refund);
        }

        return mapToResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponse modifyReservationDates(Long reservationId, String clientEmail, LocalDate newCheckIn, LocalDate newCheckOut) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        if (!reservation.getUser().getEmail().equals(clientEmail)) {
            throw new BusinessException("No tiene permiso para modificar esta reserva");
        }

        if (reservation.getModificationCount() >= 1) {
            throw new BusinessException("Solo se permite modificar las fechas una vez por reserva");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.CHECKED_OUT || reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            throw new BusinessException("La reserva no esta en estado valido para modificacion");
        }

        if (!newCheckIn.isBefore(newCheckOut) || newCheckIn.isBefore(LocalDate.now())) {
            throw new BusinessException("Fechas invalidas");
        }

        if (java.time.LocalDateTime.now().isAfter(reservation.getCheckInDate().atStartOfDay().minusHours(48))) {
            throw new BusinessException("No se pueden modificar fechas con menos de 48 horas de anticipacion al check-in original");
        }

        long overlapping = reservationRepository.countOverlappingReservationsExcludingId(
                reservation.getRoomCategory().getId(), newCheckIn, newCheckOut, 
                java.util.Arrays.asList(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN),
                reservation.getId()
        );

        long totalRooms = roomRepository.countByCategoryId(reservation.getRoomCategory().getId());
        
        if (overlapping >= totalRooms) {
            throw new BusinessException("No hay disponibilidad en la categoria para las nuevas fechas");
        }

        long newNights = java.time.temporal.ChronoUnit.DAYS.between(newCheckIn, newCheckOut);
        java.math.BigDecimal newTotalPrice = java.math.BigDecimal.valueOf(reservation.getRoomCategory().getBasePrice()).multiply(java.math.BigDecimal.valueOf(newNights));

        reservation.setCheckInDate(newCheckIn);
        reservation.setCheckOutDate(newCheckOut);
        reservation.setTotalPrice(newTotalPrice);
        reservation.setModificationCount(reservation.getModificationCount() + 1);

        return mapToResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponse checkOut(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
                
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BusinessException("Solo las reservas con Check-In pueden hacer Check-Out");
        }
        
        Room room = reservation.getRoom();
        if (room != null) {
            room.setStatus(RoomStatus.CLEANING);
            roomRepository.save(room);
        }
        
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        return mapToResponse(reservationRepository.save(reservation));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessException("Las fechas son obligatorias.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BusinessException("La fecha de Check-In no puede ser en el pasado.");
        }
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new BusinessException("La fecha de Check-Out debe ser posterior al Check-In.");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > 30) {
            throw new BusinessException("La estadia maxima permitida es de 30 dias.");
        }
        if (checkIn.isAfter(LocalDate.now().plusYears(1))) {
            throw new BusinessException("No se pueden hacer reservas con mas de 1 anio de anticipacion.");
        }
    }

    private ReservationResponse mapToResponse(Reservation res) {
        return ReservationResponse.builder()
                .id(res.getId())
                .guestId(res.getGuest().getId())
                .guestName(res.getGuest().getFirstName() + " " + res.getGuest().getLastName())
                .categoryId(res.getRoomCategory().getId())
                .categoryName(res.getRoomCategory().getName())
                .roomId(res.getRoom() != null ? res.getRoom().getId() : null)
                .roomNumber(res.getRoom() != null ? res.getRoom().getRoomNumber() : null)
                .checkInDate(res.getCheckInDate())
                .checkOutDate(res.getCheckOutDate())
                .status(res.getStatus().name())
                .totalPrice(res.getTotalPrice())
                .notes(res.getNotes())
                .createdAt(res.getCreatedAt())
                .cancelledBy(res.getCancelledBy() != null ? res.getCancelledBy().name() : null)
                .cancellationReason(res.getCancellationReason())
                .modificationCount(res.getModificationCount())
                .build();
    }
    @Override
    @Transactional
    public ReservationResponse createWebReservation(com.hotel.app.dto.request.ClientReservationRequest request, String clientEmail) {
        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        RoomCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));

        User user = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Guest guest = guestRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new BusinessException("No se encontro un perfil de huesped asociado a su cuenta"));

        // Validar limite de 2 reservas PENDING y max 5 reservas activas
        List<Reservation> userReservations = reservationRepository.findByUserId(user.getId());
        long pendingCount = userReservations.stream().filter(r -> r.getStatus() == ReservationStatus.PENDING).count();
        long activeCount = userReservations.stream().filter(r -> ACTIVE_STATUSES.contains(r.getStatus())).count();

        if (pendingCount >= 2) {
            throw new ConflictException("Solo puede tener un maximo de 2 reservas pendientes de pago simultaneamente.");
        }
        if (activeCount >= 5) {
            throw new ConflictException("Ha alcanzado el limite maximo de 5 reservas activas.");
        }

        // Buscar habitacion disponible (la primera que encontremos que no se solape)
        List<Room> roomsInCategory = roomRepository.findByEstadoAndCategory(RoomStatus.AVAILABLE, category.getId());
        Room assignedRoom = null;

        for (Room room : roomsInCategory) {
            // Verificar solapamiento especifico para esta habitacion
            long overlaps = reservationRepository.countOverlappingRoomReservations(room.getId(), request.getCheckInDate(), request.getCheckOutDate(), ACTIVE_STATUSES);
            if (overlaps == 0) {
                assignedRoom = room;
                break;
            }
        }

        if (assignedRoom == null) {
            throw new ConflictException("No hay habitaciones disponibles en esta categoria para las fechas seleccionadas.");
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalPrice = BigDecimal.valueOf(category.getBasePrice()).multiply(BigDecimal.valueOf(nights));

        String bookingCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Reservation reservation = Reservation.builder()
                .guest(guest)
                .user(user)
                .roomCategory(category)
                .room(assignedRoom)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .status(ReservationStatus.PENDING)
                .totalPrice(totalPrice)
                .bookingCode(bookingCode)
                .originChannel("WEB")
                .modificationCount(0)
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(15))
                .build();

        reservation = reservationRepository.save(reservation);
        log.info("Web reservation created: {} with room {}", bookingCode, assignedRoom.getRoomNumber());
        return mapToResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getClientReservations(String clientEmail) {
        User user = userRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return reservationRepository.findByUserIdOrderByCheckInDateDesc(user.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }


    @Override
    public ReservationResponse getReservationByCode(String bookingCode) {
        return mapToResponse(reservationRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada")));
    }
}



