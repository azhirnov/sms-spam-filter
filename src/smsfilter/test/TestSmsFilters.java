package smsfilter.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import smsfilter.app.Preferences;
import smsfilter.utils.*;
import smsfilter.classes.MySmsMessage;
import smsfilter.classes.SmsFilters;
import junit.framework.Assert;


//
// Test Sms Filters
//

public final class TestSmsFilters extends SmsFilters
{
	private static final String	TAG = "Tests";

	
// types //
	
	//
	// Name Filter
	//
	private static class TestNameFilter extends NameFilter
	{
		public TestNameFilter(boolean allow) {
			super(allow);
		}
		
		public void AddNumber (String sender) {
			_items.add( NUMBER + sender );
		}
		
		public void AddNumberRange (String sender) {
			_items.add( NUMBER_RANGE + sender );
		}
		
		public void AddPartOfName (String sender) {
			_items.add( PART_OF_NAME + sender );
		}
		
		public void Apply (String sender, String body, long time, FilterTrace trace)
		{
			super.Apply( new SmsMessageData( new MySmsMessage( sender, body, time ),
					new ArrayList<String>() ), trace );
		}
		
		public static void RunTests ()
		{
			FilterTrace	trace = new FilterTrace();
			{
				TestNameFilter	nf = new TestNameFilter( true );
				
				nf.AddNumber( "1111" );
				nf.Apply( "1111", "...", 0, trace ); 	CheckTrace( trace, 1 );
				nf.Apply( "2222", "...", 0, trace );	CheckTrace( trace, 0 );
				nf.Apply( "21111", "...", 0, trace );	CheckTrace( trace, 0 );
			}
			
			{
				TestNameFilter	nf = new TestNameFilter( false );
				
				nf.AddNumber( "1111" );
				nf.Apply( "2222", "...", 0, trace );	CheckTrace( trace,  0 );
				nf.Apply( "1111", "...", 0, trace );	CheckTrace( trace, -1 );
			}
			
			{
				TestNameFilter	nf = new TestNameFilter( true );
				
				nf.AddNumberRange( "+7499" );
				nf.Apply( "+749981928921", "...", 0, trace );	CheckTrace( trace, 1 );
				nf.Apply( "+781272376237", "...", 0, trace );	CheckTrace( trace, 0 );
				nf.Apply( "849972376237", "...", 0, trace );	CheckTrace( trace, 0 );
			}
			
			{
				TestNameFilter	nf = new TestNameFilter( false );
				
				nf.AddNumber( "rsb.ru" );
				nf.Apply( "rsb2.ru", "...", 0, trace );	CheckTrace( trace, 0 );
				nf.Apply( "rsb.ru", "...", 0, trace );	CheckTrace( trace, -1 );
				nf.Apply( "RSB.RU", "...", 0, trace );	CheckTrace( trace, -1 );
			}
			
			{
				TestNameFilter	nf = new TestNameFilter( false );
				
				nf.AddPartOfName( "part" );
				nf.Apply( "p a r t", "...", 0, trace );				CheckTrace( trace, 0 );
				nf.Apply( "part", "...", 0, trace );				CheckTrace( trace, -1 );
				nf.Apply( "this is partofword", "...", 0, trace );	CheckTrace( trace, -1 );
			}
		}
	}
	
	
	//
	// Date Filter
	//
	private static class TestDateFilter extends DateFilter
	{
		public TestDateFilter(boolean allow) {
			super(allow);
		}
		
		public void AddItem (Date from, Date to)
		{
			int dfrom = from.getHours() * 60 + from.getMinutes();
			int dto   = to.getHours() * 60 + to.getMinutes();
			
			_items.add( new TimeInterval( dfrom, dto ) );
		}
		
		public void Apply (String sender, String body, long time, FilterTrace trace)
		{
			super.Apply( new SmsMessageData( new MySmsMessage( sender, body, time ),
					new ArrayList<String>() ), trace );
		}
		
