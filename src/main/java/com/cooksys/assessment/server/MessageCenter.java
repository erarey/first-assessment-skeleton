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

	//private List<ClientHandler> clientsNew = new ArrayList<ClientHandler>();
	
	//private List<Message> messagesTemp = new ArrayList<>();

	private List<Message> messages = new ArrayList<>();

	private List<Message> undeliverableMessages = new ArrayList<>();
	
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
		//System.out.println("message center got a message");
		messages.add(message);
		
	}

	public synchronized void addClientHandler(ClientHandler clientHandler) {
		//System.out.println("Before add: " + clientsSyncd.size());
		
		clientsSyncd.add(clientHandler);
		
		//System.out.println("After add: " + clientsSyncd.size());
		
		//List<ClientHandler> clientsTemp = new ArrayList<ClientHandler>();
		
		//System.out.println("In temp: " + clientsTemp.size());
		
		//clientsNew.add(clientHandler);
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
					msgNew.setUsername(cl.thisUsername);
					msgNew.setCommand("connect");
					msgNew.setTimestamp(new Date().toString());
					//msgNew.setContents("has connected");
					cl.hasBeenAnnounced = true;
					passMessageToMessageCenter(msgNew);
				}
				
				if (msg.getCommand() != null && msg.getCommand().startsWith("ATSIGN"))
				{
					//System.out.println("Send was: " + msg.getCommand().substring(6));
					//System.out.println("User: " + cl.thisUsername);
					if (cl.thisUsername.equals(msg.getCommand().substring(6)))
					{
						//System.out.println("found recipient of private message!");
						cl.addMessage(new Message(msg));
					}
					else
					{
						undeliverableMessages.add(new Message(msg));
						System.out.println("A message was not deliverable");
					}
				}
				else
				{
				cl.addMessage(new Message(msg)); //may need to create new message, copy old, send
				}
			}
		}
		//System.out.println("messages before clear " + messages.size());
		messages.clear();
		
		//TODO: Handle undeliverableMessages here.
	}
	
	public synchronized Message userlistRequest(ClientHandler requester)
	{
		String re = System.getProperty("line.separator");
		
		String userlist = "";
		
		Set<ClientHandler> clientsTemp = new HashSet<ClientHandler>();
		
		// clientsTemp sometimes ends up with repeats of single users, likely a concurrency problem,
		// but putting the list into a HashSet makes the problem invisible. Since nothing is being
		// concurrently modified, only copied, no errors are thrown.
		
		synchronized (clientsSyncd)
		{
			clientsTemp.addAll(clientsSyncd);
		}
		
		for (ClientHandler ch : clientsTemp)
		{
			userlist += ("" + ch.thisUsername + re);
		}
		
		System.out.println(userlist);
		
		Message msg = new Message();
		msg.setContents("" + (new Date().toString()) + ":" + " currently connected users: " + re + userlist);
		msg.setCommand("users");
		msg.setUsername(requester.thisUsername);
		//msg.setTimestamp(timestamp);
		return msg;
	}

}