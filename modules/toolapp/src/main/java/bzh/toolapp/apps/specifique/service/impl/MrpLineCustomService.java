package bzh.toolapp.apps.specifique.service.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.persistence.Query;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.apps.stock.db.StockRules;
import com.axelor.apps.stock.db.repo.StockConfigRepository;
import com.axelor.apps.stock.db.repo.StockLocationRepository;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.StockRulesRepository;
import com.axelor.apps.stock.service.StockRulesService;
import com.axelor.apps.stock.service.config.StockConfigService;
import com.axelor.apps.supplychain.db.MrpLine;
import com.axelor.apps.supplychain.db.repo.MrpLineRepository;
import com.axelor.apps.supplychain.db.repo.MrpLineTypeRepository;
import com.axelor.apps.supplychain.db.repo.MrpRepository;
import com.axelor.apps.toolapp.db.MrpLineCustom;
import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import java.time.temporal.TemporalAdjusters;
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

    @Inject
    public MrpLineCustomService(MrpLineRepository mrpLineRepository, 
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
    }
    
    public List<MrpLineCustom>  computeMrpLineCustomList(Long productId, Long stockLocationId, LocalDate date )         
    {
        List<MrpLineCustom> mrpLineCustomList =  new ArrayList<>();
        Product product = productRepository.find(productId);
        StockLocation stockLocation = stockLocationRepository.find(stockLocationId);
        LinkedHashMap <String, Object> map = new LinkedHashMap<String, Object>();
        String filtreType = "("+MrpLineTypeRepository.ELEMENT_PURCHASE_ORDER
        +","+MrpLineTypeRepository.ELEMENT_SALE_ORDER
        +","+MrpLineTypeRepository.ELEMENT_PURCHASE_PROPOSAL
        +","+MrpLineTypeRepository.ELEMENT_MANUFACTURING_PROPOSAL+")";       

        //on récupère le dernier calcul du CBN pour cet article et cet emplacement

        MrpLine lastMrpLine = mrpLineRepository.all()
            .filter("self.product = ?1 AND self.mrp.endDate <= ?2 AND self.mrp.stockLocation = ?3 AND self.mrp.statusSelect = 2 AND self.mrpLineType.elementSelect IN "+filtreType, product, date, stockLocation).order("-id").fetchOne();
        if(lastMrpLine == null) {
            return mrpLineCustomList;
        }
        LOG.debug("Mrp : {}", lastMrpLine.getMrp());
        //on parcours les ligne de CBN 
        for (MrpLine mrpLine : mrpLineRepository.all()
            .filter("self.product = ?1 AND self.mrp.endDate <= ?2 AND self.mrp = ?3 AND self.mrpLineType.elementSelect IN "+filtreType, 
            product, 
            date,
             lastMrpLine.getMrp())
             .order("maturityDate")
             .order("mrpLineType.typeSelect")
             .order("mrpLineType.sequence")
             .order("id")
                .fetch())
        {
            //get week number from MaturityDate
            Integer weekNumber = getWeekNumber(mrpLine.getMaturityDate());
            String key = "" + weekNumber +mrpLine.getMrpLineType().getId();
            
            if(!map.containsKey(key))
            {
                MrpLineCustom mrpLineCustom = new MrpLineCustom();
                mrpLineCustom.setProduct(product);
                mrpLineCustom.setStockLocation(stockLocation);
                mrpLineCustom.setMrp(mrpLine.getMrp());
                mrpLineCustom.setMrpLineType(mrpLine.getMrpLineType());
                mrpLineCustom.setWeekNumber(weekNumber);
                mrpLineCustom.setStockLocation(stockLocation);
                mrpLineCustom.setQty(mrpLine.getQty());
                mrpLineCustom.setCumulativeQty(mrpLine.getCumulativeQty());
                mrpLineCustom.setMinQty(mrpLine.getMinQty());
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
                mrpLineCustom.setMaxLevel(mrpLine.getMaxLevel());
                map.put(key, mrpLineCustom);
            }
            else
            {
                MrpLineCustom mrpLineCustom = (MrpLineCustom)map.get(key);
                mrpLineCustom.setQty(mrpLineCustom.getQty().add(mrpLine.getQty()));
                mrpLineCustom.setCumulativeQty(mrpLine.getCumulativeQty());
                mrpLineCustom.setRelatedToSelectName(mrpLineCustom.getRelatedToSelectName()+","+ mrpLine.getRelatedToSelectName());
                map.replace(key, mrpLineCustom);
            }
        }
        //parcours la map et sauvegarde les données
        for (Entry<String,Object>  entry : map.entrySet()) {
            mrpLineCustomList.add((MrpLineCustom)entry.getValue());
        }
       return mrpLineCustomList;
    }

    //get week number from a localDate
    private Integer getWeekNumber(LocalDate date)
    {
        WeekFields weekFields = WeekFields.of(Locale.getDefault()); 
        return date.get(weekFields.weekOfWeekBasedYear());        
    }

    
    public List<BigDecimal> getValueQty( Long productId, Long stockLocationId) throws AxelorException {

        Product product = productRepository.find(productId);
        StockLocation stockLocation = stockLocationRepository.find(stockLocationId);
        List<BigDecimal> qtyList = new ArrayList<BigDecimal>();

       
        qtyList.add( this.getMinQty(product, stockLocation));

        StockLocation stockLocationVirtualCustomer = stockConfigService
            .getCustomerVirtualStockLocation(stockConfigService.getStockConfig(AuthUtils.getUser().getActiveCompany()));

        StockLocation stockLocationVirtualProduction = stockConfigService.getStockConfig(AuthUtils.getUser().getActiveCompany()).getProductionVirtualStockLocation();

        //on récupère la somme des mouvements de stock sur l'année N-2         
        qtyList.add(this.getQuantityForYear( LocalDate.now().minusYears(2).with(TemporalAdjusters.firstDayOfYear()), 
                LocalDate.now().minusYears(2).with(TemporalAdjusters.lastDayOfYear()),product,stockLocationVirtualCustomer,stockLocationVirtualProduction));
        //on récupère la somme des mouvements de stock sur l'année N-1
        qtyList.add(this.getQuantityForYear( LocalDate.now().minusYears(1).with(TemporalAdjusters.firstDayOfYear()), 
                LocalDate.now().minusYears(1).with(TemporalAdjusters.lastDayOfYear()),product,stockLocationVirtualCustomer,stockLocationVirtualProduction));
        //on récupère la somme des mouvements de stock sur l'année N
        qtyList.add(this.getQuantityForYear( LocalDate.now().minusYears(1), LocalDate.now(),product,stockLocationVirtualCustomer,stockLocationVirtualProduction));

        return qtyList;
      }

    //on récupère la quantité minimal por l'article et l'emplacement
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

    private BigDecimal getQuantityForYear(LocalDate startDate, 
        LocalDate endDate, 
        Product product, 
        StockLocation virtualCustomerLocation, 
        StockLocation virtualProductionLocation) {

        String startDateString = startDate + "";
        String endDateString = endDate+ "";
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
                        + "and (sl.id = :prodLocationId or sl.id = :custLocationId) " );
        query.setParameter("productId", product.getId());
        query.setParameter("prodLocationId", virtualProductionLocation.getId());
        query.setParameter("custLocationId", virtualCustomerLocation.getId());
        query.setParameter("startDate", LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth()));
        query.setParameter("endDate", LocalDate.of(endDate.getYear(), endDate.getMonth(), endDate.getDayOfMonth()));

        LOG.debug("Query : {}", query);
    
        List<?> result = query.getResultList();
        return (result.get(0) == null || ((BigDecimal) result.get(0)).signum() == 0)
            ? BigDecimal.ZERO
            : ((BigDecimal) result.get(0)).setScale(2, BigDecimal.ROUND_HALF_UP);
      }
    

}
