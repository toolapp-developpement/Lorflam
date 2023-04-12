package bzh.toolapp.apps.specifique.repository;

import com.avr.apps.helpdesk.service.TicketService;
import com.axelor.apps.businessproject.db.repo.SaleOrderProjectRepository;
import com.axelor.apps.helpdesk.db.Ticket;
import com.axelor.apps.helpdesk.db.repo.TicketRepository;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.inject.Beans;

public class SaleOrderSpecificeRepository extends SaleOrderProjectRepository{

    @Override
    public SaleOrder save(SaleOrder entity) {

        // MA1-I75 - Karl - begin
        TicketService ticketService = Beans.get(TicketService.class);

        if(entity.getTicket() != null && entity.getSaleOrderSeq() != null) {
            Ticket ticket = entity.getTicket();
            ticket.setSaleOrderSeq(ticketService.joinBy(ticket.getSaleOrderList(), SaleOrder::getSaleOrderSeq));
            Beans.get(TicketRepository.class).save(ticket);
        }

        // MA1-I75 - Karl - end
       return super.save(entity);
    }
}
