package com.avr.apps.helpdesk.service.impl;

import com.avr.apps.helpdesk.db.Yard;
import com.avr.apps.helpdesk.service.StockMoveCreateService;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.apps.base.service.app.AppBaseService;
import com.axelor.apps.stock.db.*;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.axelor.apps.stock.service.PartnerProductQualityRatingService;
import com.axelor.apps.stock.service.StockMoveLineService;
import com.axelor.apps.stock.service.StockMoveServiceImpl;
import com.axelor.apps.stock.service.StockMoveToolService;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;

import java.time.LocalDate;

/**
 * @author David
 * @version 1.0
 * @date 25/04/2022
 * @time 11:46
 * @Update 25/04/2022
 */
public class StockMoveCreateServiceImpl extends StockMoveServiceImpl implements StockMoveCreateService {

    protected final StockMoveToolService stockMoveToolService;

    @Inject
    public StockMoveCreateServiceImpl(
            StockMoveLineService stockMoveLineService,
            StockMoveToolService stockMoveToolService,
            StockMoveLineRepository stockMoveLineRepository,
            AppBaseService appBaseService,
            StockMoveRepository stockMoveRepository,
            PartnerProductQualityRatingService partnerProductQualityRatingService,
            ProductRepository productRepository,
            StockMoveToolService stockMoveToolService1) {
        super(
            stockMoveLineService,
            stockMoveToolService,
            stockMoveLineRepository,
            appBaseService,
            stockMoveRepository,
            partnerProductQualityRatingService,
            productRepository
        );
        this.stockMoveToolService = stockMoveToolService1;
    }

    @Override
    public StockMove createStockMove(
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
            throws AxelorException {

        StockMove stockMove =
                this.createStockMove(
                        fromAddress,
                        toAddress,
                        company,
                        fromStockLocation,
                        toStockLocation,
                        realDate,
                        estimatedDate,
                        note,
                        typeSelect);
        stockMove.setPartner(clientPartner);
        stockMove.setShipmentMode(shipmentMode);
        stockMove.setFreightCarrierMode(freightCarrierMode);
        stockMove.setCarrierPartner(carrierPartner);
        stockMove.setForwarderPartner(forwarderPartner);
        stockMove.setIncoterm(incoterm);
        stockMove.setNote(note);
        stockMove.setIsIspmRequired(stockMoveToolService.getDefaultISPM(clientPartner, toAddress));

        stockMove.setYard(yard);

        return stockMove;
    }


}
