package common;

import java.util.Arrays;
import java.net.InetSocketAddress;
import java.net.InetAddress;

/**
 * Class to manage Message sent
 *
 * @author Marais-Viau
 */
public class Message {
  private MessageType type;
  private String[] args;
	private InetSocketAddress addr;

  /**
	 * Constructor
	 *
	 * @param type the type of the message
	 * @param args arguments of the message
	 * @param addr the address where the message must be sent
	 */
  public Message(MessageType type, String[] args, InetSocketAddress addr) {
    this.type = type;
    this.args = args;
		this.addr = addr;
  }

	/**
	 * Constructor
	 *
	 * @param type the type of the message
	 * @param args arguments of the message
	 */
  public Message(MessageType type, String[] args) {
		this(type, args, null);
  }

	/**
	 * Constructor
	 *
	 * @param type the type of the message
	 */
  public Message(MessageType type) {
		this(type, null, null);
  }

	/**
	 * Argument getter
	 *
	 * @return the arguments of the message
	 */
  public String[] getArgs() {
    return args;
  }

	/**
	 * Type getter
	 *
	 * @return a message type of the message
	 */
  public MessageType getType() {
    return type;
  }

	/**
	 * Address getter
	 *
	 * @return the address of the message
	 */
  public InetSocketAddress getAddress() {
    return addr;
  }

	/**
	 * Argument setter
	 *
	 * @param args the arguments
	 */
  public void setArgs(String[] args) {
    this.args = args;
  }

	/**
	 * Type setter
	 *
	 * @param type the type of the message
	 */
  public void setType(MessageType type) {
    this.type = type;
  }

	/**
	 * Inet setter
	 *
	 * @param addr the address of the message
	 */
  public void setAddress(InetSocketAddress addr) {
    this.addr = addr;
  }

	/**
	 * Inet setter
	 *
	 * @param addr the address of the message
	 * @param port the port of the address
	 */
  public void setAddress(InetAddress addr, int port) {
    this.addr = new InetSocketAddress(addr, port);
  }

	/**
	 * Transforms the message to sending format
	 *
	 * @return a String well format for tcp
	 */
  public String toNetFormat() {
    String res = type.toString();
    res += "\n";
    if (args != null) {
      for (String arg : args) {
        res += arg + "\n";
      }
    }
    res += ".\n";
    return res;
  }

	/**
	 * Convert a String to a Message
	 *
	 * @param sMessage the message as a String
	 * @return a Message
	 */
  public static Message stringToMessage(String sMessage) {
    String[] tokens = sMessage.split("\n"); // Remove the dot at the end
    MessageType type = stringToMessageType(tokens[0]);
    String[] args = (tokens.length < 2) ? null : Arrays.copyOfRange(tokens, 1, tokens.length);
    return new Message(type, args);
  }

