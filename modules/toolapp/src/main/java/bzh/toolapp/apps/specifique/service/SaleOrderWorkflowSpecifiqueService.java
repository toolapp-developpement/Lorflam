package bzh.toolapp.apps.specifique.service;

import org.checkerframework.checker.units.qual.s;

import com.axelor.apps.account.db.repo.AnalyticMoveLineRepository;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.businessproduction.service.SaleOrderWorkflowServiceBusinessProductionImpl;
import com.axelor.apps.production.service.app.AppProductionService;
import com.axelor.apps.production.service.productionorder.ProductionOrderSaleOrderService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.sale.service.app.AppSaleService;
import com.axelor.apps.sale.service.saleorder.SaleOrderLineService;
import com.axelor.apps.supplychain.service.AccountingSituationSupplychainService;
import com.axelor.apps.supplychain.service.SaleOrderPurchaseService;
import com.axelor.apps.supplychain.service.SaleOrderStockService;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.apps.toolapp.db.InvoiceEcoTaxDetail;
import com.axelor.apps.toolapp.db.repo.InvoiceEcoTaxDetailRepository;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

// MA1-I65 : Ajout de la classe SaleOrderWorkflowSpecifiqueService
// calcul de l'éco taxe
// Karl 
public class SaleOrderWorkflowSpecifiqueService
    extends SaleOrderWorkflowServiceBusinessProductionImpl {

    @Inject private InvoiceEcoTaxDetailRepository invoiceEcoTaxDetailRepository;

    @Inject
    public SaleOrderWorkflowSpecifiqueService(SequenceService sequenceService, PartnerRepository partnerRepo,
            SaleOrderRepository saleOrderRepo, AppSaleService appSaleService, UserService userService,
            SaleOrderLineService saleOrderLineService, SaleOrderStockService saleOrderStockService,
            SaleOrderPurchaseService saleOrderPurchaseService, AppSupplychainService appSupplychainService,
            AccountingSituationSupplychainService accountingSituationSupplychainService,
            ProductionOrderSaleOrderService productionOrderSaleOrderService, AppProductionService appProductionService,
            AnalyticMoveLineRepository analyticMoveLineRepository,
            InvoiceEcoTaxDetailRepository invoiceEcoTaxDetailRepository) {
            
        super(sequenceService, partnerRepo, saleOrderRepo, appSaleService, userService, saleOrderLineService,
                saleOrderStockService, saleOrderPurchaseService, appSupplychainService, accountingSituationSupplychainService,
                productionOrderSaleOrderService, appProductionService, analyticMoveLineRepository);    
        
    }

    @Override
    @Transactional
    public void finalizeQuotation(SaleOrder saleOrder) throws AxelorException {
        super.finalizeQuotation(saleOrder);

        //on parcours les lignes de la commandes
        saleOrder.getSaleOrderLineList().forEach(saleOrderLine -> {
            //on vérifie si la ligne est une ligne de produit
            if (saleOrderLine.getProduct() != null) {
                //on vérifie si le produit est un produit éco taxé
                if (saleOrderLine.getProduct().getEcoTax() != null) {
                    //on vérifie si la ligne n'a pas déjà été traitée
                    // verifier si la ligne existe dans la table invoice_eco_tax_detail
                    // si elle n'existe pas, on la crée
                    if (invoiceEcoTaxDetailRepository
                        .all()
                        .filter("self.saleOrder = ?1 AND self.ecoTax = ?2", saleOrder, saleOrderLine.getProduct().getEcoTax()).fetchOne() == null) {
                        //on crée la ligne dans la table invoice_eco_tax_detail
                        InvoiceEcoTaxDetail invoiceEcoTaxDetail = new InvoiceEcoTaxDetail();

                        invoiceEcoTaxDetail.setSaleOrder(saleOrder);
                        invoiceEcoTaxDetail.setEcoTax(saleOrderLine.getProduct().getEcoTax());
                        //TODO gestion du calcul du montant de l'éco taxe
                        invoiceEcoTaxDetail.setAmount(saleOrderLine.getEcoTaxAmount());

                        invoiceEcoTaxDetailRepository.save(invoiceEcoTaxDetail);
                    }
                }
            }
        });

        // verifier si la ligne existe dans la table invoice_eco_tax_detail
        // si elle n'existe pas, on la crée
        
        invoiceEcoTaxDetailRepository.all().filter("self.saleOrder = ?1", saleOrder).fetchOne();

    }
    
}
