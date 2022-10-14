package bzh.toolapp.apps.specifique.service.impl;

import com.axelor.apps.base.db.CancelReason;
import com.axelor.apps.base.db.repo.CancelReasonRepository;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.production.db.ProductionOrder;
import com.axelor.apps.production.db.repo.ProductionOrderRepository;
import com.axelor.apps.production.service.manuforder.ManufOrderWorkflowService;
import com.axelor.apps.production.service.productionorder.ProductionOrderService;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.repo.SaleOrderLineRepository;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.sale.service.saleorder.SaleOrderComputeService;
import com.axelor.apps.sale.service.saleorder.SaleOrderLineService;
import com.axelor.apps.sale.service.saleorder.SaleOrderMarginService;
import com.axelor.apps.supplychain.service.SaleOrderServiceSupplychainImpl;
import com.axelor.apps.supplychain.service.SaleOrderStockService;
import com.axelor.apps.supplychain.service.app.AppSupplychainService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;

import java.util.List;

public class SaleOrderServiceSpecifiqueImpl extends SaleOrderServiceSupplychainImpl {
    @Inject
    private ProductionOrderRepository productionOrderRepository;
    @Inject
    private ProductionOrderService productionOrderService;
    @Inject
    private ManufOrderWorkflowService manufOrderWorkflowService;
    @Inject
    private CancelReasonRepository cancelReasonRepository;


    @Inject
    public SaleOrderServiceSpecifiqueImpl(
            SaleOrderLineService saleOrderLineService,
            AppBaseService appBaseService,
            SaleOrderLineRepository saleOrderLineRepo,
            SaleOrderRepository saleOrderRepo,
            SaleOrderComputeService saleOrderComputeService,
            SaleOrderMarginService saleOrderMarginService,
            AppSupplychainService appSupplychainService,
            SaleOrderStockService saleOrderStockService,
            ProductionOrderRepository productionOrderRepository,
            ProductionOrderService productionOrderService, ManufOrderWorkflowService manufOrderWorkflowService,
            CancelReasonRepository cancelReasonRepository) {
        super(
                saleOrderLineService,
                appBaseService,
                saleOrderLineRepo,
                saleOrderRepo,
                saleOrderComputeService,
                saleOrderMarginService,
                appSupplychainService,
                saleOrderStockService);
        this.productionOrderRepository = productionOrderRepository;
        this.productionOrderService = productionOrderService;
        this.manufOrderWorkflowService = manufOrderWorkflowService;
        this.cancelReasonRepository = cancelReasonRepository;
    }

    @Override
    public boolean enableEditOrder(SaleOrder saleOrder) throws AxelorException {
        // Annulation des documents de stock
        boolean checkAvailabiltyRequest = super.enableEditOrder(saleOrder);

        CancelReason cancelReason = cancelReasonRepository.all().filter("self.isDefault = true AND self.applicationType = 'com.axelor.apps.production.db.ManufOrder' " ).fetchOne();

        // Récupération de la listes des ordres de fabrication et les ordres de production
        List<ProductionOrder> productionOrders =
                productionOrderRepository
                        .all()
                        .filter("self.saleOrder = :sale_order")
                        .bind("sale_order", saleOrder.getId())
                        .fetch();

        // Boucle sur les ordres de production afin d'annuler les ordres de fabrication
                productionOrders.forEach(productionOrder ->{
                        productionOrder.getManufOrderSet().forEach(manufOrder ->
                                {
                                    try {
                                        manufOrderWorkflowService.cancel(manufOrder, cancelReason,
                                                cancelReason.getName());
                                    } catch (AxelorException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        );
                        productionOrder.setSaleOrder(null);
                        productionOrder.setArchived(true);}
                );
        return checkAvailabiltyRequest;
    }

    @Override
    public void validateChanges(SaleOrder saleOrder) throws AxelorException {
        super.validateChanges(saleOrder);
        //Création des ordres de production et des ordres de fabrication suite à la modification
        productionOrderService.createProductionOrder(saleOrder);

    }
}
