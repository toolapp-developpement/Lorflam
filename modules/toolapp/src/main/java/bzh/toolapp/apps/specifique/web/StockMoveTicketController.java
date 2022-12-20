package bzh.toolapp.apps.specifique.web;

import com.axelor.apps.helpdesk.db.Ticket;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.exception.service.TraceBackService;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class StockMoveTicketController {

  public void viewTicketfromStockMove(ActionRequest request, ActionResponse response) {
    try {
      StockMoveLine stockMoveLine = request.getContext().asType(StockMoveLine.class);
      //String id = stockMoveLine.getId().toString();
      Ticket ticket = stockMoveLine.getSaleOrderLine().getSaleOrder().getTicket();

      if (ticket != null) {
        response.setView(
            ActionView.define("Invoice")
                .model(Ticket.class.getName())
                .add("grid", "ticket-grid")
                .add("form", "ticket-form")
                .context("_showRecord", String.valueOf(ticket.getId()))
                .map());
        //response.setCanClose(true);
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }
}
