package com.avr.apps.helpdesk.modules;

import com.avr.apps.helpdesk.service.PurchaseOrderCreationWithYardService;
import com.avr.apps.helpdesk.service.TicketService;
import com.avr.apps.helpdesk.service.impl.PurchaseOrderCreateSupplychainServiceImpl;
import com.avr.apps.helpdesk.service.impl.TicketServiceImpl;
import com.axelor.app.AxelorModule;

public class TicketModule extends AxelorModule {
    @Override
    protected void configure(){
        bind(TicketService.class).to(TicketServiceImpl.class);
        bind(PurchaseOrderCreationWithYardService.class).to(PurchaseOrderCreateSupplychainServiceImpl.class);
    }
}
