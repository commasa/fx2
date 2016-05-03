package jp.commasa.fx2.dto;

public class PriceEx extends Price {

	private double movAvg1;
	private double movAvg2;
	private double alpha1;
	private double alpha2;
	private double volatility1;
	private double volatility2;
	private String status;
	private int statusCount;

	public PriceEx(Price p) {
		super(p);
	}
	public double getMovAvg1() {
		return movAvg1;
	}
	public void setMovAvg1(double movAvg1) {
		this.movAvg1 = movAvg1;
	}
	public double getMovAvg2() {
		return movAvg2;
	}
	public void setMovAvg2(double movAvg2) {
		this.movAvg2 = movAvg2;
	}
	public double getAlpha1() {
		return alpha1;
	}
	public void setAlpha1(double alpha1) {
		this.alpha1 = alpha1;
	}
	public double getAlpha2() {
		return alpha2;
	}
	public void setAlpha2(double alpha2) {
		this.alpha2 = alpha2;
	}
	public double getVolatility1() {
		return volatility1;
	}
	public void setVolatility1(double volatility1) {
		this.volatility1 = volatility1;
	}
	public double getVolatility2() {
		return volatility2;
	}
	public void setVolatility2(double volatility2) {
		this.volatility2 = volatility2;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getStatusCount() {
		return statusCount;
	}
	public void setStatusCount(int statusCount) {
		this.statusCount = statusCount;
	}

}
