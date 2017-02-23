package com.sipagent;

import gov.nist.javax.sip.header.Allow;
import gov.nist.javax.sip.header.Authorization;
import gov.nist.javax.sip.header.HeaderFactoryImpl;
import gov.nist.javax.sip.header.Supported;
import gov.nist.javax.sip.header.ims.PPreferredIdentityHeader;
import gov.nist.javax.sip.header.ims.Privacy;
import jade.core.Agent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.MediaLocator;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.rtp.SendStream;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
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
import org.apache.log4j.PropertyConfigurator;

import splitlibraries.AVTransmit2;
import splitlibraries.SdpInfo;
import splitlibraries.SdpManager;
import splitlibraries.Utils;


public class ClientAgent extends Agent implements SipListener {
	
	private static final long serialVersionUID = 1L;
	private SipStack mySipStack;
	private ListeningPoint myListeningPoint;
	private SipProvider mySipProvider;
	private MessageFactory myMessageFactory;
	private HeaderFactory myHeaderFactory;
	private AddressFactory myAddressFactory;
	private Properties myProperties;
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
	private String mySipAlias;
	private String myName;
	private String myUserID;
	private String myPassword;
	private SipFactory mySipFactory;
	private Integer myPort;
	private String myProxyIP;
	private Integer myProxyPort;
	private String myServer;
	private int myAudioPort;
	private DataSource myDataSource;
	private AudioFormat afmt;
	private TimerTask tasknew = new KeepAlive();
	private Timer myTimer = new Timer(); 
	private AVTransmit2 at;
	private Boolean Unregistring = false;
	private String destination;
	private int redial = 0;
	private int dialTimes = 0;
	private int ack = 0;
	private String useQueue = new String();

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

	private Log logger = (Log) LogFactory.getLog(ClientAgent.class);
	
	
	private MediaLocator ml;
	
	
	 

	protected void takeDown() {
		///System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.client.logic | CLASS: ClientAgent | METOD: takeDown ");//By Ju
		//System.out.println("Finalizando " + myUserID);
		Unregistring = true;
		setup();
	}

	class MyTimerTask extends TimerTask {
		ClientAgent myListener;
		public MyTimerTask (ClientAgent myListener){
			///System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.client.logic | CLASS: ClientAgent:MyTimerTask | METOD: MyTimerTask ");//By Ju
			this.myListener=myListener;
		}
		public void run() {
			///System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.client.logic | CLASS: ClientAgent:MyTimerTask | METOD: run ");//By Ju
			try{
				Request myBye = myListener.myDialog.createRequest("BYE");
				myBye.addHeader(myListener.myContactHeader);
				myListener.myClientTransaction = myListener.mySipProvider.getNewClientTransaction(myBye);
				myListener.myDialog.sendRequest(myListener.myClientTransaction); 
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
	}

	class KeepAlive extends TimerTask {
		public void run() {
			///System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.client.logic | CLASS: ClientAgent:KeepAlive | METOD: run ");//By Ju
			try {

				Address contactAddress = myAddressFactory.createAddress("sip:"+myIP+":"+myPort);
				myContactHeader = myHeaderFactory.createContactHeader(contactAddress);

				myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);

				Address routeAddress = myAddressFactory.createAddress("sip:"+myServer+";lr");
				myRouteHeader= myHeaderFactory.createRouteHeader(routeAddress);

				fromAddress=myAddressFactory.createAddress(myName + " <sip:"+ myUserID +">");
				Address registrarAddress=myAddressFactory.createAddress("sip:"+myServer);
				Address registerToAddress = fromAddress;
				ToHeader myToHeader = myHeaderFactory.createToHeader(registerToAddress, null);
				Address registerFromAddress=fromAddress;

				FromHeader myFromHeader = myHeaderFactory.createFromHeader(registerFromAddress, "647554");

				ArrayList myViaHeaders = new ArrayList();
				myViaHeaders.add(myViaHeader);

				MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
				CSeqHeader myCSeqHeader = myHeaderFactory.createCSeqHeader(1L,"REGISTER");
				ExpiresHeader myExpiresHeader=myHeaderFactory.createExpiresHeader(60000);
				CallIdHeader myCallIDHeader = mySipProvider.getNewCallId();
				//javax.sip.address.URI myRequestURI = registrarAddress.getURI();
				SipURI myRequestURI = (SipURI) registrarAddress.getURI();
				Request myRegisterRequest = myMessageFactory.createRequest(myRequestURI,"REGISTER", myCallIDHeader, myCSeqHeader, myFromHeader, myToHeader,myViaHeaders, myMaxForwardsHeader);
				myRegisterRequest.addHeader(myContactHeader);
				myRegisterRequest.addHeader(myExpiresHeader);

				myClientTransaction = mySipProvider.getNewClientTransaction(myRegisterRequest);
				myClientTransaction.sendRequest();  

				//logger.info(myRegisterRequest.toString());
				status=REGISTERING;
			} catch (Exception e) {
				e.printStackTrace();
			} 

		}

	}

