package com.cooksys.assessment.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.cooksys.assessment.model.Message;

public class MessageCenter implements Runnable {

	private List<ClientHandler> clients = new ArrayList<ClientHandler>();

	private List<ClientHandler> clientsSyncd = null;

	private List<ClientHandler> clientsNew = new ArrayList<ClientHandler>();
	
	//private List<Message> messagesTemp = new ArrayList<>();

	private List<Message> messages = new ArrayList<>();

	private Server server = null;

	public MessageCenter(Server server) {
		this.server = server;
		clientsSyncd = Collections.synchronizedList(clients);
	}

	@Override
	public void run() {
		while (true)
		{
			try {
				Thread.sleep(100);
				pushMessagestoClients();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
		}
	}

	public synchronized void passMessageToMessageCenter(Message message) {
		System.out.println("message center got a message");
		messages.add(message);
		
	}

	public synchronized void addClientHandler(ClientHandler clientHandler) {
		//System.out.println("Before add: " + clientsSyncd.size());
		
		clientsSyncd.add(clientHandler);
		
		//System.out.println("After add: " + clientsSyncd.size());
		
		//List<ClientHandler> clientsTemp = new ArrayList<ClientHandler>();
		
		//System.out.println("In temp: " + clientsTemp.size());
		
		clientsNew.add(clientHandler);
	}
	
	public synchronized void pushMessagestoClients()
	{
		//List<ClientHandler> clientsTemp = new ArrayList<ClientHandler>();
		//clientsTemp.addAll(clientsSyncd);
		
		//ListIterator<ClientHandler> cl = clientsTemp.listIterator();
		//while (cl.hasNext())
		for (ClientHandler cl : clientsSyncd)
		{
			for (Message msg : messages)
			{
				if (cl.hasBeenAnnounced == false && cl.thisUsername != "")
				{
					Message msgNew = new Message();
					msgNew.setUsername(cl.thisUsername); msgNew.setContents("has connected.");
					cl.hasBeenAnnounced = true;
					passMessageToMessageCenter(msgNew);
				}
				
				cl.addMessage(msg); //may need to create new message, copy old, send
			}
		}
		//System.out.println("messages before clear " + messages.size());
		messages.clear();
		
	}

}