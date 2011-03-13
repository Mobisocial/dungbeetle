package edu.stanford.mobisocial.dungbeetle.transport;

import java.util.*;

public abstract class MessengerService {
	private Set<StateListener> stateListeners = new HashSet<StateListener>();
	private Set<MessageListener> messageListeners = new HashSet<MessageListener>();
	private Identity ident;

	public MessengerService(Identity ident) {
		this.ident = ident;
	}

	protected Identity identity() {
		return ident;
	}

	public abstract void init();

	public abstract void sendMessage(OutgoingMessage m);

	public void addStateListener(StateListener l) {
		stateListeners.add(l);
	}

	public void addMessageListener(MessageListener l) {
		messageListeners.add(l);
	}

	public void removeStateListener(StateListener l) {
		stateListeners.remove(l);
	}

	public void removeMessageListener(MessageListener l) {
		messageListeners.remove(l);
	}

	protected void signalReady() {
		for (StateListener l : stateListeners) {
			l.onReady();
		}
	}

	protected void signalNotReady() {
		for (StateListener l : stateListeners) {
			l.onNotReady();
		}
	}

	protected void signalMessageReceived(IncomingMessage m) {
		for (MessageListener l : messageListeners) {
			l.onMessage(m);
		}
	}
}
