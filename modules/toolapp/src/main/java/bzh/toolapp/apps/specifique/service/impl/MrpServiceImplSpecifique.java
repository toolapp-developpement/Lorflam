package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.ProductCompanyService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.production.db.repo.ManufOrderRepository;
import com.axelor.apps.production.service.MrpServiceProductionImpl;
import com.axelor.apps.purchase.db.repo.PurchaseOrderLineRepository;
import com.axelor.apps.purchase.service.app.AppPurchaseService;
import com.axelor.apps.sale.db.repo.SaleOrderLineRepository;
import com.axelor.apps.stock.db.repo.StockLocationLineRepository;
import com.axelor.apps.stock.db.repo.StockLocationRepository;
import com.axelor.apps.stock.service.StockLocationService;
import com.axelor.apps.stock.service.StockRulesService;
import com.axelor.apps.supplychain.db.MrpLine;
import com.axelor.apps.supplychain.db.repo.MrpForecastRepository;
import com.axelor.apps.supplychain.db.repo.MrpLineRepository;
import com.axelor.apps.supplychain.db.repo.MrpLineTypeRepository;
import com.axelor.apps.supplychain.db.repo.MrpRepository;
import com.axelor.apps.supplychain.service.MrpLineService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MrpServiceImplSpecifique extends MrpServiceProductionImpl {
  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Inject
  public MrpServiceImplSpecifique(
      AppBaseService appBaseService,
      AppPurchaseService appPurchaseService,
      MrpRepository mrpRepository,
      StockLocationRepository stockLocationRepository,
      ProductRepository productRepository,
      StockLocationLineRepository stockLocationLineRepository,
      MrpLineTypeRepository mrpLineTypeRepository,
      PurchaseOrderLineRepository purchaseOrderLineRepository,
      SaleOrderLineRepository saleOrderLineRepository,
      MrpLineRepository mrpLineRepository,
      StockRulesService stockRulesService,
      MrpLineService mrpLineService,
      MrpForecastRepository mrpForecastRepository,
      StockLocationService stockLocationService,
      ManufOrderRepository manufOrderRepository,
      ProductCompanyService productCompanyService) {
    super(
        appBaseService,
        appPurchaseService,
        mrpRepository,
        stockLocationRepository,
        productRepository,
        stockLocationLineRepository,
        mrpLineTypeRepository,
        purchaseOrderLineRepository,
        saleOrderLineRepository,
        mrpLineRepository,
        stockRulesService,
        mrpLineService,
        mrpForecastRepository,
        stockLocationService,
        manufOrderRepository,
        productCompanyService);
  }

  @Override
  @Transactional
  protected void computeCumulativeQty(Product product) {

    List<MrpLine> mrpLineList =
        mrpLineRepository
            .all()
            .filter("self.mrp.id = ?1 AND self.product.id = ?2", mrp.getId(), product.getId())
            .order("maturityDate")
            .order("mrpLineType.typeSelect")
            .order("mrpLineType.sequence")
            .order("id")
            .fetch();

    BigDecimal previousCumulativeQty = BigDecimal.ZERO;

    for (MrpLine mrpLine : mrpLineList) {
      mrpLine.setCumulativeQty(previousCumulativeQty.add(mrpLine.getQty()));

      // If the mrp line is a proposal and the cumulative qty is superior to the min qty, we delete
      // the mrp line
      // MA1-I63 - Karl - begin
      if (this.isProposalElement(mrpLine.getMrpLineType())
          && mrpLine.getCumulativeQty().compareTo(mrpLine.getMinQty()) >= 0
          && mrpLine.getMrpLineType().getTypeSelect() != MrpLineTypeRepository.TYPE_OUT) {
        mrpLineRepository.remove(mrpLine);
        continue;
      }
      // MA1-I63 - Karl - end

      previousCumulativeQty = mrpLine.getCumulativeQty();

      log.debug(
          "Cumulative qty is ({}) for product ({}) and move ({}) at the maturity date ({})",
          previousCumulativeQty,
          mrpLine.getProduct().getFullName(),
          mrpLine.getMrpLineType().getName(),
          mrpLine.getMaturityDate());
    }
  }
}
