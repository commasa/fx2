package jp.commasa.fx2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jp.commasa.fx2.dto.Price;

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
import quickfix.field.Password;
import quickfix.field.ResetSeqNumFlag;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TargetSubID;
import quickfix.field.Text;
import quickfix.fix44.MarketDataRequest;

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

	private List<Symbol> symbols;
	private Map<String, Price> current = new HashMap<String, Price>();
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
		log.debug("fromAdmin : SessionID=" + sessionId.toString() + " , Message=" + message.getClass().getName());
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
			log.debug("toAdmin : SessionID=" + sessionID.toString() + " , Message=" + message.getClass().getName());
		} catch (Exception e) {
			log.error("ERROR toAdmin : ", e);
		}
	}

	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		log.debug("toApp : SessionID=" + sessionId.toString() + " , Message=" + message.getClass().getName());
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
						p.setDate(new Date(mdEntryDate.getValue().getTime() + mdEntryTime.getValue().getTime()));
						p.setTickNo(BigDecimal.valueOf(seqNum.getValue()));
					}
				}
			}
			for (Map.Entry<String, Price> entry : current.entrySet()) {
				algorithm.run(entry.getValue());
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
						p.setDate(new Date(mdEntryDate.getValue().getTime() + mdEntryTime.getValue().getTime()));
						p.setTickNo(BigDecimal.valueOf(seqNum.getValue()));
					}
				}
			}
			for (Map.Entry<String, Price> entry : current.entrySet()) {
				algorithm.run(entry.getValue());
			}
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

}
