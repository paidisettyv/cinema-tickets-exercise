package uk.gov.dwp.uc.pairtest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.User;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

public class TicketServiceImplTest {

    private static final User user = new User(1L);
    private TicketTypeRequest[] requests;
    private TicketServiceImpl ticketService;
    @Mock
    private TicketPaymentService paymentService;
    @Mock
    private SeatReservationService reservationService;
    @BeforeEach
    void Setup() {
        MockitoAnnotations.openMocks(this);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Test
    void Should_ReserveSeatAndMakePayment_When_AdultTicketIsPurchased(){
        requests = new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.ADULT,2),
                                            new TicketTypeRequest(TicketTypeRequest.Type.CHILD,1),
                                            new TicketTypeRequest(TicketTypeRequest.Type.INFANT,1)};
        ticketService.purchaseTickets(user, requests);
        Mockito.verify(paymentService, times(1)).makePayment(1, 50);
        Mockito.verify(reservationService, times(1)).reserveSeat(1, 3);
    }
    @Test
    void Should_ReserveSeatAndMakePayment_When_AdultTicketIsPurchasedWithMultipleRequests(){
        requests = new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.ADULT,2),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT,1),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT,3)};
        ticketService.purchaseTickets(user, requests);
        Mockito.verify(paymentService, times(1)).makePayment(1, 120);
        Mockito.verify(reservationService, times(1)).reserveSeat(1, 6);
    }
    @Test
    void Should_NeitherReserveSeatNorMakePaymentAndThrowException_When_NoAdultTicketIsPurchased() {
        requests = new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.CHILD,2)};
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(user, requests));
        assertEquals("No Adult Tickets", exception.getErrorMessage());

    }
    @Test
    void Should_ThrowInvalidPurchaseException_When_UserIsNull(){
        requests = new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.ADULT,2)};
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(null, requests));
        assertEquals("Invalid Purchase Request", exception.getErrorMessage());

    }

    @Test
    void Should_ThrowInvalidPurchaseException_When_TicketTypeRequestListIsEmpty() {
        requests = new TicketTypeRequest[]{};
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(user, requests));
        assertEquals("Invalid Purchase Request",exception.getErrorMessage());
    }

    @Test
    void Should_ThrowInvalidPurchaseException_When_TicketTypeRequestIsEmptyAndUserIsNull(){
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(null));
        assertEquals("Invalid Purchase Request", exception.getErrorMessage());
    }
}
