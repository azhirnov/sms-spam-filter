package smsfilter.classes;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import smsfilter.app.D;
import smsfilter.app.FilterActivity;
import smsfilter.app.MyApplication;
import smsfilter.dialogs.Dialogs;
import smsfilter.dialogs.InboxViewer;
import smsfilter.dialogs.InboxViewer.InboxSms;
import smsfilter.utils.*;


public abstract class SmsFilterTypes extends FiltersGroup
{
// constants //
	protected static final int		SMS_FILTER_FOR_NAME		= 1;
	protected static final int		SMS_FILTER_FOR_TEXT		= 2;
	protected static final int		SMS_FILTER_FOR_DATE		= 3;
	protected static final int		SMS_FILTER_CUSTOM		= 4;
	
	// filter save flags
	public static final int			SAVE_FLAG_NONE			= 0;
	
	// blocked pos special
	public static final int			BLOCKED_POS_NONE		= 0;
	public static final int			BLOCKED_POS_SENDER		= -1;
	public static final int			BLOCKED_POS_TIME		= -2;
	public static final int			BLOCKED_POS_ALL_TEXT	= 0x0000FFFF;

	protected static final String	TAG						= "SmsFilters";
	private static final String		ERROR_IN_CONTACT		= "<invalid contact id>";
	
	
// types //
	
	//
	// Filter Trace
	//
	
	protected final static class FilterTraceItem
	{
		public int		groupId;
		public int		itemId;
		public int		filterType;
		public boolean	allowed;
		public int		posLength;		// position and length of blocked word in text 
		///
		public FilterTraceItem (int gr, int item, int type, int posLength, boolean allowed) {
			this.groupId 	= gr;
			this.itemId	 	= item;
			this.filterType = type;
			this.posLength 	= posLength;
			this.allowed 	= allowed;
		}
	}
	
	protected final static class FilterTrace
	{
		public ArrayList< FilterTraceItem >	items			= new ArrayList< FilterTraceItem >();
		public ArrayList< FilterTraceItem >	groups			= new ArrayList< FilterTraceItem >();
		public int							currGroup 		= 0;
		public int							passed			= 0;
		public int							blocked			= 0;
		public int							passedGroups	= 0;
		public int							blockedGroups	= 0;
		///
		public FilterTrace () {}
		
		public final void Add (int filterType, int itemId, int posLength, boolean allowed)
		{
			items.add( new FilterTraceItem( currGroup, itemId, filterType, posLength, allowed ) );
			if ( allowed )	++passed;
			else			++blocked;
		}
		
		public final void AddGroup (int filterType, int posLength, boolean allowed)
		{
			groups.add( new FilterTraceItem( currGroup, -1, filterType, posLength, allowed ) );
			if ( allowed )	++passedGroups;
			else			++blockedGroups;
		}
		
		public final void Clear () {
			items.clear();
			groups.clear();
			passed	  		= 0;
			blocked	  		= 0;
			currGroup 		= 0;
			passedGroups	= 0;
			blockedGroups 	= 0;
		}
	}
	
	
	//
	// Contact
	//
	
	protected static final class Contact implements Serializable
	{
		private static final long serialVersionUID = 1L;
		
		public String				id		= "";
		public String				name	= "";
		public ArrayList<String>	numbers	= new ArrayList<String>();
		
		public final boolean Contains (String number) {
			for (String n : numbers) {
				if ( number.equals( n ) )
					return true;
			}
			return false;
		}
	}
	
	
	//
	// Sms Message Data
	//
	public static final class SmsMessageData extends MySmsMessage
	{
	// variables //
		public final ArrayList<String>	contacts;
		public final int				time;		// hours + minutes
		
		
	// methods //
		public SmsMessageData (MySmsMessage sms, ArrayList<String> contacts)
		{
			super( sms );
			
			final long	MIN_IN_DAY 		= 24 * 60;
			final long	MILLIS_IN_MIN	= 60 * 1000;
			
			this.time = (int)((sms.timeMillis / MILLIS_IN_MIN) % MIN_IN_DAY);
			this.contacts = contacts;
		}
	}
	
	
	//
	// Base Filter
	//
	protected static abstract class BaseFilter extends FiltersGroup.Filter
	{
		// returns true if message can pass
		public abstract void		Apply (SmsMessageData sms, FilterTrace trace);
		public abstract void		Load (Object data, HashMap<String,Contact> contacts);
		public abstract Object		Save (int flags);
		public abstract void 		AddItem (FilterActivity act, SmsFilters smsFilters);
		public abstract int	 		GetFilterTypeId ();
		public abstract boolean 	IsAllowed ();
		public abstract boolean		IsEnabled ();
		public abstract void		SetEnabled (boolean enabled);
		public abstract void		DeleteItem (int index);
		public abstract void		OnItemEdit (int index, FilterActivity act, final SmsFilters smsFilters);
		public abstract boolean		AppendItems (BaseFilter filter);
	}
	
	
	//
	// Name Filter (Sender)
	//
	protected static class NameFilter extends BaseFilter
	{
		// constants
		protected static final char		NUMBER		 = 'ƾ';
		protected static final char		ALL_CONTACTS = 'ǂ';
		protected static final char		FROM_CONTACT = 'ƿ';
		protected static final char		NUMBER_RANGE = 'ʧ';
		protected static final char		PART_OF_NAME = 'Ȼ';
		
		// variables
		protected final HashMap<String,Contact>	_contacts = new HashMap< String, Contact >();
		protected String						_name;
		protected ArrayList<String>				_items;
		protected boolean						_allow;
		protected boolean						_enabled;
		
		
		// constructor
		public NameFilter (boolean allow)
		{
			_allow 	= allow;
			_items	= new ArrayList<String>();
			_name 	= MyApplication.GetString( _allow ? D.string.allow_sender : D.string.block_sender );
			_enabled = true;
		}

		// GetName
		@Override
		public final String GetName () {
			return _name;
		}

