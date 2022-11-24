package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.purchase.db.SupplierCatalog;
import com.axelor.apps.purchase.db.repo.SupplierCatalogRepository;
import com.axelor.apps.stock.db.Inventory;
import com.axelor.apps.stock.db.StockLocationLine;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.InventoryRepository;
import com.axelor.apps.stock.db.repo.StockLocationLineRepository;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.apps.stock.db.repo.TrackingNumberRepository;
import com.axelor.apps.stock.service.InventoryLineService;
import com.axelor.apps.stock.service.InventoryService;
import com.axelor.apps.stock.service.StockLocationLineService;
import com.axelor.apps.stock.service.StockMoveLineService;
import com.axelor.apps.stock.service.StockMoveService;
import com.axelor.apps.stock.service.config.StockConfigService;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryServiceSpecifiqueImpl extends InventoryService {

  StockMoveLineRepository stockMoveLineRepository;
  SupplierCatalogRepository supplierCatalogRepository;

  @Inject
  public InventoryServiceSpecifiqueImpl(
      InventoryLineService inventoryLineService,
      SequenceService sequenceService,
      StockConfigService stockConfigService,
      ProductRepository productRepo,
      InventoryRepository inventoryRepo,
      StockMoveRepository stockMoveRepo,
      StockLocationLineService stockLocationLineService,
      StockMoveService stockMoveService,
      StockMoveLineService stockMoveLineService,
      StockLocationLineRepository stockLocationLineRepository,
      TrackingNumberRepository trackingNumberRepository,
      AppBaseService appBaseService,
      StockMoveLineRepository stockMoveLineRepository,
      SupplierCatalogRepository supplierCatalogRepository) {
    super(
        inventoryLineService,
        sequenceService,
        stockConfigService,
        productRepo,
        inventoryRepo,
        stockMoveRepo,
        stockLocationLineService,
        stockMoveService,
        stockMoveLineService,
        stockLocationLineRepository,
        trackingNumberRepository,
        appBaseService);
    this.stockMoveLineRepository = stockMoveLineRepository;
    this.supplierCatalogRepository = supplierCatalogRepository;
  }

  @Override
  public List<? extends StockLocationLine> getStockLocationLines(Inventory inventory) {
    
    // copie de la méthode de la classe parente    
    String query = "(self.stockLocation = ? OR self.detailsStockLocation = ?)";
    List<Object> params = new ArrayList<>();

    params.add(inventory.getStockLocation());
    params.add(inventory.getStockLocation());

    if (inventory.getExcludeOutOfStock()) {
      query += " and self.currentQty > 0";
    }

    if (!inventory.getIncludeObsolete()) {
      query += " and (self.product.endDate > ? or self.product.endDate is null)";
      params.add(inventory.getPlannedEndDateT().toLocalDate());
    }

    if (inventory.getProductFamily() != null) {
      query += " and self.product.productFamily = ?";
      params.add(inventory.getProductFamily());
    }

    if (inventory.getProductCategory() != null) {
      query += " and self.product.productCategory = ?";
      params.add(inventory.getProductCategory());
    }

    if (inventory.getProduct() != null) {
      query += " and self.product = ?";
      params.add(inventory.getProduct());
    }

    if (!Strings.isNullOrEmpty(inventory.getFromRack())) {
      query += " and self.rack >= ?";
      params.add(inventory.getFromRack());
    }

    if (!Strings.isNullOrEmpty(inventory.getToRack())) {
      query += " and self.rack <= ?";
      params.add(inventory.getToRack());
    }

    // ajout de code spécifique
    if (inventory.getSupplier() != null)
    {
      query += this.filterInventoryFromStockMove(inventory);
      //query += this.filterInventoryFromSupplierCatalog(inventory);
    }
    //fin du code spécifique

    return stockLocationLineRepository.all().filter(query, params.toArray()).fetch();
  }

  // on va chercher les lignes de mouvement de stock qui correspondent au filtre fournisseur
  // on filtre ensuite les lignes d'inventaires dont l'article et éventuellement le
  // numéro de lot qui sont cohérents par rapport aux lignes de mouvement de stock pour ce
  // fournisseur
  // il y a probablement une meilleure façon de faire, code potentiellement à améliorer s'il y a
  // des soucis de performance
  private String filterInventoryFromStockMove(Inventory inventory)
  {
    String idString;
    String query = "";
     
    ArrayList<Long> listTrackingNumber = new ArrayList<>();
    //ArrayList<Long> listStockLocation = new ArrayList<>();
    ArrayList<Long> listProduct = new ArrayList<>();

    // on récupère les lignes de mouvement de stock qui correspondent au filtre fournisseur
    for (StockMoveLine stockMoveLine :
        stockMoveLineRepository
            .all()
            .filter("self.stockMove.partner =  ?", inventory.getSupplier())
            .fetch()) {
      // on remplit les listes avec les valeurs des lignes de mouvement de stock
      if (stockMoveLine.getTrackingNumber() != null
          && !listTrackingNumber.contains(stockMoveLine.getTrackingNumber().getId())) {
        listTrackingNumber.add(stockMoveLine.getTrackingNumber().getId());
      }
      /*
      if (stockMoveLine.getStockMove().getToStockLocation() != null
          && !listStockLocation.contains(
              stockMoveLine.getStockMove().getToStockLocation().getId())) {
        listStockLocation.add(stockMoveLine.getStockMove().getToStockLocation().getId());
      }
      */
      if (!listProduct.contains(stockMoveLine.getProduct().getId())) {
        listProduct.add(stockMoveLine.getProduct().getId());
      }
    }
    // on filtre les lignes d'inventaires selon la liste de numéro de série du fournisseur
    if (!listTrackingNumber.isEmpty()) {        
      idString = listTrackingNumber.stream().map(l -> l.toString()).collect(Collectors.joining(","));
      query += "and self.trackingNumber.id IN ("+ idString +")";
    }
    // on filtre les lignes d'inventaires selon la liste des emplacements où on a mis la
    // marchandise du fournisseur
    // Commenté car la marchandise peut avoir été déplacée depuis l'entrée
    /*
    if (!listStockLocation.isEmpty()) {
      idString = listStockLocation.stream().map(l -> l.toString()).collect(Collectors.joining(","));
      query += "and (self.stockLocation.id IN ("+ idString +") OR self.detailsStockLocation.id IN ("+ idString +"))";       
    }
    */
    // on filtre les lignes d'inventaires selon la liste des articles effectivement envoyé par ce
    // fournisseur
    if (!listProduct.isEmpty()) {
      idString = listProduct.stream().map(l -> l.toString()).collect(Collectors.joining(","));
      query += "and self.product.id IN (" + idString + ")";        
    }
    

    return query;
  }

  private String filterInventoryFromSupplierCatalog(Inventory inventory)
  {
    ArrayList<Long> listProduct = new ArrayList<>();
    String query = "";
    String idString;
    //on parcourt la liste des articles du catalogue fournisseur
    for (SupplierCatalog supplierCatalog :
        supplierCatalogRepository
            .all()
            .filter("self.supplierPartner =  ?", inventory.getSupplier())
            .fetch()) {
      // on remplit les listes avec les valeurs des lignes de mouvement de stock      
      if (!listProduct.contains(supplierCatalog.getProduct().getId())) {
        listProduct.add(supplierCatalog.getProduct().getId());
      }
    }
    // on filtre les lignes d'inventaires selon la liste des articles du catalogue fournisseur
    if (!listProduct.isEmpty()) {
      idString = listProduct.stream().map(l -> l.toString()).collect(Collectors.joining(","));
      query += "and self.product.id IN (" + idString + ")";        
    }
    return query;
  }
}
