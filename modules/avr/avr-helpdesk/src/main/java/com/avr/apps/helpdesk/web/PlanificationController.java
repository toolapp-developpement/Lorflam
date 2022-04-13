package com.avr.apps.helpdesk.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.exception.AxelorException;
import com.axelor.apps.account.service.invoice.generator.InvoiceGenerator;

public class PlanificationController {

	private final Logger logger = LoggerFactory.getLogger(InvoiceGenerator.class);

	public void prepared(ActionRequest request, ActionResponse response) throws AxelorException {
	
		Context context = request.getContext();
		StockMove sm = null;
		
		this.logger.debug("Num StockMove est {}", context.get("id"));
		
		if(context.get("id") != null) {
			Long stockMoveId = (Long) request.getContext().get("id");
			sm = Beans.get(StockMoveRepository.class).find(stockMoveId);
			sm.setStatusSelect(5);
		}
		
	}
}
