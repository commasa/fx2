package jp.commasa.fx2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldNotFound;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.Initiator;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.RuntimeError;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.ThreadedSocketInitiator;

public class FIXInitiator {

	private Logger log = LogManager.getLogger(this.getClass());
	private Initiator initiator;
	private OandaApp application;

	public static void main(String[] args) throws ConfigError, InterruptedException, IOException, SessionNotFound, RuntimeError, FieldNotFound {
		InputStream inputStream = FIXInitiator.class.getResourceAsStream("/oanda.cfg");
		ResourceBundle bundle = ResourceBundle.getBundle("FIXInitiator");
		FIXInitiator main = new FIXInitiator();
		main.loadSetting(inputStream, bundle);
		main.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				main.stop();
			}
		});
		while (true) {
			Thread.sleep(1000L);
		}
	}

	public FIXInitiator() {
		log.info("============================================================");
	}
	
	public void loadSetting(InputStream inputStream, ResourceBundle bundle) throws ConfigError  {
		SessionSettings sessionSettings = new SessionSettings(inputStream);
		application = new OandaApp(sessionSettings, bundle);
		FileStoreFactory fileStoreFactory = new FileStoreFactory(sessionSettings);
		LogFactory logFactory = new FileLogFactory(sessionSettings);
		MessageFactory messageFactory = new DefaultMessageFactory();
		initiator = new ThreadedSocketInitiator(application, fileStoreFactory, sessionSettings, logFactory, messageFactory);
		log.info("loadSetting : OK");
	}

	public void start() throws RuntimeError, ConfigError, SessionNotFound, FieldNotFound  {
		initiator.start();
		log.info("start : OK");
	}

	public void stop() {
		application.close();
		initiator.stop();
		log.info("stop : OK");
	}
}
