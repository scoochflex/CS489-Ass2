package Assignment2.P2P.serverPackage;


/**
* Assignment2/P2P/serverPackage/fileListHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from Server
* Tuesday, December 13, 2016 12:52:56 AM EST
*/

public final class fileListHolder implements org.omg.CORBA.portable.Streamable
{
  public Assignment2.P2P.fileInfo value[] = null;

  public fileListHolder ()
  {
  }

  public fileListHolder (Assignment2.P2P.fileInfo[] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = Assignment2.P2P.serverPackage.fileListHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    Assignment2.P2P.serverPackage.fileListHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return Assignment2.P2P.serverPackage.fileListHelper.type ();
  }

}