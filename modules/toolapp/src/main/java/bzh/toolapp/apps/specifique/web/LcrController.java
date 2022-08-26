package bzh.toolapp.apps.specifique.web;

import java.time.LocalDate;

import com.axelor.apps.base.db.Company;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.meta.CallMethod;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;

import bzh.toolapp.apps.specifique.service.lcr.LcrService;

public class LcrController {
	@CallMethod
	public void generateFile(LocalDate dueDate) throws AxelorException {
		System.out.println("La date du jour est : " + dueDate);

	}

	public void preparedFile(ActionRequest request, ActionResponse response) throws AxelorException {

		Context context = request.getContext();
		LocalDate dueDate = (LocalDate) context.get("dueDate");
		Company company = (Company) context.get("Company");
		Beans.get(LcrService.class).generateLcrFile(dueDate, company);

		System.out.println("La date du jour est : ");
	}
}
