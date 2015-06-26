package org.biocaddie.citationanalysis.retrievedata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * After retrieving the citationLinks using RetrieveCitationFromEutils.java, this class retrieves the summary of each PubMed Id.
 * The input of this class is all_pubmed_id.txt, which is generated by RetrieveCitationFromEutils.java.
 * *PubMed Id | Count)  size: 1878468
 * 8392631 | 7
 * 8123469 | 4
 * 8123468 | 8
 * ...........
 * Then we retrieve the summary information (title, authors, journalName etc.) of all retrieved (~2 million) PubMed Id's using eutils/esummary service. 
 * We retrieve the data in a batch mode, each HttpRequest retrieves XML file for 10K PubMed Ids or less for the last one. 
 * In order to make sure that each XML file is received completely, we checked whether the Http Response (Code = 200) is successful, and the endTag is correct. 
 * If there is a problem, the program prints the error message and exits.
 * Sample Http Request:
 * http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?tool=BioCADDIE&email=abc@ucsd.edu&retmode=xml&db=pubmed&id=3453543,1211212,.......
 * 
 * This program generates a single XML file as output: all_citations_summary.xml
 * This single xml file contains the summary of all retrieved pubMed ids, ~2 million.
 */
public class RetrieveSummaryFromEutils {
	
	final static Charset ENCODING = StandardCharsets.UTF_8;
	final static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");	

