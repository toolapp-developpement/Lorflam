package bzh.toolapp.apps.specifique.service;

import java.io.IOException;
import java.util.List;

import com.axelor.apps.base.db.repo.PrintingSettingsRepository;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.Template;
import com.axelor.apps.message.service.TemplateMessageService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.toolapp.db.InvoiceEcoTaxDetail;
import com.axelor.apps.toolapp.db.repo.InvoiceEcoTaxDetailRepository;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;


public class SaleOrderRecalculateService {
    private SaleOrderRepository saleOrderRepo;
    private InvoiceEcoTaxDetailRepository invoiceEcoTaxDetailRepository;
    @Inject
    public SaleOrderRecalculateService(SaleOrderRepository saleOrderRepo, InvoiceEcoTaxDetailRepository invoiceEcoTaxDetailRepository) {
        this.saleOrderRepo = saleOrderRepo;
        this.invoiceEcoTaxDetailRepository = invoiceEcoTaxDetailRepository;
    }

    //MA1-I65 - Karl Alexandersson
    public void recalculateEcoTax()
    {
        //parcours de toutes les commandes validées et non facturées
        List<SaleOrder> saleOrderList = saleOrderRepo.all()
            .filter("self.statusSelect = 2 OR self.statusSelect = 3")
            .fetch();
        for (SaleOrder saleOrder : saleOrderList) {
            try {
                calculateEcoTax(saleOrder);
            } catch (AxelorException e) {
                e.printStackTrace();
            }
        }

    }

    @Transactional
    public void calculateEcoTax(SaleOrder saleOrder) throws AxelorException {
        invoiceEcoTaxDetailRepository = Beans.get(InvoiceEcoTaxDetailRepository.class);
        Boolean hasLine = false;
        //on parcours les lignes de la commandes
        for (SaleOrderLine saleOrderLine : saleOrder.getSaleOrderLineList()) {
            //on vérifie si la ligne est une ligne de produit
            if (saleOrderLine.getProduct() != null && saleOrderLine.getProduct().getEcoTax() != null && saleOrderLine.getSaleOrder() != null) {
                //on vérifie si le produit est un produit éco taxé
                if (saleOrderLine.getProduct().getEcoTax() != null) {
                    InvoiceEcoTaxDetail invoiceEcoTaxDetail = invoiceEcoTaxDetailRepository
                        .all()
                        .filter("self.saleOrder = ?1 AND self.ecoTax = ?2"
                        , saleOrder
                        , saleOrderLine.getProduct().getEcoTax()).fetchOne();
                    if (invoiceEcoTaxDetail == null) {
                        //on crée la ligne dans la table invoice_eco_tax_detail
                         invoiceEcoTaxDetail = new InvoiceEcoTaxDetail();
                    }
                    invoiceEcoTaxDetail.setSaleOrder(saleOrder);
                    invoiceEcoTaxDetail.setEcoTax(saleOrderLine.getProduct().getEcoTax());
                    //TODO gestion du calcul du montant de l'éco taxe
                    invoiceEcoTaxDetail.setAmount(saleOrderLine.getEcoTaxAmount());

                    invoiceEcoTaxDetailRepository.save(invoiceEcoTaxDetail);
                    hasLine = true;
                    
                }
            }
        };
        if (!hasLine) {
            saleOrder.setEcoTaxDetails(null);
            return;
        }

        PrintingSettingsRepository printingSettingsRepo = Beans.get(PrintingSettingsRepository.class);
        if (printingSettingsRepo.all().fetchOne().getEcoTaxTemplate() != null) {
            Template template = printingSettingsRepo.all().fetchOne().getEcoTaxTemplate();

            TemplateMessageService templateMessageService = Beans.get(TemplateMessageService.class);
            Message message = new Message();
            try {
                message = templateMessageService.generateMessage(saleOrder.getId(), "com.axelor.apps.sale.db.SaleOrder", "SaleOrder", template, true);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
                
                e.printStackTrace();
            }
            String ecoTaxeText = message.getContent();
            saleOrder.setEcoTaxDetails(ecoTaxeText);

        }

        saleOrderRepo.save(saleOrder);


    }

    //MA1-I88 - Karl Alexandersson
    @Transactional
    public void recalcultateStockMoveLineRef()
    {
        // Selection de tous les stockMoveLine qui ont un stockMove.statusSelect=2 AND stockMove.typeSelect !=3 AND   stockMove.custSaleOrderSeq =''
       StockMoveLineRepository stockMoveLineRepository = Beans.get(StockMoveLineRepository.class);
         List<StockMoveLine> stockMoveLineList = stockMoveLineRepository.all()
            .filter("self.stockMove.statusSelect = 2 AND self.stockMove.typeSelect != 3 AND self.stockMove.custSaleOrderSeq = null")
            .fetch();

        // Pour chaque stockMoveLine, on met à jour le custSaleOrderSeq
        for (StockMoveLine sml : stockMoveLineList) {
            String saleOrderSeq = new String();
            if (sml.getSaleOrderLine() != null && sml.getSaleOrderLine().getSaleOrder() != null) {
                saleOrderSeq = sml.getSaleOrderLine().getSaleOrder().getSaleOrderSeq();
            } else if (sml.getProducedManufOrder() != null
                && sml.getProducedManufOrder().getSaleOrderSet() != null) {
                if (!sml.getProducedManufOrder().getSaleOrderSet().isEmpty()) {
                SaleOrder so = sml.getProducedManufOrder().getSaleOrderSet().iterator().next();
                saleOrderSeq = so.getSaleOrderSeq();
                }
            } else if (sml.getConsumedManufOrder() != null
                && sml.getConsumedManufOrder().getSaleOrderSet() != null) {
                if (!sml.getConsumedManufOrder().getSaleOrderSet().isEmpty()) {
                SaleOrder so = sml.getConsumedManufOrder().getSaleOrderSet().iterator().next();
                saleOrderSeq = so.getSaleOrderSeq();
                }
            }
            if(saleOrderSeq != null && !saleOrderSeq.isEmpty())
            {
                sml.setCustSaleOrderSeq(saleOrderSeq);
                stockMoveLineRepository.save(sml);
            }
        }
    }
}
