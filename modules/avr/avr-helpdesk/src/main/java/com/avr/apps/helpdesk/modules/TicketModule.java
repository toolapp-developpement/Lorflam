package com.avr.apps.helpdesk.modules;

import com.avr.apps.helpdesk.repository.StockMoveLineAvrHelpdeskRepository;
import com.avr.apps.helpdesk.service.TicketService;
import com.avr.apps.helpdesk.service.impl.TicketServiceImpl;
import com.axelor.app.AxelorModule;
import com.axelor.apps.supplychain.db.repo.StockMoveLineSupplychainRepository;

public class TicketModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(TicketService.class).to(TicketServiceImpl.class);
    bind(StockMoveLineProductionRepository.class).to(StockMoveLineAvrHelpdeskRepository.class);
  }
}
