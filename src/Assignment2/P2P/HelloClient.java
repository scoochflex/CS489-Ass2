package Assignment2.P2P;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;

import com.sun.glass.events.KeyEvent;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Label;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.omg.CORBA.*;
public class HelloClient
{
  static server helloImpl;

  public static void main(String args[])
    {
      
    //1. Create the frame.
      JFrame frame = new JFrame("Client");

      //2. Optional: What happens when the frame closes?
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      //3. Create components and put them in the frame.
      //...create emptyLabel...
      JFormattedTextField serverAddress = new JFormattedTextField();
      serverAddress.setValue("127.0.0.1:1050");
      serverAddress.setColumns(10);
      frame.add(serverAddress);
      JButton b1 = new JButton("Start client!");
      b1.setVerticalTextPosition(AbstractButton.CENTER);
      b1.setHorizontalTextPosition(AbstractButton.LEADING); //aka LEFT, for left-to-right locales
      b1.setMnemonic(KeyEvent.VK_D);
      b1.setActionCommand("connect");
      frame.add(b1);
      //4. Size the frame.
      frame.pack();

      //5. Show it.
      frame.setVisible(true);
      
      try{
    	 //get a connection to the database...
    	 //query for the ior file for an available servant...
    	//Create orb based around the ior thats been retrieved from the dbase...
        // create and initialize the ORB
        ORB orb = ORB.init(args, null);

        // get the root naming context
        org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
        // Use NamingContextExt instead of NamingContext. This is 
        // part of the Interoperable naming Service.  
        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
        
        helloImpl = serverHelper.narrow(ncRef.resolve_str("Hello"));
        
        helloImpl.registerFile("tmp.txt", "/path/to/tmp", "127.0.0.1:80");
        
        System.out.println("Obtained a handle on server object: " + helloImpl);


        } catch (Exception e) {
          System.out.println("ERROR : " + e) ;
          e.printStackTrace(System.out);
          }
    }

}
