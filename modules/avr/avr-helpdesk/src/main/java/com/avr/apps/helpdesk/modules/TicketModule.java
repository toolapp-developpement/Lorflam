package com.avr.apps.helpdesk.modules;

import com.axelor.apps.production.db.repo.StockMoveLineProductionRepository;
import com.avr.apps.helpdesk.repository.StockMoveLineAvrHelpdeskRepository;
import com.avr.apps.helpdesk.service.TicketService;
import com.avr.apps.helpdesk.service.impl.TicketServiceImpl;
import com.axelor.app.AxelorModule;


public class TicketModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(TicketService.class).to(TicketServiceImpl.class);
    bind(StockMoveLineProductionRepository.class).to(StockMoveLineAvrHelpdeskRepository.class);
  }
}
