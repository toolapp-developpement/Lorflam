package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.production.db.CostSheet;
import com.axelor.apps.production.db.CostSheetLine;
import com.axelor.apps.production.db.ManufOrder;
import com.axelor.apps.production.db.ProdProduct;
import com.axelor.apps.production.db.repo.BillOfMaterialRepository;
import com.axelor.apps.production.db.repo.CostSheetRepository;
import com.axelor.apps.production.service.app.AppProductionService;
import com.axelor.apps.production.service.costsheet.CostSheetLineService;
import com.axelor.apps.production.service.costsheet.CostSheetService;
import com.axelor.apps.production.service.costsheet.CostSheetServiceImpl;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.exception.AxelorException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CostSheetServiceSpecifiqueImpl extends CostSheetServiceImpl {

  public CostSheetServiceSpecifiqueImpl(
      AppProductionService appProductionService,
      UnitConversionService unitConversionService,
      CostSheetLineService costSheetLineService,
      AppBaseService appBaseService,
      BillOfMaterialRepository billOfMaterialRepo) {
    super(
        appProductionService,
        unitConversionService,
        costSheetLineService,
        appBaseService,
        billOfMaterialRepo);
  }

  @Override
  protected void computeConsumedProduct(
      int bomLevel,
      LocalDate previousCostSheetDate,
      CostSheetLine parentCostSheetLine,
      List<StockMoveLine> consumedStockMoveLineList,
      List<ProdProduct> toConsumeProdProductList,
      BigDecimal ratio)
      throws AxelorException {

    CostSheet parentCostSheet = parentCostSheetLine.getCostSheet();
    int calculationTypeSelect = parentCostSheet.getCalculationTypeSelect();
    LocalDate calculationDate = parentCostSheet.getCalculationDate();

    Map<List<Object>, BigDecimal> consumedStockMoveLinePerProductAndUnit =
        getTotalQtyPerProductAndUnit(
            consumedStockMoveLineList,
            calculationDate,
            previousCostSheetDate,
            calculationTypeSelect);

    for (List<Object> keys : consumedStockMoveLinePerProductAndUnit.keySet()) {

      Iterator<Object> iterator = keys.iterator();
      Product product = (Product) iterator.next();
      Unit unit = (Unit) iterator.next();
      BigDecimal realQty = consumedStockMoveLinePerProductAndUnit.get(keys);

      if (product == null) {
        continue;
      }

      BigDecimal valuationQty = BigDecimal.ZERO;

      if (calculationTypeSelect == CostSheetRepository.CALCULATION_WORK_IN_PROGRESS) {

        BigDecimal plannedConsumeQty =
            computeTotalQtyPerUnit(toConsumeProdProductList, product, unit);

        valuationQty = realQty.subtract(plannedConsumeQty.multiply(ratio));
      }
      // <-- MA1-I42 Karl Alexandersson
      else {
        valuationQty = realQty;
      }
      // MA1-I42 Karl Alexandersson -->

      valuationQty =
          valuationQty.setScale(appBaseService.getNbDecimalDigitForQty(), RoundingMode.HALF_UP);

      if (valuationQty.compareTo(BigDecimal.ZERO) == 0) {
        continue;
      }

      costSheetLineService.createConsumedProductCostSheetLine(
          parentCostSheet.getManufOrder().getCompany(),
          product,
          unit,
          bomLevel,
          parentCostSheetLine,
          valuationQty,
          CostSheetService.ORIGIN_MANUF_ORDER,
          null);
    }
  }

  @Override
  protected Map<List<Object>, BigDecimal> getTotalQtyPerProductAndUnit(
      List<StockMoveLine> stockMoveLineList,
      LocalDate calculationDate,
      LocalDate previousCostSheetDate,
      int calculationType) {

    Map<List<Object>, BigDecimal> stockMoveLinePerProductAndUnitMap = new HashMap<>();

    if (stockMoveLineList == null) {
      return stockMoveLinePerProductAndUnitMap;
    }

    for (StockMoveLine stockMoveLine : stockMoveLineList) {

      StockMove stockMove = stockMoveLine.getStockMove();

      if (stockMove == null
          || (StockMoveRepository.STATUS_REALIZED != stockMoveLine.getStockMove().getStatusSelect()
              // <-- MA1-I42 Karl Alexandersson
              && StockMoveRepository.STATUS_PLANNED
                  != stockMoveLine.getStockMove().getStatusSelect())
      // MA1-I42 Karl Alexandersson -->
      ) {
        continue;
      }

      if ((calculationType == CostSheetRepository.CALCULATION_PARTIAL_END_OF_PRODUCTION
              || calculationType == CostSheetRepository.CALCULATION_END_OF_PRODUCTION)
          && previousCostSheetDate != null
          && !previousCostSheetDate.isBefore(stockMove.getRealDate())) {
        continue;

      } else if (calculationType == CostSheetRepository.CALCULATION_WORK_IN_PROGRESS
          && calculationDate.isBefore(stockMove.getRealDate())) {
        continue;
      }

      Product productKey = stockMoveLine.getProduct();
      Unit unitKey = stockMoveLine.getUnit();

      List<Object> keys = new ArrayList<Object>();
      keys.add(productKey);
      keys.add(unitKey);

      BigDecimal qty = stockMoveLinePerProductAndUnitMap.get(keys);

      if (qty == null) {
        qty = BigDecimal.ZERO;
      }

      stockMoveLinePerProductAndUnitMap.put(keys, qty.add(stockMoveLine.getRealQty()));
    }

    return stockMoveLinePerProductAndUnitMap;
  }

  @Override
  protected BigDecimal getTotalToProduceQty(ManufOrder manufOrder) throws AxelorException {

    BigDecimal totalProducedQty = BigDecimal.ZERO;

    for (StockMoveLine stockMoveLine : manufOrder.getProducedStockMoveLineList()) {

      if (stockMoveLine.getUnit().equals(manufOrder.getUnit())
          && (stockMoveLine.getStockMove().getStatusSelect() == StockMoveRepository.STATUS_PLANNED
              || stockMoveLine.getStockMove().getStatusSelect()
                  == StockMoveRepository.STATUS_REALIZED)) {

        // <-- MA1-I42 Karl Alexandersson
        if (manufOrder.getProduct() != stockMoveLine.getProduct()) continue;
        // MA1-I42 Karl Alexandersson -->
        Product product = stockMoveLine.getProduct();
        totalProducedQty =
            totalProducedQty.add(
                unitConversionService.convert(
                    stockMoveLine.getUnit(),
                    costSheet.getManufOrder().getUnit(),
                    stockMoveLine.getQty(),
                    stockMoveLine.getQty().scale(),
                    product));
      }
    }

    return totalProducedQty;
  }
}
