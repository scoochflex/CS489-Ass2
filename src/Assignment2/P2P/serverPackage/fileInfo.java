package Assignment2.P2P.serverPackage;


/**
* Assignment2/P2P/serverPackage/fileInfo.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from Server
* Monday, December 12, 2016 8:56:27 PM EST
*/

public final class fileInfo implements org.omg.CORBA.portable.IDLEntity
{
  public int fid = (int)0;
  public String name = null;
  public String path = null;
  public String address = null;
  public long size = (long)0;

  public fileInfo ()
  {
  } // ctor

  public fileInfo (int _fid, String _name, String _path, String _address, long _size)
  {
    fid = _fid;
    name = _name;
    path = _path;
    address = _address;
    size = _size;
  } // ctor

} // class fileInfo