package linguo.example.com.linguo;

import android.app.Activity;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.rivescript.RiveScript;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.rivescript.Config.Builder.utf8;

import org.apache.commons.io.*;

/**
 * Created by christophrrb on 12/23/18.
 */

public class ChatFragment extends Fragment {

	public static final String CHAT_FRAGMENT_TAG = "ChatFragment";
	private RecyclerView mRecyclerView;
	private Activity mParentActivity;
	public static List<Message> mMessages = new ArrayList<Message>();

	private static int mMessagePosition = 0;

	private MessageAdapter mAdapter;
	private EditText mEditText;
	private ImageButton mSendButton;

	/**
	 * To not make mBot start the conversation upon rotating the device.
	 */
	private boolean mHelpMessage;
	public final String FIRST_RUN_KEY = "FIRST_RUN";
	public static RiveScript mBot = new RiveScript(utf8()
			.unicodePunctuation("[¿.,!?;:]")
			.build());

	public static ChatFragment mFragment;
	private boolean mGetName;
	private String mName;
	private boolean mGetReasonLearning;
	private String mReasonLearning;

	public static ChatFragment newInstance() {
		mFragment = new ChatFragment();
		return mFragment;
	}

	private SQLiteDatabase db;

	MediaPlayer mp;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = new DatabaseHelper(getContext()).getWritableDatabase();

		mHelpMessage = (savedInstanceState == null || savedInstanceState.getBoolean(FIRST_RUN_KEY));

		mp = MediaPlayer.create(getActivity().getApplicationContext(), R.raw.pop);

		// Load a directory full of RiveScript documents (.rive files) //TODO Make this a one-time operation.
		//To do this, copy every file in the assets folder to the getFilesDir() folder.

		AssetManager assetManager = getActivity().getAssets();
		try {
			String[] fileList = assetManager.list("chats");
			Log.d(CHAT_FRAGMENT_TAG, Arrays.toString(assetManager.list("chats")));
			new File(getActivity().getFilesDir() + File.separator + "chats").mkdirs();
			for (int i = 0; i < assetManager.list("chats").length; i++) {
				File f = new File(getActivity().getFilesDir() + File.separator + "chats" + File.separator + fileList[i]);
				f.createNewFile();
				Log.d(CHAT_FRAGMENT_TAG, getActivity().getFilesDir() + File.separator + fileList[i]);
				FileUtils.copyInputStreamToFile(assetManager.open("chats" + File.separator + fileList[i]), f);
			}

			mBot.loadDirectory(getActivity().getFilesDir() + File.separator + "chats");
		} catch (IOException e) {
			Log.d(CHAT_FRAGMENT_TAG, e.toString());
		}

