package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.exceptions.IExceptionMessage;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.StockRules;
import com.axelor.apps.stock.db.repo.StockConfigRepository;
import com.axelor.apps.stock.db.repo.StockLocationRepository;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.StockRulesRepository;
import com.axelor.apps.stock.service.StockRulesService;
import com.axelor.apps.stock.service.config.StockConfigService;
import com.axelor.apps.supplychain.db.Mrp;
import com.axelor.apps.supplychain.db.MrpLine;
import com.axelor.apps.supplychain.db.repo.MrpLineRepository;
import com.axelor.apps.supplychain.db.repo.MrpLineTypeRepository;
import com.axelor.apps.supplychain.db.repo.MrpRepository;
import com.axelor.apps.toolapp.db.MrpLineCustom;
import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.i18n.I18n;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MrpLineCustomService {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  MrpLineRepository mrpLineRepository;
  ProductRepository productRepository;
  StockLocationRepository stockLocationRepository;
  StockRulesService stockRulesService;
  StockMoveLineRepository stockMoveLineRepository;
  StockConfigRepository stockConfigRepository;
  StockConfigService stockConfigService;
  MrpRepository mrpRepository;
  Map<Long, Integer> productMap;
  Map<Long, List<BigDecimal>> productQtyMap;

  @Inject
  public MrpLineCustomService(
      MrpLineRepository mrpLineRepository,
      ProductRepository productRepository,
      StockLocationRepository stockLocationRepository,
      StockRulesService stockRulesService,
      StockMoveLineRepository stockMoveLineRepository,
      StockConfigRepository stockConfigRepository,
      StockConfigService stockConfigService,
      MrpRepository mrpRepository) {
    this.mrpLineRepository = mrpLineRepository;
    this.productRepository = productRepository;
    this.stockLocationRepository = stockLocationRepository;
    this.stockRulesService = stockRulesService;
    this.stockMoveLineRepository = stockMoveLineRepository;
    this.stockConfigRepository = stockConfigRepository;
    this.stockConfigService = stockConfigService;
    this.mrpRepository = mrpRepository;
    this.productMap = Maps.newHashMap();
    this.productQtyMap = Maps.newHashMap();
  }

  public List<MrpLineCustom> computeMrpLineCustomList(
      Long productId, Long stockLocationId, LocalDate date) throws AxelorException {
    List<MrpLineCustom> mrpLineCustomList = new ArrayList<>();
    Product product = productRepository.find(productId);
    StockLocation stockLocation = stockLocationRepository.find(stockLocationId);
    LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
    /*
    String filtreType =
        "("
            + MrpLineTypeRepository.ELEMENT_PURCHASE_ORDER
            + ","
            + MrpLineTypeRepository.ELEMENT_SALE_ORDER
            + ","
            + MrpLineTypeRepository.ELEMENT_PURCHASE_PROPOSAL
            + ","
            + MrpLineTypeRepository.ELEMENT_MANUFACTURING_PROPOSAL
            + ")";
            */

    // on récupère le dernier calcul du CBN pour cet article et cet emplacement

    MrpLine lastMrpLine =
        mrpLineRepository
            .all()
            .filter(
                "self.product = ?1 AND self.mrp.endDate <= ?2 AND self.mrp.stockLocation = ?3 AND self.mrp.statusSelect = 2 ",
                //AND self.mrpLineType.elementSelect IN "
                 //   + filtreType,
                product,
                date,
                stockLocation)
            .order("-mrp.endDateTime")
            .fetchOne();
    if (lastMrpLine == null) {
      return mrpLineCustomList;
    }
    LOG.debug("Mrp : {}", lastMrpLine.getMrp());
    this.assignProductAndLevel(product, lastMrpLine.getMrp());
    // on parcours les ligne de CBN
    for (MrpLine mrpLine :
        mrpLineRepository
            .all()
            .filter(
                "self.product.id IN (?1) AND self.mrp.endDate <= ?2 AND self.mrp = ?3 ",
                this.productMap.keySet(),
                date,
                lastMrpLine.getMrp())
            .order("maturityDate")
            .order("mrpLineType.typeSelect")
            .order("mrpLineType.sequence")
            .order("id")
            .fetch()) {
      // si les totaux existent déjà dans la map, on va les chercher
      Long productIdMrpLine = mrpLine.getProduct().getId();
      if (!productQtyMap.containsKey(productIdMrpLine)) {
        productQtyMap.put(productIdMrpLine, this.getValueQty(productIdMrpLine, stockLocationId));
      }
      List<BigDecimal> valueList;
      valueList = productQtyMap.get(mrpLine.getProduct().getId());
      BigDecimal minQty = valueList.get(0);
      BigDecimal n2 = valueList.get(1);
      BigDecimal n1 = valueList.get(2);
      BigDecimal n = valueList.get(3);

      // get week number from MaturityDate
      Integer weekNumber = getWeekNumber(mrpLine.getMaturityDate());
      String key =
          "" + weekNumber + mrpLine.getMrpLineType().getId() + mrpLine.getProduct().getId();
      // si la ligne n'existe pas pour l'article la semaine et le type, alors on la crée
      if (!map.containsKey(key)) {
        MrpLineCustom mrpLineCustom = new MrpLineCustom();
        mrpLineCustom.setProduct(mrpLine.getProduct());
        mrpLineCustom.setStockLocation(mrpLine.getStockLocation());
        mrpLineCustom.setMrp(mrpLine.getMrp());
        mrpLineCustom.setMrpLineType(mrpLine.getMrpLineType());
        mrpLineCustom.setWeekNumber(weekNumber);
        mrpLineCustom.setStockLocation(stockLocation);
        mrpLineCustom.setQty(mrpLine.getQty());
        mrpLineCustom.setCumulativeQty(mrpLine.getCumulativeQty());
        mrpLineCustom.setMinQty(minQty);
        mrpLineCustom.setQtyN2(n2);
        mrpLineCustom.setQtyN1(n1);
        mrpLineCustom.setQtyN(n);
        mrpLineCustom.setIdealQty(mrpLine.getIdealQty());
        mrpLineCustom.setReOrderQty(mrpLine.getReOrderQty());
        mrpLineCustom.setMaturityDate(mrpLine.getMaturityDate());
        mrpLineCustom.setPartner(mrpLine.getPartner());
        mrpLineCustom.setSupplierPartner(mrpLine.getSupplierPartner());
        mrpLineCustom.setProposalGenerated(mrpLine.getProposalGenerated());
        mrpLineCustom.setProposalSelect(mrpLine.getProposalSelect());
        mrpLineCustom.setProposalSelectId(mrpLine.getProposalSelectId());
        mrpLineCustom.setProposalToProcess(mrpLine.getProposalToProcess());
        mrpLineCustom.setRelatedToSelectName(mrpLine.getRelatedToSelectName());
        mrpLineCustom.setMaxLevel(this.getMaxLevel(mrpLine.getProduct(), mrpLine.getMaxLevel()));
        map.put(key, mrpLineCustom);
      }
      // sinon on mets à jour les quantités
      else {
        MrpLineCustom mrpLineCustom = (MrpLineCustom) map.get(key);
        mrpLineCustom.setQty(mrpLineCustom.getQty().add(mrpLine.getQty()));
        mrpLineCustom.setCumulativeQty(mrpLine.getCumulativeQty());
        mrpLineCustom.setRelatedToSelectName(
            mrpLineCustom.getRelatedToSelectName() + "," + mrpLine.getRelatedToSelectName());
        map.replace(key, mrpLineCustom);
      }
    }
    // parcours la map et sauvegarde les données
    for (Entry<String, Object> entry : map.entrySet()) {
      mrpLineCustomList.add((MrpLineCustom) entry.getValue());
    }
    return mrpLineCustomList;
  }

  // get week number from a localDate
  private Integer getWeekNumber(LocalDate date) {
    WeekFields weekFields = WeekFields.of(Locale.getDefault());
    return date.get(weekFields.weekOfWeekBasedYear());
  }

  public List<BigDecimal> getValueQty(Long productId, Long stockLocationId) throws AxelorException {

    Product product = productRepository.find(productId);
    StockLocation stockLocation = stockLocationRepository.find(stockLocationId);
    List<BigDecimal> qtyList = new ArrayList<BigDecimal>();

    qtyList.add(this.getMinQty(product, stockLocation));

    StockLocation stockLocationVirtualCustomer =
        stockConfigService.getCustomerVirtualStockLocation(
            stockConfigService.getStockConfig(AuthUtils.getUser().getActiveCompany()));

    StockLocation stockLocationVirtualProduction =
        stockConfigService
            .getStockConfig(AuthUtils.getUser().getActiveCompany())
            .getProductionVirtualStockLocation();

    // on récupère la somme des mouvements de stock sur l'année N-2
    qtyList.add(
        this.getQuantityForYear(
            LocalDate.now().minusYears(2).with(TemporalAdjusters.firstDayOfYear()),
            LocalDate.now().minusYears(2).with(TemporalAdjusters.lastDayOfYear()),
            product,
            stockLocationVirtualCustomer,
            stockLocationVirtualProduction));
    // on récupère la somme des mouvements de stock sur l'année N-1
    qtyList.add(
        this.getQuantityForYear(
            LocalDate.now().minusYears(1).with(TemporalAdjusters.firstDayOfYear()),
            LocalDate.now().minusYears(1).with(TemporalAdjusters.lastDayOfYear()),
            product,
            stockLocationVirtualCustomer,
            stockLocationVirtualProduction));
    // on récupère la somme des mouvements de stock sur l'année N
    qtyList.add(
        this.getQuantityForYear(
            LocalDate.now().minusYears(1),
            LocalDate.now(),
            product,
            stockLocationVirtualCustomer,
            stockLocationVirtualProduction));

    return qtyList;
  }

  // on récupère la quantité minimal por l'article et l'emplacement
  private BigDecimal getMinQty(Product product, StockLocation stockLocation) {
    BigDecimal qty = BigDecimal.ZERO;

    StockRules stockRules =
        stockRulesService.getStockRules(
            product,
            stockLocation,
            StockRulesRepository.TYPE_FUTURE,
            StockRulesRepository.USE_CASE_USED_FOR_MRP);

    if (stockRules != null) {
      return stockRules.getMinQty();
    }
    return qty;
  }

  private BigDecimal getQuantityForYear(
      LocalDate startDate,
      LocalDate endDate,
      Product product,
      StockLocation virtualCustomerLocation,
      StockLocation virtualProductionLocation) {

    Query query =
        JPA.em()
            .createQuery(
                "SELECT sum(sml.realQty) FROM StockMoveLine as sml "
                    + "join StockMove as sm "
                    + "on sm.id = sml.stockMove "
                    + "join StockLocation as sl "
                    + "ON sl.id = sm.toStockLocation "
                    + "where sm.realDate between :startDate and :endDate "
                    + "and sml.product.id = :productId "
                    + "and (sl.id = :prodLocationId or sl.id = :custLocationId) ");
    query.setParameter("productId", product.getId());
    query.setParameter("prodLocationId", virtualProductionLocation.getId());
    query.setParameter("custLocationId", virtualCustomerLocation.getId());
    query.setParameter(
        "startDate",
        LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth()));
    query.setParameter(
        "endDate", LocalDate.of(endDate.getYear(), endDate.getMonth(), endDate.getDayOfMonth()));

    LOG.debug("Query : {}", query);

    List<?> result = query.getResultList();
    return (result.get(0) == null || ((BigDecimal) result.get(0)).signum() == 0)
        ? BigDecimal.ZERO
        : ((BigDecimal) result.get(0)).setScale(2, BigDecimal.ROUND_HALF_UP);
  }

  protected void assignProductAndLevel(Product product, Mrp mrp) throws AxelorException {

    if (product.getDefaultBillOfMaterial() != null
        && mrp.getMrpTypeSelect() == MrpRepository.MRP_TYPE_MRP) {
      this.assignProductLevel(product.getDefaultBillOfMaterial(), 0);
    } else {
      LOG.debug("Add product: {}", product.getFullName());
      this.productMap.put(product.getId(), this.getMaxLevel(product, 0));
    }
  }

  /**
   * Update the level of Bill of materials. The highest for each product (0: product with parent, 1:
   * product with a parent, 2: product with a parent that have a parent, ...)
   *
   * @param billOfMaterial
   * @param level
   */
  protected void assignProductLevel(BillOfMaterial billOfMaterial, int level)
      throws AxelorException {

    if (level > 100) {
      if (billOfMaterial == null || billOfMaterial.getProduct() == null) {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.MRP_BOM_LEVEL_TOO_HIGH));
      } else {
        throw new AxelorException(
            TraceBackRepository.CATEGORY_CONFIGURATION_ERROR,
            I18n.get(IExceptionMessage.MRP_BOM_LEVEL_TOO_HIGH_PRODUCT),
            billOfMaterial.getProduct().getFullName());
      }
    }

    Product product = billOfMaterial.getProduct();
    this.productMap.put(product.getId(), this.getMaxLevel(product, level));

    level = level + 1;
    if (billOfMaterial.getBillOfMaterialSet() != null
        && !billOfMaterial.getBillOfMaterialSet().isEmpty()) {

      for (BillOfMaterial subBillOfMaterial : billOfMaterial.getBillOfMaterialSet()) {

        Product subProduct = subBillOfMaterial.getProduct();

        if (this.isMrpProduct(subProduct)) {
          this.assignProductLevel(subBillOfMaterial, level);

          if (subProduct.getDefaultBillOfMaterial() != null) {
            this.assignProductLevel(subProduct.getDefaultBillOfMaterial(), level);
          }
        }
      }
    }
  }

  public int getMaxLevel(Product product, int level) {

    if (this.productMap.containsKey(product.getId())) {
      return Math.max(level, this.productMap.get(product.getId()));
    }

    return level;
  }

  public boolean isMrpProduct(Product product) {

    if (product != null
        && !product.getExcludeFromMrp()
        && product.getProductTypeSelect().equals(ProductRepository.PRODUCT_TYPE_STORABLE)) {

      return true;
    }

    return false;
  }
}
