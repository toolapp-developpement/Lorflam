package bzh.toolapp.apps.specifique.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.units.qual.h;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.account.db.InvoiceLine;
import com.axelor.apps.account.db.repo.InvoiceLineRepository;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.app.AppAccountService;
import com.axelor.apps.account.service.config.AccountConfigService;
import com.axelor.apps.account.service.invoice.InvoiceLineService;
import com.axelor.apps.account.service.invoice.factory.CancelFactory;
import com.axelor.apps.account.service.invoice.factory.ValidateFactory;
import com.axelor.apps.account.service.invoice.factory.VentilateFactory;
import com.axelor.apps.account.service.move.MoveToolService;
import com.axelor.apps.base.db.repo.PrintingSettingsRepository;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.apps.base.service.alarm.AlarmEngineService;
import com.axelor.apps.businessproject.service.InvoiceServiceProjectImpl;
import com.axelor.apps.message.db.Message;
import com.axelor.apps.message.db.Template;
import com.axelor.apps.message.service.TemplateMessageService;
import com.axelor.apps.toolapp.db.InvoiceEcoTaxDetail;
import com.axelor.apps.toolapp.db.repo.InvoiceEcoTaxDetailRepository;
import com.axelor.exception.AxelorException;
import com.axelor.inject.Beans;
import com.google.inject.Inject;
import com.axelor.apps.cash.management.service.InvoiceEstimatedPaymentService;
import com.axelor.apps.cash.management.service.InvoiceServiceManagementImpl;

// MA1-I65 : Ajout de la classe InvoiceServiceSpecifiqueImpl
// calcul de l'éco taxe
// Karl 
public class InvoiceServiceSpecifiqueImpl extends InvoiceServiceManagementImpl{ 
    
    @Inject private InvoiceEcoTaxDetailRepository invoiceEcoTaxDetailRepository;

    @Inject
    public InvoiceServiceSpecifiqueImpl(ValidateFactory validateFactory, VentilateFactory ventilateFactory,
            CancelFactory cancelFactory, AlarmEngineService<Invoice> alarmEngineService, InvoiceRepository invoiceRepo,
            AppAccountService appAccountService, PartnerService partnerService, InvoiceLineService invoiceLineService,
            AccountConfigService accountConfigService, MoveToolService moveToolService,
            InvoiceLineRepository invoiceLineRepo, InvoiceEstimatedPaymentService invoiceEstimatedPaymentService) {
        super(validateFactory, ventilateFactory, cancelFactory, alarmEngineService, invoiceRepo, appAccountService,
                partnerService, invoiceLineService, accountConfigService, moveToolService, invoiceLineRepo,
                invoiceEstimatedPaymentService);
    }
    

     @Override
    public void validate(final Invoice invoice) throws AxelorException {
        
        super.validate(invoice);
        boolean hasLine = false;
        //on parcours les lignes de la commandes
        for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
            //on vérifie si la ligne est une ligne de produit
            if (invoiceLine.getProduct() != null) {
                //on vérifie si le produit est un produit éco taxé
                if (invoiceLine.getProduct().getEcoTax() != null) {
                    InvoiceEcoTaxDetail invoiceEcoTaxDetail = invoiceEcoTaxDetailRepository
                        .all()
                        .filter("self.invoice = ?1 AND self.ecoTax = ?2"
                        , invoice
                        , invoiceLine.getProduct().getEcoTax()).fetchOne();
                    if (invoiceEcoTaxDetail == null) {
                        //on crée la ligne dans la table invoice_eco_tax_detail
                         invoiceEcoTaxDetail = new InvoiceEcoTaxDetail();
                    }
                    invoiceEcoTaxDetail.setInvoice(invoice);
                    invoiceEcoTaxDetail.setEcoTax(invoiceLine.getProduct().getEcoTax());

                    invoiceLine.setEcoTax(invoiceLine.getProduct().getEcoTax());

                    InvoiceLineRepository invoiceLineRepo = Beans.get(InvoiceLineRepository.class);
                    invoiceLineRepo.save(invoiceLine);
                    //TODO gestion du calcul du montant de l'éco taxe
                    invoiceEcoTaxDetail.setAmount(invoiceLine.getEcoTaxAmount());

                    invoiceEcoTaxDetailRepository.save(invoiceEcoTaxDetail);
                    hasLine = true;
                }
            }
        };

        if (!hasLine) {
            invoice.setEcoTaxDetails(null);
            invoiceRepo.save(invoice);
            return;
        }

        PrintingSettingsRepository printingSettingsRepo = Beans.get(PrintingSettingsRepository.class);
        if (printingSettingsRepo.all().fetchOne().getEcoTaxInvoiceTemplate() != null) {
            Template template = printingSettingsRepo.all().fetchOne().getEcoTaxInvoiceTemplate();

            TemplateMessageService templateMessageService = Beans.get(TemplateMessageService.class);
            Message message = new Message();
            try {
                message = templateMessageService.generateMessage(invoice.getId(), "com.axelor.apps.account.db.Invoice", "Invoice", template, true);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
                
                e.printStackTrace();
            }
            String ecoTaxeText = message.getContent();
            invoice.setEcoTaxDetails(ecoTaxeText);
                        
        }

        invoiceRepo.save(invoice);
    }
}