		// Apply
		@Override
		public final void Apply (SmsMessageData sms, FilterTrace trace)
		{
			if ( !_enabled )
				return;
			
			int	i = 0;
			int j = 0;
			
			for (String c : _items)
			{
				final char		fc  = c.charAt(0);
				final String	str = c.substring(1);
				
				switch ( fc )
				{
					case ALL_CONTACTS :
					{
						if ( ! sms.contacts.isEmpty() ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_SENDER, _allow );
							++j;
						}
						break;
					}
						
					case FROM_CONTACT :
					{
						Contact cnt = _contacts.get( str );
						
						if ( cnt == null ) {
							Logger.E( TAG, "unknown contact id: " + str );
							break;
						}

						for (String n : cnt.numbers) {
							if ( n.equalsIgnoreCase( sms.sender ) ) {
								trace.Add( GetFilterTypeId(), i, BLOCKED_POS_SENDER, _allow );
								++j;
								break;
							}
						}
						break;
					}
						
					case NUMBER :
					{
						if ( sms.sender.equalsIgnoreCase( str ) ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_SENDER, _allow );
							++j;
						}
						break;
					}
					
					case NUMBER_RANGE :
					{
						if ( sms.sender.startsWith( str ) ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_SENDER, _allow );
							++j;
						}
						break;
					}
					
					case PART_OF_NAME :
					{
						if ( StringUtils.FindSubStringSpecial( 	sms.sender.toUpperCase().toCharArray(),
																str.toUpperCase().toCharArray() ) )
						{
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_SENDER, _allow );
							++j;
						}
						break;
					}
					
