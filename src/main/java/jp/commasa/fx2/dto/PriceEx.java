package jp.commasa.fx2.dto;

public class PriceEx extends Price {

	private double diff;
	private double diffsp;
	
	public PriceEx(Price p) {
		super(p);
	}
	
	public double getDiff() {
		return diff;
	}
	public void setDiff(double diff) {
		this.diff = diff;
	}
	public double getDiffsp() {
		return diffsp;
	}
	public void setDiffsp(double diffsp) {
		this.diffsp = diffsp;
	}
	
}
