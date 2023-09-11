package bzh.toolapp.apps.specifique.web;


import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import bzh.toolapp.apps.specifique.service.SaleOrderRecalculateService;

//MA1-I65 - Karl Alexandersson
public class SaleOrderRecalculateSpecificController {
    public void recalculateEcoTax(ActionRequest request, ActionResponse response) {
   
        SaleOrderRecalculateService saleOrderRecalculateService = Beans.get(SaleOrderRecalculateService.class);
        saleOrderRecalculateService.recalculateEcoTax();
    
  }

  public void recalcultateStockMoveLineRef(ActionRequest request, ActionResponse response) {
   
        SaleOrderRecalculateService saleOrderRecalculateService = Beans.get(SaleOrderRecalculateService.class);
        saleOrderRecalculateService.recalcultateStockMoveLineRef();
    
  }
}
