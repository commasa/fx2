package jp.commasa.fx2.dto;

import java.math.BigDecimal;

public class Order {

	private String symbol;
	private BigDecimal amount;
	private BigDecimal tickNo;
	
	public Order(String symbol, BigDecimal amount, BigDecimal tickNo) {
		this.symbol = symbol;
		this.amount = (amount==null ? BigDecimal.ZERO : amount);
		this.tickNo = (tickNo==null ? BigDecimal.ZERO : tickNo);
	}
	
	public String getSymbol() {
		return symbol;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public BigDecimal getTickNo() {
		return tickNo;
	}
	public char getSide() {
		int c = this.amount.compareTo(BigDecimal.ZERO);
		if (c>0) return '1';
		if (c<0) return '2';
		return '0';
	}
	public double getQty() {
		return this.amount.abs().doubleValue();
	}
	
}
