package se.sics.kompics.kdld.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.monitor.P2pMonitorConfiguration;
import se.sics.kompics.web.jetty.JettyWebServerInit;

/**
 * This class is not thread-safe for writing, although it is thread-safe for reading.
 * @author jdowling
 *
 */
public abstract class Configuration {
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

	public final static String BOOTSTRAP_CONFIG_FILE = "config/bootstrap.properties";
	public final static String MONITOR_CONFIG_FILE = "config/monitor.properties";
	
	public final static String PROP_IP = "ip";
	public final static String PROP_PORT = "port";
	
	public final static String PROP_BOOTSTRAP_PORT = "bootstrap.server.port";
	public final static String PROP_BOOTSTRAP_IP = "bootstrap.server.ip";
	public final static String PROP_BOOTSTRAP_EVICT_AFTER = "bootstrap.evict.after";
	public final static String PROP_BOOTSTRAP_REFRESH_PERIOD = "client.refresh.period";
	public final static String PROP_BOOTSTRAP_RETRY_PERIOD = "client.retry.period";
	public final static String PROP_BOOTSTRAP_RETRY_COUNT = "client.retry.count";
	
	
	public final static String PROP_MONITOR_IP = "monitor.server.ip";
	public final static String PROP_MONITOR_PORT = "monitor.server.port";
	public final static String PROP_MONITOR_ID = "monitor.server.id";
	public final static String PROP_MONITOR_EVICT_AFTER = "monitor.evict.after";
	public final static String PROP_MONITOR_REFRESH_PERIOD = "client.refresh.period";
	public final static String PROP_MONITOR_RETRY_PERIOD = "client.retry.period";
	public final static String PROP_MONITOR_RETRY_COUNT = "client.retry.count";
	
	public final static String PROP_NUM_PEERS = "number.peers";
	public final static String PROP_NUM_WORKERS = "number.workers";
	
	/*
	 * Non-publicly accessible
	 */
	protected static final String DEFAULT_IP = "localhost";
	protected static final int DEFAULT_PORT = 0;
	
	protected static final String DEFAULT_BOOTSTRAP_IP = "localhost";
	protected static final int DEFAULT_BOOTSTRAP_PORT = 20002;
	protected static final int DEFAULT_BOOTSTRAP_ID = Integer.MAX_VALUE;

	protected static final String DEFAULT_MONITOR_IP = "localhost";
	protected static final int DEFAULT_MONITOR_PORT = 20001;
	protected static final int DEFAULT_MONITOR_ID = Integer.MAX_VALUE - 1;

	protected static final int DEFAULT_EVICT_AFTER_SECS = 600;
	protected static final int DEFAULT_REFRESH_PERIOD = 10;
	protected static final int DEFAULT_RETRY_PERIOD = 500;
	protected static final int DEFAULT_RETRY_COUNT = 3;

	protected static final int DEFAULT_NUM_WORKERS = 1;
	protected static final int DEFAULT_NUM_PEERS = 1;

	protected static final int DEFAULT_NET_PORT = 20000;

	protected static final int DEFAULT_WEB_PORT = 8080;
	protected static final int DEFAULT_WEB_REQUEST_TIMEOUT_MS = 10000;
	protected static final int DEFAULT_WEB_THREADS = 2;

	protected static final int DEFAULT_CONTROLLER_PORT = 9090;
	protected static final int DEFAULT_CONTROLLER_REQUEST_TIMEOUT_MS = 10000;
	protected static final String DEFAULT_CONTROLLER_IP = "localhost";

	/*
	 * Cache instances of these variables from Apache Configuration objects.
	 */
	protected InetAddress ip = null;

	protected int webPort = DEFAULT_WEB_PORT;

	protected int bootstrapId = DEFAULT_BOOTSTRAP_ID;

	protected Address peer0Address = null;

	protected P2pMonitorConfiguration monitorConfiguration = null;

	protected int webRequestTimeout = DEFAULT_WEB_REQUEST_TIMEOUT_MS;

	protected int webThreads = DEFAULT_WEB_THREADS;

	protected String webAddress;

	protected String homepage;

	protected JettyWebServerInit jettyWebServerInit = null;

	protected BootstrapConfiguration bootConfiguration = null;

	/**
	 * Singleton instance of configuration
	 */
	protected static Configuration configuration = null;

	
	protected PropertiesConfiguration bootstrapConfig;
	protected PropertiesConfiguration monitorConfig;
	
	protected CompositeConfiguration compositeConfig = new CompositeConfiguration();

