package jp.commasa.fx2.dto;

import java.math.BigDecimal;

public class PriceEx extends Price {

	private BigDecimal diff;
	private BigDecimal diffsp;
	private BigDecimal sumshort = BigDecimal.ZERO;
	private BigDecimal sumlong = BigDecimal.ZERO;
	private BigDecimal netopen;
	private BigDecimal netclose;
	
	public PriceEx(Price p) {
		super(p);
	}	
	public BigDecimal getDiff() {
		return diff;
	}
	public void setDiff(BigDecimal diff) {
		this.diff = diff;
	}
	public BigDecimal getDiffsp() {
		return diffsp;
	}
	public void setDiffsp(BigDecimal diffsp) {
		this.diffsp = diffsp;
	}
	public BigDecimal getSumshort() {
		return sumshort;
	}
	public void setSumshort(BigDecimal sumshort) {
		this.sumshort = sumshort;
	}
	public BigDecimal getSumlong() {
		return sumlong;
	}
	public void setSumlong(BigDecimal sumlong) {
		this.sumlong = sumlong;
	}
	public BigDecimal getNetopen() {
		return netopen;
	}
	public void setNetopen(BigDecimal netopen) {
		this.netopen = netopen;
	}
	public BigDecimal getNetclose() {
		return netclose;
	}
	public void setNetclose(BigDecimal netclose) {
		this.netclose = netclose;
	}
	
}
