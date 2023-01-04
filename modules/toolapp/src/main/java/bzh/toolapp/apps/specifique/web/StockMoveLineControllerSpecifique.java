package bzh.toolapp.apps.specifique.web;

import com.axelor.apps.base.service.PriceListService;
import com.axelor.apps.production.web.StockMoveLineController;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import java.math.BigDecimal;

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

    response.setValue(
        "priceDiscounted",
        computeDiscount(
            stockMoveLine.getUnitPriceUntaxed(),
            stockMoveLine.getDiscountAmount(),
            stockMoveLine.getDiscountTypeSelect()));
  }

  private BigDecimal computeDiscount(
      BigDecimal unitPrice, BigDecimal discountAmount, Integer discountTypeSelect) {
    PriceListService priceListService = Beans.get(PriceListService.class);

    return priceListService.computeDiscount(unitPrice, discountTypeSelect, discountAmount);
  }
}
