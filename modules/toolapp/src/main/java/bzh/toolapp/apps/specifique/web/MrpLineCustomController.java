package bzh.toolapp.apps.specifique.web;

import bzh.toolapp.apps.specifique.service.impl.MrpLineCustomService;
import com.axelor.apps.toolapp.db.MrpLineCustom;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MrpLineCustomController {

  public void fillMrpLineCustom(ActionRequest request, ActionResponse response) {
    try {
      Context context = request.getContext();
      Long productId = null;
      if (context.get("product") != null) {
        productId = Long.parseLong(((LinkedHashMap) context.get("product")).get("id").toString());
      }

      Long stockLocationId = null;
      if (context.get("stockLocation") != null) {
        stockLocationId =
            Long.parseLong(((LinkedHashMap) context.get("stockLocation")).get("id").toString());
      }

      Object endDateContext = context.get("endDate");
      LocalDate endDate = null;
      if (endDateContext != null) {
        endDate = LocalDate.parse(endDateContext.toString());
      }

      List<MrpLineCustom> mrpLineCustomList = new ArrayList<>();
      if (productId != null && stockLocationId != null && endDate != null) {
        mrpLineCustomList =
            Beans.get(MrpLineCustomService.class)
                .computeMrpLineCustomList(productId, stockLocationId, endDate);
      }

      response.setValue("$mrpLineCustomList", mrpLineCustomList);

      List<BigDecimal> valueList = new ArrayList<>();
      if (productId != null && stockLocationId != null) {
        valueList = Beans.get(MrpLineCustomService.class).getValueQty(productId, stockLocationId);
        response.setValue("$minQty", valueList.get(0));
        response.setValue("$qtyN2", valueList.get(1));
        response.setValue("$qtyN1", valueList.get(2));
        response.setValue("$qtyN", valueList.get(3));
      }

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }
}
