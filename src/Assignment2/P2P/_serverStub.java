package Assignment2.P2P;


/**
* Assignment2/P2P/_serverStub.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from Server
* Friday, December 9, 2016 7:32:14 PM EST
*/

public class _serverStub extends org.omg.CORBA.portable.ObjectImpl implements Assignment2.P2P.server
{

  public boolean getFileLocation (int fid, org.omg.CORBA.StringHolder path, org.omg.CORBA.StringHolder clientAddress)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getFileLocation", true);
                $out.write_long (fid);
                $in = _invoke ($out);
                boolean $result = $in.read_boolean ();
                path.value = $in.read_string ();
                clientAddress.value = $in.read_string ();
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return getFileLocation (fid, path, clientAddress        );
            } finally {
                _releaseReply ($in);
            }
  } // getFileLocation

  public void registerFile (String filename, String path, String clientAddress)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("registerFile", true);
                $out.write_string (filename);
                $out.write_string (path);
                $out.write_string (clientAddress);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                registerFile (filename, path, clientAddress        );
            } finally {
                _releaseReply ($in);
            }
  } // registerFile

  public boolean unRegisterFile (int fid)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("unRegisterFile", true);
                $out.write_long (fid);
                $in = _invoke ($out);
                boolean $result = $in.read_boolean ();
                return $result;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                return unRegisterFile (fid        );
            } finally {
                _releaseReply ($in);
            }
  } // unRegisterFile

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:Assignment2/P2P/server:1.0"};

  public String[] _ids ()
  {
    return (String[])__ids.clone ();
  }

  private void readObject (java.io.ObjectInputStream s) throws java.io.IOException
  {
     String str = s.readUTF ();
     String[] args = null;
     java.util.Properties props = null;
     org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init (args, props);
   try {
     org.omg.CORBA.Object obj = orb.string_to_object (str);
     org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl) obj)._get_delegate ();
     _set_delegate (delegate);
   } finally {
     orb.destroy() ;
   }
  }

  private void writeObject (java.io.ObjectOutputStream s) throws java.io.IOException
  {
     String[] args = null;
     java.util.Properties props = null;
     org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init (args, props);
   try {
     String str = orb.object_to_string (this);
     s.writeUTF (str);
   } finally {
     orb.destroy() ;
   }
  }
} // class _serverStub
