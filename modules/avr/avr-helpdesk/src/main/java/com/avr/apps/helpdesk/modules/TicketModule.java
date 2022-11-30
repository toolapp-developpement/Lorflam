package com.avr.apps.helpdesk.modules;

import com.avr.apps.helpdesk.service.PurchaseOrderCreationService;
import com.avr.apps.helpdesk.service.StockMoveCreateService;
import com.avr.apps.helpdesk.service.TicketService;
import com.avr.apps.helpdesk.service.impl.*;
import com.axelor.app.AxelorModule;
import com.axelor.apps.businessproject.service.ProjectPurchaseServiceImpl;
import com.axelor.apps.supplychain.service.PurchaseOrderStockServiceImpl;
import com.axelor.apps.supplychain.service.SaleOrderStockServiceImpl;

public class TicketModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(TicketService.class).to(TicketServiceImpl.class);
    bind(PurchaseOrderCreationService.class).to(PurchaseOrderCreateSupplychainServiceImpl.class);
    bind(StockMoveCreateService.class).to(StockMoveCreateServiceImpl.class);
    bind(SaleOrderStockServiceImpl.class).to(SaleOrderCreateStockMoveServiceImpl.class);
    bind(ProjectPurchaseServiceImpl.class).to(SaleOrderCreatePurchaseServiceImpl.class);
    bind(PurchaseOrderStockServiceImpl.class).to(PurchaseOrderCreateStockServiceImpl.class);
  }
}
