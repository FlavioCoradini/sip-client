package com.sipagent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.protocol.DataSource;

public class ConfigCalleeFile {
	
	public DataSource ds;
	private MediaLocator ml;
	
	public String audioFileLocation;
	
	
	public String myUserID;
	public String myPassword;
	public String myIp;
	public Integer myPort;
	public Integer myAudioPort;
	public Integer myAudioFormat;
	public String myProxy;
	public String myServerDomain;

	
	public ConfigCalleeFile() {
		
		
//		Object televotoConf[] = new Object[9];
//		String aux[];
//		int i=0;
//		
//		try { 
//			FileReader arq = new FileReader("/televoto.conf"); 
//			BufferedReader lerArq = new BufferedReader(arq); 
//			String linha = lerArq.readLine();
//			while (linha != null) {
//				if (!linha.startsWith("#")) {
//					aux = linha.split("=");
//					televotoConf[i] = aux[1]; 
//					linha = lerArq.readLine(); 
//					i++;
//				} else
//					linha = lerArq.readLine();
//			}
//			 
//			arq.close(); 
//		} catch (IOException e) 
//		{ 
//			System.err.printf("Erro na abertura do arquivo: %s.\n", e.getMessage()); 
//			System.exit(0);
//		} 
		
		
		
		
		myUserID = "1200";
		myPassword = "P4ssw0rd";
		
		myIp = "172.16.28.1";
		myPort = 5062;
		
		myAudioPort = 30000;
		myAudioFormat = 4;
		
		myProxy = "172.16.28.3:4060";
		
		myServerDomain = "ims.vi.ifes.edu.br";
		
		
		

		
		
		
		//cria um componente MediaLocator associado a mensagem do Televoto
		if ((ml = new MediaLocator("file:" + "silvio.wav")) == null) {
			
			System.err.println("Cannot build media locator from: " + "silvio.wav");
			System.exit(0);
		}

		//cria um DataSource associado ao MediaLocator para ser clonado
		
		try {
			ds = (DataSource) Manager.createDataSource(ml);
		} catch (Exception e) {
			System.err.println("Cannot create DataSource from: " + ml);
			System.exit(0);
		}
		ds = (DataSource) Manager.createCloneableDataSource(ds);

		if (ds == null) {
			System.err.println("Cannot clone the given DataSource");
			System.exit(0);
		}
		
		
		
	}
	
	
	
	
	
	
}
