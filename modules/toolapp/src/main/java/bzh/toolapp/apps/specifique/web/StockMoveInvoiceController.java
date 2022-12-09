package bzh.toolapp.apps.specifique.web;

import com.axelor.apps.account.db.Invoice;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.stock.db.StockMove;
import com.axelor.apps.supplychain.service.StockMoveMultiInvoiceService;
import com.axelor.db.JPA;
import com.axelor.exception.service.TraceBackService;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.meta.schema.actions.ActionView.ActionViewBuilder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Joiner;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StockMoveInvoiceController {
  /**
   * Called from mass invoicing out stock move form view. Call method to check for missing fields.
   * If there are missing fields, show a wizard. Else call {@link
   * StockMoveMultiInvoiceService#createInvoiceFromMultiOutgoingStockMove(List)} and show the
   * generated invoice.
   *
   * @param request
   * @param response
   */
  public void generateInvoiceConcatOutStockMoveCheckMissingFields(
      ActionRequest request, ActionResponse response) {
    try {
      List<StockMove> stockMoveList = new ArrayList<>();
      List<Long> stockMoveIdList = new ArrayList<>();

      // No confirmation pop-up, stock Moves are content in a parameter list
      @SuppressWarnings("unchecked")
      List<Map> stockMoveMap = (List<Map>) request.getContext().get("customerStockMoveToInvoice");

      // Get StockMove id
      for (Map map : stockMoveMap) {
        stockMoveIdList.add(Long.valueOf((Integer) map.get("id")));
      }

      // Get StockMove detail
      for (Long stockMoveId : stockMoveIdList) {
        stockMoveList.add(JPA.em().find(StockMove.class, stockMoveId));
      }

      // Get Partner id
      List<Long> partnerIdList = this.getPartnerIds(stockMoveList);
      // Loop on partner Id to create invoice
      for (Long pil : partnerIdList) {
        // Send stock move without Sales Order
        List<StockMove> stockMoveListToInvoice = this.getStockMoves(pil, stockMoveList);
        if (stockMoveListToInvoice.size() != 0) {
          this.sendToInvoice(response, stockMoveListToInvoice);
        }
      }
      response.setFlash("Nombre de facture : " + partnerIdList.size());

    } catch (Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  // Send StockMoves to invoice
  private void sendToInvoice(ActionResponse response, List<StockMove> stockMoveList) {
    try {
      List<Long> stockMoveIdList = new ArrayList<>();

      for (StockMove sml : stockMoveList) {
        stockMoveIdList.add(sml.getId());
      }

      Map<String, Object> mapResult =
          Beans.get(StockMoveMultiInvoiceService.class)
              .areFieldsConflictedToGenerateCustInvoice(stockMoveList);
      boolean paymentConditionToCheck =
          (Boolean) mapResult.getOrDefault("paymentConditionToCheck", false);
      boolean paymentModeToCheck = (Boolean) mapResult.getOrDefault("paymentModeToCheck", false);
      boolean contactPartnerToCheck =
          (Boolean) mapResult.getOrDefault("contactPartnerToCheck", false);

      StockMove stockMove = stockMoveList.get(0);
      Partner partner = stockMove.getPartner();
      if (paymentConditionToCheck || paymentModeToCheck || contactPartnerToCheck) {
        ActionViewBuilder confirmView =
            ActionView.define("StockMove")
                .model(StockMove.class.getName())
                .add("form", "stock-move-supplychain-concat-cust-invoice-confirm-form")
                .param("popup", "true")
                .param("show-toolbar", "false")
                .param("show-confirm", "false")
                .param("popup-save", "false")
                .param("forceEdit", "true");

        if (paymentConditionToCheck) {
          confirmView.context("contextPaymentConditionToCheck", "true");
        } else {
          confirmView.context("paymentCondition", mapResult.get("paymentCondition"));
        }

        if (paymentModeToCheck) {
          confirmView.context("contextPaymentModeToCheck", "true");
        } else {
          confirmView.context("paymentMode", mapResult.get("paymentMode"));
        }
        if (contactPartnerToCheck) {
          confirmView.context("contextContactPartnerToCheck", "true");
          confirmView.context("contextPartnerId", partner.getId().toString());
        } else {
          confirmView.context("contactPartner", mapResult.get("contactPartner"));
        }
        confirmView.context("customerStockMoveToInvoice", Joiner.on(",").join(stockMoveIdList));
        response.setView(confirmView.map());
      } else {
        Optional<Invoice> invoice =
            Beans.get(StockMoveMultiInvoiceService.class)
                .createInvoiceFromMultiOutgoingStockMove(stockMoveList);
        /*
         * invoice.ifPresent(inv ->
         * response.setView(ActionView.define("Invoice").model(Invoice.class.getName())
         * .add("grid", "invoice-grid").add("form", "invoice-form")
         * .param("search-filters", "customer-invoices-filters").param("forceEdit",
         * "true") .context("_operationTypeSelect", inv.getOperationTypeSelect())
         * .context("todayDate",
         * Beans.get(AppSupplychainService.class).getTodayDate(stockMove.getCompany()))
         * .context("_showRecord", String.valueOf(inv.getId())).map()));
         */
      }
    } catch (final Exception e) {
      TraceBackService.trace(response, e);
    }
  }

  private List<StockMove> getStockMoves(Long idPartner, List<StockMove> stockMoveList) {
    List<StockMove> stockMoveToInvoice = new ArrayList<>();

    // Loop on the list
    for (StockMove sm : stockMoveList) {
      if (sm.getPartner().getId().equals(idPartner)) {
        // Add the stock move to send to the invoice if the amount greater than 0
        if (sm.getExTaxTotal().compareTo(BigDecimal.ZERO) > 0) {
          stockMoveToInvoice.add(sm);
        }
      }
    }

    return stockMoveToInvoice;
  }

  // Get partner id from stock moves
  private List<Long> getPartnerIds(List<StockMove> stockMoveList) {
    List<Long> partnerIdList = new ArrayList<>();
    // Collect partner id
    for (StockMove sm : stockMoveList) {
      // Add only if the partner id is unknown
      if (!partnerIdList.contains(sm.getPartner().getId())) {
        partnerIdList.add(sm.getPartner().getId());
      }
    }
    return partnerIdList;
  }
}
