package jp.commasa.fx2.dto;

import java.math.BigDecimal;

public class Report extends Trans {

	private String origClOrdID;
	private String clOrdID;
	private String execID;
	private String execType;
	private String ordStatus;
	private int ordRejReason;
	private String account;
	private BigDecimal orderQty;
	private String ordType;
	private BigDecimal price;
	private BigDecimal stopPx;
	private String timeInForce;
	private String expireTime;
	private BigDecimal lastQty;
	private BigDecimal lastPx;
	private BigDecimal leavesQty;
	private String cxlRejResponseTo;
	private BigDecimal cxlRejReason;
	private String text;

	public String getOrigClOrdID() {
		return origClOrdID;
	}
	public void setOrigClOrdID(String origClOrdID) {
		this.origClOrdID = origClOrdID;
	}
	public String getClOrdID() {
		return clOrdID;
	}
	public void setClOrdID(String clOrdID) {
		this.clOrdID = clOrdID;
	}
	public String getExecID() {
		return execID;
	}
	public void setExecID(String execID) {
		this.execID = execID;
	}
	public String getExecType() {
		return execType;
	}
	public void setExecType(String execType) {
		this.execType = execType;
	}
	public String getOrdStatus() {
		return ordStatus;
	}
	public void setOrdStatus(String ordStatus) {
		this.ordStatus = ordStatus;
	}
	public int getOrdRejReason() {
		return ordRejReason;
	}
	public void setOrdRejReason(int ordRejReason) {
		this.ordRejReason = ordRejReason;
	}
	public String getAccount() {
		return account;
	}
	public void setAccount(String account) {
		this.account = account;
	}
	public BigDecimal getOrderQty() {
		return orderQty;
	}
	public void setOrderQty(BigDecimal orderQty) {
		this.orderQty = orderQty;
	}
	public String getOrdType() {
		return ordType;
	}
	public void setOrdType(String ordType) {
		this.ordType = ordType;
	}
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public BigDecimal getStopPx() {
		return stopPx;
	}
	public void setStopPx(BigDecimal stopPx) {
		this.stopPx = stopPx;
	}
	public String getTimeInForce() {
		return timeInForce;
	}
	public void setTimeInForce(String timeInForce) {
		this.timeInForce = timeInForce;
	}
	public String getExpireTime() {
		return expireTime;
	}
	public void setExpireTime(String expireTime) {
		this.expireTime = expireTime;
	}
	public BigDecimal getLastQty() {
		return lastQty;
	}
	public void setLastQty(BigDecimal lastQty) {
		this.lastQty = lastQty;
	}
	public BigDecimal getLastPx() {
		return lastPx;
	}
	public void setLastPx(BigDecimal lastPx) {
		this.lastPx = lastPx;
	}
	public BigDecimal getLeavesQty() {
		return leavesQty;
	}
	public void setLeavesQty(BigDecimal leavesQty) {
		this.leavesQty = leavesQty;
	}
	public String getCxlRejResponseTo() {
		return cxlRejResponseTo;
	}
	public void setCxlRejResponseTo(String cxlRejResponseTo) {
		this.cxlRejResponseTo = cxlRejResponseTo;
	}
	public BigDecimal getCxlRejReason() {
		return cxlRejReason;
	}
	public void setCxlRejReason(BigDecimal cxlRejReason) {
		this.cxlRejReason = cxlRejReason;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

	public void setSide(char side) {
		this.side = String.valueOf(side);
	}
	public void setExecType(char execType) {
		this.execType = String.valueOf(execType);
	}
	public void setOrdStatus(char ordStatus) {
		this.ordStatus = String.valueOf(ordStatus);
	}
	public void setOrdType(char ordType) {
		this.ordType = String.valueOf(ordType);
	}
	public void setTimeInForce(char timeInForce) {
		this.timeInForce = String.valueOf(timeInForce);
	}

	@Override
	public String toString() {
		return "Reports [orderID=" + orderID + ", origClOrdID=" + origClOrdID
				+ ", clOrdID=" + clOrdID + ", execID=" + execID + ", execType="
				+ execType + ", ordStatus=" + ordStatus + ", ordRejReason="
				+ ordRejReason + ", account=" + account + ", symbol=" + symbol
				+ ", side=" + side + ", orderQty=" + orderQty + ", ordType="
				+ ordType + ", price=" + price + ", stopPx=" + stopPx
				+ ", timeInForce=" + timeInForce + ", expireTime=" + expireTime
				+ ", lastQty=" + lastQty + ", lastPx=" + lastPx
				+ ", leavesQty=" + leavesQty + ", cumQty=" + cumQty
				+ ", avgPx=" + avgPx + ", transactTime=" + transactTime
				+ ", cxlRejResponseTo=" + cxlRejResponseTo + ", cxlRejReason="
				+ cxlRejReason + ", text=" + text + "]";
	}

	public static Trans getTrans(Report report) {
		Trans result = new Trans();
		result.orderID = report.orderID;
		result.symbol = report.symbol;
		result.side = report.side;
		result.cumQty = report.cumQty;
		result.avgPx = report.avgPx;
		result.transactTime = report.transactTime;
		return result;
	}
}
