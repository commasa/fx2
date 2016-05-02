package jp.commasa.fx2.dto;

import java.math.BigDecimal;

public class Trans {

	protected String orderID;
	protected String symbol;
	protected String side;
	protected BigDecimal cumQty;
	protected BigDecimal avgPx;
	protected String transactTime;

	public String getOrderID() {
		return orderID;
	}
	public void setOrderID(String orderID) {
		this.orderID = orderID;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public String getSide() {
		return side;
	}
	public void setSide(String side) {
		this.side = side;
	}
	public BigDecimal getCumQty() {
		return cumQty;
	}
	public void setCumQty(BigDecimal cumQty) {
		this.cumQty = cumQty;
	}
	public BigDecimal getAvgPx() {
		return avgPx;
	}
	public void setAvgPx(BigDecimal avgPx) {
		this.avgPx = avgPx;
	}
	public String getTransactTime() {
		return transactTime;
	}
	public void setTransactTime(String transactTime) {
		this.transactTime = transactTime;
	}

	@Override
	public String toString() {
		return "Trans [orderID=" + orderID + ", symbol=" + symbol + ", side=" + side + ", cumQty=" + cumQty + ", avgPx="
				+ avgPx + ", transactTime=" + transactTime + "]";
	}

}
