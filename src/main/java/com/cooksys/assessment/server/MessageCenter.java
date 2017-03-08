package com.cooksys.assessment.server;

import java.util.ArrayList;
import java.util.List;

import com.cooksys.assessment.model.Message;

public class MessageCenter implements Runnable {

	private List<ClientHandler> clients = new ArrayList<ClientHandler>();

	private List<ClientHandler> clientsSyncd = null;

	volatile private List<Message> messagesTemp = new ArrayList<>();

	private List<Message> messages = new ArrayList<>();

	private Server server = null;

	public MessageCenter(Server server) {
		this.server = server;

	}

	@Override
	public void run() {

	}

	public synchronized void passMessageToMessageCenter(Message message) {
		
		
	}

}