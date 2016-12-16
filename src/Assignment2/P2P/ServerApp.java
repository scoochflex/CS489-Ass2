package Assignment2.P2P;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.PortableServer.ServantLocatorPackage.*;


import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;

import java.sql.Connection;
import java.sql.Driver;

import javafx.util.Pair;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


class ServerImpl extends serverPOA{
	DBConnectionManager dBaseMan = new DBConnectionManager();
	private ORB orb;
	
	//Open connection to dbase on orb registration
	public void ServerImpl(ORB orb_val) {
	    orb = orb_val;
	    dBaseMan = new DBConnectionManager();
	}

	@Override
	public boolean getFileLocation(int fid, StringHolder path, StringHolder clientAddress) {		
		//Returns key = path, value = address pair
		Pair<String, String> res = dBaseMan.getFileLocation(fid);
		if(res.getKey()!=""){
			clientAddress=new StringHolder(res.getKey());
			clientAddress=new StringHolder(res.getValue());
			return true;
		}
		
		return false;
	}

	@Override
	public void registerFile(String filename, String path, String clientAddress, long size) {
		// TODO Auto-generated method stub
		//return fid if successful
		dBaseMan.registerFile(filename, path, clientAddress, size);
		System.out.println("Registered file: " + filename);
	}

	@Override
	public boolean unRegisterFile(int fid) {
		return dBaseMan.unRegisterFile(fid);	
	}

	@Override
	public fileInfo[] getAllSharedFiles() {
		fileInfo[] results = dBaseMan.getAllAvailableFiles();
		if(results==null){
			results = new fileInfo[0];
		}
		return results;
	}

	@Override
	public fileInfo[] searchFilesByName(String filename) {
		fileInfo[] results = dBaseMan.searchForFile(filename);
		if(results==null){
			results = new fileInfo[0];
		}
		return results;		
	}
	
	@Override
	public fileInfo[] searchFilesByAddress(String address) {
		fileInfo[] results = dBaseMan.searchFilesAddress(address);
		if(results==null){
			results = new fileInfo[0];
		}
		return results;		
	}
	
}

class PoaServantLocator extends LocalObject implements ServantLocator {
    public Servant preinvoke(byte[] oid, POA adapter,
                             String operation,
                             CookieHolder the_cookie) throws ForwardRequest {
        try {
        	ServerImpl servantObj = new ServerImpl();
            System.out.println("PoaServantLocator.preinvoke(): Created \"" +
                               servantObj.getClass().getName() + "\" " +
                               "servant object for \"" + adapter.the_name() +
                               "\"");
            return servantObj;
        } catch (Exception e) {
            System.err.println("preinvoke: Caught exception - " + e);
        }
        return null;
    }

    public void postinvoke(byte[] oid, POA adapter,
                           String operation,
                           java.lang.Object the_cookie,
                           Servant the_servant) {
        try {
            System.out.println("PoaServantLocator.postinvoke(): For \"" +
                               adapter.the_name() + "\" adapter of servant " +
                               "object type \"" +
                               the_servant.getClass().getName() + "\"");
        } catch (Exception e) {
            System.err.println("postinvoke: Caught exception - " + e);
        }
    }
}

public class ServerApp {
	  public static void main(String args[]) {
		    try{
		      // create and initialize the ORB
		      ORB orb = ORB.init(args, System.getProperties());

		      // get reference to rootpoa & activate the POAManager
		      POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
		      rootPoa.the_POAManager().activate();

		      Policy poaPolicy[] = new Policy[2];
	            poaPolicy[0] = rootPoa.create_servant_retention_policy(
	                ServantRetentionPolicyValue.NON_RETAIN);
	            poaPolicy[1] = rootPoa.create_request_processing_policy(
	                RequestProcessingPolicyValue.USE_SERVANT_MANAGER);
	            System.out.println("Server: Set POA policy as NON_RETAIN and " +
	                               "USE_SERVANT_MANAGER");

	            POA poa1 = rootPoa.create_POA("HelloPoa", null, poaPolicy);
	            poa1.the_POAManager().activate();
	            System.out.println("Server: Created and activated child POA " +
	                               "\"" + poa1.the_name() + "\"");

	            poa1.set_servant_manager(new PoaServantLocator());
	            System.out.println("Server: Associated the servant manager of " +
	                               "type servant locator with \"" + 
	                               poa1.the_name() + "\"");

	            // This create_reference operation does not cause an activation, 
	            // the resulting object reference will be exported and passed to 
	            // client, so that subsequent requests on the reference will cause
	            // the appropriate servant manager to be invoked
	            org.omg.CORBA.Object objectRef = poa1.create_reference(
	                serverHelper.id());
	            System.out.println("Server: Created a CORBA object reference " +
	                               "from id \"" + serverHelper.id() + "\""); 

	            NamingContext rootContext = NamingContextHelper.narrow(
	                orb.resolve_initial_references("NameService"));
	            NameComponent name[] = {new NameComponent("Server", "")};
	            rootContext.rebind(name, objectRef);
	            System.out.println("Server: Exported the CORBA object reference " +
	                               "to NameService");

	            System.out.println("Server: Ready and waiting for requests ...");
	            orb.run();
	          /*  
		      // create servant and register it with the ORB
		      ServerImpl helloImpl = new ServerImpl();
		      helloImpl.setORB(orb); 

		      // get object reference from the servant
		      org.omg.CORBA.Object ref = rootpoa.servant_to_reference(helloImpl);
		      server href = serverHelper.narrow(ref);
		          
		      // get the root naming context
		      // NameService invokes the name service
		      org.omg.CORBA.Object objRef =
		          orb.resolve_initial_references("NameService");
		      // Use NamingContextExt which is part of the Interoperable
		      // Naming Service (INS) specification.
		      NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

		      // bind the Object Reference in Naming
		      String name = "server";
		      NameComponent path[] = ncRef.to_name( name );
		      ncRef.rebind(path, href);

		      System.out.println("HelloServer ready and waiting ...");

		      // wait for invocations from clients
		      orb.run();
		      */
		    } 
		        
		      catch (Exception e) {
		        System.err.println("ERROR: " + e);
		        e.printStackTrace(System.out);
		      }
		          
		      System.out.println("HelloServer Exiting ...");		        
		  }
}

