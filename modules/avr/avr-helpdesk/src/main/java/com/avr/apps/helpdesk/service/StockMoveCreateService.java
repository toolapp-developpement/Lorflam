package com.avr.apps.helpdesk.service;

import com.avr.apps.helpdesk.db.Yard;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.stock.db.*;
import com.axelor.exception.AxelorException;

import java.time.LocalDate;

/**
 * @author David
 * @version 1.0
 * @date 25/04/2022
 * @time 12:07
 * @Update 25/04/2022
 */
public interface StockMoveCreateService {
    StockMove createStockMove(
            Address fromAddress,
            Address toAddress,
            Company company,
            Partner clientPartner,
            StockLocation fromStockLocation,
            StockLocation toStockLocation,
            LocalDate realDate,
            LocalDate estimatedDate,
            String note,
            ShipmentMode shipmentMode,
            FreightCarrierMode freightCarrierMode,
            Partner carrierPartner,
            Partner forwarderPartner,
            Incoterm incoterm,
            int typeSelect,
            Yard yard
    )
            throws AxelorException;
}
