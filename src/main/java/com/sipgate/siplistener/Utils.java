package com.sipgate.siplistener;

import java.security.MessageDigest;

import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;



public class Utils {
	
	public static AuthorizationHeader makeAuthHeader(HeaderFactory headerFactory, Response resp, Request req, String username, String password) {
		try {
			// Authenticate header with challenge we need to reply to
			WWWAuthenticateHeader ah_c = (WWWAuthenticateHeader) resp.getHeader(WWWAuthenticateHeader.NAME);
			if(ah_c == null) {
				ah_c = (WWWAuthenticateHeader)resp.getHeader(ProxyAuthenticateHeader.NAME);
			}
	
			// Authorization header we will build with response to challenge
	
			// assemble data we need to create response string
			URI request_uri = req.getRequestURI();
			String request_method = req.getMethod();
			String nonce = ah_c.getNonce();
			String algrm = ah_c.getAlgorithm();
			String realm = ah_c.getRealm();
			String qop = ah_c.getQop();
			String opaque = ah_c.getOpaque();
			
			qop = "auth";
			if(qop == null || qop.length() == 0) {
				return generateAuthHeader(headerFactory, ah_c, nonce, realm, password, algrm, request_method, request_uri, opaque, username);
			} else {
				return generateAuthHeader(headerFactory, ah_c, nonce, realm, password, algrm, request_method, request_uri, "0a4f113b" , "00000001", qop, opaque, username);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//qop is empty or undefined
	public static AuthorizationHeader generateAuthHeader(HeaderFactory headerFactory, WWWAuthenticateHeader ah_c, String nonce, String realm, 
			String password, String algrm, String request_method, URI request_uri, String opaque, String username) {
		try {
			MessageDigest mdigest = MessageDigest.getInstance(algrm);
			AuthorizationHeader ah_r = headerFactory.createAuthorizationHeader(ah_c.getScheme());
			
			String a1 = username + ":" + realm + ":" + password;
			String ha1 = toHexString(mdigest.digest(a1.getBytes()));	
			
			String a2 = request_method.toUpperCase() + ":" + request_uri;
			String ha2 = toHexString(mdigest.digest(a2.getBytes()));	
			
			String finalStr = ha1 + ":" + nonce + ":" + ha2;
		
			String response = toHexString(mdigest.digest(finalStr.getBytes()));
	
			ah_r.setRealm(realm);
			ah_r.setNonce(nonce);
			ah_r.setUsername(username);
			ah_r.setURI(request_uri);
			ah_r.setAlgorithm(algrm);
			ah_r.setResponse(response);
			ah_r.setAlgorithm(algrm);
			
			if(opaque != null) {
				ah_r.setOpaque(opaque);
			}
			return ah_r;
	} catch(Exception e) {
		e.printStackTrace();
	}
	return null;
}
	
	//qop present
	public static AuthorizationHeader generateAuthHeader(HeaderFactory headerFactory, WWWAuthenticateHeader ah_c, String nonce, String realm, 
				String password, String algrm, String request_method, URI request_uri, String cNonce, 
				String nonceCount, String qop, String opaque, String username) {
			try {
				MessageDigest mdigest = MessageDigest.getInstance(algrm);
				AuthorizationHeader ah_r = headerFactory.createAuthorizationHeader(ah_c.getScheme());
				
				String a1 = username + ":" + realm + ":" + password;
				String ha1 = toHexString(mdigest.digest(a1.getBytes()));	

				
				String a2 = request_method.toUpperCase() + ":" + request_uri;
				String ha2 = toHexString(mdigest.digest(a2.getBytes()));	
				
				String finalStr = ha1 + ":" + nonce + ":" + nonceCount + ":" + cNonce + ":" + qop + ":" + ha2;
			
				String response = toHexString(mdigest.digest(finalStr.getBytes()));
		
				ah_r.setRealm(realm);
				ah_r.setNonce(nonce);
				ah_r.setUsername(username);
				ah_r.setURI(request_uri);
				ah_r.setAlgorithm(algrm);
				ah_r.setResponse(response);
				ah_r.setCNonce(cNonce);
				ah_r.setAlgorithm(algrm);
				ah_r.setNonceCount(Integer.parseInt(nonceCount));
				if(opaque != null) {
					ah_r.setOpaque(opaque);
				}
				ah_r.setQop(qop);
				return ah_r;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6',
		'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	

	/**
	 * From Nist/JAIN examples: convert an array of bytes to an hexadecimal
	 * string
	 * 
	 * @return a string (length = 2 * b.length)
	 * @param b
	 *            bytes array to convert to a hexadecimal string
	 */
	static String toHexString(byte b[]) {
		int pos = 0;
		char[] c = new char[b.length * 2];
		for (int i = 0; i < b.length; i++) {
			c[pos++] = toHex[(b[i] >> 4) & 0x0F];
			c[pos++] = toHex[b[i] & 0x0f];
		}
		return new String(c);
	}

}
