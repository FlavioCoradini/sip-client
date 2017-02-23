package splitlibraries;


/*
 * @(#)AVTransmit2.java	1.4 01/03/13
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

import java.io.IOException;
import java.net.InetAddress;

import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.Time;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.protocol.SourceCloneable;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;



public class AVTransmit2 {

	// Input MediaLocator
	// Can be a file or http or capture source
	private DataSource ds;
	private String ipAddress;
	private int portBase;
	private AudioFormat audioFormat;
	private String myName;

	public Processor processor = null;
	private RTPManager rtpMgrs[];
	private DataSource dataOutput = null;
	public SendStream sendStream;
	private boolean mediaTransmitting = true;

	public AVTransmit2(DataSource ds,
			String ipAddress,
			int pb,
			AudioFormat format,
			String myName) {

		this.ds = ds;
		this.ipAddress = ipAddress;
		this.portBase = pb;
		this.audioFormat = format;
		this.myName = myName;
	}
	
	public void start() {
		String result = new String();
		result = this.startTransmiter();
		// result will be non-null if there was an error. The return
		// value is a String describing the possible error. Print it.
		if (result != null) {
			System.out.println("Error : " + result);
			System.exit(0);
		}

		System.out.println("Start transmission from " + myName);
		
	}

	public SendStream getSendStream () {
		return sendStream;
	}

	/**
	 * Starts the transmission. Returns null if transmission started ok.
	 * Otherwise it returns a string with the reason why the setup failed.
	 */
	public synchronized String startTransmiter() {
		String result;

		// Create a processor for the specified media locator
		// and program it to output JPEG/RTP
		result = createProcessor();
		if (result != null) {
			processor.deallocate();
			processor.close();
			return result;

		}
			
		// Create an RTP session to transmit the output of the
		// processor to the specified IP address and port no.
		result = createTransmitter();
		if (result != null) {
			processor.deallocate();
			processor.close();
			processor = null;
			return result;
		}

		// Start the transmission
		processor.start();

		return null;
	}

	/**
	 * Stops the transmission if already started
	 */
	public void stopTransmiter() {

		synchronized (this) {
			if (processor != null) {
				if (mediaTransmitting) {
					System.out.println("ainda estou transmitindo...");
				} else {
					System.out.println("Acabou a mensagem ...");
				}
				processor.stop();
				processor.close();
				//processor = null;
				for (int i = 0; i < rtpMgrs.length; i++) {
					rtpMgrs[i].removeTargets( "Session ended.");
					rtpMgrs[i].dispose();
				}
				System.out.println("Transmission ended from " + myName);
			}
		}
	}

	private String createProcessor() {
		DataSource clonedDataSource;
		if (processor== null) {
		// Try to create a processor to handle the input media locator
			clonedDataSource = ((SourceCloneable) ds).createClone();
			
			if (ds == null) {
			    System.err.println("Cannot clone the given DataSource");
			    System.exit(0);
			}
			try {
				processor = javax.media.Manager.createProcessor(clonedDataSource);
			} catch (NoProcessorException npe) {
				return "Couldn't create processor";
			} catch (IOException ioe) {
				return "IOException creating processor";
			} 

			// Wait for it to configure
			boolean result = waitForState(processor, Processor.Configured);
			if (result == false)
				return "Couldn't configure processor";

			// Get the tracks from the processor
			TrackControl [] tracks = processor.getTrackControls();
	
			// Do we have atleast one track?
			if (tracks == null || tracks.length < 1)
				return "Couldn't find tracks in processor";

			// Set the output content descriptor to RAW_RTP
			// This will limit the supported formats reported from
			// Track.getSupportedFormats to only valid RTP formats.		
			ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
			processor.setContentDescriptor(cd);

			Format supported[];
			Format chosen = null;

			// Program the tracks
			supported = tracks[0].getSupportedFormats();
			
			// We've set the output content to the RAW_RTP.
			// So all the supported formats should work with RTP.
			// We'll just pick the first one.

			if (supported.length > 0) {
				for (int i=0; i <= supported.length-1; i++) {
					if (supported[i].matches(audioFormat)) 
						chosen = supported[i];
				}
				if (chosen == null)
					chosen = supported[3];
				tracks[0].setFormat(chosen);
				//System.err.println("Track 0 is set to transmit as:" + chosen);
			} else
				tracks[0].setEnabled(false);

			// Realize the processor. This will internally create a flow
			// graph and attempt to create an output datasource for JPEG/RTP
			// audio frames.
			result = waitForState(processor, Controller.Realized);
			if (result == false)
				return "Couldn't realize processor";

			// Get the output data source of the processor
			dataOutput = processor.getDataOutput();
			//return null;
		}
		return null;
	}
    

	/**
	 * Use the RTPManager API to create sessions for each media 
	 * track of the processor.
	 */
	private String createTransmitter() {

		// Cheated.  Should have checked the type.
		PushBufferDataSource pbds = (PushBufferDataSource)dataOutput;
		PushBufferStream pbss[] = pbds.getStreams();

		rtpMgrs = new RTPManager[pbss.length];
		SessionAddress localAddr, destAddr;
		InetAddress ipAddr;
		int port;
		for (int i = 0; i < pbss.length; i++) {
			try {
				rtpMgrs[i] = RTPManager.newInstance();	    

				// The local session address will be created on the
				// same port as the the target port. This is necessary
				// if you use AVTransmit2 in conjunction with JMStudio.
				// JMStudio assumes -  in a unicast session - that the
				// transmitter transmits from the same port it is receiving
				// on and sends RTCP Receiver Reports back to this port of
				// the transmitting host.

				port = portBase;
				ipAddr = InetAddress.getByName(ipAddress);

				
				
				//localAddr = new SessionAddress( InetAddress.getLocalHost(), port);

				
				
				// Mundança para que não ocorra conflito de porta em testes no mesmo computador
				
				localAddr = new SessionAddress( ipAddr,	port + 5);

				destAddr = new SessionAddress( ipAddr, port);

				
				
				
				
				rtpMgrs[i].initialize( localAddr);

				rtpMgrs[i].addTarget( destAddr);

				
				
				System.err.println( "TRANSMITTER:  Created RTP session: " + ipAddress + " " + port);

				
				
				sendStream = rtpMgrs[i].createSendStream(dataOutput, i);
				sendStream.start();
			} catch (Exception  e) {
				return e.getMessage();
			}
		}

		return null;
	}

	/****************************************************************
	 * Convenience methods to handle processor's state changes.
	 ****************************************************************/

	private Integer stateLock = new Integer(0);
	private boolean failed = false;

	Integer getStateLock() {
		return stateLock;
	}

	void setFailed() {
		failed = true;
	}

	private synchronized boolean waitForState(Processor p, int state) {
		p.addControllerListener(new StateListener());
		failed = false;

		// Call the required method on the processor
		if (state == Processor.Configured) {
			p.configure();
		} else if (state == Processor.Realized) {
			p.realize();
		}

		// Wait until we get an event that confirms the
		// success of the method, or a failure event.
		// See StateListener inner class
		while (p.getState() < state && !failed) {
			synchronized (getStateLock()) {
				try {
					getStateLock().wait();
				} catch (InterruptedException ie) {
					return false;
				}
			}
		}

		if (failed)
			return false;
		else
			return true;
	}

	/****************************************************************
	 * Inner Classes
	 ****************************************************************/

	class StateListener implements ControllerListener {

		public void controllerUpdate(ControllerEvent ce) {

			// If there was an error during configure or
			// realize, the processor will be closed
			if (ce instanceof ControllerClosedEvent)
				setFailed();

			// All controller events, send a notification
			// to the waiting thread in waitForState method.
			if (ce instanceof ControllerEvent) {
				synchronized (getStateLock()) {
					getStateLock().notifyAll();
				}
			}
			if (ce instanceof EndOfMediaEvent) {
				mediaTransmitting = false;
				stopTransmiter();
			}
			
		}
	}

}
