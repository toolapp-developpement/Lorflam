package com.avr.apps.helpdesk.web;

import com.avr.apps.helpdesk.service.TicketService;
import com.axelor.apps.helpdesk.db.Ticket;
import com.axelor.apps.purchase.db.PurchaseRequest;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import java.util.List;

public class TicketController {
  @Inject TicketService ticketService;

  /**
   * @param request
   * @param response
   */
  public void joinSaleOrderAndStockMoveBy(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Ticket ticket = request.getContext().asType(Ticket.class);
    List<SaleOrder> saleOrderList =
        ticketService.computedSequenceSaleOrderIfNotExist(ticket.getSaleOrderList());
    List<PurchaseRequest> purchaseRequestList =
        ticketService.computedSequencePurchaseRequestIfNotExist(ticket.getPurchaseRequestList());
    List<StockMove> stockMoveList =
        ticketService.computedSequenceStockMoveIfNotExist(ticket.getStockMoveList());
    response.setValue(
        "saleOrderSeq", ticketService.joinBy(saleOrderList, SaleOrder::getSaleOrderSeq));
    response.setValue(
        "stockMoveSeq", ticketService.joinBy(stockMoveList, StockMove::getStockMoveSeq));
    response.setValue("saleOrderList", saleOrderList);
    response.setValue("stockMoveList", stockMoveList);
    response.setValue("purchaseRequestList", purchaseRequestList);
  }
  

  public void newSaleOrder(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Ticket ticket = request.getContext().asType(Ticket.class);
    response.setView(
            ActionView.define(I18n.get("Sale order"))
                .model("com.axelor.apps.sale.db.SaleOrder")
                .add("form", "sale-order-form")
                .param("forceEdit", "true")
                .context("__parent__", ticket)
                .map());
  }
}
