package com.avr.apps.helpdesk.service;

import com.avr.apps.helpdesk.db.Yard;
import com.axelor.apps.base.db.*;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.stock.db.StockLocation;
import com.axelor.auth.db.User;
import com.axelor.exception.AxelorException;

import java.time.LocalDate;

/**
 * @author David
 * @version 1.0
 * @date 25/04/2022
 * @time 11:29
 * @Update 25/04/2022
 */
public interface PurchaseOrderCreationService {
    PurchaseOrder createPurchaseOrder(
            User buyerUser,
            Company company,
            Partner contactPartner,
            Currency currency,
            LocalDate deliveryDate,
            String internalReference,
            String externalReference,
            LocalDate orderDate,
            PriceList priceList,
            Partner supplierPartner,
            TradingName tradingName,
            Yard yard)
            throws AxelorException;

    PurchaseOrder createPurchaseOrder(
            User buyerUser,
            Company company,
            Partner contactPartner,
            Currency currency,
            LocalDate deliveryDate,
            String internalReference,
            String externalReference,
            StockLocation stockLocation,
            LocalDate orderDate,
            PriceList priceList,
            Partner supplierPartner,
            TradingName tradingName,
            Yard yard)
            throws AxelorException;
}
