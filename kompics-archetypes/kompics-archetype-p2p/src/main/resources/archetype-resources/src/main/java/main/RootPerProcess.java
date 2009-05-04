package ${package}.main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ${package}.main.event.ApplicationInit;
import ${package}.main.event.HelloComponentInit;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.launch.Topology;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.mina.MinaNetwork;
import se.sics.kompics.network.mina.MinaNetworkInit;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;

/**
 * The <code>ExecutionGroup</code> class.
 * 
 */
public class RootPerProcess extends ComponentDefinition {
	
	static {
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}
	private static int selfId;
	private static String commandScript;
	Topology topology;

	Component time;
	Component network;
	Component helloComponent;
	Component commandProcessor;

	
	private static final Logger logger = LoggerFactory
	.getLogger(RootPerProcess.class);

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		if (args.length != 2)
		{
			System.out.println("This main is called by the distributed system launcher program.");
			System.out.println("Usage: <prog> id \"commandscript\"");
			System.out.println("Num of args was " + args.length);
			System.exit(-1);
		}
		
		selfId = Integer.parseInt(args[0]);
		commandScript = args[1];

		Kompics.createAndStart(RootPerProcess.class);
	}

	/**
	 * Instantiates a new assignment0 group0.
	 */
	public RootPerProcess() {
		
		String prop = System.getProperty("topology");
		topology = Topology.load(prop, selfId);
		
		// create components
		time = create(JavaTimer.class);
		network = create(MinaNetwork.class);
		helloComponent = create(HelloComponent.class);			
		commandProcessor = create(CommandProcessor.class);

		// handle possible faults in the components
		subscribe(handleFault, time.getControl());
		subscribe(handleFault, network.getControl());
		subscribe(handleFault, helloComponent.getControl());
		subscribe(handleFault, commandProcessor.getControl());

		// XXX test that this topology is the same one generated by Application
		Address self = topology.getSelfAddress();
		List<Address> neighbourList = new ArrayList<Address>(topology.getNeighbors(self));

		logger.info("My id = " + selfId + " ; Num neighbours = " + neighbourList.size());

		trigger(new MinaNetworkInit(self), network.getControl());
		
		connect(helloComponent.getNegative(Network.class), network
				.getPositive(Network.class));
		connect(helloComponent.getNegative(Timer.class), time
				.getPositive(Timer.class));
		
		connect(helloComponent.getPositive(HelloPort.class), commandProcessor.getNegative(HelloPort.class));
		
		connect(commandProcessor.getNegative(Timer.class), time
				.getPositive(Timer.class));

		trigger(new HelloComponentInit(selfId, self, neighbourList), helloComponent.getControl());		
		trigger(new ApplicationInit(commandScript, new HashSet<Address>(neighbourList), self),
				commandProcessor.getControl());				
	}

	Handler<Fault> handleFault = new Handler<Fault>() {
		public void handle(Fault fault) {
			fault.getFault().printStackTrace(System.err);
		}
	};
	
}