	public void setup () {

		try {

			if (!Unregistring) { //Se é um registro
				

//				//myDataSource = Activator.ds;
				//afmt = Activator.afmt;
				
				destination = Config.destination;				
				dialTimes = 30; 				
				useQueue = "no"; 				
				mySipAlias = Config.sipdomain; 
				myAudioPort= 30000; 			
				myProxyIP = Config.proxyIp;
				myProxyPort = Config.proxyPort;
				mySipFactory = SipFactory.getInstance();
				mySipFactory.setPathName("gov.nist");
				
				myName = Config.username;
				myUserID = Config.username + "@" + Config.sipdomain;
				myPassword = Config.password;
				myServer = Config.sipdomain;
				myPort = Config.myPort;
				myIP = Config.myIp;
				
			}
			mySdpManager=new SdpManager();
			answerInfo=new SdpInfo();
			offerInfo=new SdpInfo();

			myProperties = new Properties();
			myProperties.setProperty("javax.sip.STACK_NAME", myName);
			myProperties.setProperty("javax.sip.OUTBOUND_PROXY", myProxyIP + ":" + myProxyPort + "/UDP"); //Proxy
			mySipStack = mySipFactory.createSipStack(myProperties);
			myMessageFactory = mySipFactory.createMessageFactory();
			myHeaderFactory = mySipFactory.createHeaderFactory();
			myAddressFactory = mySipFactory.createAddressFactory();
			if (!Unregistring) {
				myListeningPoint = mySipStack.createListeningPoint(myIP, myPort, "udp");
				mySipProvider = mySipStack.createSipProvider(myListeningPoint);
				mySipProvider.addSipListener(this);
			}

			myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);

			Address routeAddress = myAddressFactory.createAddress("sip:"+myServer+";lr");
			myRouteHeader= myHeaderFactory.createRouteHeader(routeAddress);

			fromAddress=myAddressFactory.createAddress(myName + " <sip:"+ myUserID +">");

			Address registrarAddress=myAddressFactory.createAddress("sip:"+myServer);
			Address registerToAddress = fromAddress;
			Address registerFromAddress=fromAddress;

			ToHeader myToHeader = myHeaderFactory.createToHeader(registerToAddress, null);
			FromHeader myFromHeader = myHeaderFactory.createFromHeader(registerFromAddress, "647554");

			ArrayList myViaHeaders = new ArrayList();
			myViaHeaders.add(myViaHeader);

			MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
			Random random = new Random();
			CSeqHeader myCSeqHeader = myHeaderFactory.createCSeqHeader(random.nextInt(1000) * 1L,"REGISTER");
			
			CallIdHeader myCallIDHeader = mySipProvider.getNewCallId();
			SipURI myRequestURI = (SipURI) registrarAddress.getURI();
			Request myRegisterRequest = myMessageFactory.createRequest(myRequestURI,"REGISTER", myCallIDHeader, myCSeqHeader, myFromHeader, myToHeader,myViaHeaders, myMaxForwardsHeader);
			
			//Expires	
			ExpiresHeader myExpiresHeader;
			if (Unregistring) {
				myContactHeader.setExpires(0);
				myExpiresHeader = myHeaderFactory.createExpiresHeader(0);
			} else {
				myExpiresHeader = myHeaderFactory.createExpiresHeader(60000);
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

			//logger.info(myRegisterRequest.toString());
			//System.out.println(">>> " + myRegisterRequest.toString());
			status=REGISTERING;
		}catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getTime() {
		///System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.client.logic | CLASS: ClientAgent | METOD: getTime ");//By Ju
		
		StringBuilder sb = new StringBuilder();
		GregorianCalendar d = new GregorianCalendar();
		sb.append(d.get(GregorianCalendar.HOUR_OF_DAY));
		sb.append(":");
		sb.append(d.get(GregorianCalendar.MINUTE));
		sb.append(":");
		sb.append(d.get(GregorianCalendar.SECOND));
		return sb.toString();
	}
	
	public void setOff(SipStack mySipStack){
		///System.out.println("=> BUNDLE: br.ufes.inf.ngn.televoto.client.logic | CLASS: ClientAgent | METOD: setOff ");//By Ju
		try{
			mySipProvider.removeSipListener(this);
			mySipProvider.removeListeningPoint(myListeningPoint);
			mySipStack.deleteListeningPoint(myListeningPoint);
			mySipStack.deleteSipProvider(mySipProvider);
			myListeningPoint=null;
			mySipProvider=null;
			mySipStack=null;
			//myRingTool=null;
			myTimer.cancel();
			System.out.println("Finalizado...");
		}
		catch(Exception e){}
	}
	
	
	public void SendMedia(String IP, int aport) {

		SendStream ss;
		// Create a audio transmit object with the specified params.
		at = new AVTransmit2(myDataSource, IP, aport, afmt, myName);
		// Start the transmission
		at.start();

	}

	public void processRequest(RequestEvent requestReceivedEvent) {


		String method;
		Response myResponse;
		ToHeader myToHeader;
		Request myRequest = requestReceivedEvent.getRequest();
		method=myRequest.getMethod();
		
		
		logger.info("Request");	

		if (!method.equals("CANCEL")) {
			myServerTransaction = requestReceivedEvent.getServerTransaction();
		}
		try{
		
			switch (status) {

			case WAIT_PROV:
				status=WAIT_ACK;
				System.out.println("Chamando....");
				break;

			case IDLE:
				if (method.equals("INVITE")) {
					
					if (myServerTransaction == null) {
						myServerTransaction = mySipProvider.getNewServerTransaction(myRequest);
					}
					byte[] cont=(byte[]) myRequest.getContent();
					offerInfo=mySdpManager.getSdp(cont);

					answerInfo.IpAddress=myIP;
					answerInfo.aport=myAudioPort;
					answerInfo.aformat=offerInfo.aformat;

					if (useQueue.equals("yes")) {
						
						status=ESTABLISHED;
						
					} else {
						
						//Envio do PROVISIONAL RESPONSE 180
						
						myResponse=myMessageFactory.createResponse(180,myRequest);
						myResponse.addHeader(myContactHeader);
						myToHeader = (ToHeader) myResponse.getHeader("To");
						myToHeader.setTag("454326");
						myServerTransaction.sendResponse(myResponse);
						myDialog=myServerTransaction.getDialog();
						//logger.info(">>> "+myResponse.toString());
						status=WAIT_ACK;
						
					}
					//inicio do envio do ACK ao cliente em confirmacao ao PROV_180 e envio do SDP
					Request originalRequest = myServerTransaction.getRequest();
					myResponse = myMessageFactory.createResponse(200, originalRequest);
					//System.out.println(originalRequest.toString());
					myToHeader = (ToHeader) myResponse.getHeader("To");
					myResponse.addHeader(myContactHeader);


					//SEND ANSWER SDP

					ContentTypeHeader contentTypeHeader=myHeaderFactory.createContentTypeHeader("application","sdp");
					byte[] content=mySdpManager.createSdp(answerInfo);
					myResponse.setContent(content,contentTypeHeader);

					//aguardando ACK do cliente
					myServerTransaction.sendResponse(myResponse); 
					myDialog = myServerTransaction.getDialog();
					
					new Timer().schedule(new MyTimerTask(this),500000); //Aqui!!!
					//logger.info(">>> " + myResponse.toString());

				}
				if (method.equals("OPTIONS")) {
					if (myServerTransaction == null) {
						myServerTransaction = mySipProvider.getNewServerTransaction(myRequest);
					}
					
					Request originalRequest = myServerTransaction.getRequest();
					myResponse = myMessageFactory.createResponse(200, originalRequest);
					myResponse.addHeader(myContactHeader);
					myServerTransaction.sendResponse(myResponse); 
				}

				break;
				
			case ESTABLISHED:
				
				if (method.equals("BYE")) {
					
					//capturar o timestamp
					//logger.info(myName + ";BYE;" +  getTime());
					System.out.println(myName + ": BYE");
					myResponse=myMessageFactory.createResponse(200,myRequest);
					myResponse.addHeader(myContactHeader);
					myServerTransaction.sendResponse(myResponse); 
					//logger.info(">>> "+myResponse.toString());
					if (ack == 1) {
						ack=0;
						redial ++;
						
						if (redial <= dialTimes) {
							//inserir captura timestamp
							logger.info(myName + ";BYE;" +  getTime());
							System.out.println(myName + ": rediscagem " + redial + " para: " + destination);
							
							String dstURI[] = destination.split("@");
							String dstSipAlias = dstURI[0];
							Address destinationAddress = myAddressFactory.createAddress(dstSipAlias + " <sip:"+ destination + ">");
							javax.sip.address.URI myRequestURI=destinationAddress.getURI();
							
							Address addressOfRecord = myAddressFactory.createAddress(mySipAlias + " <sip:" + mySipAlias + "@" + myIP + ":" + myPort + ">");
							//HeaderFactory comentei 
							myHeaderFactory = mySipFactory.createHeaderFactory();
							//ViaHeader comentei 
							myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);
							ArrayList viaHeaders=new ArrayList();
							viaHeaders.add(myViaHeader);
							MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
							CallIdHeader myCallIdHeader = mySipProvider.getNewCallId();
							CSeqHeader myCSeqHeader=myHeaderFactory.createCSeqHeader(1L,"INVITE");
							FromHeader myFromHeader=myHeaderFactory.createFromHeader(addressOfRecord,"456249");
							myToHeader=myHeaderFactory.createToHeader(destinationAddress,null);

							myRequest = myMessageFactory.createRequest(myRequestURI,"INVITE",
									myCallIdHeader, myCSeqHeader,myFromHeader,myToHeader, viaHeaders, myMaxForwardsHeader);

							Address contactAddress = myAddressFactory.createAddress("<sip:" + mySipAlias + "@" + myIP + ":" + myPort + ">");
							myContactHeader = myHeaderFactory.createContactHeader(contactAddress);
							myRequest.addHeader(myContactHeader);

							SdpInfo offerInfo=new SdpInfo();

							offerInfo.IpAddress=myIP;
							offerInfo.aport=myAudioPort;
							
							
							offerInfo.aformat=0;

							ContentTypeHeader contentTypeHeader=myHeaderFactory.createContentTypeHeader("application","sdp");
							byte[] content=mySdpManager.createSdp(offerInfo);
							myRequest.setContent(content,contentTypeHeader);

							myClientTransaction=mySipProvider.getNewClientTransaction(myRequest);
							myClientTransaction.sendRequest(); 
							logger.info(myName + ";INVITE;" +  getTime());

							status=WAIT_PROV;
							
						} else {
							
							status=IDLE;
							System.out.println(myName + ": pronto.");
						}
					} else 
					ack++;		
				}
				break;

			case RINGING:
				if (method.equals("CANCEL")) {
					ServerTransaction myCancelServerTransaction=requestReceivedEvent.getServerTransaction();
					Request originalRequest=myServerTransaction.getRequest();
					myResponse=myMessageFactory.createResponse(487,originalRequest);
					myServerTransaction.sendResponse(myResponse); 
					Response myCancelResponse=myMessageFactory.createResponse(200,myRequest);
					myCancelServerTransaction.sendResponse(myCancelResponse); 
					//logger.info(">>> "+myResponse.toString());
					//logger.info(">>> "+myCancelResponse.toString());
					status=IDLE;
				}
				break;

			case WAIT_ACK:
				if (method.equals("ACK")) {
					status=ESTABLISHED;
					System.out.println("Conectado ...");
					//SendMedia(offerInfo.IpAddress, offerInfo.aport);
				}

				break;
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void processResponse(ResponseEvent responseReceivedEvent) {
		
		try {
			
			logger.info("Response");	
			
			Response myResponse = responseReceivedEvent.getResponse();
			//logger.info("<<< "+myResponse.toString());	

			ClientTransaction thisClientTransaction = responseReceivedEvent.getClientTransaction();
			
			int myStatusCode = myResponse.getStatusCode();
						
			switch(status){
				
			case WAIT_PROV:
				
				if (!thisClientTransaction.equals(myClientTransaction))
					myClientTransaction = thisClientTransaction; 
				
				if (myStatusCode < 200) {
					
					status=WAIT_FINAL;
					myDialog=thisClientTransaction.getDialog();
					
				} else if (myStatusCode < 300) {
					
					myDialog=thisClientTransaction.getDialog();
					CSeqHeader originalCSeq=(CSeqHeader) myClientTransaction.getRequest().getHeader(CSeqHeader.NAME);
					long numseq=originalCSeq.getSeqNumber();
					Request myAck = myDialog.createAck(numseq);
					myAck.addHeader(myContactHeader);
					myDialog.sendAck(myAck); 
					byte[] cont=(byte[]) myResponse.getContent();
					answerInfo=mySdpManager.getSdp(cont);
					
					status=ESTABLISHED;
				}
				else {
					if (myStatusCode == 401) {
		

					} else {
						
						status=IDLE;
						CSeqHeader originalCSeq=(CSeqHeader) myClientTransaction.getRequest().getHeader(CSeqHeader.NAME);
						long numseq=originalCSeq.getSeqNumber();
						Request myAck = myDialog.createAck(numseq);
						myAck.addHeader(myContactHeader);
						myDialog.sendAck(myAck); 

					}
				}
				break;

			case WAIT_FINAL:
				
				if (!thisClientTransaction.equals(myClientTransaction))
					myClientTransaction = thisClientTransaction; 
				
				if (myStatusCode<200) {
					status=WAIT_FINAL;
					myDialog=thisClientTransaction.getDialog();
				}
				else if (myStatusCode<300) {
					
					if (useQueue.equals("yes")) {
						status=IDLE;
					} else {
						status=ESTABLISHED;
					}
										
					myDialog = thisClientTransaction.getDialog();
					CSeqHeader originalCSeq=(CSeqHeader) myClientTransaction.getRequest().getHeader(CSeqHeader.NAME);
					long numseq=originalCSeq.getSeqNumber();
					Request myAck = myDialog.createAck(numseq); 
					myAck.addHeader(myContactHeader);
					myDialog.sendAck(myAck); 
					byte[] cont=(byte[]) myResponse.getContent();
					answerInfo=mySdpManager.getSdp(cont);
					
				}
				else {
					// Cancelando requisição ao cliente
					Request myCancelRequest = myClientTransaction.createCancel();
					ClientTransaction myCancelClientTransaction = mySipProvider.getNewClientTransaction(myCancelRequest);
					myCancelClientTransaction.sendRequest(); 
					status = IDLE;
				}
				break;

			case REGISTERING:
				
				if (!thisClientTransaction.equals(myClientTransaction))
					myClientTransaction = thisClientTransaction; 

				if (myStatusCode==200) {
					status=IDLE;
					
					if (!Unregistring) {
						
						System.out.println(myName + ": Registrado");
						
						//inserir captura de timestamp
						//logger.info(myName + ";INVITE;" +  getTime());
						//Random gerador = new Random();
						//int temp = gerador.nextInt(10); 
						//System.out.println(myName + ": Esperando " + temp + "segundos");
						//Thread.sleep(temp);
						
						System.out.println(myName + ": chamando " + destination);

						SdpInfo offerInfo = new SdpInfo();
						offerInfo.IpAddress = myIP;
						offerInfo.aport = myAudioPort;
						offerInfo.aformat = 0;
						byte[] content = mySdpManager.createSdp(offerInfo);
 				
						//Via
						myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);
						myViaHeader.setRPort();
						ArrayList viaHeaders=new ArrayList();
						viaHeaders.add(myViaHeader);
						
						//From
						Address addressOfRecord = myAddressFactory.createAddress("<sip:"+ myUserID +">");
						FromHeader myFromHeader=myHeaderFactory.createFromHeader(addressOfRecord,"456249");
						
						//To
						String dstURI[] = destination.split("@");
						String dstSipAlias = dstURI[0];
						Address destinationAddress = myAddressFactory.createAddress("<sip:"+ destination + ">");
						javax.sip.address.URI myRequestURI = destinationAddress.getURI();
						ToHeader myToHeader = myHeaderFactory.createToHeader(destinationAddress,null);
																								
						//MaxForwards
						MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
						
						//Call-ID
						CallIdHeader myCallIdHeader = mySipProvider.getNewCallId();
						
						//CSeq
						Random random = new Random();
						CSeqHeader myCSeqHeader=myHeaderFactory.createCSeqHeader(random.nextInt(1000) * 1L,"INVITE");
						
						//Create SIP request
						Request myRequest = myMessageFactory.createRequest(myRequestURI,"INVITE", myCallIdHeader,myCSeqHeader,myFromHeader,myToHeader,viaHeaders,myMaxForwardsHeader);
						
						//Contact
						Address contactAddress = myAddressFactory.createAddress("<sip:" + mySipAlias + "@" + myIP + ":" + myPort + ";transport=udp>");
						myContactHeader = myHeaderFactory.createContactHeader(contactAddress);
						myRequest.addHeader(myContactHeader);
						
						//Allow
						Allow myAllow = new Allow();
						myAllow.setMethod("INVITE, ACK, CANCEL, BYE, MESSAGE, OPTIONS, NOTIFY, PRACK, UPDATE, REFER");
						myRequest.addHeader(myAllow);
						
						//Privacy
						Privacy myPrivacy = new Privacy("none");
						myRequest.addHeader(myPrivacy);
						
						//PPreferredIdentity	
						HeaderFactoryImpl myHeaderFactoryImpl = new HeaderFactoryImpl();
						PPreferredIdentityHeader myPPreferredIdentityHeader = myHeaderFactoryImpl.createPPreferredIdentityHeader(myAddressFactory.createAddress("sip:"+ myName+ '@' +myServer));
						myRequest.addHeader(myPPreferredIdentityHeader);
						
						//Route
						Address routeAddress = myAddressFactory.createAddress("sip:orig@scscf."+myServer+":6060;lr");
						//RouteHeader comentei 
						myRouteHeader = myHeaderFactory.createRouteHeader(routeAddress);
						myRequest.addHeader(myRouteHeader);
						
						//Para proxy funcionar
						SipURI outboundProxyURI = myAddressFactory.createSipURI("proxy", myProxyIP);
						outboundProxyURI.setLrParam();
						outboundProxyURI.setPort(myProxyPort);
						myRouteHeader = myHeaderFactory.createRouteHeader(myAddressFactory.createAddress(outboundProxyURI));
						myRequest.addFirst(myRouteHeader);
						
						//Content Type
						ContentTypeHeader contentTypeHeader=myHeaderFactory.createContentTypeHeader("application","sdp");
						myRequest.setContent(content,contentTypeHeader);
						
						//ClientTransaction 
						myClientTransaction = mySipProvider.getNewClientTransaction(myRequest);
						myClientTransaction.setRetransmitTimer(700);
						myClientTransaction.sendRequest(); 
						
						status=WAIT_PROV;
						
						logger.info(">>> "+myRequest.toString());				
						
					} else {
						this.setOff(mySipStack);
					}
				}
				else {
					if (myStatusCode==403) {

						System.out.println("Problemas com credenciais!\n");
						
					} else if (myStatusCode==401) {

							myName = (String) this.getLocalName();
							myUserID = (String) this.getLocalName() + "@" + myServer;

							Address contactAddress = myAddressFactory.createAddress("sip:"+ myName+ '@' + myIP+":"+myPort);
							myContactHeader = myHeaderFactory.createContactHeader(contactAddress);

							myViaHeader = myHeaderFactory.createViaHeader(myIP, myPort,"udp", null);

							fromAddress=myAddressFactory.createAddress(myName + " <sip:"+myUserID+">");

							Address registrarAddress=myAddressFactory.createAddress("sip:"+myServer);
							Address registerToAddress = fromAddress;
							Address registerFromAddress=fromAddress;

							ToHeader myToHeader = myHeaderFactory.createToHeader(registerToAddress, null);
							FromHeader myFromHeader = myHeaderFactory.createFromHeader(registerFromAddress, "647554");

							ArrayList myViaHeaders = new ArrayList();
							myViaHeaders.add(myViaHeader);
							
							//System.out.println("myClientTransaction.getRequest():"+ myClientTransaction.getRequest());
							CSeqHeader originalCSeq = (CSeqHeader) myClientTransaction.getRequest().getHeader(CSeqHeader.NAME);
							long numseq=originalCSeq.getSeqNumber();
							MaxForwardsHeader myMaxForwardsHeader = myHeaderFactory.createMaxForwardsHeader(70);
							CSeqHeader myCSeqHeader = myHeaderFactory.createCSeqHeader(numseq + 1L,"REGISTER");
							
							CallIdHeader myCallID = (CallIdHeader) myClientTransaction.getRequest().getHeader(CallIdHeader.NAME);
							CallIdHeader myCallIDHeader = myCallID;
							SipURI myRequestURI = (SipURI) registrarAddress.getURI();
							Request myRegisterRequest = myMessageFactory.createRequest(myRequestURI,"REGISTER", myCallIDHeader, myCSeqHeader, myFromHeader, myToHeader,myViaHeaders, myMaxForwardsHeader);
							myRegisterRequest.addHeader(myContactHeader);
							
							//Expires						
							ExpiresHeader myExpiresHeader;
							if (Unregistring) {
								myExpiresHeader=myHeaderFactory.createExpiresHeader(0);
								myContactHeader.setExpires(0);
							} else {
								myExpiresHeader=myHeaderFactory.createExpiresHeader(60000);
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
														
							myWWWAuthenticateHeader = Utils.makeAuthHeader(myHeaderFactory, myResponse, myRegisterRequest, myUserID, myPassword);
							myRegisterRequest.addHeader(myWWWAuthenticateHeader);

							myClientTransaction = mySipProvider.getNewClientTransaction(myRegisterRequest);
							myClientTransaction.sendRequest(); 

							//logger.info(">>> "+myRegisterRequest.toString());
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
		System.out.println(myName + " :  " + timeoutEvent.getTimeout());
		//System.out.println(">>Timeout:" + timeoutEvent.getClientTransaction().getRequest());
		//System.out.println(">>Timeout:" + timeoutEvent.getServerTransaction().getRequest());
	}

	public void processTransactionTerminated(TransactionTerminatedEvent tevent) {
		//System.out.println("processTransactionTerminated");
	}

	public void processDialogTerminated(DialogTerminatedEvent tevent) {
		//System.out.println("processDialogTerminated");

	}

	public void processIOException(IOExceptionEvent tevent) {

	}

}
