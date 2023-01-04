package bzh.toolapp.apps.specifique.web;

import com.axelor.apps.production.web.StockMoveLineController;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.exception.AxelorException;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

public class StockMoveLineControllerSpecifique extends StockMoveLineController {

  @Override
  public void compute(ActionRequest request, ActionResponse response) throws AxelorException {
    StockMoveLine stockMoveLine = request.getContext().asType(StockMoveLine.class);
    super.compute(request, response);
    response.setValue("priceDiscounted", stockMoveLine.getUnitPriceUntaxed());
    response.setValue("discountTypeSelect", 1);
  }

  public void computeDiscountPrice(ActionRequest request, ActionResponse response) {
    StockMoveLine stockMoveLine = request.getContext().asType(StockMoveLine.class);

    response.setValue("priceDiscounted", stockMoveLine.getUnitPriceUntaxed());
    response.setValue("discountTypeSelect", 1);
  }
}
