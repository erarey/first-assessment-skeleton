package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
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

	Queue<Message> unreadMessages = new LinkedList<Message>();

	Queue<Message> unsentMessages = new LinkedList<Message>();

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

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				
				if (hasBeenAnnounced == false && thisUsername != "") {
					Message msgNew = new Message();
					msgNew.setUsername(thisUsername);
					msgNew.setContents(thisUsername + " has connected.");
					hasBeenAnnounced = true;
					messageCenter.passMessageToMessageCenter(msgNew);
				}
				
				if (reader.ready()) {
					String raw = reader.readLine();
					Message message = mapper.readValue(raw, Message.class);

					switch (message.getCommand()) {
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						thisUsername = message.getUsername();
						System.out.println(thisUsername);
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast":
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
						// String response2 =
						// mapper.writeValueAsString(message);
						// writer.write(response2);
						// writer.flush();
						unsentMessages.add(message);
						break;
					case "whisper":
						log.info("user <{}> whispered <{}>", message.getUsername(), message.getContents());
						// String response2 =
						// mapper.writeValueAsString(message);
						// writer.write(response2);
						// writer.flush();
						unsentMessages.add(message);
						break;
					}
				} else {
					while (!unsentMessages.isEmpty()) {
						messageCenter.passMessageToMessageCenter(unsentMessages.remove());
					}

					displayMessages(writer, mapper);
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

	public synchronized void addMessage(Message message) {
		//log.debug("adding a message at client handler");
		unreadMessages.add(message);
		//log.debug("unreadMessages: " + (new Integer(unreadMessages.size()).toString()));

	}

	public synchronized void displayMessages(PrintWriter writer, ObjectMapper mapper) {
		// log.debug("unreadMessages B: " + (new
		// Integer(unreadMessages.size()).toString()));

		while (!unreadMessages.isEmpty()) {
			log.debug("trying to send from within clienthandler");
			String response3;
			try {
				response3 = mapper.writeValueAsString(unreadMessages.remove());
				writer.write(response3);
				writer.flush();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}

		}
	}

}
