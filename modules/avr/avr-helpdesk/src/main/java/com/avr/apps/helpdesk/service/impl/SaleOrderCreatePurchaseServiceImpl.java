package com.avr.apps.helpdesk.service.impl;

import com.avr.apps.helpdesk.service.PurchaseOrderCreationWithYardService;
import com.axelor.apps.account.db.repo.AccountConfigRepository;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.repo.PriceListRepository;
import com.axelor.apps.base.service.PartnerPriceListService;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.repo.PurchaseOrderRepository;
import com.axelor.apps.purchase.service.PurchaseOrderService;
import com.axelor.apps.purchase.service.config.PurchaseConfigService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.stock.service.StockLocationService;
import com.axelor.apps.supplychain.service.PurchaseOrderLineServiceSupplyChain;
import com.axelor.apps.supplychain.service.PurchaseOrderSupplychainService;
import com.axelor.apps.supplychain.service.SaleOrderPurchaseServiceImpl;
import com.axelor.auth.AuthUtils;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David
 * @version 1.0
 * @date 25/04/2022
 * @time 11:32
 * @Update 25/04/2022
 */
public class SaleOrderCreatePurchaseServiceImpl extends SaleOrderPurchaseServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final PurchaseOrderCreationWithYardService purchaseOrderCreationWithYardService;

    @Inject
    public SaleOrderCreatePurchaseServiceImpl(
        PurchaseOrderSupplychainService purchaseOrderSupplychainService,
        PurchaseOrderLineServiceSupplyChain purchaseOrderLineServiceSupplychain,
        PurchaseOrderService purchaseOrderService,
        PurchaseOrderCreationWithYardService purchaseOrderCreationWithYardService
    ) {
        super(purchaseOrderSupplychainService, purchaseOrderLineServiceSupplychain, purchaseOrderService);
        this.purchaseOrderCreationWithYardService = purchaseOrderCreationWithYardService;
    }

    @Override
    @Transactional(rollbackOn = { Exception.class })
    public PurchaseOrder createPurchaseOrder(Partner supplierPartner, List<SaleOrderLine> saleOrderLineList, SaleOrder saleOrder)
        throws AxelorException {
        LOG.debug("Création d'une commande fournisseur pour le devis client : {}", saleOrder.getSaleOrderSeq());

        PurchaseOrder purchaseOrder = purchaseOrderCreationWithYardService.createPurchaseOrder(
            AuthUtils.getUser(),
            saleOrder.getCompany(),
            supplierPartner.getContactPartnerSet().size() == 1 ? supplierPartner.getContactPartnerSet().iterator().next() : null,
            supplierPartner.getCurrency(),
            null,
            saleOrder.getSaleOrderSeq(),
            saleOrder.getExternalReference(),
            saleOrder.getDirectOrderLocation()
                ? saleOrder.getStockLocation()
                : Beans.get(StockLocationService.class).getDefaultReceiptStockLocation(saleOrder.getCompany()),
            Beans.get(AppBaseService.class).getTodayDate(saleOrder.getCompany()),
            Beans.get(PartnerPriceListService.class).getDefaultPriceList(supplierPartner, PriceListRepository.TYPE_PURCHASE),
            supplierPartner,
            saleOrder.getTradingName(),
            saleOrder.getYard()
        );

        purchaseOrder.setGeneratedSaleOrderId(saleOrder.getId());
        purchaseOrder.setGroupProductsOnPrintings(supplierPartner.getGroupProductsOnPrintings());

        Integer atiChoice = Beans
            .get(PurchaseConfigService.class)
            .getPurchaseConfig(saleOrder.getCompany())
            .getPurchaseOrderInAtiSelect();
        if (atiChoice == AccountConfigRepository.INVOICE_ATI_ALWAYS || atiChoice == AccountConfigRepository.INVOICE_ATI_DEFAULT) {
            purchaseOrder.setInAti(true);
        } else {
            purchaseOrder.setInAti(false);
        }

        for (SaleOrderLine saleOrderLine : saleOrderLineList) {
            if (saleOrderLine.getProduct() != null) {
                purchaseOrder.addPurchaseOrderLineListItem(
                    purchaseOrderLineServiceSupplychain.createPurchaseOrderLine(purchaseOrder, saleOrderLine)
                );
            }
        }

        purchaseOrderService.computePurchaseOrder(purchaseOrder);

        purchaseOrder.setNotes(supplierPartner.getPurchaseOrderComments());

        Beans.get(PurchaseOrderRepository.class).save(purchaseOrder);

        return purchaseOrder;
    }
}