		public static void RunTests ()
		{
			final long	MILLIS_IN_MIN	= 60 * 1000;
			
			Date 	d 		= new Date();
			long  	time	= (d.getMinutes() + d.getHours() * 60) * MILLIS_IN_MIN;
			
			FilterTrace	trace = new FilterTrace();
			
			{
				TestDateFilter	df = new TestDateFilter( true );
				
				//df.AddItem( DateCtor( 7, 30 ), DateCtor( 22, 0 ) );
				df.AddItem( DateOffset( -1, 0 ), DateOffset( +1, 0 ) );
				df.Apply( "", "", time, trace ); 	CheckTrace( trace, 1 );
			}

			{
				TestDateFilter	df = new TestDateFilter( true );
				
				df.AddItem( DateOffset( -3, 0 ), DateOffset( 0, -30 ) );
				df.Apply( "", "", time, trace ); 	CheckTrace( trace, 0 );
			}

			{
				TestDateFilter	df = new TestDateFilter( true );
				
				df.AddItem( DateOffset( +2, 0 ), DateOffset( +5, 30 ) );
				df.Apply( "", "", time, trace ); 	CheckTrace( trace, 0 );
			}

			{
				TestDateFilter	df = new TestDateFilter( false );
				
				df.AddItem( DateOffset( -3, 0 ), DateOffset( 0, -30 ) );
				df.Apply( "", "", time, trace ); 	CheckTrace( trace, 0 );
			}
		}
		
		public static Date DateOffset (int hours, int minutes)
		{
			Date	 d = new Date();
		    Calendar c = Calendar.getInstance();
		    c.setTime( d );
		    c.add( Calendar.HOUR, hours ); 
		    c.add( Calendar.MINUTE, minutes );
		    d.setTime( c.getTime().getTime() );
			return d;
		}
	}
	
	
	//
	// Text Filter
	//
	private static class TestTextFilter extends TextFilter
	{
		public TestTextFilter(boolean allow) {
			super(allow);
		}
		
		public void AddWholeWordItem (String contentWord) {
			_items.add( WHOLE_WORD + contentWord );
		}
		
		public void AddPartOfWordItem (String contentWord) {
			_items.add( PART_OF_WORD + contentWord );
		}
		
		public void Apply (String sender, String body, long time, FilterTrace trace)
		{
			super.Apply( new SmsMessageData( new MySmsMessage( sender, body, time ),
					new ArrayList<String>() ), trace );
		}
		
