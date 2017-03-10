package com.cooksys.assessment.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;

public class Server implements Runnable {
	private Logger log = LoggerFactory.getLogger(Server.class);

	private int port;

	private ExecutorService executor;

	boolean open = true;

	private Server thisServer = this;

	public Server(int port, ExecutorService executor) {
		super();
		this.port = port;
		this.executor = executor;
	}

	public void run() {
		//clientsSyncd = Collections.synchronizedList(clients);
		
		MessageCenter messageCenter = new MessageCenter(thisServer);
		
		executor.execute(messageCenter);
		
		log.info("server started");
		ServerSocket ss;
		try {
			ss = new ServerSocket(port);
			while (true) {
				Thread.sleep(500);
				Socket socket = ss.accept();
				ClientHandler handler = new ClientHandler(socket, messageCenter);
				executor.execute(handler);

			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
