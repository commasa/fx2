package jp.commasa.fx2.dto;

import java.math.BigDecimal;

public class Price {
	private String symbol;
	private double bid;
	private double ask;
	private String date;
	private BigDecimal tickNo;

	public Price() {};

	public Price(Price p) {
		this.symbol = ( p.symbol == null ? "" : new String(p.symbol) );
		this.bid = p.bid;
		this.ask = p.ask;
		this.date = new String( p.date == null ? "" : p.date );
		this.tickNo = ( p.tickNo == null ? null : new BigDecimal(p.tickNo.toPlainString()) );
	}

	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public double getBid() {
		return bid;
	}
	public void setBid(double bid) {
		this.bid = bid;
	}
	public double getAsk() {
		return ask;
	}
	public void setAsk(double ask) {
		this.ask = ask;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public BigDecimal getTickNo() {
		return tickNo;
	}
	public void setTickNo(BigDecimal tickNo) {
		this.tickNo = tickNo;
	}
	public double getMid() {
		return (ask + bid) / 2;
	}

	@Override
	public String toString() {
		return "Price [symbol=" + symbol + ", bid=" + bid + ", ask=" + ask 
				+ ", date=" + date + ", tickNo=" + tickNo + "]";
	}

}
