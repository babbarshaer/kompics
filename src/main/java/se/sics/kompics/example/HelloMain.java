package se.sics.kompics.example;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.FaultEvent;
import se.sics.kompics.api.Kompics;

public class HelloMain {

	/**
	 * @param args
	 * @throws NullPointerException
	 * @throws NotCompliantMBeanException
	 * @throws MBeanRegistrationException
	 * @throws InstanceAlreadyExistsException
	 * @throws MalformedObjectNameException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			MalformedObjectNameException, InstanceAlreadyExistsException,
			MBeanRegistrationException, NotCompliantMBeanException,
			NullPointerException {
		// get the bootstrap Kompics component
		Kompics kompics = new Kompics(3, 3);
		Component boot = kompics.getBootstrapComponent();

		// create a fault channel
		Channel faultChannel = boot.createChannel(FaultEvent.class);

		// create a request and a response channel
		Channel requestChannel = boot.createChannel(InputEvent.class);
		Channel responseChannel = boot.createChannel(OutputEvent.class);

		// create a universe component
		Component universeComponent = boot.createComponent(
				"se.sics.kompics.example.UniverseComponent", faultChannel,
				requestChannel, responseChannel);

		// create a fault handler component
		Component faultHandlerComponent = boot.createComponent(
				"se.sics.kompics.example.FaultHandlerComponent", faultChannel,
				faultChannel);

		// start components
		universeComponent.start();
		faultHandlerComponent.start();

		System.out.println("TRIGGER INPUT in MAIN");
		// trigger an input event in the request channel
		boot.triggerEvent(new InputEvent(1, "a"), requestChannel);
		boot.triggerEvent(new InputEvent(1, "b"), requestChannel);
		boot.triggerEvent(new InputEvent(2, "a"), requestChannel);
		boot.triggerEvent(new InputEvent(2, "b"), requestChannel);
	}
}