package com.luaridaworks.httpuploader;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

public class Myhttpd extends NanoHTTPD {

	String prIpAddress = "127.0.0.1";
	int prHttpPort = 8080;
			
	public Myhttpd(int port) {
		super(port);
		
		prHttpPort = port;
		//this.setTempFileManagerFactory(new ExampleManagerFactory());
		
		//wlan0のIPアドレスを取得します
		prIpAddress = getMyIpAddress("wlan0");

	}

	@Override
	  public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms,
          Map<String, String> files)
	{
		String namekey = "upfile";
		Log.i( MainActivity.TAG,method + " '" + uri + "' ");
	   
	   String msg = "<html><head>";
	   msg += "<html><head>\n";
	   msg += "<meta http-equiv=\"Content-Type\" content=\"text/html\"; charset=\"UTF-8\">\n";
	   msg += "<title>PocketDuino Uploader</title>\n";
	   msg += "</head>\n";
	   msg += "<body><legend>Upload hex file.</legend>\n";
	   msg += "<form enctype=\"multipart/form-data\" method=\"post\" action=\"\">\n";
	   msg += "<p><input type=\"file\" name=" + namekey + " /></p>\n";
	   msg += "<p><input type=\"submit\" value=\"Up Load\" /></p>\n";
	   msg += "</form>\n";
	   msg += "</body></html>\n";
	   
	   final String filename = files.get( namekey );
	   if(filename != null && !filename.equals("")){
		   //prUploadfilename = parms.get(namekey);
		   MainActivity.MainActivity_Handler.post(new Runnable() {
			   //run()の中の処理はActivityThreadで実行されます。
			   public void run() {
				   MainActivity.MainActivity_Thread.fileUpload(filename);
			   }
		   });
//		   setPicture( filename );
	   }
	   
	   return new NanoHTTPD.Response(Response.Status.OK, MIME_HTML, msg);
	   //return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
	  }
	
	//**************************************************
	// IPアドレスを取得します
	//**************************************************
	private String getMyIpAddress(String lan){
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
			while(interfaces.hasMoreElements()){

				NetworkInterface network = interfaces.nextElement();
				
				if(network.getName().equals( lan )){
					Enumeration<InetAddress> addresses = network.getInetAddresses();
					while(addresses.hasMoreElements()){
					
						byte[] address = addresses.nextElement().getAddress();
						if(address.length == 4){

							String ipaddress = String.valueOf(Integer.parseInt(String.format("%X", address[0]),16));
							
							for(int i=1; i<4; i++){
								ipaddress += "." + String.valueOf(Integer.parseInt(String.format("%X", address[i]),16));
							}
							return ipaddress;
						}
					}
				}
			}
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
		return "127.0.0.1";
	}
	
	public String getIP(){
		return prIpAddress;
	}
}