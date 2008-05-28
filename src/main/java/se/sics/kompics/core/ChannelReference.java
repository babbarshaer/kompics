package se.sics.kompics.core;

import java.util.EnumSet;
import java.util.Set;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.capability.CapabilityNotSupportedException;
import se.sics.kompics.api.capability.ChannelCapabilityFlags;

public class ChannelReference implements Channel {

	/**
	 * The channel referenced by this channel reference.
	 */
	private ChannelCore channelCore;

	/**
	 * Set of capabilities offered on the referenced channel.
	 */
	private EnumSet<ChannelCapabilityFlags> channelCapabilities;

	/**
	 * The identifier of the component that is capable to use this reference. If
	 * <code>null</code> any component is capable to use this reference.
	 */
	private ComponentUUID capable;

	/**
	 * Constructs a channel reference. This constructor is only visible in the
	 * se.sics.kompics.core package.
	 * 
	 * @param channelCore
	 * @param channelCapabilities
	 */
	ChannelReference(ChannelCore channelCore,
			EnumSet<ChannelCapabilityFlags> channelCapabilities) {
		super();

		if (channelCore == null) {
			throw new RuntimeException(
					"Cannot create a channel reference to a null channel.");
		}

		this.channelCore = channelCore;

		if (channelCapabilities == null) {
			this.channelCapabilities = EnumSet
					.noneOf(ChannelCapabilityFlags.class);
		} else {
			this.channelCapabilities = channelCapabilities;
		}
	}

	/**
	 * Constructs a channel reference. This constructor is visible only in the
	 * se.sics.kompics.core package.
	 * 
	 * @param channelCore
	 * @param channelCapabilities
	 * @param capable
	 */
	ChannelReference(ChannelCore channelCore,
			EnumSet<ChannelCapabilityFlags> channelCapabilities,
			ComponentUUID capable) {
		this(channelCore, channelCapabilities);
		this.capable = capable;
	}

	void revoke() {
		channelCore = null;
	}

	ChannelCore getChannelCore() {
		return channelCore;
	}

	public void addEventType(Class<? extends Event> eventType) {
		if (channelCapabilities.contains(ChannelCapabilityFlags.ADD_EVENT_TYPE)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			channelCore.addEventType(eventType);
			return;
		}
		throw new CapabilityNotSupportedException(
				ChannelCapabilityFlags.ADD_EVENT_TYPE);
	}

	public boolean hasEventType(Class<? extends Event> eventType) {
		return channelCore.hasEventType(eventType);
	}

	public void removeEventType(Class<? extends Event> eventType) {
		if (channelCapabilities
				.contains(ChannelCapabilityFlags.REMOVE_EVENT_TYPE)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			channelCore.removeEventType(eventType);
			return;
		}
		throw new CapabilityNotSupportedException(
				ChannelCapabilityFlags.REMOVE_EVENT_TYPE);
	}

	public Set<Class<? extends Event>> getEventTypes() {
		return channelCore.getEventTypes();
	}

	ChannelCore addSubscription(Subscription subscription) {
		if (channelCapabilities.contains(ChannelCapabilityFlags.SUBSCRIBE)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			channelCore.addSubscription(subscription);
			return channelCore;
		}
		throw new CapabilityNotSupportedException(
				ChannelCapabilityFlags.SUBSCRIBE);
	}

	void publishEventCore(EventCore eventCore) {
		if (channelCapabilities.contains(ChannelCapabilityFlags.PUBLISH)
				&& (capable == null || capable.equals(ComponentUUID.get()))) {
			channelCore.publishEventCore(eventCore);
			return;
		}
		throw new CapabilityNotSupportedException(
				ChannelCapabilityFlags.PUBLISH);
	}

}