					default :
						Logger.E( TAG, "unknown type of sender: \"" + c + "\"" );
						break;
				}
				
				++i;
			}
			
			if ( j == 0 ) {
				trace.AddGroup( GetFilterTypeId(), BLOCKED_POS_SENDER, !_allow );
			}
		}

		// AddItem
		@Override
		public final void AddItem (final FilterActivity activity, final SmsFilters smsFilters)
		{
			final Dialog	dlg = new Dialog( activity );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.add_sender_filter );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView title			= (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView addContact 	= (TextView) dlg.findViewById( D.id.addFromContacts );
			TextView addManual		= (TextView) dlg.findViewById( D.id.manualEntry );
			TextView allContacts	= (TextView) dlg.findViewById( D.id.allContacts );
			TextView numbRange		= (TextView) dlg.findViewById( D.id.numbersRange );
			TextView contactNumber	= (TextView) dlg.findViewById( D.id.addFromContactNumber );
			//TextView addFromCall 	= (TextView) dlg.findViewById( D.id.addFromCallLogs );
			TextView addInbox 		= (TextView) dlg.findViewById( D.id.addFromInbox );
			
			addInbox.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
					AddFromInbox( activity, smsFilters );
				}
			});
			/*
			addFromCall.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
					AddFromCallLogs( activity, smsFilters );
				}
			});
			*/
			addContact.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
					AddFromContacts( activity, smsFilters );
				}
			});
			addManual.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
					ManualAdd( activity, smsFilters );
				}
			});
			allContacts.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
					_AddItem( ""+ALL_CONTACTS, true );
					smsFilters._OnFilterChanged();
				}
			});
			numbRange.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
					ManualAddNumbersRange( activity, smsFilters );
				}
			});
			contactNumber.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
					AddContactFromNumber( activity, smsFilters );
				}
			});
			
			title.setText( D.string.add_sender_from );

			dlg.show();
		}

		// AddFromCallLogs
		//private void AddFromCallLogs (FilterActivity act, SmsFilters smsFilters) {}
		
		// AddFromInbox
		private final void AddFromInbox (FilterActivity act, final SmsFilters smsFilters)
		{
			InboxViewer	viewer = new InboxViewer( act );
			
			viewer.Show( new InboxViewer.OnItemChoosen() {
				@Override
				public void OnResult (InboxSms sms)
				{
					boolean	res = false;
					
					if ( sms.contactId != null )
					{
						Contact	cnt = new Contact();
						res = _UpdateContact( sms.contactId, cnt );
						
						final String	contId = cnt.id + "-" + MyApplication.GetDeviceId();
						
						if ( res && _AddItem( FROM_CONTACT + contId, true ) ) {
							_contacts.put( contId, cnt );
							res = true;
						}
					}

					if ( ! res ) {
						_AddItem( NUMBER + sms.sender, true );
					}
					smsFilters._OnFilterChanged();
				}
			}, null );
		}
		
		// AddContactFromNumber
		private final void AddContactFromNumber (final FilterActivity act, final SmsFilters smsFilters)
		{
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            
			act.StartActivityForResult( intent, new FilterActivity.ActivityResultCallback()
			{
				@Override
				public boolean OnResult (int resultCode, Intent data) {
					if ( resultCode == Activity.RESULT_OK )
					{
						final String[]	str = new String[1];
						
						if ( MyUtils.GetPhoneNumber( data, str ) ) {
							_AddItem( NUMBER + str[0], true );
							smsFilters._OnFilterChanged();
						}
					}
					return true;
				}
				
				@Override
				public int GetResultId () {
					return 0x102;
				}
			});
		}
		
		
		// AddFromContacts
		private final void AddFromContacts (final FilterActivity act, final SmsFilters smsFilters)
		{
			final Intent intent = new Intent( Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI );
			
			act.StartActivityForResult( intent, new FilterActivity.ActivityResultCallback()
			{
				@Override
				public boolean OnResult (int resultCode, Intent data) {
					if ( resultCode == Activity.RESULT_OK ) {
						try {
							Contact	cnt = new Contact();
							
							boolean	res = _GetContact( data.getData(), cnt );
							
							final String	contId = cnt.id + "-" + MyApplication.GetDeviceId();
							
							if ( res && _AddItem( FROM_CONTACT + contId, true ) )
								_contacts.put( contId, cnt );
							
							smsFilters._OnFilterChanged();
						}
						catch (Exception e) {
							Logger.OnCatchException( TAG, e );
						}
					}
					return true;
				}
				
				@Override
				public int GetResultId () {
					return 0x100;
				}
			});
		}

		// ManualAdd
		private final void ManualAdd (FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialog	dlg = new Dialog( act );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.manual_add_contact );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView title		= (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView btnAppend 	= (TextView) dlg.findViewById( D.id.btnAppendFilter );
			TextView btnCancel 	= (TextView) dlg.findViewById( D.id.btnCancelFilter );

			final EditText 		edit = (EditText) dlg.findViewById( D.id.filterSenderEdit );
			final RadioGroup	mode = (RadioGroup) dlg.findViewById( D.id.radioGroup1 );
			
			btnAppend.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v)
				{
					char modeId = ' ';
					
					switch ( mode.getCheckedRadioButtonId() )
					{
						case D.id.wholeWord :	modeId = NUMBER;		break;
						case D.id.partOfWord :	modeId = PART_OF_NAME;	break;
						default :				return;
					}
					
					_AddItem( modeId + edit.getText().toString(), true );
					dlg.dismiss();
					smsFilters._OnFilterChanged();
				}
			});
			
			btnCancel.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v) {
					dlg.dismiss();
				}
			});
			
			title.setText( _name );
			dlg.show();
		}

		// ManualAddNumbersRange
		private final void ManualAddNumbersRange (FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialog	dlg = new Dialog( act );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.manual_add_contact );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView title		= (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView btnAppend 	= (TextView) dlg.findViewById( D.id.btnAppendFilter );
			TextView btnCancel 	= (TextView) dlg.findViewById( D.id.btnCancelFilter );
			
			((TextView) dlg.findViewById( D.id.text )).setText( D.string.write_numbers_range );

			final EditText edit = (EditText) dlg.findViewById( D.id.filterSenderEdit );
			
			btnAppend.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v) {
					_AddItem( NUMBER_RANGE + edit.getText().toString(), true );
					dlg.dismiss();
					smsFilters._OnFilterChanged();
				}
			});
			
			btnCancel.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v) {
					dlg.dismiss();
				}
			});
			
			title.setText( _name );
			dlg.show();
		}
		
		// _AddItem
		private final boolean _AddItem (String item, boolean notify)
		{
			for (String s : _items) {
				if ( item.equalsIgnoreCase( s ) )
				{
					if ( notify ) {
						MyApplication.ShowToast( D.string.item_already_listed, Toast.LENGTH_LONG );
					}
					return false;
				}
			}
			_items.add( item );
			return true;
		}
		
		// AddSender
		public final void AddSender (String sender)
		{
			String[]	data = new String[2];
			
			if ( MyUtils.GetContactIdAndNameFromNumber( sender, data ) )
			{
				Contact	cnt = new Contact();
				boolean res = _UpdateContact( data[0], cnt );
				
				final String	contId = cnt.id + "-" + MyApplication.GetDeviceId();
				
				if ( res && _AddItem( FROM_CONTACT + contId, false ) ) {
					_contacts.put( contId, cnt );
					return;
				}
			}
			
			_AddItem( NUMBER + sender, false );
		}
		
		// AddAllContacts
		public final void AddAllContacts ()
		{
			_AddItem( ALL_CONTACTS + "", false );
		}

		// Load
		@SuppressWarnings("unchecked")
		@Override
		public final void Load (Object data, HashMap<String,Contact> contacts)
		{
			_contacts.clear();
			
			if ( data instanceof ArrayList<?> ) {
				_items = (ArrayList<String>) data;
			}
			else {
				Logger.E( TAG, "instance of data is not a ArrayList" );
				return;
			}
			
			for (String s : _items)
			{
				final char	c = s.charAt(0);

				if ( c == FROM_CONTACT )
				{
					String	key = s.substring(1);
					
					Contact cnt = (Contact) contacts.get( key );
					
					if ( cnt == null ) {
						Logger.E( TAG, "contact id: " + key + " not defined!" );
						continue;
					}
					
					_contacts.put( key, cnt );
				}
			}
		}

		// Save
		@Override
		public final Object Save (int flags)
		{
			return _items;
		}

		// GetFilterTypeId
		@Override
		public final int GetFilterTypeId () {
			return SMS_FILTER_FOR_NAME;
		}

		// GetItemText
		@Override
		public final String GetItemText (int index)
		{
			final String 	s = _items.get( index );
			final char		c = s.charAt(0);
			
			switch ( c )
			{
				case ALL_CONTACTS :
					return MyApplication.GetString( D.string.filter_all_contacts );
					
				case FROM_CONTACT :
				{
					Contact cnt = _contacts.get( s.substring(1) );
					
					if ( cnt != null )
						return cnt.name;
					
					return ERROR_IN_CONTACT;
				}
			}
			return s.substring(1);
		}
		
		// GetItemIcon
		@Override
		public final int GetItemIcon (int index) {
			final char	c = _items.get( index ).charAt(0);

			switch ( c )
			{
				case ALL_CONTACTS :	return D.drawable.i_all_contacts;
				case FROM_CONTACT :	return D.drawable.i_contact;
				case PART_OF_NAME :
				case NUMBER :		return D.drawable.i_phone_number;
				case NUMBER_RANGE :	return D.drawable.i_num_range;
			}
			
			return 0;
		}

		@Override
		public final int GetItemsCount () {
			return _items.size();
		}

		@Override
		public final boolean IsAllowed () {
			return _allow;
		}

		@Override
		public final boolean IsEnabled () {
			return _enabled;
		}

		@Override
		public final void SetEnabled (boolean enabled) {
			_enabled = enabled;
		}
		
		@Override
		public final void DeleteItem (int index) {
			_items.remove( index );
		}

		// OnItemEdit
		@Override
		public final void OnItemEdit (int index, FilterActivity act, final SmsFilters smsFilters)
		{
			final char	c = _items.get( index ).charAt(0);

			switch ( c )
			{
				case NUMBER :
				case PART_OF_NAME :
					_EditPhoneNumber( index, act, smsFilters, c );
					return;
				
				case NUMBER_RANGE :
					_EditPhoneNumbersRange( index, act, smsFilters );
					return;
					
				case FROM_CONTACT :
					_ViewContact( index, act, smsFilters );
					return;
					
				default :
				{
					final Dialogs.OkDialog	dlg = new Dialogs.OkDialog( act );
					dlg.GetTitleView().setText( _name );
					dlg.GetQuestionView().setText( GetItemText( index ) );
					dlg.Show();
				}
			}
		}

		// _ViewContact
		private final void _ViewContact (final int index, FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialogs.OkDialog	dlg = new Dialogs.OkDialog( act );

			final String	key   = _items.get( index ).substring(1);
			final Contact	value = _contacts.get( key );
			
			dlg.GetTitleView().setText( value.name );
			
			StringBuilder	builder = new StringBuilder();
			
			for (String n : value.numbers) {
				builder.append( n );
				builder.append( "\n" );
			}
			TextView quest = dlg.GetQuestionView();
			quest.setText( builder.toString() );
			quest.setMovementMethod( new ScrollingMovementMethod() );
			quest.setGravity( Gravity.CENTER_HORIZONTAL );
			quest.setLineSpacing( 0.0f, 1.3f );
			
			dlg.GetOKButton().setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.Close();
				}
			});
			
			dlg.Show();
		}
		
		// _EditPhoneNumber
		private final void _EditPhoneNumber (final int index, FilterActivity act, final SmsFilters smsFilters, char modeId)
		{
			final Dialog	dlg = new Dialog( act );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.manual_add_contact );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView title		= (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView btnAppend 	= (TextView) dlg.findViewById( D.id.btnAppendFilter );
			TextView btnCancel 	= (TextView) dlg.findViewById( D.id.btnCancelFilter );
			btnAppend.setText( D.string.btn_replace );

			final EditText 		edit = (EditText) dlg.findViewById( D.id.filterSenderEdit );
			final RadioGroup	mode = (RadioGroup) dlg.findViewById( D.id.radioGroup1 );
			
			edit.setText( _items.get( index ).substring(1) );

			int	radioBtnId = -1;
			
			switch ( modeId )
			{
				case NUMBER :		radioBtnId = D.id.wholeWord;	break;
				case PART_OF_NAME :	radioBtnId = D.id.partOfWord;	break;
			}
			mode.check( radioBtnId );
			
			btnAppend.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v)
				{
					char modeId = ' ';
					
					switch ( mode.getCheckedRadioButtonId() )
					{
						case D.id.wholeWord :	modeId = NUMBER;		break;
						case D.id.partOfWord :	modeId = PART_OF_NAME;	break;
						default :				return;
					}
					
					_items.set( index, modeId + edit.getText().toString() );
					dlg.dismiss();
					smsFilters._OnFilterChanged();
				}
			});
			
			btnCancel.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
				}
			});
			
			title.setText( _name );
			dlg.show();
		}
		
		// _EditPhoneNumbersRange
		private final void _EditPhoneNumbersRange (final int index, FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialog	dlg = new Dialog( act );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.manual_add_contact );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView title		= (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView btnAppend 	= (TextView) dlg.findViewById( D.id.btnAppendFilter );
			TextView btnCancel 	= (TextView) dlg.findViewById( D.id.btnCancelFilter );
			btnAppend.setText( D.string.btn_replace );
			
			((TextView) dlg.findViewById( D.id.text )).setText( D.string.write_numbers_range );

			final EditText edit = (EditText) dlg.findViewById( D.id.filterSenderEdit );
			edit.setText( _items.get( index ).substring(1) );
			
			btnAppend.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					_items.set( index, NUMBER_RANGE + edit.getText().toString() );
					dlg.dismiss();
					smsFilters._OnFilterChanged();
				}
			});
			
			btnCancel.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
				}
			});
			
			title.setText( _name );
			dlg.show();
		}
		
		// AppendItems
		@Override
		public final boolean AppendItems (BaseFilter filter)
		{
			if ( GetFilterTypeId() != filter.GetFilterTypeId() )
				return false;
			
			final NameFilter	nf = (NameFilter) filter;
			
			for (String s : nf._items) {
				_AddItem( s, false );
			}
			return true;
		}
		
		// RemoveUnusedContacts
		public final void RemoveUnusedContacts ()
		{
			HashMap< String, Contact >	used = new HashMap< String, Contact >();
			
			for (String s : _items)
			{
				final char	c = s.charAt(0);
				
				if ( c == FROM_CONTACT )
				{
					final String	key = s.substring(1);
					final Contact	value = _contacts.get( key );
					
					used.put( key, value );
				}
			}
			
			_contacts.clear();
			_contacts.putAll( used );
		}
		
		// UpdateContactsInfo
		public final void UpdateContactsInfo ()
		{
			final String	devId = MyApplication.GetDeviceId();
			
			for (Entry< String, Contact > en : _contacts.entrySet() )
			{
				final String key = en.getKey();
				final int	 div = key.indexOf('-');
				final String con = key.substring( 0, div );
				final String dId = key.substring( div+1 );

				if ( devId.equalsIgnoreCase( dId ) )
				{
					Contact cnt = new Contact();
					
					if ( _UpdateContact( con, cnt ) ) {
						_contacts.put( key, cnt );
					}
				}
			}
		}
		
		
		public final HashMap< String, Contact > GetContacts () {
			return _contacts;
		}
	}
	
	
	//
	// Date Filter
	//
	protected static class DateFilter extends BaseFilter
	{
		// types
		protected static final class TimeInterval implements Serializable
		{
			private static final long serialVersionUID = 1L;
		
			public int			from;	// minutes
			public int			to;
			private boolean		wrap;	// true if from > to
			///
			public TimeInterval (int from, int to) {
				this.from = from;
				this.to   = to;
				this.wrap = !( from < to );
			}
			
			public final String ToString () {
				final DateFormat df = new SimpleDateFormat( "HH:mm" );
				final String s = df.format( new Date( 0, 0, 0, from / 60, from % 60 ) ) + " - " +
								 df.format( new Date( 0, 0, 0, to / 60, to % 60 ) );
				return s;
			}
			
			public final boolean Check (Date d) {
				final int time = d.getHours() * 60 + d.getMinutes();
				return wrap ? IsOutter( time ) : IsInner( time );
			}
			
			public final boolean Check (int time) {
				return wrap ? IsOutter( time ) : IsInner( time );
			}
			
			private final boolean IsInner (int d) {
				return ( d >= from && d <= to );
			}
			
			private final boolean IsOutter (int d) {
				return ( d >= from || d <= to );
			}
			
			public final boolean Equal (TimeInterval right) {
				return 	this.from == right.from &&
						this.to   == right.to   &&
						this.wrap == right.wrap;
			}
		}
		
		
		// variables
		protected String					_name;
		protected ArrayList<TimeInterval>	_items;
		protected boolean					_allow;
		protected boolean					_enabled;
		
		
		// constructor
		public DateFilter (boolean allow)
		{
			_allow 	= allow;
			_items	= new ArrayList<TimeInterval>();
			_name	= MyApplication.GetString( _allow ? D.string.allow_time : D.string.block_time );
			_enabled = true;
		}

		// GetName
		@Override
		public final String GetName () {
			return _name;
		}

		// Apply
		@Override
		public final void Apply (SmsMessageData sms, FilterTrace trace)
		{
			if ( !_enabled )
				return;
			
			int	i = 0;
			int j = 0;
			
			for (TimeInterval ti : _items)
			{
				if ( ti.Check( sms.time ) )
				{
					++j;
					trace.Add( GetFilterTypeId(), i, BLOCKED_POS_TIME, _allow );
				}
				++i;
			}

			if ( j == 0 ) {
				trace.AddGroup( GetFilterTypeId(), BLOCKED_POS_TIME, !_allow );
			}
		}

		// AddItem
		@Override
		public final void AddItem (FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialog	dlg = new Dialog( act );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.add_time_filter );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView title		= (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView btnAppend 	= (TextView) dlg.findViewById( D.id.btnAppendFilter );
			TextView btnCancel 	= (TextView) dlg.findViewById( D.id.btnCancelFilter );

			final TimePicker	timeFrom = (TimePicker) dlg.findViewById( D.id.timePicker1 );
			final TimePicker	timeTo   = (TimePicker) dlg.findViewById( D.id.timePicker2 );
			
			timeFrom.setIs24HourView( true );
			timeTo.setIs24HourView( true );
			
			btnAppend.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v)
				{
					int	dfrom = timeFrom.getCurrentHour() * 60 + timeFrom.getCurrentMinute();
					int	dto   = timeTo.getCurrentHour() * 60 +   timeTo.getCurrentMinute();
					
					_AddItem( new TimeInterval( dfrom, dto ), true );
					dlg.dismiss();
					smsFilters._OnFilterChanged();
				}
			});
			
			btnCancel.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v) {
					dlg.dismiss();
				}
			});
			
			title.setText( _name );
			dlg.show();
		}
		
		// _AddItem
		private final void _AddItem (TimeInterval item, boolean notify)
		{
			for (TimeInterval s : _items) {
				if ( item.Equal( s ) )
				{
					if ( notify ) {
						MyApplication.ShowToast( D.string.item_already_listed, Toast.LENGTH_LONG );
					}
					return;
				}
			}
			_items.add( item );
		}

		// Load
		@SuppressWarnings("unchecked")
		@Override
		public final void Load (Object data, HashMap<String,Contact> contacts)
		{
			if ( data instanceof ArrayList<?> ) {
				_items = (ArrayList<TimeInterval>) data;
			}
			else {
				Logger.E( TAG, "instance of data is not a ArrayList" );
			}
		}

		// Save
		@Override
		public final Object Save (int flags) {
			return _items;
		}

		@Override
		public final int GetFilterTypeId () {
			return SMS_FILTER_FOR_DATE;
		}

		@Override
		public final String GetItemText (int index) {
			return _items.get( index ).ToString();
		}

		@Override
		public final int GetItemIcon (int index) {
			return D.drawable.i_time;
		}
		
		@Override
		public final int GetItemsCount () {
			return _items.size();
		}

		@Override
		public final boolean IsAllowed () {
			return _allow;
		}

		@Override
		public final boolean IsEnabled () {
			return _enabled;
		}

		@Override
		public final void SetEnabled (boolean enabled) {
			_enabled = enabled;
		}

		@Override
		public final void DeleteItem (int index) {
			_items.remove( index );
		}

		@Override
		public void OnItemEdit (final int index, FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialog	dlg = new Dialog( act );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.add_time_filter );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView title		= (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView btnAppend 	= (TextView) dlg.findViewById( D.id.btnAppendFilter );
			TextView btnCancel 	= (TextView) dlg.findViewById( D.id.btnCancelFilter );
			btnAppend.setText( D.string.btn_replace );
			
			final TimePicker	timeFrom = (TimePicker) dlg.findViewById( D.id.timePicker1 );
			final TimePicker	timeTo   = (TimePicker) dlg.findViewById( D.id.timePicker2 );
			
			timeFrom.setIs24HourView( true );
			timeTo.setIs24HourView( true );
			
			TimeInterval	ti = _items.get( index );
			timeFrom.setCurrentHour( ti.from / 60 );
			timeFrom.setCurrentMinute( ti.from % 60 );
			timeTo.setCurrentHour( ti.to / 60 );
			timeTo.setCurrentMinute( ti.to % 60 );
			
			btnAppend.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v)
				{
					int	dfrom = timeFrom.getCurrentHour() * 60 + timeFrom.getCurrentMinute();
					int	dto   = timeTo.getCurrentHour() * 60 +   timeTo.getCurrentMinute();
					
					_items.set( index, new TimeInterval( dfrom, dto ) );
					dlg.dismiss();
					smsFilters._OnFilterChanged();
				}
			});
			
			btnCancel.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
				}
			});
			
			title.setText( _name );
			dlg.show();
		}
		
		@Override
		public final boolean AppendItems (BaseFilter filter)
		{
			if ( GetFilterTypeId() != filter.GetFilterTypeId() )
				return false;
			
			final DateFilter	nf = (DateFilter) filter;
			
			for (TimeInterval s : nf._items) {
				_AddItem( s, false );
			}
			return true;
		}
	}
	
	
	//
	// Text Filter
	//
	protected static class TextFilter extends BaseFilter
	{
		// constants
		protected static final char		PART_OF_WORD	= 'ƪ';
		protected static final char		WHOLE_WORD		= 'ƺ';
		protected static final char		REG_EXP			= 'Ħ';
		
		// variables
		protected String				_name;
		protected ArrayList<String>		_items;
		protected boolean				_allow;
		protected boolean				_enabled;
		
		
		// constructor
		public TextFilter (boolean allow)
		{
			_allow 	= allow;
			_items	= new ArrayList<String>();
			_name	= MyApplication.GetString( _allow ? D.string.allow_content : D.string.block_content );
			_enabled = true;
		}

		// GetName
		@Override
		public final String GetName () {
			return _name;
		}

		// Apply
		@Override
		public final void Apply (SmsMessageData sms, FilterTrace trace)
		{
			if ( !_enabled )
				return;
			
			int	i = 0;
			int	j = 0;
			
			final char[]	textUpper 	= sms.body.toUpperCase().toCharArray();
			
			for (String c : _items)
			{
				final char		fc  	 = c.charAt(0);
				final String	str 	 = c.substring(1);
				final char[]	csUpper = str.toUpperCase().toCharArray();
				
				switch ( fc )
				{
					case PART_OF_WORD :
					{
						if ( StringUtils.FindSubStringSpecial( textUpper, csUpper ) ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_ALL_TEXT, _allow );
							++j;
						}
						break;
					}
					
					case WHOLE_WORD :
					{
						if ( StringUtils.FindWordSpecial( textUpper, csUpper ) ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_ALL_TEXT, _allow );
							++j;
						}
						break;
					}
					
					case REG_EXP :
					{
						if ( sms.body.matches( str ) ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_ALL_TEXT, _allow );
							++j;
						}
						break;
					}
					
					default :
						Logger.E( TAG, "unknown type of content word: \"" + fc + "\"" );
						break;
				}
				
				++i;
			}

			if ( j == 0 ) {
				trace.AddGroup( GetFilterTypeId(), BLOCKED_POS_ALL_TEXT, !_allow );
			}
		}

		// AddItem
		@Override
		public final void AddItem (FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialog	dlg = new Dialog( act );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.add_content_filter );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView 	title		 = (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView 	btnAppend 	 = (TextView) dlg.findViewById( D.id.btnAppendFilter );
			TextView 	btnCancel 	 = (TextView) dlg.findViewById( D.id.btnCancelFilter );
			
			final RadioGroup	mode = (RadioGroup) dlg.findViewById( D.id.radioGroup1 );
			final EditText		edit = (EditText) dlg.findViewById( D.id.filterKeyEdit );
			
			btnAppend.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v)
				{
					char modeId = ' ';
					
					switch ( mode.getCheckedRadioButtonId() )
					{
						case D.id.wholeWord :	modeId = WHOLE_WORD;	break;
						case D.id.partOfWord :	modeId = PART_OF_WORD;	break;
						case D.id.regExp :		modeId = REG_EXP;		break;
						default :				return;
					}
					
					_AddItem( modeId + edit.getText().toString(), true );
					dlg.dismiss();
					smsFilters._OnFilterChanged();
				}
			});
			btnCancel.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v) {
					dlg.dismiss();
				}
			});
			
			title.setText( _name );
			dlg.show();
		}
		
		// _AddItem
		private final void _AddItem (String item, boolean notify)
		{
			for (String s : _items) {
				if ( item.substring(1).equalsIgnoreCase( s.substring(1) ) )
				{
					if ( notify ) {
						MyApplication.ShowToast( D.string.item_already_listed, Toast.LENGTH_LONG );
					}
					return;
				}
			}
			
			_items.add( item );
		}

		// Load
		@SuppressWarnings("unchecked")
		@Override
		public final void Load (Object data, HashMap<String,Contact> contacts)
		{
			if ( data instanceof ArrayList<?> ) {
				_items = (ArrayList<String>) data;
			}
			else {
				Logger.E( TAG, "instance of data is not a ArrayList" );
			}
		}

		@Override
		public final Object Save (int flags) {
			return _items;
		}

		@Override
		public final int GetFilterTypeId () {
			return SMS_FILTER_FOR_TEXT;
		}

		@Override
		public final String GetItemText (int index) {
			return _items.get( index ).substring(1);
		}
		
		@Override
		public final int GetItemIcon (int index) {
			final char	c  = _items.get( index ).charAt(0);

			switch ( c )
			{
				case PART_OF_WORD :	return D.drawable.i_word;
				case WHOLE_WORD :	return D.drawable.i_word;
				case REG_EXP :		return D.drawable.i_regexp;
			}
			return 0;
		}

		@Override
		public final int GetItemsCount () {
			return _items.size();
		}

		@Override
		public final boolean IsAllowed () {
			return _allow;
		}

		@Override
		public final boolean IsEnabled () {
			return _enabled;
		}

		@Override
		public final void SetEnabled (boolean enabled) {
			_enabled = enabled;
		}

		@Override
		public final void DeleteItem (int index) {
			_items.remove( index );
		}

		@Override
		public final void OnItemEdit (final int index, FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialog	dlg = new Dialog( act );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.add_content_filter );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView 	title		 = (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView 	btnAppend 	 = (TextView) dlg.findViewById( D.id.btnAppendFilter );
			TextView 	btnCancel 	 = (TextView) dlg.findViewById( D.id.btnCancelFilter );
			btnAppend.setText( D.string.btn_replace );
			
			final RadioGroup	mode = (RadioGroup) dlg.findViewById( D.id.radioGroup1 );
			final EditText		edit = (EditText) dlg.findViewById( D.id.filterKeyEdit );
			
			final char	c = _items.get(index).charAt(0);
			int	radioBtnId = -1;
			
			switch ( c )
			{
				case WHOLE_WORD :	radioBtnId = D.id.wholeWord;	break;
				case PART_OF_WORD :	radioBtnId = D.id.partOfWord;	break;
				case REG_EXP :		radioBtnId = D.id.regExp;		break;
			}
			edit.setText( _items.get(index).substring(1) );
			mode.check( radioBtnId );
			
			btnAppend.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v)
				{
					char modeId = ' ';
					
					switch ( mode.getCheckedRadioButtonId() )
					{
						case D.id.wholeWord :	modeId = WHOLE_WORD;	break;
						case D.id.partOfWord :	modeId = PART_OF_WORD;	break;
						case D.id.regExp :		modeId = REG_EXP;		break;
					}
					
					_items.set( index, modeId + edit.getText().toString() );
					dlg.dismiss();
					smsFilters._OnFilterChanged();
				}
			});
			btnCancel.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick (View v) {
					dlg.dismiss();
				}
			});
			
			title.setText( _name );
			dlg.show();
		}
		
		@Override
		public final boolean AppendItems (BaseFilter filter)
		{
			if ( GetFilterTypeId() != filter.GetFilterTypeId() )
				return false;
			
			final TextFilter	nf = (TextFilter) filter;
			
			for (String s : nf._items) {
				_AddItem( s, false );
			}
			return true;
		}
	}
	
	
	
	//
	// Custom Filter
	//
	protected static class CustomFilter extends BaseFilter
	{
		// constants
		protected static final char		BLOCK_URL			= 'ş';
		protected static final char		PHONE_NUMBER		= 'ǝ';
		protected static final char		DIFF_LANG_IN_WORD	= 'Ŧ';
		protected static final char		NOT_PHONE_SENDER	= 'Ƽ';
		
		
		// variables
		protected String				_name;
		protected ArrayList<String>		_items;
		protected final boolean			_allow;
		protected boolean				_enabled;
		
		
		// constructor
		public CustomFilter ()
		{
			_allow 	= false;
			_items	= new ArrayList<String>();
			_name	= MyApplication.GetString( D.string.custom_filters );
			_enabled = true;
		}

		// GetName
		@Override
		public final String GetName () {
			return _name;
		}

		// Apply
		@Override
		public final void Apply (SmsMessageData sms, FilterTrace trace)
		{
			if ( !_enabled )
				return;
			
			int	i = 0;
			int	j = 0;
			
			final char[]	text	= sms.body.toCharArray();
			final char[]	sender	= sms.sender.toCharArray();
			
			for (String c : _items)
			{
				final char	fc  = c.charAt(0);
				
				switch ( fc )
				{
					case BLOCK_URL :
					{
						if ( StringUtils.FindUrlsInString( sms.body.toUpperCase() ) ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_ALL_TEXT, _allow );
							++j;
						}
						break;
					}
					
					case PHONE_NUMBER :
					{
						if ( StringUtils.FindNumber( text, 4 ) ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_ALL_TEXT, _allow );
							++j;
						}
						break;
					}
					
					case DIFF_LANG_IN_WORD :
					{
						if ( StringUtils.HasLanguageSwitchingInWord( text ) ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_ALL_TEXT, _allow );
							++j;
						}
						break;
					}
					
					case NOT_PHONE_SENDER :
					{
						if ( ! StringUtils.IsPhoneNumber( sender ) ) {
							trace.Add( GetFilterTypeId(), i, BLOCKED_POS_SENDER, _allow );
							++j;
						}
						break;
					}
					
					default :
						Logger.E( TAG, "unknown type of content word: \"" + fc + "\"" );
						break;
				}
				
				++i;
			}

			if ( j == 0 ) {
				trace.AddGroup( GetFilterTypeId(), BLOCKED_POS_NONE, !_allow );
			}
		}

		// AddItem
		@Override
		public final void AddItem (FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialog	dlg = new Dialog( act );
			dlg.requestWindowFeature( Window.FEATURE_NO_TITLE );
			dlg.setContentView( D.layout.add_custom_filter );
			dlg.setCancelable( true );
			dlg.setCanceledOnTouchOutside( false );
			
			TextView	title			= (TextView) dlg.findViewById( D.id.dlgTitle );
			TextView	blockUrls		= (TextView) dlg.findViewById( D.id.blockUrls );
			TextView	numberInText	= (TextView) dlg.findViewById( D.id.numberInText );
			TextView	diffLangInWord	= (TextView) dlg.findViewById( D.id.diffLangInWord );
			TextView	senderIsntNumber= (TextView) dlg.findViewById( D.id.senderIsntNumber );
			
			blockUrls.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v) {
					_AddItem( BLOCK_URL, true );
					smsFilters._OnFilterChanged();
					dlg.dismiss();
				}
			});
			numberInText.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v) {
					_AddItem( PHONE_NUMBER, true );
					smsFilters._OnFilterChanged();
					dlg.dismiss();
				}
			});
			diffLangInWord.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v) {
					_AddItem( DIFF_LANG_IN_WORD, true );
					smsFilters._OnFilterChanged();
					dlg.dismiss();
				}
			});
			senderIsntNumber.setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick (View v) {
					_AddItem( NOT_PHONE_SENDER, true );
					smsFilters._OnFilterChanged();
					dlg.dismiss();
				}
			});
			
			title.setText( _name );
			dlg.show();
		}
		
		// _AddItem
		private final void _AddItem (char c, boolean notify) {
			for (String s : _items) {
				if ( s.charAt(0) == c )
				{
					if ( notify ) {
						MyApplication.ShowToast( D.string.item_already_listed, Toast.LENGTH_LONG );
					}
					return;
				}
			}
			_items.add( "" + c );
		}
		
		public final void AddBlockUrls ()					{ _AddItem( BLOCK_URL, false ); }
		public final void AddBlockPhoneNumber () 			{ _AddItem( PHONE_NUMBER, false ); }
		public final void AddNotPhoneSender ()				{ _AddItem( NOT_PHONE_SENDER, false ); }
		public final void AddReplacementWithSimilarChar ()	{ _AddItem( DIFF_LANG_IN_WORD, false ); }

		// Load
		@SuppressWarnings("unchecked")
		@Override
		public final void Load (Object data, HashMap<String,Contact> contacts)
		{
			if ( data instanceof ArrayList<?> ) {
				_items = (ArrayList<String>) data;
			}
			else {
				Logger.E( TAG, "instance of data is not a ArrayList" );
			}
		}

		@Override
		public final Object Save (int flags) {
			return (Object) _items;
		}

		@Override
		public final int GetFilterTypeId () {
			return SMS_FILTER_CUSTOM;
		}

		// GetItemText
		@Override
		public final String GetItemText (int index) {
			final char	fc  = _items.get( index ).charAt(0);

			switch ( fc )
			{
				case BLOCK_URL :
					return MyApplication.GetString( D.string.custom_block_url );
					
				case PHONE_NUMBER :
					return MyApplication.GetString( D.string.custom_number_in_text );
					
				case DIFF_LANG_IN_WORD :
					return MyApplication.GetString( D.string.custom_diff_lang_in_word );
					
				case NOT_PHONE_SENDER :
					return MyApplication.GetString( D.string.custom_sender_isnt_number );
			}
			return "";
		}
		
		// GetItemIcon
		@Override
		public final int GetItemIcon (int index) {
			final char	fc  = _items.get( index ).charAt(0);

			switch ( fc )
			{
				case BLOCK_URL :			return D.drawable.i_block_url;
				case PHONE_NUMBER :			return D.drawable.i_block_phone_number;
				case DIFF_LANG_IN_WORD  :	return D.drawable.i_similar_char;
				//case NOT_PHONE_SENDER :		return R.drawable.i_;	// TODO
			}
			return 0;
		}

		@Override
		public final int GetItemsCount () {
			return _items.size();
		}

		@Override
		public final boolean IsAllowed () {
			return _allow;
		}

		@Override
		public final boolean IsEnabled () {
			return _enabled;
		}

		@Override
		public final void SetEnabled (boolean enabled) {
			_enabled = enabled;
		}

		@Override
		public final void DeleteItem (int index) {
			_items.remove( index );
		}

		@Override
		public final void OnItemEdit (final int index, FilterActivity act, final SmsFilters smsFilters)
		{
			final Dialogs.OkDialog	dlg = new Dialogs.OkDialog( act );

			final char	fc   = _items.get( index ).charAt(0);
			String		text = "";

			switch ( fc )
			{
				case BLOCK_URL :
					text = MyApplication.GetString( D.string.flt_info_block_urls );
					break;
				
				case PHONE_NUMBER :
					text = MyApplication.GetString( D.string.flt_info_phone_number );
					break;
					
				case DIFF_LANG_IN_WORD :
					text = MyApplication.GetString( D.string.flt_info_replacemt_char );
					break;
					
				case NOT_PHONE_SENDER :
					text = MyApplication.GetString( D.string.flt_info_sender_is_number );
					break;
			}
			
			dlg.GetTitleView().setText( _name );
			dlg.GetQuestionView().setText( text );
			dlg.GetQuestionView().setMovementMethod( new ScrollingMovementMethod() );
			dlg.Show();
		}
		
		// AppendItems
		@Override
		public final boolean AppendItems (BaseFilter filter)
		{
			if ( GetFilterTypeId() != filter.GetFilterTypeId() )
				return false;
			
			final CustomFilter	nf = (CustomFilter) filter;
			
			for (String s : nf._items) {
				_AddItem( s.charAt(0), false );
			}
			return true;
		}
	}
	
	
