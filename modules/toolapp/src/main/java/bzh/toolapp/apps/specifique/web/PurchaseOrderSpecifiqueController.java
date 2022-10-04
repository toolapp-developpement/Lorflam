package bzh.toolapp.apps.specifique.web;

import bzh.toolapp.apps.specifique.exception.IExceptionSpecifiqueMessage;
import bzh.toolapp.apps.specifique.service.SpecifiqueService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.repo.PurchaseOrderRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class PurchaseOrderSpecifiqueController {
  public void enableEditOrder(ActionRequest request, ActionResponse response) {
    PurchaseOrder purchaseOrder =
        Beans.get(PurchaseOrderRepository.class)
            .find(request.getContext().asType(PurchaseOrder.class).getId());

    try {
      boolean checkAvailabiltyRequest =
          Beans.get(SpecifiqueService.class).enableEditPurchaseOrder(purchaseOrder);
      response.setReload(true);
      if (checkAvailabiltyRequest) {
        response.setNotify(I18n.get(IExceptionSpecifiqueMessage.PURCHASE_ORDER_EDIT_ORDER_NOTIFY));
      }
    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }
}