	/**
	 * The main function.
	 * @param args: It only accepts one input argument: all_pubmed_id.txt file.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		//Get the inputFileName from user 
	/*	if(args.length != 1){
    		System.out.println("Call: java org.biocaddie.citation.retrievedata.RetrieveSummaryFromEutils <all_pubmed_id.txt>");
    		System.exit(1);
    	}
		String fileNameFullPath = args[0];		
		String fileSeparator = System.getProperty("file.separator");		
		String pathToFile = fileNameFullPath.substring(0, fileNameFullPath.lastIndexOf(fileSeparator)+1);		
		
		//Get the pubMed Summary for all pubmedId's
    	getPubMedSummary(fileNameFullPath, pathToFile+"all_citations_summary.xml");
*/
    	//Get the summary of whole PubMed starting from 1 to 26 million
    	getWholePubMedSummary("/Users/ali/Documents/BioCaddie/data/citation/april_29/whole_pubmed/all_citations_summary.xml");
    	//Done...
	}

	/**
	 * Retrieve pubMed summary of each retrieved pubmedIds and write all of them into a new XML file ("newXmlFileName")
	 * @param fileName : input file name (all_pubmed_id.txt) which is contains all pubmed ids to be retrieved using eutils/esummary.
	 * @param newXmlFileName: the name of the xml file, which will be generated during the program. It contains the summary of all retrieved pubmed ids.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static synchronized void getWholePubMedSummary(String newXmlFileName) throws Exception{

	  /*System.out.println("Start reading "+ fileName);	    
	    Map<String, Integer> all_pubmed_id_map = new HashMap<String, Integer>(); //all pubmed id's
	    //STEP 1: Read the file into the all_pubmed_id_map 
	    BufferedReader reader = Files.newBufferedReader(Paths.get(fileName), ENCODING);
		String line = null; 
	    while ((line = reader.readLine()) != null) {
	    	//skip the header line and any empty lines
	    	if (line.startsWith("*") || line.trim().equals("")) //first line or last line
	    		continue;
	    	int pos = line.indexOf("|"); 
	    	String existingPmId =  line.substring(0, pos-1);
	    	String cnt = line.substring(pos+2);	    	
	    	all_pubmed_id_map.put(existingPmId, new Integer(cnt));
	    }
	    */
	    System.out.println("Will retrieve the whole pubmed summary for between 1 and 26M");
	    System.out.println("It may take around ... hour or more ...");
	    System.out.println("Start Time: " + dateFormat.format(new Date()));	    
	    
	    //STEP 2: each HTTPRequest will retrieve the summary of 10K pmIds, because of that generate requestIdListVector where each item contains requestIdList for 10K 
    	Vector<StringBuffer> requestIdListVector = new Vector<StringBuffer>(); //keep requestLists of 10K size
		StringBuffer requestIdList = new StringBuffer(); 
		int cnt_tmp = 0;
	    //for (Iterator<Map.Entry<String, Integer>> iter = all_pubmed_id_map.entrySet().iterator(); iter.hasNext(); ) {
	    for (int i = 1; i<=26000000; i++){
	    	//Map.Entry<String, Integer> entry = iter.next();	    
	    	if (cnt_tmp == 0)
	    		requestIdList.append("&id=");
	    	else
	    		requestIdList.append(",");
	    			    	 
	    	//requestIdList.append(entry.getKey());
	    	requestIdList.append(i);
	    	cnt_tmp++;
	    	if (cnt_tmp == 10000){
	    		requestIdListVector.add(requestIdList);
	    		requestIdList = new StringBuffer(); 
	    		cnt_tmp = 0;
	    	}	    	
	    }
	    //add the last one too, if it is not empty
	    if (requestIdList.length() > 0){
    		requestIdListVector.add(requestIdList);
    		requestIdList = new StringBuffer(); 
    		cnt_tmp = 0;
	    }
	    
	    //STEP 3: iterate through the requestIdListVector, and send HTTPRequest for each item
		String elink_url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi";
	    String toolEmailLinkname ="tool=BioCADDIE&email=altunkay@hawaii.edu&retmode=xml&db=pubmed";
	    
	    BufferedWriter out = new BufferedWriter(new FileWriter(new File(newXmlFileName)));
	    for (int i = 0; i < requestIdListVector.size(); i++){

	    	boolean firstRequestList = false; 
	    	boolean lastRequestList = false;
	    	if (i == 0)
	    		firstRequestList = true;
	    	if (i == (requestIdListVector.size()-1))
	    		lastRequestList = true;

	    	String elink_urlParameters = toolEmailLinkname + requestIdListVector.get(i).toString();
			StringBuffer outStringBuffer = new StringBuffer(); // this is for out file		

			try {
				eutilsHttpRequest(firstRequestList, lastRequestList, elink_url, elink_urlParameters, outStringBuffer);
				out.write(outStringBuffer.toString());	out.flush();  
			} catch (Exception e) {
				//If there is an exception such as "SocketException", connection problem or truncated XML, wait 10 seconds and retry. 
				//If the error appears again, don't catch it again, so the program will exit.
				String errorMessage = e.getMessage(); 
				String currentTime = dateFormat.format(new Date());
			    System.out.println("Exception: " + errorMessage + " currentTime:" + currentTime);

			    Thread.sleep(10000);  		  

			    outStringBuffer = new StringBuffer(); //outAllStringBuffer = new StringBuffer(); newPmIdMap.clear(); // clear variables
				eutilsHttpRequest(firstRequestList, lastRequestList, elink_url, elink_urlParameters, outStringBuffer);
				out.write(outStringBuffer.toString());	out.flush();  
			}			
	    }
	    
        //flush and close the outstream
        out.flush();  
        out.close();        
		
	    System.out.println("End Time  : " + dateFormat.format(new Date()));	    
	    System.out.println("DONE !!!");
	}	
	/**
	 * Retrieve pubMed summary of each retrieved pubmedIds and write all of them into a new XML file ("newXmlFileName")
	 * @param fileName : input file name (all_pubmed_id.txt) which is contains all pubmed ids to be retrieved using eutils/esummary.
	 * @param newXmlFileName: the name of the xml file, which will be generated during the program. It contains the summary of all retrieved pubmed ids.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static synchronized void getPubMedSummary(String fileName, String newXmlFileName) throws Exception{

		System.out.println("Start reading "+ fileName);
	    
	    Map<String, Integer> all_pubmed_id_map = new HashMap<String, Integer>(); //all pubmed id's
	    //STEP 1: Read the file into the all_pubmed_id_map 
	    BufferedReader reader = Files.newBufferedReader(Paths.get(fileName), ENCODING);
		String line = null; 
	    while ((line = reader.readLine()) != null) {
	    	//skip the header line and any empty lines
	    	if (line.startsWith("*") || line.trim().equals("")) //first line or last line
	    		continue;
	    	int pos = line.indexOf("|"); 
	    	String existingPmId =  line.substring(0, pos-1);
	    	String cnt = line.substring(pos+2);	    	
	    	all_pubmed_id_map.put(existingPmId, new Integer(cnt));
	    }
	    System.out.println("Will retrieve the pubmed summary for " + all_pubmed_id_map.size() + " pubmed ids.");
	    System.out.println("It may take around one hour or more ...");
	    System.out.println("Start Time: " + dateFormat.format(new Date()));	    
	    
	    //STEP 2: each HTTPRequest will retrieve the summary of 10K pmIds, because of that generate requestIdListVector where each item contains requestIdList for 10K 
    	Vector<StringBuffer> requestIdListVector = new Vector<StringBuffer>(); //keep requestLists of 10K size
		StringBuffer requestIdList = new StringBuffer(); 
		int cnt_tmp = 0;
	    for (Iterator<Map.Entry<String, Integer>> iter = all_pubmed_id_map.entrySet().iterator(); iter.hasNext(); ) {	    	
	    	Map.Entry<String, Integer> entry = iter.next();	    
	    	if (cnt_tmp == 0)
	    		requestIdList.append("&id=");
	    	else
	    		requestIdList.append(",");
	    			    	 
	    	requestIdList.append(entry.getKey());
	    	cnt_tmp++;
	    	if (cnt_tmp == 10000){
	    		requestIdListVector.add(requestIdList);
	    		requestIdList = new StringBuffer(); 
	    		cnt_tmp = 0;
	    	}	    	
	    }
	    //add the last one too, if it is not empty
	    if (requestIdList.length() > 0){
    		requestIdListVector.add(requestIdList);
    		requestIdList = new StringBuffer(); 
    		cnt_tmp = 0;
	    }
	    
	    //STEP 3: iterate through the requestIdListVector, and send HTTPRequest for each item
		String elink_url = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi";
	    String toolEmailLinkname ="tool=BioCADDIE&email=altunkay@hawaii.edu&retmode=xml&db=pubmed";
	    
	    BufferedWriter out = new BufferedWriter(new FileWriter(new File(newXmlFileName)));
	    for (int i = 0; i < requestIdListVector.size(); i++){

	    	boolean firstRequestList = false; 
	    	boolean lastRequestList = false;
	    	if (i == 0)
	    		firstRequestList = true;
	    	if (i == (requestIdListVector.size()-1))
	    		lastRequestList = true;

	    	String elink_urlParameters = toolEmailLinkname + requestIdListVector.get(i).toString();
			StringBuffer outStringBuffer = new StringBuffer(); // this is for out file		

			try {
				eutilsHttpRequest(firstRequestList, lastRequestList, elink_url, elink_urlParameters, outStringBuffer);
				out.write(outStringBuffer.toString());	out.flush();  
			} catch (Exception e) {
				//If there is an exception such as "SocketException", connection problem or truncated XML, wait 10 seconds and retry. 
				//If the error appears again, don't catch it again, so the program will exit.
				String errorMessage = e.getMessage(); 
				String currentTime = dateFormat.format(new Date());
			    System.out.println("Exception: " + errorMessage + " currentTime:" + currentTime);

			    Thread.sleep(10000);  		  

			    outStringBuffer = new StringBuffer(); //outAllStringBuffer = new StringBuffer(); newPmIdMap.clear(); // clear variables
				eutilsHttpRequest(firstRequestList, lastRequestList, elink_url, elink_urlParameters, outStringBuffer);
				out.write(outStringBuffer.toString());	out.flush();  
			}			
	    }
	    
        //flush and close the outstream
        out.flush();  
        out.close();        
		
	    System.out.println("End Time  : " + dateFormat.format(new Date()));	    
	    System.out.println("DONE !!!");
	}

	public static synchronized void eutilsHttpRequest(boolean firstRequestList, boolean lastRequestList, String elink_url, String elink_urlParameters, StringBuffer outStringBuffer) throws Exception{

    	//HTTP Connection
	    //String elink_urlParameters = toolEmailLinkname + requestIdListVector.get(i).toString();	    
	    URL url = new URL(elink_url);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    conn.setRequestMethod("POST");
	    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
	    conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");			    
	    conn.setDoOutput(true);        

	    //Send post request
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(elink_urlParameters);
		wr.flush();
		wr.close();
		
		//Receive the response qnd save/write it to a new xml file
		int responseCode = conn.getResponseCode();
		if (responseCode != 200){
			throw new Exception("!!! ERROR HTTP Response is not successfull. HTTP Response Code : " + responseCode);			
		}
    
		InputStreamReader inStream = new InputStreamReader(conn.getInputStream());
		BufferedReader in = new BufferedReader(inStream);
		String previousLine=""; //used to check whether the XML file received completely	    
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
						
			//write the header of the XML only from the first HTTP request 
			if (inputLine.trim().startsWith("<?xml") || inputLine.trim().startsWith("<!DOCTYPE") || inputLine.trim().startsWith("<eSummaryResult>")){
				if (firstRequestList){
					//out.write(inputLine); 	out.newLine();						
					outStringBuffer.append(inputLine); 
					outStringBuffer.append(System.lineSeparator());
		        }	
	        	previousLine = inputLine.trim();					
		        continue;
			}
			
			//write the footer of XML only from the last HTTP request
			if (inputLine.trim().startsWith("</eSummaryResult>")){
				if (lastRequestList){
					//out.write(inputLine);		        	out.newLine();
					outStringBuffer.append(inputLine); 
					outStringBuffer.append(System.lineSeparator());
		        }
	        	previousLine = inputLine.trim();
				continue;
			}
			
			//if there is an error, print it. such as: <ERROR>UID=22434765: cannot get document summary</ERROR>
		//	if (inputLine.trim().startsWith("<ERROR>")) 
		//		System.out.println(inputLine.trim());				
			
			//write the content of each HTTP request 
			outStringBuffer.append(inputLine); 
			outStringBuffer.append(System.lineSeparator());
	        //out.write(inputLine);         out.newLine();        
	        
	        inputLine = inputLine.trim();
	        if (inputLine.length() > 0)
	        	previousLine = inputLine;	        		        
		}

		//check that the previous XML file received completely
		if (!previousLine.equals("</eSummaryResult>")){
			throw new Exception("!!! ERROR: XML file did not received completely over HTTP request.");
		}
		
        //close them
		in.close(); 
		inStream.close();
		conn.disconnect();
	    Thread.sleep(1000);  //wait 1 second between HttpRequests		    			    
		
	}	
}