// variables //
	
	protected MyExpandableListAdapter			_adapter		= null;
	
	
	
// methods //
	
	// SetAdapter
	@Override
	public final void SetAdapter (MyExpandableListAdapter adapter) {
		_adapter = adapter;
	}
	
	
	// _Refresh
	public final void Refresh () {
		if ( _adapter != null )
			_adapter.notifyDataSetChanged();
	}
	
	
	// _OnFilterChanged
	protected final void _OnFilterChanged ()
	{
		Refresh();
		Save( null, SAVE_FLAG_NONE );
	}
	
	
	// UpdateContact
	public final static
	boolean _UpdateContact (String contactId, Contact cnt)
	{
		return _GetContact( Uri.parse( "content://com.android.contacts/contacts/" + contactId ), cnt );
	}
	
	
	// GetContact
	public final static
	boolean _GetContact (Uri contactData, Contact cnt)
	{
        Cursor 	c	 	= null;
        Cursor	c1		= null;
        String	hasPhone = "";
		
		try {
			Context	ctx = MyApplication.GetContext();
			
			ContentResolver cr = ctx.getContentResolver();
			
			c = cr.query( contactData, null, null, null, null );
			
			if ( c.moveToFirst() )
			{
			    cnt.id   = c.getString( c.getColumnIndex( ContactsContract.Contacts._ID ) );
			    cnt.name = c.getString( c.getColumnIndex( ContactsContract.Contacts.DISPLAY_NAME ) );
			    hasPhone = c.getString( c.getColumnIndex( ContactsContract.Contacts.HAS_PHONE_NUMBER ) );
			}
			
			c1 = cr.query(
		            CommonDataKinds.Phone.CONTENT_URI, 
		            null, 
		            CommonDataKinds.Phone.CONTACT_ID +" = ?", 
		            new String[]{ cnt.id }, null );

		    while ( c1.moveToNext() )
		    {
		    	cnt.numbers.add( c1.getString( c1.getColumnIndex( CommonDataKinds.Phone.NUMBER ) ) );
		    } 
		}
		catch (Exception e) {
			Logger.OnCatchException( TAG, e );
		}
        finally {
	        if ( c != null ) {
	            c.close();
	            c = null;
	        }
	        if ( c1 != null ) {
	        	c1.close();
	        	c1 = null;
	        }
        }
		
		return hasPhone.equals("1");
	}
	
	
// interface //
	
	protected abstract boolean Save (String filename, int flags);
	
}
