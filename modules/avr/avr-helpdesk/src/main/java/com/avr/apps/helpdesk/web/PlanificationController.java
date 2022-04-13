package com.avr.apps.helpdesk.web

import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.camunda.bpm.model.dmn.instance.Context;

import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class PlanificationController throws AxelorException {

	public void prepared(ActionRequest request, ActionResponse response) {
		Context context = request.getContext();
		StockMove sm = null;
		
		if(context.get("id") != null) {
			Long stockMoveId = (Long) request.getContext().get("id");
			sm = Beans.get(StockMoveRepository.class).find(stockMoveId);
			sm.setStatusSelect(5);
		}
		response.setReload(true);
	}
}