		public static void RunTests ()
		{
			FilterTrace	trace = new FilterTrace();
			{
				TestTextFilter	tf = new TestTextFilter( true );
				
				tf.AddWholeWordItem( "taxi" );
				tf.Apply( "...", "asd taxi sdgs", 0, trace ); 		CheckTrace( trace, 1 );
				tf.Apply( "...", "dfdvsc Taxi asd", 0, trace ); 	CheckTrace( trace, 1 );
				tf.Apply( "...", "asd taXI ascva", 0, trace ); 		CheckTrace( trace, 1 );
				tf.Apply( "...", "asd tахi ascva", 0, trace ); 		CheckTrace( trace, 1 );
				
				tf.Apply( "...", "acaad tahi efdc", 0, trace ); 	CheckTrace( trace, 0 );
			}
			
			{
				TestTextFilter	tf = new TestTextFilter( true );
				
				tf.AddPartOfWordItem( "подарок" );
				tf.Apply( "...", "ыдлв флды подарок. под", 0, trace ); 	CheckTrace( trace, 1 );
				tf.Apply( "...", "ыдлв флды пOдарOк под", 0, trace ); 	CheckTrace( trace, 1 );
				tf.Apply( "...", "ыдлв флды п0дар0к под", 0, trace ); 	CheckTrace( trace, 1 );
				tf.Apply( "...", "лыв флдывл п0дap0Kывта", 0, trace ); 	CheckTrace( trace, 1 );
				
				tf.Apply( "...", "лыв флдывл п0дapo", 0, trace ); 		CheckTrace( trace, 0 );
			}
			
			{
				TestTextFilter	tf = new TestTextFilter( true );
				
				tf.AddWholeWordItem( "подарок" );
				tf.Apply( "...", "ыдлв флды подарок. под", 0, trace ); 		CheckTrace( trace, 1 );
				tf.Apply( "...", "ыдлв флды пOдарOк/под", 0, trace ); 		CheckTrace( trace, 1 );
				tf.Apply( "...", "ыдлв флды @п0дар0к\" под", 0, trace ); 	CheckTrace( trace, 1 );
				
				tf.Apply( "...", "лыв флдывл п0дap0Kывта", 0, trace ); 		CheckTrace( trace, 0 );
				tf.Apply( "...", "лыв флдывл п0дapo", 0, trace ); 			CheckTrace( trace, 0 );
			}
			
			{
				TestTextFilter	tf = new TestTextFilter( true );
				
				tf.AddWholeWordItem( "такси" );
				tf.Apply( "...", "asd такси sdgs", 0, trace ); 		CheckTrace( trace, 1 );
				tf.Apply( "...", "dfdvsc “акси asd", 0, trace ); 	CheckTrace( trace, 1 );
				tf.Apply( "...", "asd та —и ascva", 0, trace ); 	CheckTrace( trace, 1 );
				
				tf.Apply( "...", "acaad тахи efdc", 0, trace ); 	CheckTrace( trace, 0 );
			}
			
			{
				TestTextFilter	tf = new TestTextFilter( false );
				
				tf.AddWholeWordItem( "taxi" );
				tf.Apply( "...", "vac taxi beda", 0, trace ); 		CheckTrace( trace, -1 );
				tf.Apply( "...", "fgvs Taxi aswwss", 0, trace ); 	CheckTrace( trace, -1 );
				tf.Apply( "...", "sdcsda taXI ddscc", 0, trace ); 	CheckTrace( trace, -1 );
				
				tf.Apply( "...", "sdcwdcs tahi ddssd", 0, trace );	CheckTrace( trace, 0 );
			}
			
			{
				TestTextFilter	tf = new TestTextFilter( false );
				
				tf.AddWholeWordItem( "такси" );
				tf.Apply( "...", "vac такси beda", 0, trace ); 		CheckTrace( trace, -1 );
				tf.Apply( "...", "fgvs “акси aswwss", 0, trace ); 	CheckTrace( trace, -1 );
				tf.Apply( "...", "sdcsda та —и ddscc", 0, trace ); 	CheckTrace( trace, -1 );
				
				tf.Apply( "...", "sdcwdcs тахи ddssd", 0, trace );	CheckTrace( trace, 0 );
			}
			
			{
				TestTextFilter	tf = new TestTextFilter( false );
				
				tf.AddPartOfWordItem( "taxi" );
				tf.Apply( "...", "vac taxibeda", 0, trace ); 		CheckTrace( trace, -1 );
				tf.Apply( "...", "fgvs Taxiaswwss", 0, trace ); 	CheckTrace( trace, -1 );
				tf.Apply( "...", "sdcsda taXIddscc", 0, trace ); 	CheckTrace( trace, -1 );
				
				tf.Apply( "...", "sdcwdcs tahiddssd", 0, trace );	CheckTrace( trace, 0 );
			}
			
			{
				TestTextFilter	tf = new TestTextFilter( true );
				
				tf.AddPartOfWordItem( "@" );
				tf.Apply( "...", " some@mail.com ", 0, trace ); 	CheckTrace( trace, 1 );
				tf.Apply( "...", " other msg ", 0, trace ); 		CheckTrace( trace, 0 );
			}
		}
	}
	
	
	//
	// Custom Filter
	//
	private static class TestCustomFilter extends CustomFilter
	{
		public TestCustomFilter () {
		}
		
		public void Apply (String sender, String body, long time, FilterTrace trace)
		{
			super.Apply( new SmsMessageData( new MySmsMessage( sender, body, time ),
					new ArrayList<String>() ), trace );
		}
		
