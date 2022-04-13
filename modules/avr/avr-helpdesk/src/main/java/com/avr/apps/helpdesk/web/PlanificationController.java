package com.avr.apps.helpdesk.web;

import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.axelor.exception.AxelorException;

public class PlanificationController {

	public void prepared(ActionRequest request, ActionResponse response) throws AxelorException {
		
		private final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
		
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
