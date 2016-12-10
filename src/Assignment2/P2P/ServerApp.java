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
	DBConnectionManager dBaseMan;
	private ORB orb;
	
	//Open connection to dbase on orb registration
	public void setORB(ORB orb_val) {
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
	public void registerFile(String filename, String path, String clientAddress) {
		// TODO Auto-generated method stub
		//return fid if successful
		dBaseMan.registerFile(filename, path, clientAddress);
		System.out.println("Registered file: " + filename);
	}

	@Override
	public boolean unRegisterFile(int fid) {
		return dBaseMan.unRegisterFile(fid);	
	}
	
}

public class ServerApp {
	  public static void main(String args[]) {
		    try{
		      // create and initialize the ORB
		      ORB orb = ORB.init(args, null);

		      // get reference to rootpoa & activate the POAManager
		      POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
		      rootpoa.the_POAManager().activate();

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
		      String name = "Hello";
		      NameComponent path[] = ncRef.to_name( name );
		      ncRef.rebind(path, href);

		      System.out.println("HelloServer ready and waiting ...");

		      // wait for invocations from clients
		      orb.run();
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
    
    //Search for the file specified by the filename entered and return a <filename, fid> list
    public List<Pair<String, Integer>> searchForFile(String filename){
    	List<Pair<String, Integer>> res = new ArrayList<Pair<String, Integer>>();
    	Statement query;
		ResultSet results;
		boolean rowFound = false;
    	try {
    		query = dbConnection.createStatement();
			results = query.executeQuery("SELECT * from `available_files` WHERE `filename`='" + filename + "'");
			rowFound = results.next();	    	
	    	while(rowFound){
	    		res.add(new Pair<String, Integer>(results.getString(1),results.getInt(0)));
	    		rowFound = results.next();
	    	}
		} catch (SQLException e) {
			e.printStackTrace();
			res = null;
		}
    	return res;
    }
    
    //Search for all files and return a <filename, fid> list
    public List<Pair<String, Integer>> getAllAvailableFiles(){
    	List<Pair<String, Integer>> res = new ArrayList<Pair<String, Integer>>();
    	Statement query;
		ResultSet results;
		boolean rowFound = false;
    	try {
    		query = dbConnection.createStatement();
			results = query.executeQuery("SELECT * from `available_files`");
			rowFound = results.next();	    	
	    	while(rowFound){
	    		res.add(new Pair<String, Integer>(results.getString(1),results.getInt(0)));
	    		rowFound = results.next();
	    	}
		} catch (SQLException e) {
			e.printStackTrace();
			res = null;
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
    public boolean registerFile(String filename, String filepath, String clientAddress) {
		Statement query;
		int results;
		boolean res = false;
		try {
			query = dbConnection.createStatement();
			results = query.executeUpdate("INSERT INTO `available_files` (`fid`, `name`, `path`, `address`) VALUES (NULL,'" + filename + "','" + filepath + "','" + clientAddress +"')");
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


