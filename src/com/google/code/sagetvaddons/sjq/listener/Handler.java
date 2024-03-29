/*
 *      Copyright 2010 Battams, Derek
 *       
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */
package com.google.code.sagetvaddons.sjq.listener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.log4j.Logger;


/**
 * @author dbattams
 *
 */
final public class Handler implements Runnable {

	static private final String CMD_QUIT = "QUIT";
	static public final ThreadLocal<SocketDetails> SOCKET_DETAILS = new ThreadLocal<SocketDetails>();
	
	private Socket sock;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private String cmdPkg;
	private Logger log;
	private String logPkg;

	Handler(Socket sock, String cmdPkg, String logPkg) throws IOException {
		this.sock = sock;
		this.out = new ObjectOutputStream(sock.getOutputStream());
		this.out.flush();
		this.in = new ObjectInputStream(sock.getInputStream());
		this.cmdPkg = cmdPkg;
		this.logPkg = logPkg;
		log = Logger.getLogger(logPkg + "." + Handler.class.getSimpleName());
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			SOCKET_DETAILS.set(new SocketDetails(sock.getLocalAddress().getHostAddress(), sock.getLocalPort(), sock.getInetAddress().getHostAddress(), sock.getPort()));
			String cmdName = null;
			Command cmd = null;
			while(cmdName == null || !cmdName.toUpperCase().equals(CMD_QUIT)) {
				cmdName = in.readUTF();
				log.info("CMD: " + cmdName + " :: PEER: " + sock.getInetAddress() + ":" + sock.getPort());
				cmd = CommandFactory.get(cmdName, cmdPkg, in, out, logPkg);
				if(cmd != null) {
					if(!cmdName.toUpperCase().equals(CMD_QUIT)) {
						out.writeUTF(NetworkAck.OK);
						out.flush();
					}
					cmd.execute();
				} else {
					out.writeUTF(NetworkAck.ERR + "Unrecognized command [" + cmdName + "]");
					out.flush();
				}
			}
		} catch (IOException e) {
			log.error("IOError: " + SOCKET_DETAILS.get().getRemoteAddress() + ":" + SOCKET_DETAILS.get().getRemotePort(), e);
		} finally {
			try {
				if(in != null)
					in.close();
				if(out != null)
					out.close();
				if(sock != null)
					sock.close();
			} catch(IOException e) {
				log.error("IOError", e);
			}
		}
	}
}
