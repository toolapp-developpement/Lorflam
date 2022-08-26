package bzh.toolapp.apps.specifique.service.etatstock;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.TempBomTree;
import com.axelor.apps.production.db.repo.ManufOrderRepository;
import com.axelor.apps.production.db.repo.TempBomTreeRepository;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.apps.purchase.db.repo.PurchaseOrderLineRepository;
import com.axelor.apps.purchase.db.repo.PurchaseOrderRepository;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.apps.sale.db.repo.SaleOrderLineRepository;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.stock.db.StockLocationLine;
import com.axelor.apps.stock.db.StockMoveLine;
import com.axelor.apps.stock.db.repo.StockLocationLineRepository;
import com.axelor.apps.stock.db.repo.StockLocationRepository;
import com.axelor.apps.stock.db.repo.StockMoveLineRepository;
import com.axelor.apps.stock.db.repo.StockMoveRepository;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class BillOfMaterialServiceSpecifiqueImpl implements BillOfMaterialServiceSpecifique {
	private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	@Inject
	private TempBomTreeRepository tempBomTreeRepo;
	@Inject
	private StockMoveLineRepository stockMoveLineRepository;
	@Inject
	private PurchaseOrderLineRepository purchaseOrderLineRepository;
	@Inject
	private SaleOrderLineRepository saleOrderLineRepository;
	@Inject
	private StockLocationLineRepository stockLocationLineRepository;
	private List<Long> processedBom;

	@Transactional
	public TempBomTree getBomTree(BillOfMaterial bom, BillOfMaterial parentBom, TempBomTree parent,
			boolean useProductDefaultBom, Long stockLocationId, Long companyId) {

		TempBomTree bomTree;
		if (parentBom == null) {
			bomTree = tempBomTreeRepo.all().filter("self.bom = ?1 and self.parentBom = null", bom).fetchOne();
		} else {
			bomTree = tempBomTreeRepo.all().filter("self.bom = ?1 and self.parentBom = ?2", bom, parentBom).fetchOne();
		}

		if (bomTree == null) {
			bomTree = new TempBomTree();
		}

		bomTree.setProdProcess(bom.getProdProcess());
		bomTree.setProduct(bom.getProduct());
		bomTree.setQty(bom.getQty());
		bomTree.setUnit(bom.getUnit());
		bomTree.setParentBom(parentBom);
		bomTree.setParent(parent);
		bomTree.setBom(bom);

		bomTree.setRealQty(
				getRealQty(bom.getProduct().getId(), stockLocationId, companyId, StockMoveRepository.STATUS_REALIZED));

		bomTree.setFutureQty(
				getRealQty(bom.getProduct().getId(), stockLocationId, companyId, StockMoveRepository.STATUS_PLANNED));

		bomTree.setReservedQty(getReservedQty(bom.getProduct().getId(), stockLocationId, companyId,
				StockMoveRepository.STATUS_PLANNED));

		bomTree.setRequestedReservedQty(getRequestedReservedQty(bom.getProduct().getId(), stockLocationId, companyId,
				StockMoveRepository.STATUS_PLANNED));

		bomTree.setPurchaseOrderQty(getPurchaseOrderQty(bom.getProduct().getId(), stockLocationId, companyId));

		bomTree.setSaleOrderQty(getSaleOrderQty(bom.getProduct().getId(), stockLocationId, companyId));

		bomTree.setAvailableQty(getAvailableStock(bom.getProduct().getId(), stockLocationId, companyId));

		bomTree.setBuildingQty(getBuildingQty(bom.getProduct().getId(), stockLocationId, companyId,
				StockMoveRepository.STATUS_PLANNED));

		bomTree.setConsumeManufOrderQty(getConsumeAndMissingManufOrderQty(bom.getProduct().getId(), stockLocationId,
				companyId, StockMoveRepository.STATUS_PLANNED));

		bomTree.setMissingManufOrderQty(getConsumeAndMissingManufOrderQty(bom.getProduct().getId(), stockLocationId,
				companyId, StockMoveRepository.STATUS_PLANNED));

		bomTree = tempBomTreeRepo.save(bomTree);

		processedBom.add(bom.getId());

		List<Long> validBomIds = processChildBom(bom, bomTree, useProductDefaultBom, stockLocationId, companyId);

		validBomIds.add(0L);

		removeInvalidTree(validBomIds, bom);

		return bomTree;
	}

	private BigDecimal getConsumeAndMissingManufOrderQty(Long productId, Long stockLocationId, Long companyId,
			Integer statusSelect) {
		BigDecimal qty = BigDecimal.ZERO;
		List<Integer> statusList = new ArrayList<>();

		statusList.add(ManufOrderRepository.STATUS_IN_PROGRESS);
		statusList.add(ManufOrderRepository.STATUS_STANDBY);

		List<StockMoveLine> stockMoveLineList = stockMoveLineRepository.all().filter("self.product.id = ?1 "
				+ "AND self.stockMove.statusSelect = ?2 " + "AND self.stockMove.fromStockLocation.typeSelect != ?3 "
				+ "AND ( ( self.consumedManufOrder IS NOT NULL AND self.consumedManufOrder.statusSelect IN ( ?4, ?5 ) ) "
				+ "OR ( self.consumedOperationOrder IS NOT NULL AND self.consumedOperationOrder.statusSelect IN ( ?4, ?5 ) ) ) "
				+ "AND ( self.stockMove.company.id IN ?6 OR 0 IN ?6 ) "
				+ "AND ( self.stockMove.fromStockLocation.id IN ?7 OR 0 IN ?7 )", productId, statusSelect,
				StockLocationRepository.TYPE_VIRTUAL, ManufOrderRepository.STATUS_IN_PROGRESS,
				ManufOrderRepository.STATUS_STANDBY, companyId, stockLocationId).fetch();

		qty = stockMoveLineList.stream().map((line) -> line.getRealQty()).reduce(BigDecimal.ZERO, BigDecimal::add);
		return qty;
	}

	private BigDecimal getBuildingQty(Long productId, Long stockLocationId, Long companyId, Integer statusSelect) {
		BigDecimal qty = BigDecimal.ZERO;

		List<StockMoveLine> stockMoveLineList = stockMoveLineRepository.all().filter("self.stockMove.statusSelect = ?1 "
				+ "AND self.product.id = ?2 " + "AND self.stockMove.toStockLocation.typeSelect != ?3 "
				+ "AND self.producedManufOrder IS NOT NULL " + "AND self.producedManufOrder.statusSelect IN ( ?4, ?5 ) "
				+ "AND ( self.stockMove.company.id IN ?6 OR 0 IN ?6 ) "
				+ "AND ( self.stockMove.toStockLocation.id IN ?7 OR 0 IN ?7 )", statusSelect, productId,
				StockLocationRepository.TYPE_VIRTUAL, ManufOrderRepository.STATUS_IN_PROGRESS,
				ManufOrderRepository.STATUS_STANDBY, companyId, stockLocationId).fetch();

		qty = stockMoveLineList.stream().map((line) -> line.getRealQty()).reduce(BigDecimal.ZERO, BigDecimal::add);
		return qty;
	}

	private BigDecimal getRealQty(Long productId, Long stockLocationId, Long companyId, Integer statusSelect) {
		BigDecimal qty = BigDecimal.ZERO;

		List<StockMoveLine> stockMoveLineList = stockMoveLineRepository.all()
				.filter("self.stockMove.statusSelect = ?1 " + "AND self.product.id = ?2 "
						+ "AND (self.stockMove.toStockLocation.id IN ?3 OR 0 IN ?3) "
						+ "AND (self.stockMove.company.id IN ?4 OR ?4 IS NULL)", statusSelect, productId,
						stockLocationId, companyId)
				.fetch();

		qty = stockMoveLineList.stream().map((line) -> line.getRealQty()).reduce(BigDecimal.ZERO, BigDecimal::add);
		return qty;
	}

	private BigDecimal getReservedQty(Long productId, Long stockLocationId, Long companyId, Integer statusSelect) {
		BigDecimal qty = BigDecimal.ZERO;

		List<StockMoveLine> stockMoveLineList = stockMoveLineRepository.all()
				.filter("self.reservedQty > 0 " + "AND self.stockMove.statusSelect = ?1 " + "AND self.product.id = ?2 "
						+ "AND (self.stockMove.toStockLocation.id IN ?3 OR 0 IN ?3) "
						+ "AND (self.stockMove.company.id IN ?4 OR ?4 IS NULL)", statusSelect, productId,
						stockLocationId, companyId)
				.fetch();

		qty = stockMoveLineList.stream().map((line) -> line.getReservedQty()).reduce(BigDecimal.ZERO, BigDecimal::add);
		return qty;
	}

	private BigDecimal getRequestedReservedQty(Long productId, Long stockLocationId, Long companyId,
			Integer statusSelect) {
		BigDecimal qty = BigDecimal.ZERO;

		List<StockMoveLine> stockMoveLineList = stockMoveLineRepository.all()
				.filter("self.requestedReservedQty > 0 " + "AND self.stockMove.statusSelect = ?1 "
						+ "AND self.product.id = ?2 " + "AND (self.stockMove.toStockLocation.id IN ?3 OR 0 IN ?3) "
						+ "AND (self.stockMove.company.id IN ?4 OR ?4 IS NULL)", statusSelect, productId,
						stockLocationId, companyId)
				.fetch();

		qty = stockMoveLineList.stream().map((line) -> line.getRequestedReservedQty()).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		return qty;
	}

	private BigDecimal getPurchaseOrderQty(Long productId, Long stockLocationId, Long companyId) {
		BigDecimal qty = BigDecimal.ZERO;

		List<PurchaseOrderLine> purchaseOrderLine = purchaseOrderLineRepository.all().filter(
				"self.product.id = ?1 AND self.receiptState != ?2 " + "AND self.purchaseOrder.statusSelect = ?3 "
						+ "AND (self.purchaseOrder.company.id IN ?4 OR ?4 IS NULL) "
						+ "AND (self.purchaseOrder.stockLocation.id IN ?5 OR 0 IN ?5)",
				productId, PurchaseOrderLineRepository.RECEIPT_STATE_RECEIVED, PurchaseOrderRepository.STATUS_VALIDATED,
				companyId, stockLocationId).fetch();
		qty = purchaseOrderLine.stream().map((line) -> line.getQty()).reduce(BigDecimal.ZERO, BigDecimal::add);

		return qty;
	}

	private BigDecimal getSaleOrderQty(Long productId, Long stockLocationId, Long companyId) {
		BigDecimal qty = BigDecimal.ZERO;

		List<SaleOrderLine> saleOrderLine = saleOrderLineRepository.all()
				.filter("self.product.id = ?1 AND self.deliveryState != ?2 " + "AND self.saleOrder.statusSelect = ?3 "
						+ "AND (self.saleOrder.company.id IN ?4 OR ?4 IS NULL) "
						+ "AND (self.saleOrder.stockLocation.id IN ?5 OR 0 IN ?5)", productId,
						SaleOrderLineRepository.DELIVERY_STATE_DELIVERED, SaleOrderRepository.STATUS_ORDER_CONFIRMED,
						companyId, stockLocationId)
				.fetch();
		qty = saleOrderLine.stream().map((line) -> line.getQty()).reduce(BigDecimal.ZERO, BigDecimal::add);

		return qty;
	}

	private BigDecimal getAvailableStock(Long productId, Long stockLocationId, Long companyId) {
		BigDecimal futureQty = BigDecimal.ZERO;
		BigDecimal currentQty = BigDecimal.ZERO;
		BigDecimal qty = BigDecimal.ZERO;

		List<StockLocationLine> stockLocationLine = stockLocationLineRepository.all().filter("self.product.id = ?1 "
				+ "AND self.stockLocation.typeSelect != ?2 "
				+ "AND ( self.stockLocation.company.id IN ?3 OR ?3 IS NULL) "
				+ "AND ( self.stockLocation.id IN ?4 OR ?4 IS NULL ) "
				+ "AND ( self.currentQty != 0 OR self.futureQty != 0 ) "
				+ "AND ( self.stockLocation.isNotInCalculStock = false OR self.stockLocation.isNotInCalculStock IS NULL )",
				productId, StockLocationRepository.TYPE_VIRTUAL, companyId, stockLocationId).fetch();

		futureQty = stockLocationLine.stream().map((line) -> line.getFutureQty()).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		currentQty = stockLocationLine.stream().map((line) -> line.getCurrentQty()).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		qty = futureQty.add(currentQty);

		return qty;
	}

	private List<Long> processChildBom(BillOfMaterial bom, TempBomTree bomTree, boolean useProductDefaultBom,
			Long stockLocationId, Long companyId) {

		List<Long> validBomIds = new ArrayList<Long>();

		for (BillOfMaterial childBom : bom.getBillOfMaterialSet()) {

			if (useProductDefaultBom && CollectionUtils.isEmpty(childBom.getBillOfMaterialSet())
					&& childBom.getProduct() != null && childBom.getProduct().getDefaultBillOfMaterial() != null) {
				childBom = childBom.getProduct().getDefaultBillOfMaterial();
			}

			if (!processedBom.contains(childBom.getId())) {
				getBomTree(childBom, bom, bomTree, useProductDefaultBom, stockLocationId, companyId);
			} else {
				log.debug("Already processed: {}", childBom.getId());
			}
			validBomIds.add(childBom.getId());
		}

		return validBomIds;
	}

	@Override
	public TempBomTree generateTree(BillOfMaterial billOfMaterial, boolean useProductDefaultBom, Long stockLocationId,
			Long companyId) {
		processedBom = new ArrayList<>();

		return getBomTree(billOfMaterial, null, null, useProductDefaultBom, stockLocationId, companyId);
	}

	@Transactional
	public void removeInvalidTree(List<Long> validBomIds, BillOfMaterial bom) {

		List<TempBomTree> invalidBomTrees = tempBomTreeRepo.all()
				.filter("self.bom.id not in (?1) and self.parentBom = ?2", validBomIds, bom).fetch();

		log.debug("Invalid bom trees: {}", invalidBomTrees);

		if (!invalidBomTrees.isEmpty()) {
			List<TempBomTree> childBomTrees = tempBomTreeRepo.all().filter("self.parent in (?1)", invalidBomTrees)
					.fetch();

			for (TempBomTree childBomTree : childBomTrees) {
				childBomTree.setParent(null);
				tempBomTreeRepo.save(childBomTree);
			}
		}

		for (TempBomTree invalidBomTree : invalidBomTrees) {
			tempBomTreeRepo.remove(invalidBomTree);
		}
	}
}
