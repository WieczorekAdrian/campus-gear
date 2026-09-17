package com.campusgear.demo;

import com.campusgear.demo.dto.LoanResponseDTO;
import com.campusgear.demo.dto.LoanReturnDTO;
import com.campusgear.demo.entity.EquipmentEntity;
import com.campusgear.demo.entity.LoanEntity;
import com.campusgear.demo.entity.ReservationEntity;
import com.campusgear.demo.entity.UserEntity;
import com.campusgear.demo.exception.ReservationConflictException;
import com.campusgear.demo.exception.ResourceNotFoundException;
import com.campusgear.demo.repository.DefectReportEntityRepository;
import com.campusgear.demo.repository.LoanEntityRepository;
import com.campusgear.demo.repository.ReservationEntityRepository;
import com.campusgear.demo.repository.UserEntityRepository;
import com.campusgear.demo.service.LoanService;
import com.campusgear.demo.status.EquipmentStatus;
import com.campusgear.demo.status.ReservationStatus;
import com.campusgear.demo.status.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanEntityRepository loanRepository;

    @Mock
    private ReservationEntityRepository reservationRepository;

    @Mock
    private UserEntityRepository userRepository;

    @Mock
    private DefectReportEntityRepository defectReportRepository;

    @InjectMocks
    private LoanService loanService;

    private UserEntity user(String email, Long id, Role role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private EquipmentEntity equipment(Long id) {
        EquipmentEntity equipment = new EquipmentEntity();
        equipment.setId(id);
        equipment.setDeviceType("Laptop");
        equipment.setTechnicalSpecification("Dell XPS");
        equipment.setSerialNumber("DELL-12345");
        equipment.setLocation("Main Hall");
        equipment.setStatus(EquipmentStatus.DOSTEPNY);
        return equipment;
    }

    private ReservationEntity reservation(Long id, Long ownerId) {
        ReservationEntity r = new ReservationEntity();
        r.setId(id);
        r.setStartDate(LocalDateTime.now().plusDays(1));
        r.setEndDate(LocalDateTime.now().plusDays(3));
        r.setStatus(ReservationStatus.AKTYWNA);
        r.setUser(user("owner@campus.edu.pl", ownerId, Role.ROLE_STUDENT));
        r.setEquipment(equipment(1L));
        return r;
    }

    @Test
    void shouldForbidIssueForOwner() {
        ReservationEntity r = reservation(5L, 10L);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        when(loanRepository.existsByReservationId(5L)).thenReturn(false);
        when(userRepository.findByEmail("owner@campus.edu.pl"))
                .thenReturn(Optional.of(user("owner@campus.edu.pl", 10L, Role.ROLE_STUDENT)));

        assertThatThrownBy(() -> loanService.issueLoan(5L, "owner@campus.edu.pl"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("tylko opiekun");
    }

    @Test
    void shouldLetOpiekunIssueForeignLoan() {
        ReservationEntity r = reservation(5L, 10L);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        when(loanRepository.existsByReservationId(5L)).thenReturn(false);
        when(userRepository.findByEmail("opiekun@campus.edu.pl"))
                .thenReturn(Optional.of(user("opiekun@campus.edu.pl", 20L, Role.ROLE_OPIEKUN)));
        when(userRepository.getReferenceById(10L))
                .thenReturn(user("owner@campus.edu.pl", 10L, Role.ROLE_STUDENT));
        when(loanRepository.save(any())).thenAnswer(inv -> {
            LoanEntity loan = inv.getArgument(0);
            loan.setId(50L);
            return loan;
        });

        LoanResponseDTO result = loanService.issueLoan(5L, "opiekun@campus.edu.pl");

        assertThat(result.id()).isEqualTo(50L);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.WYPOZYCZONA);
    }

    @Test
    void shouldRejectIssueForNonActiveReservation() {
        ReservationEntity r = reservation(5L, 10L);
        r.setStatus(ReservationStatus.ANULOWANA);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> loanService.issueLoan(5L, "owner@campus.edu.pl"))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("tylko aktywną");
    }

    @Test
    void shouldRejectDoubleIssue() {
        ReservationEntity r = reservation(5L, 10L);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        when(loanRepository.existsByReservationId(5L)).thenReturn(true);

        assertThatThrownBy(() -> loanService.issueLoan(5L, "owner@campus.edu.pl"))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("już zrealizowana");
    }

    @Test
    void shouldForbidIssueForForeignStudent() {
        ReservationEntity r = reservation(5L, 10L);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(r));
        when(loanRepository.existsByReservationId(5L)).thenReturn(false);
        when(userRepository.findByEmail("other@campus.edu.pl"))
                .thenReturn(Optional.of(user("other@campus.edu.pl", 30L, Role.ROLE_STUDENT)));

        assertThatThrownBy(() -> loanService.issueLoan(5L, "other@campus.edu.pl"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private LoanEntity activeLoan(Long id, Long ownerId) {
        LoanEntity loan = new LoanEntity();
        loan.setId(id);
        loan.setBorrowDate(LocalDateTime.now().minusDays(1));
        loan.setExpectedReturnDate(LocalDateTime.now().plusDays(2));
        loan.setUser(user("owner@campus.edu.pl", ownerId, Role.ROLE_STUDENT));
        loan.setEquipment(equipment(1L));
        loan.getEquipment().setStatus(EquipmentStatus.WYPOZYCZONY);
        ReservationEntity r = reservation(5L, ownerId);
        loan.setReservation(r);
        return loan;
    }

    @Test
    void shouldRequestReturn() {
        LoanEntity loan = activeLoan(50L, 10L);
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));
        when(userRepository.findByEmail("owner@campus.edu.pl"))
                .thenReturn(Optional.of(user("owner@campus.edu.pl", 10L, Role.ROLE_STUDENT)));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponseDTO result = loanService.requestReturn(50L, "owner@campus.edu.pl");

        assertThat(result.returnRequestedAt()).isNotNull();
        assertThat(result.actualReturnDate()).isNull();
        assertThat(loan.getEquipment().getStatus()).isEqualTo(EquipmentStatus.WYPOZYCZONY);
    }

    @Test
    void shouldRejectDoubleRequest() {
        LoanEntity loan = activeLoan(50L, 10L);
        loan.setReturnRequestedAt(LocalDateTime.now().minusHours(1));
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.requestReturn(50L, "owner@campus.edu.pl"))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("czeka na odbiór");
    }

    @Test
    void shouldForbidRequestForForeignStudent() {
        LoanEntity loan = activeLoan(50L, 10L);
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));
        when(userRepository.findByEmail("other@campus.edu.pl"))
                .thenReturn(Optional.of(user("other@campus.edu.pl", 30L, Role.ROLE_STUDENT)));

        assertThatThrownBy(() -> loanService.requestReturn(50L, "other@campus.edu.pl"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldConfirmReturnAndFlipStatuses() {
        LoanEntity loan = activeLoan(50L, 10L);
        loan.setReturnRequestedAt(LocalDateTime.now().minusHours(1));
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));
        when(userRepository.findByEmail("opiekun@campus.edu.pl"))
                .thenReturn(Optional.of(user("opiekun@campus.edu.pl", 20L, Role.ROLE_OPIEKUN)));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponseDTO result = loanService.confirmReturn(50L, "opiekun@campus.edu.pl",
                new LoanReturnDTO(false, null));

        assertThat(result.actualReturnDate()).isNotNull();
        assertThat(loan.getEquipment().getStatus()).isEqualTo(EquipmentStatus.DOSTEPNY);
        assertThat(loan.getReservation().getStatus()).isEqualTo(ReservationStatus.ZAKONCZONA);
        verify(defectReportRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void shouldReportDefectOnDamagedConfirm() {
        LoanEntity loan = activeLoan(50L, 10L);
        loan.setReturnRequestedAt(LocalDateTime.now().minusHours(1));
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));
        when(userRepository.findByEmail("opiekun@campus.edu.pl"))
                .thenReturn(Optional.of(user("opiekun@campus.edu.pl", 20L, Role.ROLE_OPIEKUN)));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponseDTO result = loanService.confirmReturn(50L, "opiekun@campus.edu.pl",
                new LoanReturnDTO(true, "Pęknięta obudowa"));

        assertThat(result.actualReturnDate()).isNotNull();
        assertThat(loan.getEquipment().getStatus()).isEqualTo(EquipmentStatus.SERWISOWANY);
        verify(defectReportRepository).save(any());
    }

    @Test
    void shouldRejectDoubleConfirm() {
        LoanEntity loan = activeLoan(50L, 10L);
        loan.setActualReturnDate(LocalDateTime.now().minusHours(1));
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.confirmReturn(50L, "opiekun@campus.edu.pl",
                new LoanReturnDTO(false, null)))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("już zwrócony");
    }

    @Test
    void shouldForbidConfirmForStudent() {
        LoanEntity loan = activeLoan(50L, 10L);
        when(loanRepository.findById(50L)).thenReturn(Optional.of(loan));
        when(userRepository.findByEmail("owner@campus.edu.pl"))
                .thenReturn(Optional.of(user("owner@campus.edu.pl", 10L, Role.ROLE_STUDENT)));

        assertThatThrownBy(() -> loanService.confirmReturn(50L, "owner@campus.edu.pl",
                new LoanReturnDTO(false, null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("tylko opiekun");
    }

    @Test
    void shouldReturnMyLoans() {
        when(userRepository.findByEmail("a@campus.edu.pl"))
                .thenReturn(Optional.of(user("a@campus.edu.pl", 10L, Role.ROLE_STUDENT)));
        when(loanRepository.findByUser_EmailOrderByBorrowDateDesc("a@campus.edu.pl"))
                .thenReturn(List.of(activeLoan(50L, 10L)));

        List<LoanResponseDTO> result = loanService.getMyLoans("a@campus.edu.pl");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(50L);
        verify(loanRepository).findByUser_EmailOrderByBorrowDateDesc("a@campus.edu.pl");
    }

    @Test
    void shouldReturnAllActiveLoans() {
        when(loanRepository.findByActualReturnDateIsNullOrderByBorrowDateDesc())
                .thenReturn(List.of(activeLoan(50L, 10L)));

        List<LoanResponseDTO> result = loanService.getAllLoans(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userEmail()).isEqualTo("owner@campus.edu.pl");
    }

    @Test
    void shouldReturnAllLoansIncludingReturned() {
        LoanEntity returned = activeLoan(50L, 10L);
        returned.setActualReturnDate(LocalDateTime.now());
        when(loanRepository.findAllByOrderByBorrowDateDesc()).thenReturn(List.of(returned));

        List<LoanResponseDTO> result = loanService.getAllLoans(false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).actualReturnDate()).isNotNull();
    }
}
