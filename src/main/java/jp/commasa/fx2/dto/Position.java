package jp.commasa.fx2.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Position {

	private Logger log = LogManager.getLogger(this.getClass());
	private BigDecimal cost;
	private BigDecimal amount = BigDecimal.ZERO;
	private List<Trans> reports = new ArrayList<Trans>();
	private int orderCount = 0;
	private BigDecimal changeamount = BigDecimal.ZERO;
	private BigDecimal totalamount = BigDecimal.ZERO;
	
	public BigDecimal getCost() {
		return cost;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void addReport(Trans report) {
		int c = this.amount.compareTo(BigDecimal.ZERO);
		if (reports.size() < 1 || c == 0) {
			this.reports.clear();
			this.reports.add(report);
			this.cost = report.getAvgPx();
			if (report.getSide().equals("1")) this.amount = report.getCumQty();
			if (report.getSide().equals("2")) this.amount = report.getCumQty().multiply(BigDecimal.valueOf(-1));
		} else {
			log.trace(" >>> this.amount="+amount+" side="+report.getSide()+" cumqty="+report.getCumQty());
			if (c > 0) {
				// 買いポジに買いを追加
				if (report.getSide().equals("1")) {
					this.reports.add(report);
					this.amount = this.amount.add(report.getCumQty());
				}
				// 買いポジに売りを追加
				if (report.getSide().equals("2")) {
					BigDecimal amt = report.getCumQty();
					while (reports.size()>0 && amt.compareTo(BigDecimal.ZERO)>0) {
						BigDecimal tmp = reports.get(0).getCumQty();
						amt = amt.subtract(tmp);
						if (amt.compareTo(BigDecimal.ZERO) >= 0) {
							reports.remove(0);
						} else {
							reports.get(0).setCumQty(tmp.subtract(amt));
						}
					}
					if (amt.compareTo(BigDecimal.ZERO)>0) {
						report.setCumQty(amt);
						reports.add(report);
					}
					this.amount = this.amount.subtract(report.getCumQty());
				}
			} else if (c < 0) {
				// 売りポジに買いを追加
				if (report.getSide().equals("1")) {
					BigDecimal amt = report.getCumQty();
					while (reports.size()>0 && amt.compareTo(BigDecimal.ZERO)>0) {
						BigDecimal tmp = reports.get(0).getCumQty();
						amt = amt.subtract(tmp);
						if (amt.compareTo(BigDecimal.ZERO) >= 0) {
							reports.remove(0);
						} else {
							reports.get(0).setCumQty(tmp.subtract(amt));
						}
					}
					if (amt.compareTo(BigDecimal.ZERO)>0) {
						report.setCumQty(amt);
						reports.add(report);
					}
					this.amount = this.amount.add(report.getCumQty());
				}
				// 売りポジに売りを追加
				if (report.getSide().equals("2")) {
					this.reports.add(report);
					this.amount = this.amount.subtract(report.getCumQty());
				}
			}
			// コストを再算出
			BigDecimal total = BigDecimal.ZERO;
			BigDecimal amt = BigDecimal.ZERO;
			for (Trans r : reports) {
				total = total.add(r.getAvgPx().multiply(r.getCumQty()));
				amt = amt.add(r.getCumQty());
			}
			if (amt.compareTo(BigDecimal.ZERO)==0) {
				this.cost = BigDecimal.ZERO;
			} else {
				this.cost = total.divide(amt, 6, BigDecimal.ROUND_HALF_UP);				
			}
			log.trace(" <<< this.amount="+amount+" this.cost="+cost);
		}
		if (report.getSide().equals("1")) this.changeamount = this.changeamount.add(report.getCumQty().multiply(report.getAvgPx()));
		if (report.getSide().equals("2")) this.changeamount = this.changeamount.subtract(report.getCumQty().multiply(report.getAvgPx()));
		this.totalamount = this.totalamount.add(report.getCumQty());
	} 

	public String getPL(BigDecimal p) {
		if (p==null) return "price is invalid.";
		BigDecimal pl = this.amount.multiply(p).subtract(this.changeamount);
		BigDecimal unit = pl.divide(this.totalamount, 4, BigDecimal.ROUND_HALF_UP);
		return "PL = " + pl.toPlainString() + " UNIT = " + unit.toPlainString();
	}

	public void reset() {
		this.cost = BigDecimal.ZERO;
		this.amount = BigDecimal.ZERO;
		this.changeamount = BigDecimal.ZERO;
		this.totalamount = BigDecimal.ZERO;
		this.reports.clear();
	}

	public synchronized void orderCount(int updown) {
		this.orderCount = this.orderCount + updown;
	}
	public int getOrderCount() {
		return this.orderCount;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (Trans r : reports) {
			sb.append("{"+r.toString()+"},");
		}
		return "Position [log=" + log + ", cost=" + cost + ", amount=" + amount + ", reports=" + sb.toString() + "]";
	}
	
}