	/**
	 * Helper non-public fields
	 */
	protected Options options = new Options();

	protected CommandLine line;

	/**
	 * If configuration object already created, it returns the existing
	 * instance. Otherwise, creates and initializes an instance of a subclass of
	 * Configuration defined by configType parameter.
	 * 
	 * @param configType
	 * @param args
	 * @return
	 * @throws IOException
	 */
	public static synchronized Configuration init(Class<? extends Configuration> classname, String[] args)
			throws IOException, ConfigurationException {

//		if (configuration != null) {
//			return configuration;
//		}

		try {
			// Create instance of subclass of Configuration and call its
			// constructor (String[]) using reflection
			// XXX improve exception processing
			Constructor<? extends Configuration> constructor = classname
					.getConstructor(String[].class);
			configuration = constructor.newInstance((Object) args);
		} catch (SecurityException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (InstantiationException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (InvocationTargetException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new ConfigurationException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new ConfigurationException(e.getMessage());
		}

		return configuration;
	}

	/**
	 * You should call this constructor from the main method of your Main class,
	 * with the args parameters from your main method.
	 * 
	 * @param args
	 * @return
	 * @throws IOException
	 */
	protected Configuration(String[] args) throws IOException {
		
		try {
			bootstrapConfig = new PropertiesConfiguration(BOOTSTRAP_CONFIG_FILE);
			bootstrapConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
			compositeConfig.addConfiguration(bootstrapConfig);
		}
		catch (ConfigurationException e)
		{
			logger.warn("Bootstrap configuration file not found, using default values: " 
					+ BOOTSTRAP_CONFIG_FILE);
		}

		try {
			monitorConfig = new PropertiesConfiguration(MONITOR_CONFIG_FILE);
			monitorConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
			compositeConfig.addConfiguration(monitorConfig);
		}
		catch (ConfigurationException e)
		{
			logger.warn("Monitor configuration file not found, using default values: " 
					+ MONITOR_CONFIG_FILE);
		}
		
		// Users can override the default options from the command line
		Option ipOption = new Option("ip", true, "IP address (or hostname) for Kompics instance to listen on");
		ipOption.setArgName("ip");
		options.addOption(ipOption);

		Option portOption = new Option("port", true, "Port for Kompics instance to listen on");
		portOption.setArgName("port");
		options.addOption(portOption);

		
		Option bootstrapIpOption = new Option("bIp", true, "Bootstrap server ip address");
		bootstrapIpOption.setArgName("address");
		options.addOption(bootstrapIpOption);

		Option bootstrapPortOption = new Option("bPort", true, "Bootstrap server bootstrapPort");
		bootstrapPortOption.setArgName("number");
		options.addOption(bootstrapPortOption);

		Option numWorkersOption = new Option("workers", true, "Number of Workers to create");
		numWorkersOption.setArgName("number");
		options.addOption(numWorkersOption);

		Option monitorIpOption = new Option("mIp", true, "Peer Monitor server ip address");
		monitorIpOption.setArgName("address");
		options.addOption(monitorIpOption);

		Option monitorPortOption = new Option("mPort", true, "Peer Monitor port");
		monitorPortOption.setArgName("number");
		options.addOption(monitorPortOption);

		Option monitorIdOption = new Option("mId", true, "Peer Monitor id");
		monitorIdOption.setArgName("id");
		options.addOption(monitorIdOption);

		Option help = new Option("help", false, "Help message printed");
		options.addOption(help);

		Option numPeersOption = new Option("peers", true, "Number of peers to simulate.");
		numPeersOption.setArgName("number");
		options.addOption(numPeersOption);

		Option monitorRefreshOption = new Option("mRefreshPeriod", true,
				"Client Monitor refresh Period");
		monitorRefreshOption.setArgName("seconds");
		options.addOption(monitorRefreshOption);

		Option bootstrapRefreshOption = new Option("bRefreshPeriod", true,
				"Bootstrap refresh Period");
		bootstrapRefreshOption.setArgName("seconds");
		options.addOption(bootstrapRefreshOption);

		// implemented by subclass
		parseAdditionalOptions(args);

		CommandLineParser parser = new GnuParser();
		try {
			line = parser.parse(options, args);
		} catch (ParseException e) {
			help("Parsing failed.  " + e.getMessage(), options);
			throw new IOException(e.getMessage());
		}
		// implemented by subclass
		processAdditionalOptions();

		if (line.hasOption(help.getOpt())) {
			help("", options);
		}

		if (line.hasOption(ipOption.getOpt())) {
			String host = new String(line.getOptionValue(ipOption.getOpt()));
			compositeConfig.setProperty(PROP_IP, host);
		}
		if (line.hasOption(portOption.getOpt())) {
			int port = new Integer(line.getOptionValue(portOption.getOpt()));
			compositeConfig.setProperty(PROP_PORT, port);
		}
		
		if (line.hasOption(bootstrapIpOption.getOpt())) {
			String bootstrapHost = new String(line.getOptionValue(bootstrapIpOption.getOpt()));
			compositeConfig.setProperty(PROP_BOOTSTRAP_IP, bootstrapHost);
		}
		if (line.hasOption(bootstrapPortOption.getOpt())) {
			int bootstrapPort = new Integer(line.getOptionValue(bootstrapPortOption.getOpt()));
			compositeConfig.setProperty(PROP_BOOTSTRAP_PORT, bootstrapPort);
		}
		if (line.hasOption(bootstrapRefreshOption.getOpt())) {
			long bootstrapRefreshPeriod = new Long(line.getOptionValue(bootstrapRefreshOption.getOpt()));
			compositeConfig.setProperty(PROP_BOOTSTRAP_REFRESH_PERIOD, bootstrapRefreshPeriod);
		}

		if (line.hasOption(numPeersOption.getOpt())) {
			int numPeers = new Integer(line.getOptionValue(numPeersOption.getOpt()));
			compositeConfig.setProperty(PROP_NUM_PEERS, numPeers);
		}

		if (line.hasOption(numWorkersOption.getOpt())) {
			int numWorkers = new Integer(line.getOptionValue(numWorkersOption.getOpt()));
			compositeConfig.setProperty(PROP_NUM_WORKERS, numWorkers );
		}

		if (line.hasOption(monitorIpOption.getOpt())) {
			String monitorHost = new String(line.getOptionValue(monitorIpOption.getOpt()));
			compositeConfig.setProperty(PROP_MONITOR_IP, monitorHost);
		}

		if (line.hasOption(monitorPortOption.getOpt())) {
			int monitorPort = new Integer(line.getOptionValue(monitorPortOption.getOpt()));
			compositeConfig.setProperty(PROP_MONITOR_PORT, monitorPort);
		}

		if (line.hasOption(monitorRefreshOption.getOpt())) {
			long monitorRefreshPeriod = new Long(line.getOptionValue(monitorRefreshOption.getOpt()));
			compositeConfig.setProperty(PROP_MONITOR_REFRESH_PERIOD, monitorRefreshPeriod);
		}
		
	}

	abstract protected void parseAdditionalOptions(String[] args) throws IOException;

	abstract protected void processAdditionalOptions() throws IOException;

	/**
	 * @param options
	 */
	protected void help(String message, Options options) {
		HelpFormatter formatter = new HelpFormatter();

		String applicationName = System.getProperty("app.name", "bootstrap-server");

		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);

		formatter.printHelp(writer, HelpFormatter.DEFAULT_WIDTH, applicationName, "", options,
				HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, "");

		writer.close();

		displayHelper(message, stringWriter.getBuffer().toString());
		System.exit(1);
	}

	protected void displayHelper(String message, String usage) {
		if (message != null) {
			logger.info(message);
		}
		logger.info(usage);
	}
	
	public static String getHomepage()
	{
		testInitialized();
		configuration.homepage = "<h2>Welcome to the Kompics Peer-to-Peer Framework!</h2>" + "<a href=\""
		+ getWebAddress() + getBootConfiguration().getBootstrapServerAddress().getId() + 
		"/" + "\">Bootstrap Server</a><br>" + "<a href=\""
		+ getWebAddress() + getMonitorConfiguration().getMonitorServerAddress().getId() 
		+ "/" + "\">Monitor Server</a><br>";
		return configuration.homepage;
	}

	/**
	 * Implemented by monitor program for concrete overlay
	 * @return id of monitor server for concrete overlay
	 */
	protected abstract Address getMonitorServerAddress();

	/**
	 * Implemented by monitor program for concrete overlay
	 * @return id of monitor server for concrete overlay
	 */
	protected abstract int getMonitorId();

	public static BootstrapConfiguration getBootConfiguration() {
		testInitialized();
		if (configuration.bootConfiguration != null)
		{
			return configuration.bootConfiguration;
		}
		configuration.bootConfiguration = setupBootstrapConfig();
		return configuration.bootConfiguration;
	}
	
	private static BootstrapConfiguration setupBootstrapConfig()
	{
		testInitialized();
		String bootstrapHost = configuration.compositeConfig.getString(PROP_BOOTSTRAP_IP, DEFAULT_BOOTSTRAP_IP);
		InetAddress bootstrapIP=null;
		try {
			bootstrapIP = InetAddress.getByName(bootstrapHost);
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage());
		}
		
		int bootstrapPort = configuration.compositeConfig.getInt(PROP_BOOTSTRAP_PORT, DEFAULT_BOOTSTRAP_PORT);
		
		Address bootstrapAddress = new Address(bootstrapIP, bootstrapPort, configuration.bootstrapId);
		return new BootstrapConfiguration(bootstrapAddress,
				configuration.compositeConfig.getInt(PROP_BOOTSTRAP_EVICT_AFTER, DEFAULT_EVICT_AFTER_SECS),
				configuration.compositeConfig.getInt(PROP_BOOTSTRAP_RETRY_PERIOD, DEFAULT_RETRY_PERIOD), 
				configuration.compositeConfig.getInt(PROP_BOOTSTRAP_RETRY_COUNT, DEFAULT_RETRY_COUNT),
				configuration.compositeConfig.getInt(PROP_BOOTSTRAP_REFRESH_PERIOD, DEFAULT_REFRESH_PERIOD), 
				configuration.webPort);
	}

