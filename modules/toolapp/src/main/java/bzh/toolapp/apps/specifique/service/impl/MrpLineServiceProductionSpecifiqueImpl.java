package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.db.repo.PriceListRepository;
import com.axelor.apps.base.service.PartnerPriceListService;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.production.db.repo.ManufOrderRepository;
import com.axelor.apps.production.db.repo.OperationOrderRepository;
import com.axelor.apps.production.service.MrpLineServiceProductionImpl;
import com.axelor.apps.production.service.manuforder.ManufOrderService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.purchase.db.repo.PurchaseOrderLineRepository;
import com.axelor.apps.purchase.db.repo.PurchaseOrderRepository;
import com.axelor.apps.purchase.service.PurchaseOrderLineService;
import com.axelor.apps.purchase.service.PurchaseOrderService;
import com.axelor.apps.sale.db.repo.SaleOrderLineRepository;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.service.StockRulesService;
import com.axelor.apps.supplychain.db.MrpLine;
import com.axelor.apps.supplychain.db.repo.MrpForecastRepository;
import com.axelor.apps.supplychain.db.repo.MrpLineOriginRepository;
import com.axelor.apps.supplychain.db.repo.MrpLineRepository;
import com.axelor.apps.supplychain.exception.IExceptionMessage;
import com.axelor.apps.supplychain.service.PurchaseOrderSupplychainService;
import com.axelor.auth.AuthUtils;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public class MrpLineServiceProductionSpecifiqueImpl extends MrpLineServiceProductionImpl {

  @Inject
  public MrpLineServiceProductionSpecifiqueImpl(
      AppBaseService appBaseService,
      PurchaseOrderSupplychainService purchaseOrderSupplychainService,
      PurchaseOrderService purchaseOrderService,
      PurchaseOrderLineService purchaseOrderLineService,
      PurchaseOrderRepository purchaseOrderRepo,
      StockRulesService stockRulesService,
      SaleOrderLineRepository saleOrderLineRepo,
      PurchaseOrderLineRepository purchaseOrderLineRepo,
      MrpForecastRepository mrpForecastRepo,
      ManufOrderService manufOrderService,
      ManufOrderRepository manufOrderRepository,
      OperationOrderRepository operationOrderRepository,
      MrpLineRepository mrpLineRepo) {
    super(
        appBaseService,
        purchaseOrderSupplychainService,
        purchaseOrderService,
        purchaseOrderLineService,
        purchaseOrderRepo,
        stockRulesService,
        saleOrderLineRepo,
        purchaseOrderLineRepo,
        mrpForecastRepo,
        manufOrderService,
        manufOrderRepository,
        operationOrderRepository,
        mrpLineRepo);
  }

  @Override
  @Transactional(rollbackOn = {Exception.class})
  protected void generatePurchaseProposal(
      MrpLine mrpLine,
      Map<Pair<Partner, LocalDate>, PurchaseOrder> purchaseOrders,
      Map<Partner, PurchaseOrder> purchaseOrdersPerSupplier,
      boolean isProposalsPerSupplier)
      throws AxelorException {

    Product product = mrpLine.getProduct();
    StockLocation stockLocation = mrpLine.getStockLocation();
    LocalDate maturityDate = mrpLine.getMaturityDate();

    Partner supplierPartner = mrpLine.getSupplierPartner();

    if (supplierPartner == null) {
      supplierPartner = product.getDefaultSupplierPartner();

      if (supplierPartner == null) {
        throw new AxelorException(
            mrpLine,
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.MRP_LINE_1),
            product.getFullName());
      }
    }

    Company company = stockLocation.getCompany();

    Pair<Partner, LocalDate> key = null;
    PurchaseOrder purchaseOrder = null;

    if (isProposalsPerSupplier) {
      if (purchaseOrdersPerSupplier != null) {
        purchaseOrder = purchaseOrdersPerSupplier.get(supplierPartner);
      }
    } else {
      if (purchaseOrders != null) {
        key = Pair.of(supplierPartner, maturityDate);
        purchaseOrder = purchaseOrders.get(key);
      }
    }

    if (purchaseOrder == null) {
      purchaseOrder =
          purchaseOrderRepo.save(
              purchaseOrderSupplychainService.createPurchaseOrder(
                  AuthUtils.getUser(),
                  company,
                  null,
                  supplierPartner.getCurrency(),
                  maturityDate,
                  this.getPurchaseOrderOrigin(mrpLine),
                  null,
                  stockLocation,
                  appBaseService.getTodayDate(company),
                  Beans.get(PartnerPriceListService.class)
                      .getDefaultPriceList(supplierPartner, PriceListRepository.TYPE_PURCHASE),
                  supplierPartner,
                  null));
      if (isProposalsPerSupplier) {
        if (purchaseOrdersPerSupplier != null) {
          purchaseOrdersPerSupplier.put(supplierPartner, purchaseOrder);
        }
      } else {
        if (purchaseOrders != null) {
          purchaseOrders.put(key, purchaseOrder);
        }
      }
      if (mrpLine.getMrpLineOriginList().size() == 1) {
        if (mrpLine
            .getMrpLineOriginList()
            .get(0)
            .getRelatedToSelect()
            .equals(MrpLineOriginRepository.RELATED_TO_SALE_ORDER_LINE)) {
          purchaseOrder.setGeneratedSaleOrderId(
              saleOrderLineRepo
                  .find(mrpLine.getMrpLineOriginList().get(0).getRelatedToSelectId())
                  .getSaleOrder()
                  .getId());
        }
      }
    }
    Unit unit = product.getPurchasesUnit();
    BigDecimal qty = mrpLine.getQty();
    if (unit == null) {
      unit = product.getUnit();
    } else {
      qty =
          Beans.get(UnitConversionService.class)
              .convert(product.getUnit(), unit, qty, qty.scale(), product);
    }

    PurchaseOrderLine poLine =
        purchaseOrderLineService.createPurchaseOrderLine(
            purchaseOrder, product, null, null, qty, unit);
    // MA1-I55 - Karl - begin
    poLine.setDesiredDelivDate(maturityDate);
    if (poLine.getDesiredDelivDate() != null
        && poLine.getDesiredDelivDate().isEqual(LocalDate.now())) {
      poLine.setDesiredDelivDate(null);
    }
    // MA1-I55 - Karl - end
    purchaseOrder.addPurchaseOrderLineListItem(poLine);

    purchaseOrderService.computePurchaseOrder(purchaseOrder);

    linkToOrder(mrpLine, purchaseOrder);
  }
}
