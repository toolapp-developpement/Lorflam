package bzh.toolapp.apps.specifique.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.businessproduction.service.ProductionOrderServiceBusinessImpl;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.ManufOrder;
import com.axelor.apps.production.db.ProductionOrder;
import com.axelor.apps.production.db.repo.ProductionOrderRepository;
import com.axelor.apps.production.service.manuforder.ManufOrderService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class ProductionOrderServiceBusinessCustomImpl extends ProductionOrderServiceBusinessImpl {

    @Inject
    public ProductionOrderServiceBusinessCustomImpl(ManufOrderService manufOrderService,
            SequenceService sequenceService, ProductionOrderRepository productionOrderRepo) {
        super(manufOrderService, sequenceService, productionOrderRepo);
        
    }

    @Override
    @Transactional(rollbackOn = {Exception.class})
    public ProductionOrder addManufOrder(
        ProductionOrder productionOrder,
        Product product,
        BillOfMaterial billOfMaterial,
        BigDecimal qtyRequested,
        LocalDateTime startDate,
        LocalDateTime endDate,
        SaleOrder saleOrder,
        int originType)
        throws AxelorException {

        ManufOrder manufOrder =
            manufOrderService.generateManufOrder(
                product,
                qtyRequested,
                ManufOrderService.DEFAULT_PRIORITY,
                ManufOrderService.IS_TO_INVOICE,
                billOfMaterial,
                startDate,
                endDate,
                originType);

        if (manufOrder != null) {
        if (saleOrder != null) {
            manufOrder.addSaleOrderSetItem(saleOrder);
            manufOrder.setClientPartner(saleOrder.getClientPartner());
            manufOrder.setMoCommentFromSaleOrder(saleOrder.getProductionNote());

            manufOrder.getConsumedStockMoveLineList().forEach(
                stockMoveLine -> {
                    stockMoveLine.setCustSaleOrderSeq(saleOrder.getSaleOrderSeq());
                }
            );

            manufOrder.getProducedStockMoveLineList().forEach(
                stockMoveLine -> {
                    stockMoveLine.setCustSaleOrderSeq(saleOrder.getSaleOrderSeq());
                }
            );
        }
        productionOrder.addManufOrderSetItem(manufOrder);
        manufOrder.addProductionOrderSetItem(productionOrder);
        }

        productionOrder = updateProductionOrderStatus(productionOrder);
        return productionOrderRepo.save(productionOrder);
    }
    
}
