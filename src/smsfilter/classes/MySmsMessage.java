package smsfilter.classes;


public class MySmsMessage
{
	public static final String	EMPTY_SENDER = "<unknown>";
	
	public String	sender		= null;
	public String	body		= null;
	public long		timeMillis	= 0;
	///
	public MySmsMessage () {
	}
	
	public MySmsMessage (MySmsMessage sms)
	{
		sender 		= sms.sender;
		body		= sms.body;
		timeMillis	= sms.timeMillis;
	}
	
	public MySmsMessage (String sender, String body, long timeMillis)
	{
		this.sender		= sender;
		this.body		= body;
		this.timeMillis	= timeMillis;
	}
}
