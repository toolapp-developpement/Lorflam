package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.UnitConversionService;
import com.axelor.apps.businessproduction.service.ProductionOrderSaleOrderServiceBusinessImpl;
import com.axelor.apps.businessproduction.service.ProductionOrderServiceBusinessImpl;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.ManufOrder;
import com.axelor.apps.production.db.ProductionOrder;
import com.axelor.apps.production.db.repo.ManufOrderRepository;
import com.axelor.apps.production.db.repo.ProductionOrderRepository;
import com.axelor.apps.production.exceptions.IExceptionMessage;
import com.axelor.apps.production.service.app.AppProductionService;
import com.axelor.apps.production.service.productionorder.ProductionOrderService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;

public class ProductionOrderSaleOrderSpecifiqueServiceImpl
    extends ProductionOrderSaleOrderServiceBusinessImpl {

  //	private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private BigDecimal qtyToProduce = BigDecimal.ZERO;
  protected ProductionOrderServiceBusinessImpl productionOrderServiceBusinessImpl;

  ManufOrderRepository manufOrderRepo;

  @Inject
  public ProductionOrderSaleOrderSpecifiqueServiceImpl(
      UnitConversionService unitConversionService,
      ProductionOrderService productionOrderService,
      ProductionOrderRepository productionOrderRepo,
      ProductionOrderServiceBusinessImpl productionOrderServiceBusinessImpl,
      AppProductionService appProductionService,
      ManufOrderRepository manufOrderRepo) {
    super(
        unitConversionService,
        productionOrderService,
        productionOrderRepo,
        productionOrderServiceBusinessImpl,
        appProductionService);

    this.productionOrderServiceBusinessImpl = productionOrderServiceBusinessImpl;
    this.manufOrderRepo = manufOrderRepo;
  }

  @Override
  public ProductionOrder generateManufOrders(
      ProductionOrder productionOrder, SaleOrderLine saleOrderLine) throws AxelorException {

    Product product = saleOrderLine.getProduct();

    if (saleOrderLine.getSaleSupplySelect() == ProductRepository.SALE_SUPPLY_PRODUCE
        && product != null
        && product.getProductTypeSelect().equals(ProductRepository.PRODUCT_TYPE_STORABLE)) {

      BillOfMaterial billOfMaterial = saleOrderLine.getBillOfMaterial();

      if (billOfMaterial == null) {
        billOfMaterial = product.getDefaultBillOfMaterial();
      }

      if (billOfMaterial == null && product.getParentProduct() != null) {
        billOfMaterial = product.getParentProduct().getDefaultBillOfMaterial();
      }

      if (billOfMaterial == null) {
        throw new AxelorException(
            saleOrderLine,
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.PRODUCTION_ORDER_SALES_ORDER_NO_BOM),
            product.getName(),
            product.getCode());
      }

      if (billOfMaterial.getProdProcess() == null) {
        return null;
      }

      Unit unit = saleOrderLine.getProduct().getUnit();

      BigDecimal qty = saleOrderLine.getQty();
      // si la quantité à produire est différente de la quantité de la ligne de commande
      if (qtyToProduce.compareTo(BigDecimal.ZERO) != 0) {
        qty = qtyToProduce;
      } else {
        if (unit != null && !unit.equals(saleOrderLine.getUnit())) {
          qty =
              unitConversionService.convert(
                  saleOrderLine.getUnit(), unit, qty, qty.scale(), saleOrderLine.getProduct());
        }
      }

      /*
       * Modification de la date de debut de planification de l'OF par la date
       * d'expedition de la ligne de la commande et si elle est vide alors
       * on applique la date de l'entete de commande de vente
       */
      LocalDateTime startedDate = LocalDateTime.now();

      if (saleOrderLine.getEstimatedDelivDate() != null) {
        startedDate = saleOrderLine.getEstimatedDelivDate().atStartOfDay();
      } else if (saleOrderLine.getSaleOrder().getDeliveryDate() != null) {
        startedDate = saleOrderLine.getSaleOrder().getDeliveryDate().atStartOfDay();
      }

      return generateManufOrders(
          productionOrder, billOfMaterial, qty, startedDate, saleOrderLine.getSaleOrder());
    }

    return null;
  }

  @Override
  @Transactional(rollbackOn = {AxelorException.class})
  public List<Long> generateProductionOrder(SaleOrder saleOrder) throws AxelorException {

    boolean oneProdOrderPerSO = appProductionService.getAppProduction().getOneProdOrderPerSO();

    List<Long> productionOrderIdList = new ArrayList<>();
    if (saleOrder.getSaleOrderLineList() == null) {
      return productionOrderIdList;
    }

    ProductionOrder productionOrder = null;

    // On rempli une map avec la quantité les lignes de commande de vente pour chaque produit
    HashMap<Long, BigDecimal> qtyPerProductMap = new HashMap<>();
    for (SaleOrderLine saleOrderLine : saleOrder.getSaleOrderLineList()) {
      //MA1-I54 - Karl - begin cas Pack = ligne de commande sans article associé
      if (saleOrderLine.getProduct() == null) {
        continue;
      }
      //MA1-I54 - Karl - end
      if (qtyPerProductMap.containsKey(saleOrderLine.getProduct().getId())) {
        qtyPerProductMap.put(
            saleOrderLine.getProduct().getId(),
            qtyPerProductMap.get(saleOrderLine.getProduct().getId()).add(saleOrderLine.getQty()));
      } else {
        qtyPerProductMap.put(saleOrderLine.getProduct().getId(), saleOrderLine.getQty());
      }
    }

    for (SaleOrderLine saleOrderLine : saleOrder.getSaleOrderLineList()) {

      BigDecimal manufOrderQty = BigDecimal.ZERO;

      // si on a plus rien à produire, on ignore la ligne
      if (qtyPerProductMap.get(saleOrderLine.getProduct().getId()).compareTo(BigDecimal.ZERO)
          == 0) {
        continue;
      }

      // on parcours les OFs de la commande de vente
      // pour calculer la quantité déjà produite pour l'article
      for (ManufOrder manufOrder :
          manufOrderRepo
              .all()
              .filter(
                  ":_saleOrderId member of self.saleOrderSet AND self.statusSelect != 1 AND self.statusSelect != 3 AND self.statusSelect != 2 AND self.product = :_product")
              .bind("_saleOrderId", saleOrderLine.getSaleOrder().getId())
              .bind("_product", saleOrderLine.getProduct())
              .fetch()) {
        manufOrderQty = manufOrderQty.add(manufOrder.getQty());
      }

      // si la quantité déjà produite est supérieur ou égale à la quantité de la ligne de commande
      // alors on ne fait rien
      if (manufOrderQty.compareTo(qtyPerProductMap.get(saleOrderLine.getProduct().getId())) >= 0) {
        continue;
      }

      // On calcul la quantité à produire = somme des quantité de la ligne de commande pour
      // l'article - quantité déjà produite
      qtyToProduce =
          qtyPerProductMap.get(saleOrderLine.getProduct().getId()).subtract(manufOrderQty);
      // On remets à zéro la quantité à produire
      qtyPerProductMap.put(saleOrderLine.getProduct().getId(), BigDecimal.ZERO);
      // sinon on crée un OF pour la quantité restante à produire
      if (productionOrder == null || !oneProdOrderPerSO) {
        productionOrder = this.createProductionOrder(saleOrder);
      }

      productionOrder = this.generateManufOrders(productionOrder, saleOrderLine);

      if (productionOrder != null && !productionOrderIdList.contains(productionOrder.getId())) {
        productionOrderIdList.add(productionOrder.getId());
      }
    }

    return productionOrderIdList;
  }
}
