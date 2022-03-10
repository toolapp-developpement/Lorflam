package com.avr.apps.helpdesk.web;

import com.avr.apps.helpdesk.service.TicketService;
import com.axelor.apps.base.db.AppStock;
import com.axelor.apps.base.service.app.AppService;
import com.axelor.apps.helpdesk.db.Ticket;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.service.configurator.ConfiguratorService;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

import java.util.List;

public class TicketController {
    @Inject
    TicketService ticketService;

    /**
     *
     * @param request
     * @param response
     */
    public void joinSaleOrderAndStockMoveBy(ActionRequest request, ActionResponse response) throws AxelorException {
        Ticket ticket = request.getContext().asType(Ticket.class);
        List<SaleOrder> saleOrderList = ticketService.computedSequenceSaleOrderIfNotExist(ticket.getSaleOrderList());
        List<PurchaseOrder> purchaseOrderList = ticketService.computedSequencePurchaseOrderIfNotExist(ticket.getPurchaseOrderList());
        List<StockMove> stockMoveList = ticketService.computedSequenceStockMoveIfNotExist(ticket.getStockMoveList());
        response.setValue("saleOrderSeq", ticketService.joinBy(saleOrderList,saleOrder -> saleOrder.getSaleOrderSeq()));
        response.setValue("stockMoveSeq", ticketService.joinBy(stockMoveList,stockMove -> stockMove.getStockMoveSeq()));
        response.setValue("saleOrderList", saleOrderList);
        response.setValue("stockMoveList", stockMoveList);
        response.setValue("purchaseOrderList", purchaseOrderList);
    }
}
