package jp.commasa.fx2.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Position {

	private BigDecimal cost;
	private BigDecimal amount = BigDecimal.ZERO;
	private List<Report> reports = new ArrayList<Report>();
	
	public BigDecimal getCost() {
		return cost;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void addReport(Report report) {
		int c = this.amount.compareTo(BigDecimal.ZERO);
		if (reports.size() < 1 || c == 0) {
			this.reports.clear();
			this.reports.add(report);
			this.cost = BigDecimal.valueOf(report.getAvgPx());
			if (report.getSide().equals("1")) this.amount = BigDecimal.valueOf(report.getCumQty());
			if (report.getSide().equals("2")) this.amount = BigDecimal.valueOf(report.getCumQty()).multiply(BigDecimal.valueOf(-1));
		} else {
			if (c > 0) {
				// 買いポジに買いを追加
				if (report.getSide().equals("1")) {
					this.reports.add(report);
					this.amount = this.amount.add(BigDecimal.valueOf(report.getCumQty()));
				}
				// 買いポジに売りを追加
				if (report.getSide().equals("2")) {
					BigDecimal amt = BigDecimal.valueOf(report.getCumQty());
					while (reports.size()>0 || amt.compareTo(BigDecimal.ZERO)>0) {
						BigDecimal tmp = BigDecimal.valueOf(reports.get(0).getCumQty());
						amt = amt.subtract(tmp);
						if (amt.compareTo(BigDecimal.ZERO) >= 0) {
							reports.remove(0);
						} else {
							reports.get(0).setCumQty(tmp.subtract(amt).doubleValue());
						}
					}
					if (amt.compareTo(BigDecimal.ZERO)>0) {
						report.setCumQty(amt.doubleValue());
						reports.add(report);
					}
					this.amount = this.amount.subtract(BigDecimal.valueOf(report.getCumQty()));
				}
			} else if (c < 0) {
				// 売りポジに買いを追加
				if (report.getSide().equals("1")) {
					BigDecimal amt = BigDecimal.valueOf(report.getCumQty());
					while (reports.size()>0 || amt.compareTo(BigDecimal.ZERO)>0) {
						BigDecimal tmp = BigDecimal.valueOf(reports.get(0).getCumQty());
						amt = amt.subtract(tmp);
						if (amt.compareTo(BigDecimal.ZERO) >= 0) {
							reports.remove(0);
						} else {
							reports.get(0).setCumQty(tmp.subtract(amt).doubleValue());
						}
					}
					if (amt.compareTo(BigDecimal.ZERO)>0) {
						report.setCumQty(amt.doubleValue());
						reports.add(report);
					}
					this.amount = this.amount.add(BigDecimal.valueOf(report.getCumQty()));
				}
				// 売りポジに売りを追加
				if (report.getSide().equals("2")) {
					this.reports.add(report);
					this.amount = this.amount.subtract(BigDecimal.valueOf(report.getCumQty()));
				}
			}
			// コストを再算出
			BigDecimal total = BigDecimal.ZERO;
			BigDecimal amt = BigDecimal.ZERO;
			for (Report r : reports) {
				total = total.add(BigDecimal.valueOf(r.getAvgPx()).multiply(BigDecimal.valueOf(r.getCumQty())));
				amt = amt.add(BigDecimal.valueOf(r.getCumQty()));
			}
			if (amt.compareTo(BigDecimal.ZERO)==0) {
				this.cost = BigDecimal.ZERO;
			} else {
				this.cost = total.divide(amt, 6, BigDecimal.ROUND_HALF_UP);				
			}
		}
	} 

	public void reset() {
		this.cost = BigDecimal.ZERO;
		this.amount = BigDecimal.ZERO;
		this.reports.clear();
	}
}