class DBConnectionManager {
    Connection dbConnection;    // The connection to the database

    public DBConnectionManager() {
    	// Make the database connection
    	try {
    		// Construct the database address
        	String dbaseURL = "jdbc:mysql://localhost:3306/files";
        	//Class.forName("com.mysql.jdbc.Driver").newInstance();
        	this.dbConnection =DriverManager.getConnection(dbaseURL, "root", "");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
    }
    
    public fileInfo[] searchFilesAddress(String address) {    	
    	fileInfo[] res = null;
    	List<fileInfo> tmpArray = new ArrayList<fileInfo>();
    	Statement query;
		ResultSet results;
		boolean rowFound = false;
    	try {
    		query = dbConnection.createStatement();
			results = query.executeQuery("SELECT * from `available_files` WHERE `address`='" + address + "'");
			rowFound = results.next();	    	
	    	while(rowFound){
	    		tmpArray.add(new fileInfo(results.getInt(1),results.getString(2),results.getString(3),results.getString(4), results.getLong(5)));
	    		rowFound = results.next();
	    	}
	    	if(!tmpArray.isEmpty()){
	    		res = new fileInfo[tmpArray.size()];
	    		tmpArray.toArray(res);
	    	}
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return res;
	}

	//Search for the file specified by the filename entered and return a <filename, fid> list
    public fileInfo[] searchForFile(String filename){
    	fileInfo[] res = null;
    	List<fileInfo> tmpArray = new ArrayList<fileInfo>();
    	Statement query;
		ResultSet results;
		boolean rowFound = false;
    	try {
    		query = dbConnection.createStatement();
			results = query.executeQuery("SELECT * from `available_files` WHERE `name`='" + filename + "'");
			rowFound = results.next();	    	
	    	while(rowFound){
	    		tmpArray.add(new fileInfo(results.getInt(1),results.getString(2),results.getString(3),results.getString(4), results.getLong(5)));
	    		rowFound = results.next();
	    	}
	    	if(!tmpArray.isEmpty()){
	    		res = new fileInfo[tmpArray.size()];
	    		tmpArray.toArray(res);
	    	}
		} catch (SQLException e) {
			e.printStackTrace();
			res = null;
		}
    	return res;
    }
    
    //Search for all files and return a <filename, fid> list
    public fileInfo[] getAllAvailableFiles(){
    	fileInfo[] res = null;
    	List<fileInfo> tmpArray = new ArrayList<fileInfo>();
    	Statement query;
		ResultSet results;
		boolean rowFound = false;
    	try {
    		query = dbConnection.createStatement();
			results = query.executeQuery("SELECT * from `available_files`");
			rowFound = results.next();	    	
	    	while(rowFound){
	    		tmpArray.add(new fileInfo(results.getInt(1),results.getString(2),results.getString(3),results.getString(4), results.getLong(5)));
	    		rowFound = results.next();
	    	}
	    	if(!tmpArray.isEmpty()){
	    		res = new fileInfo[tmpArray.size()];
	    		tmpArray.toArray(res);
	    	}
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	return res;
    }

    //return the address and path of the file specified by the entered fid and return a <path, address> list
    public Pair<String, String> getFileLocation(int fid) {
    	Pair<String, String> result = new Pair<String, String>("", "");
    	try {
    		Statement query = dbConnection.createStatement();
			ResultSet results = query.executeQuery("SELECT `address` `path` from `available_files` WHERE `fid`='" + fid + "'");
			if(results.next()){
				 result = new Pair<String, String>(results.getString(2), results.getString(3));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
        // Perform database transactions to retrieve 
        // address information
    	return result;
    }
    
    //Register a file from a client based on
    public boolean registerFile(String filename, String filepath, String clientAddress, long size) {
		Statement query;
		int results;
		boolean res = false;
		try {
			query = dbConnection.createStatement();
			results = query.executeUpdate("INSERT INTO `available_files` (`fid`, `name`, `path`, `address`, `size`) VALUES (NULL,'" + filename + "','" + filepath + "','" + clientAddress+ "','" + size +"')");
			if(results==1){
				res=true;
				//ResultSet results = query.executeQuery("SELECT MAX(fid) FROM available_files");				
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

    	return res;
    }
    
	public boolean unRegisterFile(int fid) {
    Statement query;
	int results;
	boolean res = false;
	try{
	    query = dbConnection.createStatement();
		results = query.executeUpdate("DELETE FROM `available_files` WHERE `fid` ='" + fid + "'");
		if(results==1){
			res = true;
		}
	}catch(Exception e){
		e.printStackTrace();
	}
	return res;
	}
}


