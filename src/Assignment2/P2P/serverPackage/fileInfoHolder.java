package Assignment2.P2P.serverPackage;

/**
* Assignment2/P2P/serverPackage/fileInfoHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from Server
* Monday, December 12, 2016 8:56:27 PM EST
*/

public final class fileInfoHolder implements org.omg.CORBA.portable.Streamable
{
  public Assignment2.P2P.serverPackage.fileInfo value = null;

  public fileInfoHolder ()
  {
  }

  public fileInfoHolder (Assignment2.P2P.serverPackage.fileInfo initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = Assignment2.P2P.serverPackage.fileInfoHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    Assignment2.P2P.serverPackage.fileInfoHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return Assignment2.P2P.serverPackage.fileInfoHelper.type ();
  }

}
