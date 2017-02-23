package com.sipagent;

import gov.nist.javax.sip.header.Allow;
import gov.nist.javax.sip.header.Authorization;
import gov.nist.javax.sip.header.HeaderFactoryImpl;
import gov.nist.javax.sip.header.Supported;
import gov.nist.javax.sip.header.ims.PPreferredIdentityHeader;
import gov.nist.javax.sip.header.ims.Privacy;
import jade.core.Agent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.format.*;
import javax.media.NoProcessorException;
import javax.media.Processor;
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
import javax.media.EndOfMediaEvent;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory; 	//Use to access the SIP API.
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;		//The SIP stack.
import javax.sip.TimeoutEvent;
import javax.sip.TransactionDoesNotExistException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import splitlibraries.AVReceive2;
import splitlibraries.AVTransmit2;
import splitlibraries.RTPExport;
import splitlibraries.SdpInfo;
import splitlibraries.SdpManager;
import splitlibraries.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Calendar;

import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import jade.core.ContainerID;
import jade.core.Location;
import jade.osgi.service.runtime.JadeRuntimeService;
import jade.wrapper.AgentController;


public class ClientCallee extends Agent implements SipListener {
	
	private static final long serialVersionUID = 1L;
	private SipStack mySipStack;
	private ListeningPoint myListeningPoint;
	private SipProvider mySipProvider;			//Used to send SIP messages.
	private MessageFactory myMessageFactory; 	//Used to create SIP message factory.
	private HeaderFactory myHeaderFactory;		//Used to create SIP headers.
	private AddressFactory myAddressFactory;	//Used to create URIs.
	private Properties myProperties;			//Other properties.

	private ContactHeader myContactHeader;
	private AuthorizationHeader myWWWAuthenticateHeader;
	private ViaHeader myViaHeader;
	private RouteHeader myRouteHeader;
	private Address fromAddress;
	private Dialog myDialog;
	private ClientTransaction myClientTransaction;
	private ServerTransaction myServerTransaction;
	private int status;
	private String myIP;
	private SdpManager mySdpManager;
	
	private SdpInfo answerInfo;
	private SdpInfo offerInfo;
	
	private String myName;
	private String myUserID;
	private String myPassword;
	private SipFactory mySipFactory;
	private Integer myPort;
	private String myServer;
	private String myProxy;
	private int myAudioPort;
	private DataSource myDataSource;
	private AudioFormat afmt;
	private int	myAudioFormat;
	private AVTransmit2 at;
    private Boolean Unregistring = false;
    private int totalVotos = 0;    
    private Timer timer;
    
	static final int YES=0;
	static final int NO=1;
	static final int SEND_MESSAGE=2;

	static final int UNREGISTERED=-2;
	static final int REGISTERING=-1;

	static final int IDLE=0;
	static final int WAIT_PROV=1;
	static final int WAIT_FINAL=2;
	static final int ESTABLISHED=4;
	static final int RINGING=5;
	static final int WAIT_ACK=6;
	
	
	public Connection myConnection;
	public String URL ="jdbc:mysql://172.16.28.3:3306/";
	public String usuario = "televoto"; 
	public String password = "w3w4x3f9";  
	public String DRIVER ="com.mysql.jdbc.Driver";
	
	
	
	
	private Log logger = (Log) LogFactory.getLog(ClientCallee.class);
	
	protected void takeDown() {
		

		System.out.println("Finalizando " + myUserID);
		Unregistring = true;

		try {
			DatagramSocket socket = new DatagramSocket();
			InetAddress destino = InetAddress.getByName("127.0.0.1");
			String mensagem = myName + ";" + totalVotos;
			byte[] dados = mensagem.getBytes();
			int porta = 9999;
			DatagramPacket pacote = new DatagramPacket(dados, dados.length, destino, porta);
			socket.send(pacote);
			} catch (SocketException e) { 
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace(); 
		}
		setup();
		
	}


	class KeepAlive extends TimerTask {
		
		public KeepAlive(){			
		}
		
