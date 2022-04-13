package com.avr.apps.helpdesk.web;

import java.lang.invoke.MethodHandles;

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

public class PlanificationController {

	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public void prepared(ActionRequest request, ActionResponse response) throws AxelorException {
	
		StockMove sm = request.getContext().asType(StockMove.class);
		
		this.logger.debug("Num StockMove est {}", context.get("id"));
		
		if(context.get("id") != null) {
			sm = Beans.get(StockMoveRepository.class).find(sm.getId());
			sm.setStatusSelect(5);
		}
		response.setValues(sm);
	}
}
