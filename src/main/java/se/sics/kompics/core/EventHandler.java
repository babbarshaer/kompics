package se.sics.kompics.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.ListIterator;

import se.sics.kompics.api.Event;

public class EventHandler {

	private Object handlerObject;

	private Method handlerMethod;

	private Method guardMethod;

	private Class<? extends Event> eventType;

	private LinkedList<Event> blockedEvents;

	private boolean guarded;

	public EventHandler(Object handlerObject, Method handlerMethod,
			Method guardMethod, Class<? extends Event> eventType) {
		super();
		this.handlerObject = handlerObject;
		this.handlerMethod = handlerMethod;
		this.guardMethod = guardMethod;
		this.eventType = eventType;
		this.blockedEvents = new LinkedList<Event>();
		this.guarded = true;
	}

	public EventHandler(Object handlerObject, Method handlerMethod,
			Class<? extends Event> eventType) {
		super();
		this.handlerObject = handlerObject;
		this.handlerMethod = handlerMethod;
		this.guardMethod = null;
		this.eventType = eventType;
		this.blockedEvents = null;
		this.guarded = false;
	}

	public String getName() {
		return handlerMethod.getName();
	}

	public Method getHandlerMethod() {
		return handlerMethod;
	}

	public Method getGuardMethod() {
		return guardMethod;
	}

	public Class<? extends Event> getEventType() {
		return eventType;
	}

	public boolean isGuarded() {
		return guarded;
	}

	public boolean hasBlockedEvents() {
		return (blockedEvents == null ? false : blockedEvents.size() > 0);
	}

	/**
	 * handles an event. If the handler is guarded, the guard is tested first.
	 * If the guard evaluates to false the event is enqueued locally
	 * 
	 * @param event
	 *            the event to be handled
	 * @return <code>true</code> if the handler was executed
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public boolean handleEvent(Event event) throws Throwable {
		if (guarded) {
			// test guard
			boolean allowed = (Boolean) guardMethod
					.invoke(handlerObject, event);
			if (allowed) {
				// handle event
				handlerMethod.invoke(handlerObject, event);
				return true;
			} else {
				// enqueue event locally
				blockedEvents.addLast(event);
				return false;
			}
		} else {
			// handle event
			handlerMethod.invoke(handlerObject, event);
			return true;
		}
	}

	/**
	 * @return <code>true</code> if one event was handled, <code>false</code> if
	 *         no blocked event could be executed.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public boolean handleOneBlockedEvent() throws Throwable {
		if (!guarded) {
			return false;
		}

		ListIterator<Event> iter = blockedEvents.listIterator();
		while (iter.hasNext()) {
			Event event = iter.next();

			boolean allow = (Boolean) guardMethod.invoke(handlerObject, event);
			if (allow) {
				handlerMethod.invoke(handlerObject, event);
				iter.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((guardMethod == null) ? 0 : guardMethod.hashCode());
		result = prime * result + (guarded ? 1231 : 1237);
		result = prime * result
				+ ((handlerMethod == null) ? 0 : handlerMethod.hashCode());
		result = prime * result
				+ ((handlerObject == null) ? 0 : handlerObject.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventHandler other = (EventHandler) obj;
		if (guardMethod == null) {
			if (other.guardMethod != null)
				return false;
		} else if (!guardMethod.equals(other.guardMethod))
			return false;
		if (guarded != other.guarded)
			return false;
		if (handlerMethod == null) {
			if (other.handlerMethod != null)
				return false;
		} else if (!handlerMethod.equals(other.handlerMethod))
			return false;
		if (handlerObject == null) {
			if (other.handlerObject != null)
				return false;
		} else if (!handlerObject.equals(other.handlerObject))
			return false;
		return true;
	}
}
