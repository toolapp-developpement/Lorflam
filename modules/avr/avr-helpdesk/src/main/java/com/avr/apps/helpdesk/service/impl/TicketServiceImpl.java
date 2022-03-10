package com.avr.apps.helpdesk.service.impl;

import com.avr.apps.helpdesk.service.TicketService;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.db.Model;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class TicketServiceImpl implements TicketService {

    protected final SequenceService sequenceService;

    @Inject
    public TicketServiceImpl(SequenceService sequenceService) {
        this.sequenceService = sequenceService;
    }

    @Override
    public <T extends Model, R extends String> String joinBy(List<T> list, Function<? super T, ? extends R> mapper) {
        return list == null ? "" : list.stream().map(mapper).collect(Collectors.joining(" / "));
    }

    @Override
    public List<SaleOrder> computedSequenceSaleOrderIfNotExist(List<SaleOrder> saleOrderList) throws AxelorException {
        computedSequenceIfNotExist(saleOrderList, it -> it.getSaleOrderSeq() == null, it -> it.setSaleOrderSeq(sequenceService.getDraftSequenceNumber(it)));
        return saleOrderList;
    }

    @Override
    public List<StockMove> computedSequenceStockMoveIfNotExist(List<StockMove> stockMoveList) throws AxelorException {
        computedSequenceIfNotExist(stockMoveList, it -> it.getStockMoveSeq() == null, it -> it.setStockMoveSeq(sequenceService.getDraftSequenceNumber(it)));
        return stockMoveList;
    }

    @Override
    public List<PurchaseOrder> computedSequencePurchaseOrderIfNotExist(List<PurchaseOrder> purchaseOrderList) throws AxelorException {
        computedSequenceIfNotExist(purchaseOrderList, it -> it.getPurchaseOrderSeq() == null, it -> it.setPurchaseOrderSeq(sequenceService.getDraftSequenceNumber(it)));
        return purchaseOrderList;
    }

    @Override
    public <T extends Model> List<T> computedSequenceIfNotExist(List<T> modelList, Function<T, Boolean> condition, ConsumerThrowable<T> sequenceMethod) throws AxelorException {
        if (modelList == null) return null;

        for (T bean : modelList) {
            if(condition.apply(bean)) {
                sequenceMethod.accept(bean);
            }
        }
        return modelList;
    }


}