		// Sort the replies after loading them!
		mBot.sortReplies();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_chat, container, false);

		mRecyclerView = v.findViewById(R.id.messages_recycler_view);
		LinearLayoutManager llm = new LinearLayoutManager(getActivity());
		mRecyclerView.setLayoutManager(llm);
		llm.setStackFromEnd(true);
		mParentActivity = getActivity();

		mEditText = v.findViewById(R.id.user_edit_text);
		mSendButton = v.findViewById(R.id.send_button);
		mSendButton.setEnabled(false);

		mEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				if (charSequence.length() > 0 && !(charSequence.toString().trim().equals("")))
					mSendButton.setEnabled(true);
				else
					mSendButton.setEnabled(false);
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}
		});

		mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int position = getMessagePosition();
				String editTextToString = mEditText.getText().toString().trim();
				if (mGetName) {
					mName = editTextToString;
					mGetName = false;
					mGetReasonLearning = true;
				} else if (mGetReasonLearning) {
					mReasonLearning = editTextToString;
					try {
						db.execSQL("INSERT INTO user_info('name', 'reason_learning') VALUES('" + mName + "', '" + mReasonLearning + "')"); //This looks kind of crazy because similarly to MySQL in PHP, even if you use String variables, they need to be quoted ''.
						Log.i(CHAT_FRAGMENT_TAG, mName + " and " + mReasonLearning + " were inserted into the database.");
					} catch (Exception e) {
						Log.e(CHAT_FRAGMENT_TAG, e.toString());
					}
					mGetReasonLearning = false;
				}
				mMessages.add(new Message(editTextToString, Message.USER_MESSAGE, messagePosition()));
				updateUI(position);
				mEditText.setText("");
				respond(editTextToString);
			}
		});

		updateUI(-1000);

		try {
			mMessages.get(0);
		} catch (IndexOutOfBoundsException ioe) { //Remember that this code only checks for an IndexOutOfBoundsException!
			//This code determines if the conversation should start from the introductory phase or not.
			String sGetName = "SELECT " + DatabaseHelper.NAME + " FROM " + DatabaseHelper.TABLE_NAME + " LIMIT 1";
			Cursor cursor = db.rawQuery(sGetName, null);
			String[] sResults = new String[1];
			if (cursor.moveToFirst()) {
				int i = 0;
				do {
					sResults[i] = cursor.getString(cursor.getColumnIndex(DatabaseHelper.NAME));
					i++;
				} while (cursor.moveToNext());

				mGetName = false;
				Log.i(CHAT_FRAGMENT_TAG, "sResults[0]: " + sResults[0]);

				mHelpMessage = true;
				mMessages.add(new Message("Hola " + sResults[0] + " \uD83D\uDC4B", Message.BOT_MESSAGE, messagePosition()));
				updateUI(getMessagePosition());
				respond("`ocstart`");
			} else {
				mGetName = true;
				mHelpMessage = false;
				respond("`start`");
			}


		}

		return v;
	}

	public void updateUI(int insertPosition) {
		if (mAdapter == null) {
			mAdapter = new MessageAdapter();
			mRecyclerView.setAdapter(mAdapter);
		} else
			mAdapter.notifyItemInserted(insertPosition);
			if (mAdapter.getItemCount() > 0) mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
			mp.start();

	}

	//User Message Holder
	public class UserMessageHolder extends RecyclerView.ViewHolder {

		private TextView mUserTextView;

		public UserMessageHolder(LayoutInflater inflater, ViewGroup parent) {
			super(inflater.inflate(R.layout.user_message, parent, false));

			//The super method gives a variable called itemView.

			mUserTextView = itemView.findViewById(R.id.user_text);

		}

		public void bind(Message message) {
			mUserTextView.setText(message.getText());
		}
	}

	//Bot Message Holder
	public class BotMessageHolder extends RecyclerView.ViewHolder {

		private TextView mBotTextView;

		public BotMessageHolder(LayoutInflater inflater, ViewGroup parent) {
			super(inflater.inflate(R.layout.bot_message, parent, false));

			mBotTextView = itemView.findViewById(R.id.bot_text);
		}

		public void bind(Message message) {
			mBotTextView.setText(message.getText());
		}
	}

	public class HelpMessageHolder extends RecyclerView.ViewHolder {

		private TextView mHelpMessageTextView;

		public HelpMessageHolder(LayoutInflater inflater, ViewGroup parent) {
			super(inflater.inflate(R.layout.help_message, parent, false));

			mHelpMessageTextView = itemView.findViewById(R.id.help_message);
		}

		public void bind(Message message) {
			mHelpMessageTextView.setText(message.getText());
		}
	}

	public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater layoutInflater = LayoutInflater.from(getActivity());

			switch (viewType) {
				case Message.USER_MESSAGE:
					return new UserMessageHolder(layoutInflater, parent);

				case Message.BOT_MESSAGE:
					return new BotMessageHolder(layoutInflater, parent);

				case HelpMessage.HELP_MESSAGE:
					return new HelpMessageHolder(layoutInflater, parent);

				default:
					return new BotMessageHolder(layoutInflater, parent);
			}
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			Message message = mMessages.get(position);

			switch (getItemViewType(position)) {
				case Message.USER_MESSAGE:
					((UserMessageHolder) holder).bind(message);
					break;

				case Message.BOT_MESSAGE:
					((BotMessageHolder) holder).bind(message);
					break;

				case HelpMessage.HELP_MESSAGE:
					((HelpMessageHolder) holder).bind(message);
					break;
			}
		}

		@Override
		public int getItemViewType(int position) {

			switch (mMessages.get(position).getType()) {
				case Message.USER_MESSAGE:
					return Message.USER_MESSAGE;

				case Message.BOT_MESSAGE:
					return Message.BOT_MESSAGE;

				case HelpMessage.HELP_MESSAGE:
					return HelpMessage.HELP_MESSAGE;

				default:
					return 4;
			}
		}

		@Override
		public int getItemCount() {
			return mMessages.size();
		}

	}

	public static int messagePosition() {
		int returnValue = mMessagePosition;
		mMessagePosition++;
		return returnValue;
	}

	public static int getMessagePosition() {
		return mMessagePosition;
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean(FIRST_RUN_KEY, mHelpMessage);
	}

	/**
	 * Takes a message (from user input or programmed input), gets the bot's reply, adds the bot's message to the message array, and calls updateUI(ChatFragment.messagePosition()).
	 * @param input Takes an input, either from the user or the bot.
	 */
	public void respond(String input) {
		String reply = mBot.reply("user", input);

		if (reply.contains("Con lo cual, ¿de qué quieres hablar?") || reply.contains("¿De qué quieres conversar?")) {
			mMessages.add(new Message(reply, Message.BOT_MESSAGE, messagePosition()));
			updateUI(getMessagePosition()); //TODO Add a delay for effect.
			mMessages.add(new Message("Ej: " + HelpMessage.getRandomMessage(), HelpMessage.HELP_MESSAGE, messagePosition()));
			updateUI(getMessagePosition());
		} else if (reply.startsWith("gc")) {
			respond("`language`");
		} else if (reply.equals("``getlength``")) {
			repeatedResponse(Integer.parseInt(mBot.reply("user", "`length`")));
		} else {
			final int position = getMessagePosition();
			mMessages.add(new Message(reply, Message.BOT_MESSAGE, messagePosition()));

//			//Handler to give a messaging effect.
//			try {
//				final Handler handler = new Handler();
//				handler.postDelayed(new Runnable() {
//					@Override
//					public void run() {
//						// Do something after 5s = 5000ms

//					}
//				}, 5000);
//			} catch (Exception e) {
//
//			}

			updateUI(position); //TODO Add a delay for effect.
		}
	}

	public void repeatedResponse(int iterations) {
		for (int i = 1; i <= iterations; i++) {
			int position = getMessagePosition();
			mMessages.add(new Message(mBot.reply("user", "`" + i + "`"), Message.BOT_MESSAGE, messagePosition()));
			updateUI(position);
		}

		mMessages.add(new Message(mBot.reply("user", "`ocstart`"), Message.BOT_MESSAGE, messagePosition()));
		updateUI(getMessagePosition());
		mMessages.add(new Message("Ej: " + HelpMessage.getRandomMessage(), HelpMessage.HELP_MESSAGE, messagePosition()));
		updateUI(getMessagePosition());
	}
}
