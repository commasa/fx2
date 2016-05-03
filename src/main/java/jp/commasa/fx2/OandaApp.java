package jp.commasa.fx2;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jp.commasa.fx2.dto.Order;
import jp.commasa.fx2.dto.Price;
import jp.commasa.fx2.dto.Report;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.StringField;
import quickfix.UnsupportedMessageType;
import quickfix.field.Account;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.ExpireTime;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.LinesOfText;
import quickfix.field.MDEntryDate;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryTime;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.NoMDEntries;
import quickfix.field.OrdRejReason;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Password;
import quickfix.field.ResetSeqNumFlag;
import quickfix.field.Side;
import quickfix.field.StopPx;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TargetSubID;
import quickfix.field.Text;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.NewOrderSingle;

public class OandaApp extends MessageCracker implements Application {

	private Logger log = LogManager.getLogger(this.getClass());
	private SessionSettings settings;
	private final String RATES = "RATES";
	private SessionID rateSessionID = null;
	private String rateReqID;
	private boolean isRateLogOn = false;
	private final String ORDER = "ORDER";
	private SessionID orderSessionID = null;
	private String account;
	private boolean isOrderLogOn = false;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSS");
	private SimpleDateFormat sdfOrderId = new SimpleDateFormat("yyyyMMddHHmmss");
	private DecimalFormat dfOrderId = new DecimalFormat("0000000000");
	private List<Symbol> symbols;
	private Map<String, Price> current = new HashMap<String, Price>();
	private Map<String, BigDecimal> tickNo = new HashMap<String, BigDecimal>();
	private Algorithm algorithm;

	public OandaApp(SessionSettings settings, ResourceBundle bundle)
	{
		this.settings = settings;
		List<Symbol> symbolList = new ArrayList<Symbol>();
		String strSymbols = bundle.getString("symbols");
		if (strSymbols != null && !"".equals(strSymbols)) {
			String[] ss = strSymbols.split(",");
			for (String s : ss) {
				symbolList.add(new Symbol(s.trim()));
			}
		}
		this.symbols = symbolList;
		this.algorithm = new Algorithm(bundle);
	}

