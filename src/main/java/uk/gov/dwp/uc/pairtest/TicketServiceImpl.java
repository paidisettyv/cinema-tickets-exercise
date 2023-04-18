package uk.gov.dwp.uc.pairtest;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.User;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.ThirdPartyCallException;

import java.util.Arrays;

/**
 *Implementation of TicketService interface to purchase tickets
 */
public class TicketServiceImpl implements TicketService {

    private static final Logger logger = LogManager.getLogger(TicketServiceImpl.class);
    final static int ADULT_TICKET_PRICE = 20;
    final static int CHILD_TICKET_PRICE = 10;
    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
        BasicConfigurator.configure();
    }

    /**
     * Method purchaseTickets takes in TicketType request list and user and aggregates the total number tickets
     * to be reserved and calculates the total sum to make payment.
     * Seats are only reserved if either child or Infant are accompanied by an adult
     * @param user
     * @param ticketTypeRequests
     * @throws InvalidPurchaseException
     */
    @Override
    public void purchaseTickets(User user, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        int totalAmountToPay, totalSeatsToAllocate, childTickets;
        //throws InvalidPurchaseException if either user is null or ticketTypeRequest list is empty
        if(ticketTypeRequests.length == 0 || user == null) {
            if(user !=null) logger.error("Invalid request by user " + user.getAccountId());
            throw new InvalidPurchaseException("Invalid Purchase Request");
        }
        //Filtering the list to find if there are any adult tickets
        int adultTickets = Arrays.stream(ticketTypeRequests)
                .filter(ticket -> ticket.getTicketType()== TicketTypeRequest.Type.ADULT)
                .mapToInt(TicketTypeRequest::getNoOfTickets).sum();
        //Throws error if there isn't any adult ticket
        if(adultTickets > 0) {
            childTickets = Arrays.stream(ticketTypeRequests)
                    .filter(ticket -> ticket.getTicketType()== TicketTypeRequest.Type.CHILD)
                    .mapToInt(TicketTypeRequest::getNoOfTickets).sum();
        } else {
            logger.error(String.format("User %d has no adult tickets",user.getAccountId()));
            throw new InvalidPurchaseException("No Adult Tickets");
        }
        totalSeatsToAllocate = adultTickets + childTickets;
        //calculate the amount to be paid
        totalAmountToPay = adultTickets * ADULT_TICKET_PRICE + childTickets * CHILD_TICKET_PRICE;
        try {
            makePaymentAndReserveSeats(user.getAccountId(), totalAmountToPay, totalSeatsToAllocate);
        }
        catch (ThirdPartyCallException ex){
            logger.error(String.format("Unable to make payment and reserve seat for user %d for %d seats", user.getAccountId(),totalSeatsToAllocate));
            throw new InvalidPurchaseException(ex.getErrorMessage());
        }
    }

     /**
      * method makePaymentAndReserveSeats calls third party services to make payment and reserve seats
      * @param accountId
      * @param totalAmountToPay
      * @param totalSeatsToAllocate
      * @throws RuntimeException
      */
    private void makePaymentAndReserveSeats(Long accountId, int totalAmountToPay, int totalSeatsToAllocate) throws ThirdPartyCallException {
        try { paymentService.makePayment(accountId, totalAmountToPay);
        }catch (Exception ex) {
            logger.error(String.format("Failed making payment of %d for user %d",totalAmountToPay, accountId));
            throw new ThirdPartyCallException(ex.getMessage());
        }
        try { reservationService.reserveSeat(accountId, totalSeatsToAllocate);
        }catch (Exception ex) {
            logger.info(String.format("Failed reserving %d seats for user %d", totalSeatsToAllocate,accountId));
            //Rollback payment
            throw new ThirdPartyCallException(ex.getMessage());
        }
    }
}
