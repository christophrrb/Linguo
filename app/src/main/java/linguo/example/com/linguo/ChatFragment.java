package linguo.example.com.linguo;

import android.app.Activity;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
	private Button mSendButton;

	/**
	 * To not make mBot start the conversation upon rotating the device.
	 */
	private boolean mFirstRun;
	public final String FIRST_RUN_KEY = "FIRST_RUN";
	public static RiveScript mBot = new RiveScript(utf8()
			.unicodePunctuation("[¿.,!?;:]")
			.build());

	public static ChatFragment mFragment;
	public static ChatFragment newInstance() {
		mFragment = new ChatFragment();
		return mFragment;
	}

	private DatabaseHelper db;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = new DatabaseHelper(getContext());

		mFirstRun = (savedInstanceState == null || savedInstanceState.getBoolean(FIRST_RUN_KEY));

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

		mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int position = getMessagePosition();
				String editTextToString = mEditText.getText().toString();
				mMessages.add(new Message(editTextToString, Message.USER_MESSAGE, messagePosition()));
				updateUI(position);
				mEditText.setText("");
				respond(editTextToString);
			}
		});

		updateUI(-1000);

		try {
			mMessages.get(0);
		} catch (IndexOutOfBoundsException ioe) {
			respond("`start`");
			mFirstRun = false; /*I may not need this first run variable since I was using it initially to set this if condition,
			  but I'm looking at checking if the first element of the mMessages List is null instead
			  because with the mFirsRun variable, if the app was left (without onDestroy() being called),
			  it would type in `start` anyway.
			 */
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

	public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater layoutInflater = LayoutInflater.from(getActivity());

			if (viewType == Message.USER_MESSAGE)
				return new UserMessageHolder(layoutInflater, parent);
			else //(viewType == Message.BOT_MESSAGE)
				return new BotMessageHolder(layoutInflater, parent);
			//TODO Add a view for help messages.
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
			Message message = mMessages.get(position);

			if (getItemViewType(position) == Message.USER_MESSAGE)
				((UserMessageHolder) holder).bind(message);
			else
				((BotMessageHolder) holder).bind(message);
		}

		@Override
		public int getItemCount() {
			return mMessages.size();
		}

		@Override
		public int getItemViewType(int position) {
			if (mMessages.get(position).getType() == Message.USER_MESSAGE)
				return Message.USER_MESSAGE;
			else
				return Message.BOT_MESSAGE;
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
		savedInstanceState.putBoolean(FIRST_RUN_KEY, mFirstRun);
	}

	/**
	 * Takes a message (from user input or programmed input), gets the bot's reply, adds the bot's message to the message array, and calls updateUI(ChatFragment.messagePosition()).
	 * @param input Takes an input, either from the user or the bot.
	 */
	public void respond(String input) {
		String reply = mBot.reply("user", input);

		if (reply.startsWith("gc")) {
			respond("`language`");
		} else if (reply.equals("``getlength``")) {
			repeatedResponse(Integer.parseInt(mBot.reply("user", "`length`")));
		} else {
			int position = getMessagePosition();
			mMessages.add(new Message(reply, Message.BOT_MESSAGE, messagePosition()));
			updateUI(position);
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
	}
}
