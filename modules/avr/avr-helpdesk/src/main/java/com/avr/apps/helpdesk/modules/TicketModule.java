package com.avr.apps.helpdesk.modules;

import com.axelor.app.AxelorModule;
import com.avr.apps.helpdesk.service.TicketService;
import com.avr.apps.helpdesk.service.impl.TicketServiceImpl;

public class TicketModule extends AxelorModule {
    @Override
    protected void configure(){
        bind(TicketService.class).to(TicketServiceImpl.class);
    }
}
