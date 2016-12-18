package Assignment2.P2P;

import org.omg.CosNaming.*;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

import org.omg.CORBA.*;

public class Client{
	ORB orb;
	server helloRef;

	public Client(String[] args) {
		try {
			orb = ORB.init(args, System.getProperties());
			NamingContext rootContext = NamingContextHelper.narrow(orb.resolve_initial_references("NameService"));

			// Resolves for remote object reference of HelloServer
			NameComponent name[] = { new NameComponent("Server", "") };
			this.helloRef = serverHelper.narrow(rootContext.resolve(name));
			System.out.println("\nClient: Obtained the remote object " + "reference");
		} catch (Exception e) {
			System.err.println("\nClient: Caught exception - " + e);
			e.printStackTrace();
		}
	}
	
	public void registerFile(String name, String path, String address, long size){
		// Invokes the remote sayHello() method
		System.out.println("Client: Invoking the remote operation ...");
		this.helloRef.registerFile(name, path, address, size);
	}
	
	public fileInfo[] getAllSharedFiles(){
		return this.helloRef.getAllSharedFiles();
	}
	
	public fileInfo[] searchByName(String name){
		return this.helloRef.searchFilesByName(name);
	}
	
	public fileInfo[] getAllSharedFiles(String address){
		return this.helloRef.searchFilesByAddress(address);
	}
	
	public void removeFile(int fid){
		this.helloRef.unRegisterFile(fid);
	}
}