	public static P2pMonitorConfiguration getMonitorConfiguration() {
		testInitialized();
		if (configuration.monitorConfiguration != null)
		{
			return configuration.monitorConfiguration;
		}
		configuration.monitorConfiguration = setupMonitorConfig();
		return configuration.monitorConfiguration;
	}

	private static P2pMonitorConfiguration setupMonitorConfig()
	{
		testInitialized();
		String monitorHost = configuration.compositeConfig.getString(PROP_MONITOR_IP, DEFAULT_MONITOR_IP);
		InetAddress monitorIP=null;
		try {
			monitorIP = InetAddress.getByName(monitorHost);
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage());
		}
		
		int monitorPort = configuration.compositeConfig.getInt(PROP_MONITOR_PORT, DEFAULT_MONITOR_PORT);
		int monitorId = configuration.compositeConfig.getInt(PROP_MONITOR_ID, DEFAULT_MONITOR_ID);
		
		Address monitorAddress = new Address(monitorIP, monitorPort, monitorId);
		return new P2pMonitorConfiguration(monitorAddress,
				configuration.compositeConfig.getInt(PROP_MONITOR_EVICT_AFTER, DEFAULT_EVICT_AFTER_SECS), 
				configuration.compositeConfig.getInt(PROP_MONITOR_REFRESH_PERIOD, DEFAULT_REFRESH_PERIOD), 
				configuration.webPort);
	}
	
	public static int getNumPeers() {
		testInitialized();
		return configuration.compositeConfig.getInt(PROP_NUM_PEERS, DEFAULT_NUM_PEERS);
	}

	public static InetAddress getIp() {
		testInitialized();
		if (configuration.ip == null)
		{
			try {
				configuration.ip = 
					InetAddress.getByName(configuration.compositeConfig.getString(PROP_IP, DEFAULT_IP));
			} catch (UnknownHostException e) {
				logger.warn("Couldn't get IP address for local host: " + 
						configuration.compositeConfig.getString(PROP_IP, DEFAULT_IP));
			}
		}
		return configuration.ip;
	}
	
	public static int getPort() {
		testInitialized();
		return configuration.compositeConfig.getInt(PROP_PORT, DEFAULT_PORT);
	}

	public static String getWebAddress() {
		testInitialized();
		configuration.webAddress = "http://" + getIp().getHostAddress() 
		+ ":" + configuration.webPort + "/";
		return configuration.webAddress;
	}

	public static JettyWebServerInit getJettyWebServerInit() {
		testInitialized();
		if (configuration.jettyWebServerInit == null)
		{
			configuration.jettyWebServerInit = new JettyWebServerInit(getIp(), getWebPort(), 
					configuration.webRequestTimeout, configuration.webThreads,
					getHomepage());
		}
		
		return configuration.jettyWebServerInit;
	}
	
	public static int getWebPort()
	{
		testInitialized();
		return configuration.webPort;
	}
	
	public static Address getPeer0Address() {
		testInitialized();
		if (configuration.peer0Address == null)
		{
			configuration.peer0Address = new Address(getIp(), getPort(), 0);
		}
		return configuration.peer0Address;		
	}
	
	protected static void testInitialized()
	{
		if (configuration == null)
		{
			throw new IllegalStateException("Configuration not initialized. You must call init method first, before other methods.");
		}
	}
}