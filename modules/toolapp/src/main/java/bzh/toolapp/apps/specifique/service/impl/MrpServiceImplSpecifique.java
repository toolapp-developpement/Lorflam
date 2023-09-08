package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.ProductCompanyService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.production.db.repo.ManufOrderRepository;
import com.axelor.apps.production.service.MrpServiceProductionImpl;
import com.axelor.apps.purchase.db.repo.PurchaseOrderLineRepository;
import com.axelor.apps.purchase.service.app.AppPurchaseService;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderLineRepository;
import com.axelor.apps.stock.db.repo.StockLocationLineRepository;
import com.axelor.apps.stock.db.repo.StockLocationRepository;
import com.axelor.apps.stock.service.StockLocationService;
import com.axelor.apps.stock.service.StockRulesService;
import com.axelor.apps.supplychain.db.MrpForecast;
import com.axelor.apps.supplychain.db.MrpLine;
import com.axelor.apps.supplychain.db.repo.MrpForecastRepository;
import com.axelor.apps.supplychain.db.repo.MrpLineRepository;
import com.axelor.apps.supplychain.db.repo.MrpLineTypeRepository;
import com.axelor.apps.supplychain.db.repo.MrpRepository;
import com.axelor.apps.supplychain.exception.IExceptionMessage;
import com.axelor.apps.supplychain.service.MrpLineService;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
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
    // MA1-I63 - Karl - begin
    //vérifier si une ligne d'achat existe
    boolean purchExist = mrpLineRepository
            .all()
            .filter("self.mrp.id = ?1 AND self.product.id = ?2 AND self.mrpLineType.elementSelect = ?3",
             mrp.getId(), product.getId(), MrpLineTypeRepository.ELEMENT_PURCHASE_ORDER).fetchOne() != null;
    // MA1-I63 - Karl - end


    BigDecimal previousCumulativeQty = BigDecimal.ZERO;

    for (MrpLine mrpLine : mrpLineList) {
      mrpLine.setCumulativeQty(previousCumulativeQty.add(mrpLine.getQty()));

      // If the mrp line is a proposal and the cumulative qty is superior to the min qty, we delete
      // the mrp line
      // on supprime que si l'origine est une ligne de stock disponible
      // le pb que l'on cherche à résoudre est le suivant :
      // On a du stock disponible inférieur à la quantité minimum mais on a aussi une ou plusieurs
      // commande ferme fournisseur en retard
      // dans ce cas, on ne veut pas de proposition
      // dans tous les autres cas, on doit garder la proposition générée par le CBN
      // MA1-I63 - Karl - begin
      if (purchExist
          && this.isProposalElement(mrpLine.getMrpLineType())
          && mrpLine.getCumulativeQty().compareTo(mrpLine.getMinQty()) >= 0
          && mrpLine.getMrpLineType().getTypeSelect() != MrpLineTypeRepository.TYPE_OUT
          && (mrpLine.getRelatedToSelectName() == null
              || mrpLine.getRelatedToSelectName().isEmpty())) {

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

  @Override
  protected Set<Product> getProductList() throws AxelorException {

    Set<Product> productSet = Sets.newHashSet();

    if (mrp.getProductSet() != null && !mrp.getProductSet().isEmpty()) {

      productSet.addAll(mrp.getProductSet());
    }

    if (mrp.getProductCategorySet() != null && !mrp.getProductCategorySet().isEmpty()) {

      productSet.addAll(
          productRepository
              .all()
              .filter(
                  "self.productCategory in (?1) "
                      + "AND self.productTypeSelect = ?2 "
                      + "AND self.excludeFromMrp = false "
                      + "AND self.stockManaged = true "
                      + "AND (?3 is true OR self.productSubTypeSelect = ?4) "
                      + "AND self.dtype = 'Product'",
                  mrp.getProductCategorySet(),
                  ProductRepository.PRODUCT_TYPE_STORABLE,
                  mrp.getMrpTypeSelect() == MrpRepository.MRP_TYPE_MRP,
                  ProductRepository.PRODUCT_SUB_TYPE_FINISHED_PRODUCT)
              .fetch());
    }

    if (mrp.getProductFamilySet() != null && !mrp.getProductFamilySet().isEmpty()) {

      productSet.addAll(
          productRepository
              .all()
              .filter(
                  "self.productFamily in (?1) "
                      + "AND self.productTypeSelect = ?2 AND "
                      + "self.excludeFromMrp = false "
                      + "AND self.stockManaged = true "
                      + "AND (?3 is true OR self.productSubTypeSelect = ?4) "
                      + "AND self.dtype = 'Product'",
                  mrp.getProductFamilySet(),
                  ProductRepository.PRODUCT_TYPE_STORABLE,
                  mrp.getMrpTypeSelect() == MrpRepository.MRP_TYPE_MRP,
                  ProductRepository.PRODUCT_SUB_TYPE_FINISHED_PRODUCT)
              .fetch());
    }
    if (mrp.getSaleOrderLineSet() != null) {
      for (SaleOrderLine saleOrderLine : mrp.getSaleOrderLineSet()) {
        productSet.add(saleOrderLine.getProduct());
      }
    }

    if (mrp.getMrpForecastSet() != null) {
      for (MrpForecast mrpForecast : mrp.getMrpForecastSet()) {

        productSet.add(mrpForecast.getProduct());
      }
    }

    // MA1-I64 - Karl - begin
    // filtre fournisseur

    if (mrp.getSuppliersSet() != null && !mrp.getSuppliersSet().isEmpty()) {
      Set<Product> productSupplierSet = Sets.newHashSet();
      productSupplierSet.addAll(
          productRepository
              .all()
              .filter(
                  "self.defaultSupplierPartner IN  (?1) "
                      + "AND self.productTypeSelect = ?2 "
                      + "AND self.excludeFromMrp = false "
                      + "AND self.stockManaged = true "
                      + "AND (?3 is true OR self.productSubTypeSelect = ?4) "
                      + "AND self.dtype = 'Product'",
                  mrp.getSuppliersSet(),
                  ProductRepository.PRODUCT_TYPE_STORABLE,
                  mrp.getMrpTypeSelect() == MrpRepository.MRP_TYPE_MRP,
                  ProductRepository.PRODUCT_SUB_TYPE_FINISHED_PRODUCT)
              .fetch());
        //intersection des 2 listes
        if (productSet.isEmpty()) {
            productSet.addAll(productSupplierSet);
        } else
        {
            productSet.retainAll(productSupplierSet);
        }
    }

    // MA1-I64 - Karl - end

    if (productSet.isEmpty()) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
          I18n.get(IExceptionMessage.MRP_NO_PRODUCT));
    }

    return productSet;
  } 
  

  /*
  private Set<Product> filterInventoryFromDefaultSupplier(Mrp mrp) {
    ArrayList<Long> listProduct = new ArrayList<>();
    String query = "";
    String idString;
    // on parcourt la liste des articles où le fournisseur est le fournisseur par défaut
    for (Product product :
        productRepository
            .all()
            .filter("self.defaultSupplierPartner =  ? ", inventory.getSupplier())
            .fetch()) {
      // on remplit la liste  avec les articles
      if (product.getArchived() != null && product.getArchived()) continue;
      if (!listProduct.contains(product.getId())) {
        listProduct.add(product.getId());
      }
    }
    // on construit la requête
    if (!listProduct.isEmpty()) {
      idString = listProduct.stream().map(l -> l.toString()).collect(Collectors.joining(","));
      query += "and self.product.id IN (" + idString + ")";
    } else {
      query += "and self.product.id = 0";
    }
    return query;
  }
  */
}
