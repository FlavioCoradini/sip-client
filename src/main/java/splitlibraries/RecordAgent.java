package splitlibraries;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;



public class RecordAgent extends Agent {
	
	String callId;
    String from;
    String to;
    String session;
    String aport;
    
	public Connection myConnection;
	public String URL ="jdbc:mysql://172.16.28.3:3306/";
	public String usuario = "televoto"; 
	public String password = "w3w4x3f9";  
	public String DRIVER ="com.mysql.jdbc.Driver";
	
	 protected void setup() {
		 
		 // Load the parameters
		 
		// java jade.Boot foo:FooAgent(callId, from, to, session)
		Object[] args = getArguments();
		
        callId = args[0].toString();
        from = args[1].toString();
        to = args[2].toString();
        session = args[3].toString();
        aport = args[4].toString();
	 
	    addBehaviour(new Record());
	    
	  } 

	 protected void takeDown() {
	 		
	 		
	 }
	 
	 
	private class Record extends Behaviour {
		
		public void action() {
 			
 			
			RTPExport recorder = new RTPExport("file:records/" + callId + ".wav", 20, session);
		   	 
	   	 	String result = recorder.startRecording();
			
	   	 	if(result != null) {
	   	 		System.err.println(result);
	   	 	} else {
	   	 		
	   	 		StoreCallInDB(callId, from, to);
	   	 		
	   	 		enablePort(aport);
	   	 	}
 			
 			doDelete(); // Kill the Agent
 			
 		}
 		
 		public boolean done() {
		      return true;
		} 
 	}
	
	
	
	public void StoreCallInDB(String callId, String from, String to) {
		
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Calendar cal = Calendar.getInstance();
				
			try {
				myConnection = abreConexao();
				PreparedStatement stm = myConnection.prepareStatement("INSERT INTO televoto.records(call_id, caller, callee, time ) VALUES( ?, ?, ?, ?)");
				
				stm.setString(1, callId);
				stm.setString(2, from);
				stm.setString(3, to);
				
				stm.setString(4, dateFormat.format(cal.getTime()));
				
				
				stm.executeUpdate();
				myConnection.close();
				
			} catch (SQLException e) {
				System.out.println("SQLException: " + e.getMessage());
				System.out.println("SQLState: " +  e.getSQLState());
				System.out.println("VendorError: " +  e.getErrorCode());
			
			}
		}
	
	public Connection abreConexao() throws SQLException {  
		
		try {  
			Class.forName(DRIVER );  
			Connection con = DriverManager.getConnection(URL,usuario,password);  
			return con;  

		} catch (ClassNotFoundException e) {  
			throw new SQLException(e.getMessage());  

		}  
	}
	
	

	public void enablePort(String port) {
		
		try {

			URL url = new URL("http://localhost:3000/ports/available/"+ port);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				System.out.println(output);
			}

			conn.disconnect();

		  } catch (MalformedURLException e) {

			e.printStackTrace();

		  } catch (IOException e) {

			e.printStackTrace();

		  }

	}
	 	
	 	
	 		
}
