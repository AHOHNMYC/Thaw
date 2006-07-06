package thaw.fcp;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Observable;

/* Should be the only real dep of the FCP package */
import thaw.core.Logger;


/**
 * This object manages directly the socket attached to the node.
 * After being instanciated, you should commit it to the FCPQueryManager, and then
 * commit the FCPQueryManager to the FCPQueueManager.
 * Call observer when connected / disconnected.
 */
public class FCPConnection extends Observable {

	private String nodeAddress = null;
	private int port = 0;

	private Socket socket = null;
	private InputStream in = null;
	private OutputStream out = null;

	private BufferedReader reader = null;

	/** If == 1, then will print on stdout
	 * all fcp input / output.
	 */
	private final static int DEBUG_MODE = 1;

	/**
	 * Don't connect. Call connect() for that.
	 */
	public FCPConnection(String nodeAddress,
			     int port)
	{
		if(DEBUG_MODE == 1) {
			Logger.notice(this, "DEBUG_MODE ACTIVATED");
		}

		setNodeAddress(nodeAddress);
		setNodePort(port);
	}


	/**
	 * You will probably have to use resetQueue() from the FCPQueueManager after using this function.
	 */
	public void setNodeAddress(String nodeAddress) {
		this.nodeAddress = nodeAddress;
	}

	/**
	 * You will probably have to use resetQueue() from the FCPQueueManager after using this function.
	 */
	public void setNodePort(int port) {
		this.port = port;
	}
	
	
	public void disconnect() {
		try {
			socket.close();
		} catch(java.io.IOException e) {
			Logger.warning(this, "Unable to close cleanly the connection : "+e.toString());
		}

		socket = null;
		in = null;
		out = null;

		setChanged();
		notifyObservers();
	}


	/**
	 * If already connected, disconnect before connecting.
	 * @return true if successful
	 */
	public boolean connect() {
		if(nodeAddress == null || port == 0) {
			Logger.warning(this, "Address or port not defined ! Unable to connect\n");
			return false;
		}
		
		if(socket != null && !socket.isClosed())
			disconnect();

		try {
			socket = new Socket(nodeAddress, port);

		} catch(java.net.UnknownHostException e) {
			Logger.error(this, "Error while trying to connect to "+nodeAddress+":"+port+" : "+
				     e.toString());
			socket = null;
			return false;
		} catch(java.io.IOException e) {
			Logger.error(this, "Error while trying to connect to "+nodeAddress+":"+port+" : "+
				     e.toString());
			socket = null;
			return false;
		}
		
		if(!socket.isConnected()) {
			Logger.warning(this, "Unable to connect, but no exception ?! WTF ?\n");
			Logger.warning(this, "Will try to continue ...\n");
		}
		
		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch(java.io.IOException e) {
			Logger.error(this, "Socket and connection established, but unable to get in/output streams ?! : "+e.toString());
			return false;
		}

		reader = new BufferedReader(new InputStreamReader(in));

		setChanged();
		notifyObservers();
		
		return true;
	}

	
	public boolean isConnected() {
		if(socket == null)
			return false;
		else
			return socket.isConnected();
	}

	

	public boolean write(String toWrite) {
		Logger.asIt(this, "Thaw >>> Node :");
		Logger.asIt(this, toWrite);


		if(out != null && socket != null && socket.isConnected()) {
			try {
				out.write(toWrite.getBytes());
			} catch(java.io.IOException e) {
				Logger.warning(this, "Unable to write() on the socket ?! : "+ e.toString());
				return false;
			}
		} else {
			Logger.warning(this, "Cannot write if disconnected !\n");
			return false;
		}
		return true;
	}

	/**
	 * Read a line.
	 * @return null if error
	 */
	public String readLine() {
		String result;
		
		if(in != null && socket != null && socket.isConnected()) {
			try {
				/*
				reader = new BufferedReader(new InputStreamReader(in));
				result = reader.readLine();
				*/
				
				result = reader.readLine();

				Logger.asIt(this, "Thaw <<< Node : "+result);
				
				return result;

			} catch (java.io.IOException e) {
				Logger.error(this, "Unable to read() on the socket ?! : "+e.toString());
				return null;
			}
		} else {
			Logger.warning(this, "Cannot read if disconnected => null");
		}

		return null;
	}


	/**
	 * Use this when you want to fetch the data still waiting on the socket.
	 */
	public InputStream getInputStream() {
		return in;
	}


	/**
	 * Use this when you want to send raw data.
	 */
	public OutputStream getOutputStream() {
		return out;
	}

}