	@Override
	public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		log.trace("fromAdmin : SessionID=" + sessionId.toString() + " , Message=" + message.getClass().getName());
	}

	@Override
	public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		try {
			crack(message, sessionId);
		} catch (Exception e) {
			log.error("ERROR fromApp : SessionID=" + sessionId.toString() + " , Message=" + message.toString(), e);
		}
	}

	@Override
	public void onCreate(SessionID sessionID) {
		if (RATES.equalsIgnoreCase(sessionID.getTargetSubID())) rateSessionID = sessionID;
		if (ORDER.equalsIgnoreCase(sessionID.getTargetSubID())) orderSessionID = sessionID;
		log.info("onCreate : SessionID=" + sessionID.toString());
	}

	@Override
	public void onLogon(SessionID sessionID) {
		if (RATES.equalsIgnoreCase(sessionID.getTargetSubID())) {
			synchronized (this) { isRateLogOn = true; }
			try {
				requestMarketData();
			} catch (SessionNotFound e) {
				log.error("requestMarketData : SessionID=" + sessionID.toString(), e);
			}
		}
		if (ORDER.equalsIgnoreCase(sessionID.getTargetSubID())) synchronized (this) { isOrderLogOn = true; }
		log.info("onLogon : SessionID=" + sessionID.toString());
	}

	@Override
	public void onLogout(SessionID sessionID) {
		if (RATES.equalsIgnoreCase(sessionID.getTargetSubID())) synchronized (this) { isRateLogOn = false; }
		if (ORDER.equalsIgnoreCase(sessionID.getTargetSubID())) synchronized (this) { isOrderLogOn = false; }
		log.info("onLogout : SessionID=" + sessionID.toString());
	}

	@Override
	public void toAdmin(Message message, SessionID sessionID) {
		MsgType msgType = new MsgType();
		try {
			//連続ログインで攻撃とみなされないように待機
			try {
				Random rnd = new Random();
				int wait = rnd.nextInt(20);
				Thread.sleep(wait*100);
			} catch (InterruptedException e) {}
			try {
				account = settings.getString(sessionID, "Account");
			} catch(ConfigError e) { /* RATEのときは不要なタグのため、何もしない */ }
			StringField type = message.getHeader().getField(msgType);
			message.getHeader().setField(new TargetSubID(settings.getString(sessionID, "TargetSubID")));
			if (type.valueEquals(MsgType.LOGON)) {
				if (!message.isSetField(Password.FIELD)) {
					message.setField(new Password(settings.getString(sessionID, "Password")));
				}
				message.setField(new ResetSeqNumFlag(true));
			}
			log.trace("toAdmin : SessionID=" + sessionID.toString() + " , Message=" + message.getClass().getName());
		} catch (Exception e) {
			log.error("ERROR toAdmin : ", e);
		}
	}

	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		log.trace("toApp : SessionID=" + sessionId.toString() + " , Message=" + message.getClass().getName());
	}

	public void requestMarketData() throws SessionNotFound {
		int cnt = 0;
		while (!isRateLogOn) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new SessionNotFound();
			}
			if (cnt>60) throw new SessionNotFound(); else cnt++;
		}
		MDReqID mdReqID = new MDReqID();
		SubscriptionRequestType subType = new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES);
		MarketDepth marketDepth = new MarketDepth(1);
		MarketDataRequest message = new MarketDataRequest(mdReqID, subType, marketDepth);

		rateReqID = Long.toString(System.currentTimeMillis());
		message.setField(new MDReqID(rateReqID));
		message.setField(new MDUpdateType(MDUpdateType.INCREMENTAL_REFRESH));

		MarketDataRequest.NoMDEntryTypes marketDataEntry = new MarketDataRequest.NoMDEntryTypes();
		marketDataEntry.set(new MDEntryType(MDEntryType.BID));
		message.addGroup(marketDataEntry);
		marketDataEntry.set(new MDEntryType(MDEntryType.OFFER));
		message.addGroup(marketDataEntry);

		MarketDataRequest.NoRelatedSym symbol = new MarketDataRequest.NoRelatedSym();
		for (Symbol s: symbols) {
			symbol.set(s);
			message.addGroup(symbol);
			current.put(s.getValue(), new Price());
		}

		if (message != null) {
			Session.sendToTarget(message, rateSessionID);
			log.info("QueryMarketDataRequest : " + message.toString().replace('\u0001', '|'));
		}
	}

	public void onMessage(quickfix.fix44.MarketDataSnapshotFullRefresh message, SessionID sessionID) throws FieldNotFound ,UnsupportedMessageType ,IncorrectTagValue {
		MDReqID mdReqID = new MDReqID();
		message.get(mdReqID);
		if (mdReqID.valueEquals(rateReqID)) {
			MsgSeqNum seqNum = new MsgSeqNum();
			message.getHeader().getField(seqNum);
			Symbol symbol = new Symbol();
			message.get(symbol);
			NoMDEntries noMDEntries = new NoMDEntries();
			message.get(noMDEntries);
			quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries group = new quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries();
			MDEntryType mdEntryType = new MDEntryType();
			MDEntryPx mdEntryPx = new MDEntryPx();
			MDEntrySize mdEntrySize = new MDEntrySize();
			MDEntryDate mdEntryDate = new MDEntryDate();
			MDEntryTime mdEntryTime = new MDEntryTime();
			Text text = new Text();
			for (int i=1; i<=noMDEntries.getValue(); i++) {
				message.getGroup(i, group);
				group.get(mdEntryType);
				group.get(mdEntryPx);
				group.get(mdEntrySize);
				group.get(mdEntryDate);
				group.get(mdEntryTime);
				String strText = "";
				try { group.get(text); strText = text.getValue(); } catch (FieldNotFound e) {}
				if ( "".equals(strText) ) {
					Price p = current.get(symbol.getValue());
					if (p != null) {
						p.setSymbol(symbol.getValue());
						if (mdEntryType.getValue() == '0') p.setBid(mdEntryPx.getValue());
						if (mdEntryType.getValue() == '1') p.setAsk(mdEntryPx.getValue());
						p.setDate(sdf.format(new Date(mdEntryDate.getValue().getTime() + mdEntryTime.getValue().getTime())));
						p.setTickNo(BigDecimal.valueOf(seqNum.getValue()));
					}
				}
				runAlgorithm();
			}
		}
	}
	
	private void runAlgorithm() {
		for (Map.Entry<String, Price> entry : current.entrySet()) {
			BigDecimal t = tickNo.get(entry.getKey());
			// tickNo が変更された時のみする
			if (t==null || entry.getValue().getTickNo()==null || !t.equals(entry.getValue().getTickNo())) {
				List<Order> orderList = algorithm.run(entry.getValue());
				if (orderList != null) {
					for (Order order : orderList) {
						try {
							newOrder(order);
						} catch (SessionNotFound e) {
							log.error("new Order", e);
						}
					}
				}
				tickNo.put(entry.getKey(), entry.getValue().getTickNo());
			}
		}
	}

	public void onMessage(quickfix.fix44.MarketDataIncrementalRefresh message, SessionID sessionID) throws FieldNotFound ,UnsupportedMessageType ,IncorrectTagValue {
		MDReqID mdReqID = new MDReqID();
		message.get(mdReqID);
		if (mdReqID.valueEquals(rateReqID)) {
			MsgSeqNum seqNum = new MsgSeqNum();
			message.getHeader().getField(seqNum);
			NoMDEntries noMDEntries = new NoMDEntries();
			message.get(noMDEntries);
			quickfix.fix44.MarketDataIncrementalRefresh.NoMDEntries group = new quickfix.fix44.MarketDataIncrementalRefresh.NoMDEntries();
			Symbol symbol = new Symbol();
			MDEntryType mdEntryType = new MDEntryType();
			MDEntryPx mdEntryPx = new MDEntryPx();
			MDEntrySize mdEntrySize = new MDEntrySize();
			MDEntryDate mdEntryDate = new MDEntryDate();
			MDEntryTime mdEntryTime = new MDEntryTime();
			Text text = new Text();
			for (int i=1; i<=noMDEntries.getValue(); i++) {
				message.getGroup(i, group);
				group.get(symbol);
				group.get(mdEntryType);
				group.get(mdEntryPx);
				group.get(mdEntrySize);
				group.get(mdEntryDate);
				group.get(mdEntryTime);
				String strText = "";
				try { group.get(text); strText = text.getValue(); } catch (FieldNotFound e) {}
				if ( "".equals(strText) ) {
					Price p = current.get(symbol.getValue());
					if (p != null) {
						p.setSymbol(symbol.getValue());
						if (mdEntryType.getValue() == '0') p.setBid(mdEntryPx.getValue());
						if (mdEntryType.getValue() == '1') p.setAsk(mdEntryPx.getValue());
						p.setDate(sdf.format(new Date(mdEntryDate.getValue().getTime() + mdEntryTime.getValue().getTime())));
						p.setTickNo(BigDecimal.valueOf(seqNum.getValue()));
					}
				}
			}
			runAlgorithm();
		}
	}

	public void onMessage(quickfix.fix44.News message, SessionID sessionID) throws FieldNotFound ,UnsupportedMessageType ,IncorrectTagValue {
		LinesOfText lines = new LinesOfText();
		message.get(lines);
		quickfix.fix44.News.LinesOfText group = new quickfix.fix44.News.LinesOfText();
		Text text = new Text();
		for (int i=1; i<=lines.getValue(); i++) {
			message.getGroup(i, group);
			group.get(text);
			log.info(text.getValue());
		}
	}

	@Override
	protected void onMessage(Message message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		log.info(message.getClass().getSimpleName() + " : " + message.toString().replace('\u0001', '|'));
	}

	public void newOrder(Order order) throws SessionNotFound {
		int cnt = 0;
		while (!isOrderLogOn) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new SessionNotFound();
			}
			if (cnt>60) throw new SessionNotFound(); else cnt++;
		}
		NewOrderSingle message = new NewOrderSingle();
		message.set(new ClOrdID( sdfOrderId.format(new Date()) + dfOrderId.format(order.getTickNo()) ));
		message.set(new Account(account));
		message.set(new Symbol(order.getSymbol()));
		message.set(new Side(order.getSide()));
		message.set(new OrderQty(order.getQty()));
		message.set(new OrdType(OrdType.MARKET));
		message.set(new TransactTime(new Date()));
		if (message != null) {
			Session.sendToTarget(message, orderSessionID);
			log.info("newOrder : " + message.toString().replace('\u0001', '|'));
		}
	}

	public void onMessage(quickfix.fix44.ExecutionReport message, SessionID sessionID) throws FieldNotFound ,UnsupportedMessageType ,IncorrectTagValue {
		log.info("ExecutionReport : " + message.toString().replace('\u0001', '|'));
		OrderID orderID = new OrderID();
		OrigClOrdID origClOrdID = new OrigClOrdID();
		ClOrdID clOrdID = new ClOrdID();
		ExecID execID = new ExecID();
		ExecType execType = new ExecType();
		OrdStatus ordStatus = new OrdStatus();
		OrdRejReason ordRejReason = new OrdRejReason();
		Account account = new Account();
		Symbol symbol = new Symbol();
		Side side = new Side();
		OrderQty orderQty = new OrderQty();
		OrdType ordType = new OrdType();
		quickfix.field.Price price = new quickfix.field.Price();
		StopPx stopPx = new StopPx();
		TimeInForce timeInForce = new TimeInForce();
		ExpireTime expireTime = new ExpireTime();
		LastQty lastQty = new LastQty();
		LastPx lastPx = new LastPx();
		LeavesQty leavesQty = new LeavesQty();
		CumQty cumQty = new CumQty();
		AvgPx avgPx = new AvgPx();
		TransactTime transactTime = new TransactTime();
		Text text = new Text();
		try { message.get(orderID); } catch (FieldNotFound e) {}
		try { message.get(origClOrdID); } catch (FieldNotFound e) {}
		try { message.get(clOrdID); } catch (FieldNotFound e) {}
		try { message.get(execID); } catch (FieldNotFound e) {}
		try { message.get(execType); } catch (FieldNotFound e) {}
		try { message.get(ordStatus); } catch (FieldNotFound e) {}
		try { message.get(ordRejReason); } catch (FieldNotFound e) {}
		try { message.get(account); } catch (FieldNotFound e) {}
		try { message.get(symbol); } catch (FieldNotFound e) {}
		try { message.get(side); } catch (FieldNotFound e) {}
		try { message.get(orderQty); } catch (FieldNotFound e) {}
		try { message.get(ordType); } catch (FieldNotFound e) {}
		try { message.get(price); } catch (FieldNotFound e) {}
		try { message.get(stopPx); } catch (FieldNotFound e) {}
		try { message.get(timeInForce); } catch (FieldNotFound e) {}
		try { message.get(expireTime); } catch (FieldNotFound e) {}
		try { message.get(lastQty); } catch (FieldNotFound e) {}
		try { message.get(lastPx); } catch (FieldNotFound e) {}
		try { message.get(leavesQty); } catch (FieldNotFound e) {}
		try { message.get(cumQty); } catch (FieldNotFound e) {}
		try { message.get(avgPx); } catch (FieldNotFound e) {}
		try { message.get(transactTime); } catch (FieldNotFound e) {}
		try { message.get(text); } catch (FieldNotFound e) {}
		Report report = new Report();
		report.setOrderID(orderID.getValue());
		report.setOrigClOrdID(origClOrdID.getValue());
		report.setClOrdID(clOrdID.getValue());
		report.setExecID(execID.getValue());
		report.setExecType(execType.getValue());
		report.setOrdStatus(ordStatus.getValue());
		report.setOrdRejReason(ordRejReason.getValue());
		report.setAccount(account.getValue());
		report.setSymbol(symbol.getValue());
		report.setSide(side.getValue());
		report.setOrderQty(BigDecimal.valueOf(orderQty.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP));
		report.setOrdType(ordType.getValue());
		report.setPrice(BigDecimal.valueOf(price.getValue()).setScale(5, BigDecimal.ROUND_HALF_UP));
		report.setStopPx(BigDecimal.valueOf(stopPx.getValue()).setScale(5, BigDecimal.ROUND_HALF_UP));
		report.setTimeInForce(timeInForce.getValue());
		report.setExpireTime(sdf.format(expireTime.getValue()));
		report.setLastQty(BigDecimal.valueOf(lastQty.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP));
		report.setLastPx(BigDecimal.valueOf(lastPx.getValue()).setScale(5, BigDecimal.ROUND_HALF_UP));
		report.setLeavesQty(BigDecimal.valueOf(leavesQty.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP));
		report.setCumQty(BigDecimal.valueOf(cumQty.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP));
		report.setAvgPx(BigDecimal.valueOf(avgPx.getValue()).setScale(5, BigDecimal.ROUND_HALF_UP));
		report.setTransactTime(sdf.format(transactTime.getValue()));
		report.setText(text.getValue());
		algorithm.addReport(report);
	}

	public void close () {
		log.info("application closing...");
		algorithm.finish();
		/* タイミングによってうまくいかないことがあるので一旦保留
		List<Order> orderList = algorithm.finish();
		if (orderList != null) {
			for (Order order : orderList) {
				try {
					newOrder(order);
				} catch (SessionNotFound e) {
					log.error("new Order", e);
				}
			}
		}
		try {
			log.info("waiting orders...");
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		*/
	}

}
