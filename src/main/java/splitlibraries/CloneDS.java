package splitlibraries;

import java.io.IOException;

import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSinkException;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.control.TrackControl;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.SourceCloneable;
import javax.media.rtp.RTPManager;

public class CloneDS {

	private MediaLocator locator;
	private Format format;
	private int Clones;
	private Processor processor = null;
	private RTPManager rtpMgrs[];
	private DataSource dataOutput = null;
	private DataSource dataSources[];
	private Processor myProcessor;
	private DataSink dataSink;
	private String IP;
	private int port;

	private Integer stateLock = new Integer(0);
	private boolean failed = false;

	public CloneDS(MediaLocator locator, Format format, int clones) {
		this.locator = locator;
		this.format = format;
		this.Clones = clones;
		dataSources = new DataSource[Clones+1];

	}
	
	public CloneDS(MediaLocator locator, String IP, int port, Format format) {
		this.locator = locator;
		this.IP = IP;
		this.port = port;
		this.format = format;	
	}

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
		}
	}

	public String createProcessor() {
		DataSource cloneableDataSource = null;
		
		if (locator == null)
			return "Locator is null";

		DataSource ds;
		try {
			ds = javax.media.Manager.createDataSource(locator);
		} catch (Exception e) {
			return "Couldn't create DataSource";
		}

		// Try to create a processor to handle the input media locator
		try {
			processor = javax.media.Manager.createProcessor(ds);
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
		TrackControl[] tracks = processor.getTrackControls();

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

		// Program the tracks.
		Format format = tracks[0].getFormat();
		if (tracks[0].isEnabled()) {

			supported = tracks[0].getSupportedFormats();

			if (supported.length > 0) {
				for (int i = 0; i <= supported.length-1; i++)
					if (format.equals(supported[i]))
						chosen = format;
				if (chosen == null)
					chosen = supported[3];
				tracks[0].setFormat(chosen);
				System.err.println("Track 0 is set to transmit as:");
				System.err.println("  " + chosen);
			} else
				tracks[0].setEnabled(false);
		} else
			tracks[0].setEnabled(false);

		// Realize the processor. This will internally create a flow
		// graph and attempt to create an output datasource for JPEG/RTP
		// audio frames.
		result = waitForState(processor, Controller.Realized);
		if (result == false)
			return "Couldn't realize processor";
		// Get the output data source of the processor
		myProcessor = processor;
		dataOutput = processor.getDataOutput();
        cloneableDataSource = Manager.createCloneableDataSource(dataOutput); 
		
		for (int i=0; i <= Clones; i++) 
            dataSources[i] = ((SourceCloneable)cloneableDataSource).createClone();
    	
		return null;
	}
	
	public String CreateDataSink() {
		DataSource cloneableDataSource = null;
		
		if (locator == null)
			return "Locator is null";

		DataSource ds;
		try {
			ds = javax.media.Manager.createDataSource(locator);
		} catch (Exception e) {
			return "Couldn't create DataSource";
		}

		// Try to create a processor to handle the input media locator
		try {
			processor = javax.media.Manager.createProcessor(ds);
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
		TrackControl[] tracks = processor.getTrackControls();

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

		// Program the tracks.
		Format format = tracks[0].getFormat();
		if (tracks[0].isEnabled()) {

			supported = tracks[0].getSupportedFormats();

			if (supported.length > 0) {
				for (int i = 0; i <= supported.length-1; i++)
					if (format.equals(supported[i]))
						chosen = format;
				if (chosen == null)
					chosen = supported[3];
				tracks[0].setFormat(chosen);
				System.err.println("Track 0 is set to transmit as:");
				System.err.println("  " + chosen);
			} else
				tracks[0].setEnabled(false);
		} else
			tracks[0].setEnabled(false);

		// Realize the processor. This will internally create a flow
		// graph and attempt to create an output datasource for JPEG/RTP
		// audio frames.
		result = waitForState(processor, Controller.Realized);
		if (result == false)
			return "Couldn't realize processor";
		// Get the output data source of the processor
		myProcessor = processor;
		dataOutput = processor.getDataOutput();
		
		String url= "rtp://"+ IP + ":" + port  + "/audio/1";
		 
        MediaLocator m = new MediaLocator(url);
		
		try {
			dataSink = Manager.createDataSink(dataOutput, m);
		} catch (NoDataSinkException e) {
			e.printStackTrace();
			return "Error creating DataSink for MediaLocator.";
		}
		
		return null;
	}
	
	public DataSource[] getDataSources() {
		return this.dataSources;
	}
	
	public Processor getProcessor() {
		return this.myProcessor;
	}
	public DataSink getDataSink() {
		return this.dataSink;
	}
}