  private static MessageType stringToMessageType(String stringType) {
    MessageType res;
    switch (stringType) {
      case "CONNECT":
        res = MessageType.CONNECT;
        break;
      case "CONNECT_OK":
        res = MessageType.CONNECT_OK;
        break;
      case "CONNECT_NEW_USER_OK":
        res = MessageType.CONNECT_NEW_USER_OK;
        break;
      case "CONNECT_NEW_USER_KO":
        res = MessageType.CONNECT_NEW_USER_KO;
        break;
      case "CONNECT_KO":
        res = MessageType.CONNECT_KO;
        break;
      case "NOT_CONNECTED":
        res = MessageType.NOT_CONNECTED;
        break;
      case "DISCONNECT":
        res = MessageType.DISCONNECT;
        break;
      case "POST_ANC":
        res = MessageType.POST_ANC;
        break;
      case "POST_ANC_OK":
        res = MessageType.POST_ANC_OK;
        break;
      case "POST_ANC_KO":
        res = MessageType.POST_ANC_KO;
        break;
      case "MAJ_ANC":
        res = MessageType.MAJ_ANC;
        break;
      case "MAJ_ANC_OK":
        res = MessageType.MAJ_ANC_OK;
        break;
      case "MAJ_ANC_KO":
        res = MessageType.MAJ_ANC_KO;
        break;
      case "DELETE_ANC":
        res = MessageType.DELETE_ANC;
        break;
      case "DELETE_ANC_OK":
        res = MessageType.DELETE_ANC_OK;
        break;
      case "DELETE_ANC_KO":
        res = MessageType.DELETE_ANC_KO;
        break;
      case "REQUEST_DOMAIN":
        res = MessageType.REQUEST_DOMAIN;
        break;
      case "SEND_DOMAIN_OK":
        res = MessageType.SEND_DOMAINE_OK;
        break;
      case "SEND_DOMAIN_KO":
        res = MessageType.SEND_DOMAIN_KO;
        break;
      case "REQUEST_ANC":
        res = MessageType.REQUEST_ANC;
        break;
      case "SEND_ANC_OK":
        res = MessageType.SEND_ANC_OK;
        break;
      case "SEND_ANC_KO":
        res = MessageType.SEND_ANC_KO;
        break;
      case "REQUEST_OWN_ANC":
        res = MessageType.REQUEST_OWN_ANC;
        break;
      case "SEND_OWN_ANC_OK":
        res = MessageType.SEND_OWN_ANC_OK;
        break;
      case "SEND_OWN_ANC_KO":
        res = MessageType.SEND_OWN_ANC_KO;
        break;
      case "REQUEST_IP":
        res = MessageType.REQUEST_IP;
        break;
      case "REQUEST_IP_OK":
        res = MessageType.REQUEST_IP_OK;
        break;
      case "REQUEST_IP_KO":
        res = MessageType.REQUEST_IP_KO;
        break;
      case "UNKNOWN_REQUEST":
        res = MessageType.UNKNOWN_REQUEST;
        break;
      case "MSG":
        res = MessageType.MSG;
        break;
      case "MSG_ACK":
        res = MessageType.MSG_ACK;
        break;
      default:
        throw new IllegalArgumentException("Message not supported");
    }
    return res;
  }

	/**
	 * An enumeration that represents the list of available messages
	 */
  public enum MessageType {

    CONNECT("CONNECT"),

    CONNECT_OK("CONNECT_OK"),

    CONNECT_NEW_USER_OK("CONNECT_NEW_USER_OK"),

    CONNECT_NEW_USER_KO("CONNECT_NEW_USER_KO"),

    CONNECT_KO("CONNECT_KO"),

    NOT_CONNECTED("NOT_CONNECTED"),

    DISCONNECT("DISCONNECT"),

    POST_ANC("POST_ANC"),

    POST_ANC_OK("POST_ANC_OK"),

    POST_ANC_KO("POST_ANC_KO"),

    MAJ_ANC("MAJ_ANC"),

    MAJ_ANC_OK("MAJ_ANC_OK"),

    MAJ_ANC_KO("MAJ_ANC_KO"),

    DELETE_ANC("DELETE_ANC"),

    DELETE_ANC_OK("DELETE_ANC_OK"),

    DELETE_ANC_KO("DELETE_ANC_KO"),

    REQUEST_DOMAIN("REQUEST_DOMAIN"),

    SEND_DOMAINE_OK("SEND_DOMAIN_OK"),

    SEND_DOMAIN_KO("SEND_DOMAIN_KO"),

    REQUEST_ANC("REQUEST_ANC"),

    SEND_ANC_OK("SEND_ANC_OK"),

    SEND_ANC_KO("SEND_ANC_KO"),

    REQUEST_OWN_ANC("REQUEST_OWN_ANC"),

    SEND_OWN_ANC_OK("SEND_OWN_ANC_OK"),

    SEND_OWN_ANC_KO("SEND_OWN_ANC_KO"),

    REQUEST_IP("REQUEST_IP"),

    REQUEST_IP_OK("REQUEST_IP_OK"),

    REQUEST_IP_KO("REQUEST_IP_KO"),

    UNKNOWN_REQUEST("UNKNOWN_REQUEST"),

    MSG("MSG"),

    MSG_ACK("MSG_ACK");


    private String name;

    private MessageType(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

  }

}