		public void run() {
			
			if (!Unregistring) {
			
				try {
											
					myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);
	
					fromAddress=myAddressFactory.createAddress("<sip:"+ myUserID +">");
					Address registrarAddress=myAddressFactory.createAddress("sip:"+myServer);
					Address registerToAddress = fromAddress;
					Address registerFromAddress=fromAddress;
	
					ToHeader myToHeader = myHeaderFactory.createToHeader(registerToAddress, null);
					FromHeader myFromHeader = myHeaderFactory.createFromHeader(registerFromAddress, "647554");
	
					ArrayList myViaHeaders = new ArrayList();
					myViaHeaders.add(myViaHeader);
	
					MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
					Random random = new Random();
					CSeqHeader myCSeqHeader = myHeaderFactory.createCSeqHeader(random.nextInt(1000) * 1L, "REGISTER");
					
					CallIdHeader myCallIDHeader = mySipProvider.getNewCallId();
					SipURI myRequestURI = (SipURI) registrarAddress.getURI();
					
					//Create SIP Request
					Request myRegisterRequest = myMessageFactory.createRequest(myRequestURI,"REGISTER", myCallIDHeader, myCSeqHeader, myFromHeader, myToHeader,myViaHeaders, myMaxForwardsHeader);
					
					//Expires
					ExpiresHeader myExpiresHeader;
					myExpiresHeader = myHeaderFactory.createExpiresHeader(3600);
					myRegisterRequest.addHeader(myExpiresHeader);
					
					//Allow
					Allow myAllow = new Allow();
					myAllow.setMethod("INVITE, ACK, CANCEL, BYE, MESSAGE, OPTIONS, NOTIFY, PRACK, UPDATE, REFER");
					myRegisterRequest.addHeader(myAllow);
					
					//Contact
					Address contactAddress = myAddressFactory.createAddress("sip:"+ myName+ '@' +myIP+":"+myPort + ";transport=udp");
					myContactHeader = myHeaderFactory.createContactHeader(contactAddress);
					myRegisterRequest.addHeader(myContactHeader);
					
					//Authorization
					Authorization myAuthorization = new Authorization();
					myAuthorization.setScheme("Digest");
					myAuthorization.setUsername(myUserID);
					myAuthorization.setRealm(myServer);
					myAuthorization.setNonce("");
					myAuthorization.setURI( myRegisterRequest.getRequestURI() ) ;
					myAuthorization.setResponse("");
					myRegisterRequest.addHeader(myAuthorization);
	
					//PPreferredIdentity	
					HeaderFactoryImpl myHeaderFactoryImpl = new HeaderFactoryImpl();
					PPreferredIdentityHeader myPPreferredIdentityHeader = myHeaderFactoryImpl.createPPreferredIdentityHeader(myAddressFactory.createAddress("sip:"+ myName+ '@' +myServer));
					myRegisterRequest.addHeader(myPPreferredIdentityHeader);
					
					//Privacy
					Privacy myPrivacy = new Privacy("none");
					myRegisterRequest.addHeader(myPrivacy);
					
					//Supported
					Supported mySupported = new Supported("path");
					myRegisterRequest.addHeader(mySupported);
					
					//Envia requisição SIP
					myClientTransaction = mySipProvider.getNewClientTransaction(myRegisterRequest);
					myClientTransaction.sendRequest();
	
					logger.info(myRegisterRequest.toString());
					//System.out.println(">>> " + myRegisterRequest.toString());
					status=REGISTERING;
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void stopTransmission() {
		//System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.logic | CLASS: ClientReceiver | METOD: stopTransmission"); //By Ju
		//System.out.println(ss.getSourceTransmissionStats().getBytesTransmitted());

		Request myBye = null;
		try {
			myBye = myDialog.createRequest("BYE");
		} catch (SipException e) {
			e.printStackTrace();
		}
		myBye.addHeader(myContactHeader);
		try {
			myClientTransaction= mySipProvider.getNewClientTransaction(myBye);
		} catch (TransactionUnavailableException e) {
			e.printStackTrace();
		}
		try {
			myDialog.sendRequest(myClientTransaction);
			logger.info(">>> "+myClientTransaction.getRequest().toString());
		} catch (TransactionDoesNotExistException e) {
			e.printStackTrace();
		} catch (SipException e) {
			e.printStackTrace();
		}
		System.out.println("Agente atendedor " + myName + " livre ...");
		status = IDLE;	
	}

	public void setup () {
		

		ConfigCalleeFile ConfigReceiverFile = new ConfigCalleeFile();
		
		try {
			
			if (!Unregistring) { //Se é um Registro
				
		
				myDataSource =  ConfigReceiverFile.ds;
				
	
				myName = ConfigReceiverFile.myUserID;
				myUserID = ConfigReceiverFile.myUserID + "@"+ ConfigReceiverFile.myServerDomain;
				myPassword = ConfigReceiverFile.myPassword;
				myIP = ConfigReceiverFile.myIp;
				myPort = ConfigReceiverFile.myPort;
				myAudioPort= ConfigReceiverFile.myAudioPort;
				myAudioFormat = ConfigReceiverFile.myAudioFormat;
				myServer= ConfigReceiverFile.myServerDomain;
				myProxy = ConfigReceiverFile.myProxy + "/UDP";	
				
				
			}
			
			switch (myAudioFormat) {
			case 0:
				afmt = new AudioFormat(AudioFormat.DVI_RTP, 8000, 4, 1);
				break;
			case 1:
				afmt = new AudioFormat(AudioFormat.DVI_RTP, 11025, 4, 1);
				break;
			case 2:
				afmt = new AudioFormat(AudioFormat.DVI_RTP, 22050, 4, 1);
				break;
			case 3:
				afmt = new AudioFormat(AudioFormat.ULAW_RTP, 8000, 8, 1);
				break;
			case 4:
				afmt = new AudioFormat(AudioFormat.GSM_RTP, 8000, 8, 1);
				break;
			}
			
			
			
			mySipFactory = SipFactory.getInstance();
			mySipFactory.setPathName("gov.nist");
			mySdpManager=new SdpManager();
			answerInfo=new SdpInfo();
			offerInfo=new SdpInfo();

			myProperties = new Properties();
			myProperties.setProperty("javax.sip.STACK_NAME", myName);
			myProperties.setProperty("javax.sip.OUTBOUND_PROXY", myProxy); //Proxy
			mySipStack = mySipFactory.createSipStack(myProperties);
			myMessageFactory = mySipFactory.createMessageFactory();
			myHeaderFactory = mySipFactory.createHeaderFactory();
			myAddressFactory = mySipFactory.createAddressFactory();
			
			if (!Unregistring) { //Se é um registro
				ListIterator provider = (ListIterator) mySipStack.getSipProviders();
				int i;
				for ( i=0 ; provider.hasNext() ; ++i ) provider.next();
				if (i > 0) {
					Object sipProvider = (SipProvider) provider.previous();
					mySipProvider = ((SipProvider) sipProvider);
					myListeningPoint = mySipProvider.getListeningPoint();
				} else {
					myListeningPoint = mySipStack.createListeningPoint(myIP, myPort, "udp");
					mySipProvider = mySipStack.createSipProvider(myListeningPoint);
					mySipProvider.addSipListener(this);
				}
			}
					
			myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);

			fromAddress=myAddressFactory.createAddress("<sip:"+ myUserID +">");
			Address registrarAddress=myAddressFactory.createAddress("sip:"+myServer);
			Address registerToAddress = fromAddress;
			Address registerFromAddress=fromAddress;

			ToHeader myToHeader = myHeaderFactory.createToHeader(registerToAddress, null);
			FromHeader myFromHeader = myHeaderFactory.createFromHeader(registerFromAddress, "647554");

			ArrayList myViaHeaders = new ArrayList();
			myViaHeaders.add(myViaHeader);

			MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
			Random random = new Random();
			CSeqHeader myCSeqHeader = myHeaderFactory.createCSeqHeader(random.nextInt(1000) * 1L, "REGISTER");
			
			CallIdHeader myCallIDHeader = mySipProvider.getNewCallId();
			SipURI myRequestURI = (SipURI) registrarAddress.getURI();
			
			//Create SIP Request
			Request myRegisterRequest = myMessageFactory.createRequest(myRequestURI,"REGISTER", myCallIDHeader, myCSeqHeader, myFromHeader, myToHeader,myViaHeaders, myMaxForwardsHeader);
			
			//Expires
			ExpiresHeader myExpiresHeader;
			if (Unregistring) {
				//myContactHeader.setExpires(0);
				myExpiresHeader = myHeaderFactory.createExpiresHeader(0);
			} else {
				myExpiresHeader = myHeaderFactory.createExpiresHeader(3600);
			}
			myRegisterRequest.addHeader(myExpiresHeader);
			
			//Allow
			Allow myAllow = new Allow();
			myAllow.setMethod("INVITE, ACK, CANCEL, BYE, MESSAGE, OPTIONS, NOTIFY, PRACK, UPDATE, REFER");
			myRegisterRequest.addHeader(myAllow);
			
			//Contact
			Address contactAddress = myAddressFactory.createAddress("sip:"+ myName+ '@' +myIP+":"+myPort + ";transport=udp");
			myContactHeader = myHeaderFactory.createContactHeader(contactAddress);
			myRegisterRequest.addHeader(myContactHeader);
			
			//Authorization
			Authorization myAuthorization = new Authorization();
			myAuthorization.setScheme("Digest");
			myAuthorization.setUsername(myUserID);
			myAuthorization.setRealm(myServer);
			myAuthorization.setNonce("");
			myAuthorization.setURI( myRegisterRequest.getRequestURI() ) ;
			myAuthorization.setResponse("");
			myRegisterRequest.addHeader(myAuthorization);

			//PPreferredIdentity	
			HeaderFactoryImpl myHeaderFactoryImpl = new HeaderFactoryImpl();
			PPreferredIdentityHeader myPPreferredIdentityHeader = myHeaderFactoryImpl.createPPreferredIdentityHeader(myAddressFactory.createAddress("sip:"+ myName+ '@' +myServer));
			myRegisterRequest.addHeader(myPPreferredIdentityHeader);
			
			//Privacy
			Privacy myPrivacy = new Privacy("none");
			myRegisterRequest.addHeader(myPrivacy);
			
			//Supported
			Supported mySupported = new Supported("path");
			myRegisterRequest.addHeader(mySupported);
			
			//Envia requisição SIP
			myClientTransaction = mySipProvider.getNewClientTransaction(myRegisterRequest);
			myClientTransaction.sendRequest();

			logger.info(myRegisterRequest.toString());
			//System.out.println(">>> " + myRegisterRequest.toString());
			status=REGISTERING;
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setOff(SipStack mySipStack){
		//System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.logic | CLASS: ClientReceiver | METOD: setOff"); //By Ju
		
		try{
			mySipProvider.removeSipListener(this);
			mySipProvider.removeListeningPoint(myListeningPoint);
			mySipStack.deleteListeningPoint(myListeningPoint);
			mySipStack.deleteSipProvider(mySipProvider);
			myListeningPoint=null;
			mySipProvider=null;
			mySipStack=null;
			//myRingTool=null;
			//System.out.println("Finalizado...");
		}
		catch(Exception e){}
	}
	public void SendMedia(String IP, int aport) {
		//System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.logic | CLASS: ClientReceiver | METOD: sendMedia"); //By Ju
		// Create a audio transmit object with the specified params.
		at = new AVTransmit2(myDataSource, IP, aport, afmt, myName);
		// Start the transmission
		at.start();
	}

	public synchronized void processRequest(RequestEvent requestReceivedEvent) {
		//System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.logic | CLASS: ClientReceiver | METOD: processRequest"); //By Ju
		
		String method;
		Request myRequest=requestReceivedEvent.getRequest();
		//System.out.println("<<< "+myRequest.toString());
		method = myRequest.getMethod();
		logger.info(">>> " + myRequest.toString());
		
		if (!method.equals("CANCEL")) {
			myServerTransaction=requestReceivedEvent.getServerTransaction();
		}

		
		try{			
			switch (status) {

			case IDLE:
				if (method.equals("INVITE")) {
					if (myServerTransaction == null) {
						myServerTransaction = mySipProvider.getNewServerTransaction(myRequest);
					}
					byte[] cont=(byte[]) myRequest.getContent();
					
					offerInfo=mySdpManager.getSdp(cont);

					answerInfo.IpAddress=myIP;
					answerInfo.aport=offerInfo.aport;
					answerInfo.aformat=offerInfo.aformat;

					//envio do PROVISIONAL 180
					Response myResponse=myMessageFactory.createResponse(180,myRequest);
					myResponse.addHeader(myContactHeader);
					ToHeader myToHeader = (ToHeader) myResponse.getHeader("To");
					myToHeader.setTag("454326");
					myServerTransaction.sendResponse(myResponse);
					myDialog=myServerTransaction.getDialog();
					logger.info(">>> "+myResponse.toString());
					
					status=RINGING;
					
					//inicio do envio do ACK ao cliente em confirmacao ao PROV_180 e envio do SDP
					Request originalRequest = myServerTransaction.getRequest();
					myResponse = myMessageFactory.createResponse(200, originalRequest);
					myToHeader = (ToHeader) myResponse.getHeader("To");
					myToHeader.setTag("454326");
					myResponse.addHeader(myContactHeader);

					//SEND ANSWER SDP
					ContentTypeHeader contentTypeHeader=myHeaderFactory.createContentTypeHeader("application","sdp");
					byte[] content=mySdpManager.createSdp(answerInfo);
					myResponse.setContent(content,contentTypeHeader);

					//aguardando ACK do cliente
					myServerTransaction.sendResponse(myResponse);
					myDialog = myServerTransaction.getDialog();

					logger.info(">>> " + myResponse.toString());
					//System.out.println(">>> " + myResponse.toString());
					status = WAIT_ACK;
				}
				if (method.equals("OPTIONS")) {
					if (myServerTransaction == null) {
						myServerTransaction = mySipProvider.getNewServerTransaction(myRequest);
					}
					Response myResponse;
					Request originalRequest = myServerTransaction.getRequest();
					myResponse = myMessageFactory.createResponse(200, originalRequest);
					myResponse.addHeader(myContactHeader);
					myServerTransaction.sendResponse(myResponse);
				}

				break;
			case ESTABLISHED:
				//revisar
				//adicionei para testar o problema de o asterisk nao desconctar o cliente.
				if (method.equals("INVITE")) {
					/*Response myResponse=myMessageFactory.createResponse(200,myRequest);
					myResponse.addHeader(myContactHeader);
					myServerTransaction.sendResponse(myResponse);
					logger.info(">>> " + myResponse.toString());*/
					//System.out.println(">>> " + myResponse.toString());
				}
				
				if (method.equals("ACK")) {
					logger.info("<<< " + myRequest.toString());

				}

				if (method.equals("BYE")) {
					
					at.stopTransmiter();
					
					Response myResponse=myMessageFactory.createResponse(200,myRequest);
					myResponse.addHeader(myContactHeader);
					myServerTransaction.sendResponse(myResponse);
					logger.info(">>> "+myResponse.toString());
					status=IDLE;
					
				}
				break;

			case RINGING:
				if (method.equals("CANCEL")) {
					ServerTransaction myCancelServerTransaction=requestReceivedEvent.getServerTransaction();
					Request originalRequest=myServerTransaction.getRequest();
					Response myResponse=myMessageFactory.createResponse(487,originalRequest);
					myServerTransaction.sendResponse(myResponse);
					Response myCancelResponse=myMessageFactory.createResponse(200,myRequest);
					myCancelServerTransaction.sendResponse(myCancelResponse);
					logger.info(">>> "+myResponse.toString());
					logger.info(">>> "+myCancelResponse.toString());
					status=IDLE;
				}
				break;

			case WAIT_ACK:
				if (method.equals("ACK")) {
					
					status=ESTABLISHED;
					
					// Envia o conteudo da ligacao do televoto
					// Por enquanto é simbólico
					// Mas é possível no Recorder adicionar duas seções
					// E gerar um único arquivo da ligação
					
					//SendMedia("172.16.28.1", 30000);
			
					
					// Inicia o Receptor do lado do servidor para gravar a ligação
					RecordCall(answerInfo, myRequest);
					
					
					
					
					
					
					
				}
				break;
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void processResponse(ResponseEvent responseReceivedEvent) {

		try{
			Response myResponse = responseReceivedEvent.getResponse();
			logger.info("<<< "+myResponse.toString());
			//System.out.println(myResponse.toString());
			ClientTransaction thisClientTransaction = responseReceivedEvent.getClientTransaction();
			if (Unregistring) {
				myClientTransaction = thisClientTransaction;
			} else {
				myClientTransaction = responseReceivedEvent.getClientTransaction();
			}
			int myStatusCode = myResponse.getStatusCode();
						
			switch(status){

			case WAIT_PROV:
				if (myStatusCode<200) {
					
					status=WAIT_FINAL;
					myDialog=thisClientTransaction.getDialog();
				}
				else if (myStatusCode<300) {
					myDialog=thisClientTransaction.getDialog();
					CSeqHeader originalCSeq = (CSeqHeader) myClientTransaction.getRequest().getHeader(CSeqHeader.NAME);
					long numseq = originalCSeq.getSeqNumber();
					Request myAck = myDialog.createAck(numseq);
					myAck.addHeader(myContactHeader);
					myDialog.sendAck(myAck);
					logger.info(">>> "+myAck.toString());
					status=ESTABLISHED;
					//LAST STEP IN SDP OFFER/ANSWER
					byte[] cont=(byte[]) myResponse.getContent();
					answerInfo=mySdpManager.getSdp(cont);
				}
				else {

					status=IDLE;
					CSeqHeader originalCSeq = (CSeqHeader) myClientTransaction.getRequest().getHeader(CSeqHeader.NAME);
					long numseq = originalCSeq.getSeqNumber();
					Request myAck = myDialog.createAck(numseq);
					myAck.addHeader(myContactHeader);
					myDialog.sendAck(myAck);
					logger.info(">>> "+myAck.toString());
				}
				break;

			case WAIT_FINAL:
				if (myStatusCode<200) {
					status=WAIT_FINAL;
					myDialog=thisClientTransaction.getDialog();
				}
				else if (myStatusCode<300) {
					status=ESTABLISHED;
					myDialog=thisClientTransaction.getDialog();
					CSeqHeader originalCSeq = (CSeqHeader) myClientTransaction.getRequest().getHeader(CSeqHeader.NAME);
					long numseq = originalCSeq.getSeqNumber();
					Request myAck = myDialog.createAck(numseq);
					myAck.addHeader(myContactHeader);
					myDialog.sendAck(myAck);
					logger.info(">>> "+myAck.toString());
					//LAST STEP IN SDP OFFER/ANSWER
					byte[] cont=(byte[]) myResponse.getContent();
					answerInfo=mySdpManager.getSdp(cont);
				}
				else {
					// Cancelando requisiÔøΩ‚Äπo ao cliente
					Request myCancelRequest = myClientTransaction.createCancel();
					ClientTransaction myCancelClientTransaction = mySipProvider.
							getNewClientTransaction(myCancelRequest);
					myCancelClientTransaction.sendRequest();
					logger.info(">>> " + myCancelRequest.toString());
					status = IDLE;
				}
				break;

			case REGISTERING:

				if (myStatusCode==200) {
					status=IDLE;
					if (!Unregistring) {
						System.out.println(myName + ": online");
						timer = new Timer();
						timer.schedule(new KeepAlive(),55*60*1000);//Re-register em 55 minutos
					} else {
						this.setOff(mySipStack);
					}
				}
				else {
					if (myStatusCode==403) {
						
						System.out.println("Problemas com credenciais!\n");
					} else 
					if (myStatusCode==401) {

						myName = (String) this.getLocalName();
						myUserID = (String) this.getLocalName() + "@" + myServer;

						Address contactAddress = myAddressFactory.createAddress("sip:"+ myName+ '@' + myIP+":"+myPort);
						myContactHeader = myHeaderFactory.createContactHeader(contactAddress);

						myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);

						fromAddress=myAddressFactory.createAddress("<sip:"+myUserID+">");

						Address registrarAddress=myAddressFactory.createAddress("sip:"+myServer);
						Address registerToAddress = fromAddress;
						Address registerFromAddress=fromAddress;

						ToHeader myToHeader = myHeaderFactory.createToHeader(registerToAddress, null);
						FromHeader myFromHeader = myHeaderFactory.createFromHeader(registerFromAddress, "647554");

						ArrayList myViaHeaders = new ArrayList();
						myViaHeaders.add(myViaHeader);

						CSeqHeader originalCSeq = (CSeqHeader) myClientTransaction.getRequest().getHeader(CSeqHeader.NAME);
						long numseq = originalCSeq.getSeqNumber();
						MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
						CSeqHeader myCSeqHeader = myHeaderFactory.createCSeqHeader(numseq + 1L, "REGISTER");
						
						CallIdHeader myCallID = (CallIdHeader) myClientTransaction.getRequest().getHeader(CallIdHeader.NAME);
						CallIdHeader myCallIDHeader = myCallID;
						SipURI myRequestURI = (SipURI) registrarAddress.getURI();
						Request myRegisterRequest = myMessageFactory.createRequest(myRequestURI,"REGISTER", myCallIDHeader, myCSeqHeader, myFromHeader, myToHeader,myViaHeaders, myMaxForwardsHeader);
						myRegisterRequest.addHeader(myContactHeader);
						
						//Expires				
						ExpiresHeader myExpiresHeader;
						if (Unregistring) {
							myExpiresHeader = myHeaderFactory.createExpiresHeader(0);
							myContactHeader.setExpires(0);
						} else {
							myExpiresHeader=myHeaderFactory.createExpiresHeader(3600);
						}
						myRegisterRequest.addHeader(myExpiresHeader);
						
						//Allow
						Allow myAllow = new Allow();
						myAllow.setMethod("INVITE, ACK, CANCEL, BYE, MESSAGE, OPTIONS, NOTIFY, PRACK, UPDATE, REFER");
						myRegisterRequest.addHeader(myAllow);
						
						//Privacy
						Privacy myPrivacy = new Privacy("none");
						myRegisterRequest.addHeader(myPrivacy);
						
						//PPreferredIdentity	
						HeaderFactoryImpl myHeaderFactoryImpl = new HeaderFactoryImpl();
						PPreferredIdentityHeader myPPreferredIdentityHeader = myHeaderFactoryImpl.createPPreferredIdentityHeader(myAddressFactory.createAddress("sip:"+ myName+ '@' +myServer));
						myRegisterRequest.addHeader(myPPreferredIdentityHeader);
						
						//Supported
						Supported mySupported = new Supported("path");
						myRegisterRequest.addHeader(mySupported);
						
						//Authentication
						myWWWAuthenticateHeader = Utils.makeAuthHeader(myHeaderFactory, myResponse, myRegisterRequest, myUserID, myPassword);
						myRegisterRequest.addHeader(myWWWAuthenticateHeader);

						//Envia requisição
						myClientTransaction = mySipProvider.getNewClientTransaction(myRegisterRequest);
						myClientTransaction.sendRequest();

						logger.info(">>> "+myRegisterRequest.toString());
						//System.out.println(">>> " + myRegisterRequest.toString());
						status=REGISTERING;
					}
				}
				break;

			}
		}catch(Exception excep){
			excep.printStackTrace();
		}
	}

	public void processTimeout(TimeoutEvent timeoutEvent) {
	}

	public void processTransactionTerminated(TransactionTerminatedEvent tevent) {

	}

	public void processDialogTerminated(DialogTerminatedEvent tevent) {

	}

	public void processIOException(IOExceptionEvent tevent) {

	}
	
	
	
	
	
	
	public void RecordCall(SdpInfo _answerInfo, Request _myRequest) {
		
		Object[] recorderConfig = new Object[5];
				
		String callId = _myRequest.getHeader("Call-ID").toString().replace("Call-ID: ", "").replace("\r\n", "");
		String from = _myRequest.getHeader("From").toString().replace("From: ", "").replace("\r\n", "").split(";")[0];
		String to = _myRequest.getHeader("To").toString().replace("To: ", "").replace("\r\n", "").split(";")[0];

		String session = _answerInfo.IpAddress + ":" + _answerInfo.aport + "/audio";
		
		recorderConfig[0] = callId;
		recorderConfig[1] = from;
		recorderConfig[2] = to;
		recorderConfig[3] = session;
		recorderConfig[4] = _answerInfo.aport;
		
		try {
			
			Runtime rt = Runtime.getRuntime();
			AgentContainer mainContainer = this.getContainerController();
			
			AgentController recorder = mainContainer.createNewAgent("RecorderAgent", "splitlibraries.RecordAgent", recorderConfig);
			
			recorder.start();
			
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}
	

}
