package com.avr.apps.helpdesk.service;

import com.axelor.apps.purchase.db.PurchaseRequest;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.db.Model;
import com.axelor.exception.AxelorException;
import java.util.List;
import java.util.function.Function;

public interface TicketService {

  <T extends Model, R extends String> String joinBy(
      List<T> list, Function<? super T, ? extends R> mapper);

  List<SaleOrder> computedSequenceSaleOrderIfNotExist(List<SaleOrder> saleOrderList)
      throws AxelorException;

  List<StockMove> computedSequenceStockMoveIfNotExist(List<StockMove> saleOrderList)
      throws AxelorException;

  List<PurchaseRequest> computedSequencePurchaseRequestIfNotExist(
      List<PurchaseRequest> saleOrderList) throws AxelorException;

  <T extends Model> void computedSequenceIfNotExist(
      List<T> modelList, Function<T, Boolean> condition, ConsumerThrowable<T> sequenceMethod)
      throws AxelorException;

  @FunctionalInterface
  interface ConsumerThrowable<T> {
    void accept(T t) throws AxelorException;
  }
}