		public static void RunTests ()
		{
			FilterTrace	trace = new FilterTrace();
			{
				TestCustomFilter	cf = new TestCustomFilter();
				cf.AddBlockUrls();
				cf.Apply( "", "no url here", 0, trace ); 						CheckTrace( trace, 0 );
				cf.Apply( "", " like.url but/not ", 0, trace ); 				CheckTrace( trace, 0 );
				cf.Apply( "", "  not...com/  ", 0, trace ); 					CheckTrace( trace, 0 );
				cf.Apply( "", " /sdf/.asd.ru ", 0, trace ); 					CheckTrace( trace, 0 );
				cf.Apply( "", "asas www.google.com dfsds", 0, trace ); 			CheckTrace( trace, -1 );
				cf.Apply( "", "dv http://google.com dnasc", 0, trace );			CheckTrace( trace, -1 );
				cf.Apply( "", "asd https://google.com avdfvs", 0, trace );		CheckTrace( trace, -1 );
				cf.Apply( "", "skdlf https://www.google.com asas", 0, trace );	CheckTrace( trace, -1 );
				cf.Apply( "", "ejrjkd tutu.wml.in/lo.jar mms", 0, trace );		CheckTrace( trace, -1 );
				cf.Apply( "", "sfdf yandex.ru lsdfsdk", 0, trace );				CheckTrace( trace, -1 );
				cf.Apply( "", "sfdf something.org lsdfsdk", 0, trace );			CheckTrace( trace, -1 );
				cf.Apply( "", "sfdf something.net lsdfsdk", 0, trace );			CheckTrace( trace, -1 );
			}
			{
				TestCustomFilter	cf = new TestCustomFilter();
				cf.AddBlockPhoneNumber();
				cf.Apply( "", "no phone number", 0, trace ); 		CheckTrace( trace, 0 );
				cf.Apply( "", "write sms to 2222 ", 0, trace ); 	CheckTrace( trace, -1 );
				cf.Apply( "", "call 423291", 0, trace ); 			CheckTrace( trace, -1 );
				cf.Apply( "", "call +7 (423) 291 89", 0, trace ); 	CheckTrace( trace, -1 );
				cf.Apply( "", "call +1-423-29-21", 0, trace ); 		CheckTrace( trace, -1 );
				cf.Apply( "", "call 423 - 29-", 0, trace ); 		CheckTrace( trace, -1 );
				cf.Apply( "", "“.8-800-100-55-00.", 0, trace ); 	CheckTrace( trace, -1 );
				cf.Apply( "", "formula: 4434-853=9 ", 0, trace ); 	CheckTrace( trace, 0 );
				cf.Apply( "", "buy $10.00 and ", 0, trace ); 		CheckTrace( trace, 0 );
				cf.Apply( "", "buy 300.00 RU", 0, trace ); 			CheckTrace( trace, 0 );
			}
			{
				TestCustomFilter	cf = new TestCustomFilter();
				cf.AddReplacementWithSimilarChar();
				cf.Apply( "", " такси ", 0, trace ); 				CheckTrace( trace, 0 );
				cf.Apply( "", " тaкcи ", 0, trace ); 				CheckTrace( trace, -1 );
				cf.Apply( "", " п0дар0к ", 0, trace ); 				CheckTrace( trace, 0 );
			}
			{
				TestCustomFilter	cf = new TestCustomFilter();
				cf.AddNotPhoneSender();
				cf.Apply( "4433", "...", 0, trace ); 				CheckTrace( trace, 0 );
				cf.Apply( "sender", "...", 0, trace ); 				CheckTrace( trace, -1 );
			}
		}
	}
	

	public static void Apply (int mode, ArrayList<BaseFilter> filters, SmsMessageData sms, boolean res)
	{
		final FilterTrace trace = new FilterTrace();
		
		for (int i = 0; i < filters.size(); ++i)
		{
			BaseFilter bf = filters.get(i);
			
			trace.currGroup = i;
			
			bf.Apply( sms, trace );
		}
		
		final boolean 	cmp = _TraceAnalyzer( mode, trace ) != RES_BLOCKED_AS_SPAM;
		
		ASSERT( cmp == res );
	}
	

