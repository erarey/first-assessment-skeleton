package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;

	private MessageCenter messageCenter = null;

	boolean alive = true;

	boolean prepareToDisconnect = false;

	Queue<Message> unreadMessages = new ConcurrentLinkedQueue<Message>();

	Queue<Message> unsentMessages = new ConcurrentLinkedQueue<Message>();

	String thisUsername = "";

	boolean hasBeenAnnounced = false;

	public ClientHandler(Socket socket, MessageCenter messageCenter) {
		super();
		this.socket = socket;
		this.messageCenter = messageCenter;
		messageCenter.addClientHandler(this);
	}

	public void run() {
		try {
			Thread.sleep(500);
			
			String previousCommand = "connect";
			
			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {

				if (hasBeenAnnounced == false && thisUsername != "") {
					Message msgNew = new Message();
					msgNew.setUsername(thisUsername);
					msgNew.setContents(thisUsername + " has connected.");
					msgNew.setCommand("connect");
					hasBeenAnnounced = true;
					msgNew.setTimestamp(new Date().toString());
					messageCenter.passMessageToMessageCenter(msgNew);
				}

				if (reader.ready()) {
					String raw = reader.readLine();
					Message message = mapper.readValue(raw, Message.class);
					
					System.out.println("Received: " + message.getCommand());
					if (message.getCommand().equals("lastcommand"))
					{
						System.out.println("lastcommand attempt");
						if (previousCommand.equals("connect") || previousCommand.equals("lastcommand"))
						{
							System.out.println("no prev command found");
							log.info("user <{}> did not enter a command", message.getUsername());
							message.setTimestamp(new Date().toString());
							message.setContents("Please enter a recognized command");
							message.setCommand("lastcommand");
							message.setTimestamp(new Date().toString());
							String response = mapper.writeValueAsString(message);
							writer.write(response);
							writer.flush();
							break;
						}
						else
						{
							System.out.println("attempting to reuse last command " + previousCommand);
							String addContents = message.getCommand();
							message.setContents("" + addContents + " " + message.getContents());
							message.setCommand(previousCommand);
							System.out.println("new contents: " + message.getContents());
						}
					}
					else
					{
						System.out.println("previousCommand " + message.getCommand());
						
						
					}
					//System.out.println("COMMAND COMING IN:" + message.getCommand());
					switch (message.getCommand()) {
					
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						thisUsername = message.getUsername();
						//System.out.println(thisUsername);
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						prepareToDisconnect = true;
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						message.setTimestamp(new Date().toString());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast":
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
						message.setTimestamp(new Date().toString());
						synchronized (unsentMessages) {
							unsentMessages.add(message);
						}
						break;
					case "users":
						log.info("user <{}> requested the user list");
						String response6 = mapper.writeValueAsString(messageCenter.userlistRequest(this));
						writer.write(response6);
						writer.flush();
					
					default:
						String addContents = message.getCommand();
						message.setContents("" + addContents + " " + message.getContents());
						message.setCommand(previousCommand);
						message.setTimestamp(new Date().toString());
						synchronized (unsentMessages)
						{
							unsentMessages.add(message);
						}
						break;
					}
					
					previousCommand = message.getCommand();
					
					if (message.getCommand().startsWith("ATSIGN")) {
						System.out.println("a private message was created");
						message.setTimestamp(new Date().toString());

						synchronized (unsentMessages) {
							unsentMessages.add(message);
						}
					}

				} else {
					while (!unsentMessages.isEmpty()) {
						Message msg = null;

						//System.out.println("processing unsent messages " + unsentMessages.size());
						synchronized (unsentMessages) {
							msg = unsentMessages.poll();
						}

						if (msg != null) {
							if (msg.getCommand().startsWith("ATSIGN")) {
								System.out.println("passing private message to MessageCenter");
							}
							messageCenter.passMessageToMessageCenter(msg);
						}
					}

					displayMessages(writer, mapper);
				}

				if (prepareToDisconnect == true) {
					Message msgNew = new Message();
					msgNew.setUsername(thisUsername);
					msgNew.setContents(thisUsername + " has disconnected.");
					msgNew.setCommand("disconnect");
					msgNew.setTimestamp(new Date().toString());
					messageCenter.passMessageToMessageCenter(msgNew);
					this.socket.close();
				}
			}

		} catch (IOException | InterruptedException e) {
			log.error("Something went wrong :/", e);
		}
	}

	public synchronized void addMessage(Message message) {
		// log.debug("adding a message at client handler");
		unreadMessages.add(message);
		// log.debug("unreadMessages: " + (new
		// Integer(unreadMessages.size()).toString()));

	}

	public synchronized void displayMessages(PrintWriter writer, ObjectMapper mapper) {
		// log.debug("unreadMessages B: " + (new
		// Integer(unreadMessages.size()).toString()));

		while (!unreadMessages.isEmpty()) {
			//log.debug("trying to send from within clienthandler");
			String response3;
			try {
				Message msg = null;
				// if (unreadMessages.peek() != null) {
				synchronized (unreadMessages) {
					//System.out.println("POLLING");
					msg = unreadMessages.poll();
					//System.out.println("POLLED");
					if (msg == null) System.out.println("null!");
				}

				if (msg != null) {
					//if (msg.getCommand().startsWith("**")) {
						//System.out.println("Found an unread private message");
					//}
					//System.out.println("COMMAND WAS " + msg.getCommand());
					response3 = mapper.writeValueAsString(formatMessage(msg));
					writer.write(response3);
					writer.flush();
				}
				// }
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}

		}
	}
	
	Message formatMessage(Message msg)
	{
		String com = msg.getCommand();
		
		if (com.equals("broadcast"))
		{
			com = "all";
		}
		else if (com.equals("users"))
		{
			//this should already be formatted by MessageCenter
			return msg;
		}
		else if (com.equals("echo"))
		{
			com = "echo";
		}
		else if (com.startsWith("ATSIGN"))
		{
			com = "whisper";
			msg.setCommand("whisper"); //to make it easier to color is Javascript
		}
		else if (com.equals("connect") || com.equals("disconnect"))
		{
			msg.setContents((msg.getTimestamp() +":" + " <" + msg.getUsername() + ">" + " has " + com + "ed"));
			return msg;
		}
		else
		{
			msg.setContents("A known command is required. Type disconnect to leave.");
			return msg;
			
		}
		
		if (com.equals("whisper")) msg.setContents(msg.getContents().substring(msg.getContents().indexOf(" ")+1));
		
		msg.setContents((msg.getTimestamp() + " <" + msg.getUsername() + "> " 
				+ "(" + com +"):" + msg.getContents()));
		
		 
		return msg;
		
	}
}
