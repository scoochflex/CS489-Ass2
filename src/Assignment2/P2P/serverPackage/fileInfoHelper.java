package Assignment2.P2P.serverPackage;


/**
* Assignment2/P2P/serverPackage/fileInfoHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from Server
* Monday, December 12, 2016 8:56:27 PM EST
*/

abstract public class fileInfoHelper
{
  private static String  _id = "IDL:Assignment2/P2P/server/fileInfo:1.0";

  public static void insert (org.omg.CORBA.Any a, Assignment2.P2P.serverPackage.fileInfo that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static Assignment2.P2P.serverPackage.fileInfo extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  private static boolean __active = false;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      synchronized (org.omg.CORBA.TypeCode.class)
      {
        if (__typeCode == null)
        {
          if (__active)
          {
            return org.omg.CORBA.ORB.init().create_recursive_tc ( _id );
          }
          __active = true;
          org.omg.CORBA.StructMember[] _members0 = new org.omg.CORBA.StructMember [5];
          org.omg.CORBA.TypeCode _tcOf_members0 = null;
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_long);
          _members0[0] = new org.omg.CORBA.StructMember (
            "fid",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[1] = new org.omg.CORBA.StructMember (
            "name",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[2] = new org.omg.CORBA.StructMember (
            "path",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().create_string_tc (0);
          _members0[3] = new org.omg.CORBA.StructMember (
            "address",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_longlong);
          _members0[4] = new org.omg.CORBA.StructMember (
            "size",
            _tcOf_members0,
            null);
          __typeCode = org.omg.CORBA.ORB.init ().create_struct_tc (Assignment2.P2P.serverPackage.fileInfoHelper.id (), "fileInfo", _members0);
          __active = false;
        }
      }
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static Assignment2.P2P.serverPackage.fileInfo read (org.omg.CORBA.portable.InputStream istream)
  {
    Assignment2.P2P.serverPackage.fileInfo value = new Assignment2.P2P.serverPackage.fileInfo ();
    value.fid = istream.read_long ();
    value.name = istream.read_string ();
    value.path = istream.read_string ();
    value.address = istream.read_string ();
    value.size = istream.read_longlong ();
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, Assignment2.P2P.serverPackage.fileInfo value)
  {
    ostream.write_long (value.fid);
    ostream.write_string (value.name);
    ostream.write_string (value.path);
    ostream.write_string (value.address);
    ostream.write_longlong (value.size);
  }

}