	public static void Apply (int mode, ArrayList<BaseFilter> filters, String sender,
								String msg, long time, boolean res)
	{
		Apply( mode, filters, new SmsMessageData( new MySmsMessage( sender, msg, time ),
				new ArrayList<String>() ), res );
	}
	
	
	public static void FullTest0 ()
	{
		ArrayList<BaseFilter>	filters = new ArrayList<BaseFilter>();

		final long	MILLIS_IN_MIN	= 60 * 1000;
		
		Date 	d 		= new Date(); // ( 0, 0, 0, 12, 0, 0 );
		long  	time	= (d.getMinutes() + d.getHours() * 60) * MILLIS_IN_MIN;
		long  	time2	= (d.getMinutes() + (d.getHours()-3) * 60) * MILLIS_IN_MIN;
		
		{
			TestNameFilter nf = new TestNameFilter( true );
			nf.AddNumber( "1111" );
			filters.add( nf );
		}
		{
			TestDateFilter	df = new TestDateFilter( true );
			df.AddItem( TestDateFilter.DateOffset( -2, 0 ), TestDateFilter.DateOffset( +2, 0 ) );
			filters.add( df );
		}
		{
			TestTextFilter	tf = new TestTextFilter( false );
			tf.AddWholeWordItem( "taxi" );
			filters.add( tf );
		}
		
		Apply( Preferences.AppPrefs.MODE_TYPE_MOST_TRIGGERED, filters, "1111", "...", time, true );
		Apply( Preferences.AppPrefs.MODE_TYPE_MOST_TRIGGERED, filters, "3333", "take taxi form", time, false );
		Apply( Preferences.AppPrefs.MODE_TYPE_MOST_TRIGGERED, filters, "1111", "take taxi form", time, true );
		Apply( Preferences.AppPrefs.MODE_TYPE_MOST_TRIGGERED, filters, "1111", "take taxi form", time2, false );

		Apply( Preferences.AppPrefs.MODE_TYPE_MOST_TRIGGERED, filters, "1111", "...", time2, true );
		Apply( Preferences.AppPrefs.MODE_TYPE_STRICT_TIME, 	  filters, "1111", "...", time2, false );
		
		Apply( Preferences.AppPrefs.MODE_TYPE_VERY_STRONG,	  filters, "1111", "take taxi form", time, false );
		
		//Apply( Preferences.AppPrefs.MODE_TYPE_FIRST_TRIGGERED, filters, "1111", "take taxi form", time2, true );
	}
	
	
	public static void FullTest1 ()
	{
		ArrayList<BaseFilter>	filters = new ArrayList<BaseFilter>();

		final long	MILLIS_IN_MIN	= 60 * 1000;
		
		Date 	d 		= new Date();
		long  	time	= (d.getMinutes() + d.getHours() * 60) * MILLIS_IN_MIN;	// millis
		
		{
			TestNameFilter nf = new TestNameFilter( true );
			nf.AddNumber( "1111" );
			filters.add( nf );
		}
		{
			TestDateFilter	df = new TestDateFilter( true );
			df.AddItem( TestDateFilter.DateOffset( -2, 0 ), TestDateFilter.DateOffset( +2, 0 ) );
			filters.add( df );
		}
		{
			TestTextFilter	tf = new TestTextFilter( false );
			tf.AddWholeWordItem( "taxi" );
			filters.add( tf );
		}
		
		Apply( Preferences.AppPrefs.MODE_TYPE_MOST_TRIGGERED, filters, "1111", "...", time, true );
		Apply( Preferences.AppPrefs.MODE_TYPE_MOST_TRIGGERED, filters, "3333", "take taxi form", time, false );
	}

	
	public static void RunTests ()
	{
		Logger.I( TAG, "Begin tests" );
		
		TestNameFilter.RunTests();
		TestDateFilter.RunTests();
		TestTextFilter.RunTests();
		TestCustomFilter.RunTests();
		
		FullTest0();
		FullTest1();
		
		Logger.I( TAG, "End tests" );
	}
	

	private static final void CheckTrace (FilterTrace trace, int res)
	{
		int cmp = 0;
		
		if ( trace.blocked != 0 || trace.passed != 0 ) {
			cmp = (trace.passed > trace.blocked) ? 1 : -1;
		}
		
		ASSERT( cmp == res );
		trace.Clear();
	}
	
	private static final void ASSERT (boolean expr)
	{
		if ( expr )
			return;
		
		Logger.W( TAG, "Assertion failed" );
		Assert.fail();
	}
	
}
