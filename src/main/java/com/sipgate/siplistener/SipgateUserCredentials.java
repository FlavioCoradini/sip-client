package com.sipgate.siplistener;

import gov.nist.javax.sip.clientauthutils.UserCredentials;

public class SipgateUserCredentials implements UserCredentials
{
//	private String extensionSipId = "bob";
//	private String password = "bob";
//	private String domain = "open-ims.test";
//	private String displayName = "bob";
//	private String proxy = "192.168.1.3:4060";
//	
//	private String serverDNS = "open-ims.test";
		
	
	private String extensionSipId = "1200";
	private String password = "P4ssw0rd";
	private String domain = "ims.vi.ifes.edu.br";
	private String displayName = "1200";
	private String proxy = "172.16.28.3:4060";
	
	private String serverDNS = "ims.vi.ifes.edu.br";
	
	
	
	public String getUserName()
	{
		return extensionSipId;
	}

	public String getPassword()
	{
		return password;
	}

	public String getSipDomain()
	{
		return domain;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getProxy()
	{
		return proxy;
	}
	
	public String getDNS()
	{
		return serverDNS;
	}
	

}
