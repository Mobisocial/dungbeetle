package edu.stanford.mobisocial.dungbeetle.transport;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import edu.stanford.mobisocial.dungbeetle.util.*;

import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;

public class XMPPMessengerService extends MessengerService {
	private XMPPConnection connection = null;
	private String username = null;
	private String password = null;
	private LinkedBlockingQueue<OutgoingMessage> sendQ = new LinkedBlockingQueue<OutgoingMessage>();
	public static final String XMPP_SERVER = "prpl.stanford.edu";
	private Thread sendWorker = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        OutgoingMessage m = sendQ.peek();

                        if ((m != null) && connected()) {
                            System.out
								.println("Pulled message off sendQueue. Sending.");
                            sendQ.poll();

                            String plain = m.contents();

                            try {
                                String cypher = identity().prepareOutgoingMessage(
									plain, m.toPublicKey());
                                Message msg = new Message();
                                msg.setFrom(username + "@" + XMPP_SERVER);
                                msg.setBody(cypher);

                                String jid = publicKeyToUsername(m.toPublicKey())
									+ "@" + XMPP_SERVER;
                                msg.setTo(jid);
                                connection.sendPacket(msg);
                            } catch (CryptoException e) {
                                e.printStackTrace(System.err);
                            }
                        } else {
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }
        };

	public XMPPMessengerService(Identity ident) {
		super(ident);
		username = publicKeyToUsername(ident.publicKey());
		password = username + "pass";
		sendWorker.start();
	}

	private String publicKeyToUsername(PublicKey pkey) {
		String me = null;

		try {
			me = Util.SHA1(pkey.getEncoded());
		} catch (Exception e) {
			throw new IllegalArgumentException(
                "Could not compute SHA1 of public key.");
		}

		return me.substring(0, 10);
	}

	@Override
	public void init() {
		if ((username == null) || (password == null)) {
			throw new IllegalArgumentException(
                "Must supply username and password.");
		}

		System.out.println("Logging in with " + username + " " + password);

		try {
			connection = new XMPPConnection(XMPP_SERVER);
			connection.connect();

			AccountManager mgr = connection.getAccountManager();
			Map<String, String> atts = new HashMap<String, String>();
			atts.put("name", "AnonUser");
			atts.put("email", "AnonUser@" + XMPP_SERVER);

			try {
				connection.login(username, password);
				System.out.println("Logged in!");
				handleLoggedIn();
			} catch (XMPPException e) {
				try {
					System.out
                        .println("Login failed. Attempting to create account..");
					mgr.createAccount(username, password, atts);

					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
					}

					System.out.println("Account created, logging in...");

					try {
						connection.login(username, password);
						System.out.println("Logged in!");
						handleLoggedIn();
					} catch (XMPPException ex) {
						System.err.println("Login failed.");
						System.err.println(ex);
					}
				} catch (XMPPException ex) {
					System.err.println("User account creation failed due to: ");
					System.err.println(ex);
				}
			}
		} catch (XMPPException e) {
			Throwable ex = e.getWrappedThrowable();
			ex.printStackTrace(System.err);
		}
	}

	private void handleLoggedIn() {
		assertConnected();
		connection.addConnectionListener(new ConnectionListener() {
                public void connectionClosed() {
                    System.out.println("Connection closed");
                }

                public void connectionClosedOnError(Exception e) {
                    System.out.println("Connection closed on error: " + e);
                }

                public void reconnectingIn(int i) {
                    System.out.println("Reconnecting in: " + i);
                }

                public void reconnectionFailed(Exception e) {
                    System.out.println("Reconnection failed: " + e);
                }

                public void reconnectionSuccessful() {
                    System.out.println("Reconnection successful");
                }
            });
		connection.addPacketListener(new PacketListener() {
                public void processPacket(final Packet p) {
                    if (p instanceof Message) {
                        System.out.println("Processing " + p);

                        final Message m = (Message) p;
                        final String body = m.getBody();
                        PublicKey pkey = identity().getMessagePublicKey(body);

                        if (!(m.getFrom().startsWith(publicKeyToUsername(pkey)))) {
                            System.err
								.println("WTF! public key in message does not match sender!.");

                            return;
                        }

                        try{
                            final String contents = identity().prepareIncomingMessage(
                                body, pkey);
                            signalMessageReceived(new IncomingMessage() {
                                    public String from() {
                                        return m.getFrom();
                                    }

                                    public String contents() {
                                        return contents;
                                    }

                                    public String toString() {
                                        return contents();
                                    }
                                });
                        }
                        catch(CryptoException e){
                            System.err.println("Failed in processing incoming message! Reason:");
                            e.printStackTrace(System.err);
                        }
                    } else {
                        System.out.println("Unrecognized packet " + p.toString());
                    }
                }
            }, new PacketFilter() {
                    public boolean accept(Packet p) {
                        return true;
                    }
                });
		signalReady();
	}

	private boolean connected() {
		return (connection != null) && connection.isConnected();
	}

	private void assertConnected() {
		if (!connected()) {
			throw new IllegalStateException("Not connected!");
		}
	}

	@Override
	public void sendMessage(OutgoingMessage m) {
		sendQ.offer(m);
	}
}
