package com.cooksys.assessment.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.cooksys.assessment.model.Message;

public class MessageCenter implements Runnable {

	private List<ClientHandler> clients = new ArrayList<ClientHandler>();

	private List<ClientHandler> clientsSyncd = null;

	private List<Message> messages = new ArrayList<>();

	//private List<Message> undeliverableMessages = new ArrayList<>();

	private Server server = null;

	public MessageCenter(Server server) {
		this.server = server;
		clientsSyncd = Collections.synchronizedList(clients);
	}

	@Override
	public void run() {
		while (server.open == true) {
			try {
				Thread.sleep(100);
				pushMessagestoClients();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	public synchronized void passMessageToMessageCenter(Message message) {

		messages.add(message);

	}

	public synchronized void addClientHandler(ClientHandler clientHandler) {

		clientsSyncd.add(clientHandler);
	}

	public synchronized void pushMessagestoClients() {
		for (ClientHandler cl : clientsSyncd) {
			for (Message msg : messages) {
				if (cl.hasBeenAnnounced == false && cl.getUsername() != "") {
					Message msgNew = new Message();
					msgNew.setUsername(cl.getUsername());
					msgNew.setCommand("connect");
					msgNew.setTimestamp(new Date().toString());
					cl.hasBeenAnnounced = true;
					passMessageToMessageCenter(msgNew);
				}

				if (msg.getCommand() != null && msg.getCommand().startsWith("ATSIGN")) {
					if (cl.getUsername().equals(msg.getCommand().substring(6).trim())) {
						cl.addMessage(new Message(msg));
					} else {
						//do nothing, message didn't belong to cl
					}
				} else {
					cl.addMessage(new Message(msg));
				}
			}
		}
		messages.clear();

		// TODO: Handle undeliverableMessages here.
	}

	public synchronized Message userlistRequest(ClientHandler requester) {
		String re = System.getProperty("line.separator");

		String userlist = "";

		synchronized (clientsSyncd) {

			for (ClientHandler ch : clientsSyncd) {
				userlist += ("" + ch.getUsername() + re);
			}
		}

		Message msg = new Message();
		msg.setContents("" + (new Date().toString()) + ":" + " currently connected users: " + re + userlist);
		msg.setCommand("users");
		msg.setUsername(requester.getUsername());
		return msg;
	}

}