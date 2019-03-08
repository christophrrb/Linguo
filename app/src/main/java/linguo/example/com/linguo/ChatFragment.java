package linguo.example.com.linguo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.rivescript.RiveScript;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.rivescript.Config.Builder.utf8;

/**
 * Created by christophrrb on 12/23/18.
 */

public class ChatFragment extends Fragment {

	/**
	 * This variable is used for some Log messages in Logcat.
	 * <br>
	 * {@value} is its value.
	 */
	public static final String CHAT_FRAGMENT_TAG = "ChatFragment";

	/**
	 * This variable is used to not start a conversation over upon having to redraw the layout due
	 * to exiting the app, rotating the screen, etc.
	 * <br>
	 * {@value} is its value.
	 */
	public final String FIRST_RUN_KEY = "FIRST_RUN";

	/**
	 * Parent Activity. Can be used to retrieve application context.
	 */
	private Activity mParentActivity;

	/**
	 * The current ChatFragment. This variable is used by the {@link ChatActivity} to create the
	 * Fragment.
	 */
	public static ChatFragment mFragment;

	/**
	 * Message RecyclerView
	 */
	private RecyclerView mRecyclerView;

	/**
	 * The MessageAdapter creates ViewHolders, binds Views, determines the type of View, and calls
	 * the ViewHolder's bind method.
	 */
	private MessageAdapter mAdapter;

	/**
	 * ArrayList that stores all Messages.
	 */
	public static List<Message> mMessages = new ArrayList<Message>();


	/**
	 * Determines whether a help message should display or not.
	 * @deprecated
	 */
	private boolean mHelpMessage;

	/**
	 * The RiveScript Chat Bot
	 */
	public static RiveScript mBot = new RiveScript(utf8()
			.unicodePunctuation("[¿.,!?;:]")
			.build());

	/**
	 * Determines the position of the message to be added in the third parameter of {@link Message}.
	 */
	private static int mMessagePosition = 0;

	/**
	 * This is the EditText to type in messages.
	 */
	private EditText mEditText;

	/**
	 * The Send Message Button
	 */
	private ImageButton mSendButton;

	/**
	 * MediaPlayer for voice messages. Used to create the pop sound for new messages and to play
	 * the recordings for sound messages.
	 */
	private MediaPlayer mp;

	/**
	 * This boolean determines whether the name of the user should be retrieved or not. If not, it
	 * greets the user with the name stored by the bot.
	 */
	private boolean mGetName;

	/**
	 * The user's name. Set either by the database, or if there is not one stored in the database,
	 * then form user input.
	 */
	private String mName;

	/**
	 * A variable to determine if we should store input for why the user is learning the language.
	 * @deprecated
	 */
	private boolean mGetReasonLearning;

	/**
	 * Why the user is learning the language.
	 * @deprecated
	 */
	private String mReasonLearning;

	/**
	 * The requested difficulty of the voice message.
	 */
	private String mDifficulty;

	/**
	 * Variable to determine of attempts should be counted.
	 */
	private boolean mStartAttempts = false;

	/**
	 * Number of attempts counted. Is set back to 0 when it is greater than 2 attempts.
	 */
	private int mAttempts = 0;

	/**
	 * A random sound from {@link Sound} with the difficulty that the user has requested.
	 */
	private Sound mSound;

	/**
	 * Determines if one of the conditionals in {@link #respond(String)} should be entered to get
	 * the difficulty for the sound message.
	 */
	private boolean mCheckForDifficulty;

	/**
	 * This database stores the user's name.
	 */
	private SQLiteDatabase db;

	private TextToSpeech tts;

	/**
	 * Used by ChatActivity to create a ChatFragment object
	 * @return An instance of ChatFragment
	 */
	public static ChatFragment newInstance() {
		mFragment = new ChatFragment();
		return mFragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		getActivity().setTheme(R.style.DarkTheme);

		tts = new TextToSpeech(getActivity().getApplicationContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status == TextToSpeech.ERROR)
					Log.e(CHAT_FRAGMENT_TAG, "Error in TTS");
				else
					tts.setLanguage(new Locale("es", "ES")); //Has to be done once the status has been successful, so that's why it should be in this onInit function.
			}
		});

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
		LayoutAnimationController animationController = AnimationUtils.loadLayoutAnimation(mRecyclerView.getContext(), R.anim.layout_item_slide);
		mRecyclerView.setLayoutAnimation(animationController);
//		mRecyclerView.setItemAnimator();
		mParentActivity = getActivity();
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
				mMessages.add(new Message(editTextToString, Message.USER_MESSAGE, mMessagePosition));
				updateUI(mMessagePosition++);
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
				mMessages.add(new Message("Hola " + sResults[0] + " \uD83D\uDC4B", Message.BOT_MESSAGE, mMessagePosition));
				updateUI(mMessagePosition++);
				respond("`ocstart`");
			} else {
				mGetName = true;
				mHelpMessage = false;
				respond("`start`");
			}


		}

		return v;
	}

	/**
	 * Creates an Adapter and calls the adapter to update for changes in the RecyclerView to due new
	 * messages.
	 * @param insertPosition Position where the new message was inserted.
	 */
	public void updateUI(final int insertPosition) {
		if (mAdapter == null) {
			mAdapter = new MessageAdapter();
			mRecyclerView.setAdapter(mAdapter);
		} else {
//			final Context context = mRecyclerView.getContext();
//			mRecyclerView.getAdapter().notifyItemChanged(insertPosition);
			mAdapter.notifyItemInserted(insertPosition);
		}

		if (mAdapter.getItemCount() > 0) mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
			mp.start();
	}

	public void updateUIRange(int insertRange) {
		mAdapter.notifyItemRangeInserted(mMessagePosition - insertRange, insertRange);
	}

	/**
	 * UserMessageHolder
	 */
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

	/**
	 * BotMessageHolder
	 */
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

	/**
	 * HelpMessageHolder
	 */
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

	/**
	 * SoundMessageHolder
	 */
	public class SoundMessageHolder extends RecyclerView.ViewHolder {
		private ImageButton mPlayButton;
		private ProgressBar mProgressBar;
		private MediaPlayer voice = null;
		/*
			The problem with sound seems to be that if the messages have to reload,
			SoundMessageHolder is told to set the message to the current sound, which could have
			changed if the user requested to do another sound practice.
		 */

		public SoundMessageHolder(LayoutInflater inflater, ViewGroup parent) {
			super(inflater.inflate(R.layout.sound_message, parent, false));

			mPlayButton = itemView.findViewById(R.id.play_button);
			mProgressBar = itemView.findViewById(R.id.sound_progress_bar);

			//This is the code to move the progress bar with respect to the progress of the message.
			final Handler handler = new Handler();
			final Runnable runnable = new Runnable() {

				@Override
				public void run() {
					try{
						mProgressBar.setProgress(voice.getCurrentPosition());
					}
					catch (Exception e) {
						// TODO: handle exception
					}
					finally{
						//also call the same runnable to call it at regular interval
						handler.postDelayed(this, 50);
					}
				}
			};

			mPlayButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					voice.start();
					handler.post(runnable);
				}
			});
		}

		public void bind(Message message) {
			voice = MediaPlayer.create(getActivity().getApplicationContext(), message.getResId());
			mProgressBar.setMax(voice.getDuration());
		}
	}

	/**
	 * ImageMessageHolder
	 */
	public class ImageMessageHolder extends RecyclerView.ViewHolder {
		private ImageView imageView;

		public ImageMessageHolder(LayoutInflater inflater, ViewGroup parent) {
			super(inflater.inflate(R.layout.image_message, parent, false));

			imageView = itemView.findViewById(R.id.message_image_view);
		}

		public void bind (Message message) {
//			imageView.setImageDrawable(getResources().getDrawable(message.getResId()));
			Glide.with(getContext()).load(message.getResId()).apply(new RequestOptions().transforms(new RoundedCorners(50))).into(imageView);
		}

//		imageView.setImageBitmap(
//					Bitmap.createBitmap(
//							BitmapFactory.decodeResource(
//									getResources(),
//									message.getResId())));
//		}
	}

	/**
	 * TranslateMessageHolder
	 */
	public class TranslationMessageHolder extends RecyclerView.ViewHolder {
		private TextView mTranslateMessageTextView;

		public TranslationMessageHolder(LayoutInflater inflater, ViewGroup parent) {
			super(inflater.inflate(R.layout.translation_message, parent, false));

			mTranslateMessageTextView = itemView.findViewById(R.id.translation_text);
			mTranslateMessageTextView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://translate.yandex.com/"));
					startActivity(browserIntent);
				}
			});
		}

		public void bind(Message message) {
			mTranslateMessageTextView.setText(message.getText());
		}
	}

	/**
	 * @see #mAdapter
	 */
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

				case Message.SOUND_MESSAGE:
					return new SoundMessageHolder(layoutInflater, parent);

				case Message.IMAGE_MESSAGE:
					return new ImageMessageHolder(layoutInflater, parent);

				case Message.TRANSLATION_MESSAGE:
					return new TranslationMessageHolder(layoutInflater, parent);

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

				case Message.SOUND_MESSAGE:
					((SoundMessageHolder) holder).bind(message);
					break;

				case Message.IMAGE_MESSAGE:
					((ImageMessageHolder) holder).bind(message);
					break;

				case Message.TRANSLATION_MESSAGE:
					((TranslationMessageHolder) holder).bind(message);
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

				case Message.SOUND_MESSAGE:
					return Message.SOUND_MESSAGE;

				case Message.IMAGE_MESSAGE:
					return Message.IMAGE_MESSAGE;

				case Message.TRANSLATION_MESSAGE:
					return Message.TRANSLATION_MESSAGE;

				default:
					return Message.BOT_MESSAGE;
			}
		}

		@Override
		public int getItemCount() {
			return mMessages.size();
		}

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean(FIRST_RUN_KEY, mHelpMessage);
	}

	/**
	 * Takes a message (from user input or programmed input), gets the bot's reply, adds the bot's message to the message array, and calls updateUI(ChatFragment.mMessagePosition).
	 * @param input Takes an input, either from the user or the bot.
	 */
	public String respond(String input) {
		String reply = mBot.reply("user", input);

		if (reply.contains("Con lo cual, ¿de qué quieres hablar?") || reply.contains("¿De qué quieres conversar?")) {
			mMessages.add(new Message(reply, Message.BOT_MESSAGE, mMessagePosition));
			tts.speak(mMessages.get(mMessagePosition).getText(), TextToSpeech.QUEUE_FLUSH, null);
			updateUI(mMessagePosition++); //TODO Add a delay for effect.
			mMessages.add(new Message("Ej: " + HelpMessage.getRandomMessage(), HelpMessage.HELP_MESSAGE, mMessagePosition));
			updateUI(mMessagePosition++);
		} else if (reply.startsWith("gc")) {
			respond("`language`");
		} else if (reply.equals("``getlength``")) {
			repeatedResponse(Integer.parseInt(mBot.reply("user", "`length`")));
		} else if (reply.equals("``sr``") || mCheckForDifficulty || mStartAttempts) {
			if (reply.equals("``sr``")) {
				respond("`srstart`");
				mCheckForDifficulty = true;
			} else if (reply.equals("fácil") || reply.equals("intermedio") || reply.equals("difícil")) {
				Log.i(CHAT_FRAGMENT_TAG, "mJumpToSr");
				mCheckForDifficulty = false;
				mStartAttempts = true;
				mDifficulty = reply;
				mSound = SoundMessage.getRandomSound(mDifficulty);
				mMessages.add(new Message(mSound.getResId(), Message.SOUND_MESSAGE, mMessagePosition));
				updateUI(mMessagePosition++);
				mBot.reply("user", "`srcontinuestart` " + mSound.getSpelling());
			} else if (mStartAttempts) {
				if (reply.equals("¡Qué bueno! Lo acertaste.")) {
					mMessages.add(new Message(reply, Message.BOT_MESSAGE, mMessagePosition));
					updateUI(mMessagePosition++);
					mStartAttempts = false;
					mAttempts = 0;
					respond("`ocstart`");
				} else if (mAttempts > 2) {
					Log.i(CHAT_FRAGMENT_TAG, "mAttempts");
					mStartAttempts = false;
					respond("`srquitwrong`");
					mAttempts = 0;
					respond("`ocstart`");
				} else {
					mMessages.add(new Message(reply, Message.BOT_MESSAGE, mMessagePosition));
					updateUI(mMessagePosition++);
					mAttempts++;
				}
			}

		} else if (reply.equals("``vc``")) {
			respond("`vcstart`");
			vocabularyRepeatedResponse();
		} else if (input.toLowerCase().contains("translate ") || input.toLowerCase().contains("traduce ")) {
			String stringToTranslate = input.substring(input.indexOf("e") + 2); //From the first letter of what's to be translated to the end of the String.
			new LoadTranslation().execute(stringToTranslate);
		} else {
				mMessages.add(new Message(reply, Message.BOT_MESSAGE, mMessagePosition));
				tts.speak(mMessages.get(mMessagePosition).getText(), TextToSpeech.QUEUE_FLUSH, null);

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

				updateUI(mMessagePosition++);
			}

		return reply;
	}

	/**
	 * Used to send multiple bot responses from one user input.
	 * @param iterations How many times the method should be run
	 */
	public void repeatedResponse(int iterations) {
		for (int i = 1; i <= iterations; i++) {
			mMessages.add(new Message(mBot.reply("user", "`" + i + "`"), Message.BOT_MESSAGE, mMessagePosition++));
		}

		mMessages.add(new Message(mBot.reply("user", "`ocstart`"), Message.BOT_MESSAGE, mMessagePosition++));
		mMessages.add(new Message("Ej: " + HelpMessage.getRandomMessage(), HelpMessage.HELP_MESSAGE, mMessagePosition++));

		updateUIRange(iterations + 2);
	}

	public void vocabularyRepeatedResponse() {
		int iterations = Integer.parseInt(mBot.reply("user", "`length`"));

		for (int i = 1; i<= iterations; i++) {
			/* I used Integer.parseInt() for the image and sound messages because the method is
			overloaded, mBot returns a String, so it would use the ID for a String and use the wrong
			method. */
			mMessages.add(new Message(getResources().getIdentifier(mBot.reply("user", "`" + i + " image`"), "drawable", getActivity().getPackageName()), Message.IMAGE_MESSAGE, mMessagePosition++));
			mMessages.add(new Message(mBot.reply("user", "`" + i + " text`"), Message.BOT_MESSAGE, mMessagePosition++));
			mMessages.add(new Message(getResources().getIdentifier(mBot.reply("user", "`" + i + " sound`"), "raw", getActivity().getPackageName()), Message.SOUND_MESSAGE, mMessagePosition++));
		}

		updateUI(iterations);
		respond("`ocstart`");
	}

	private class LoadTranslation extends AsyncTask<String, Void, String> {

		private Object obj = null;

		private String append = "";
		private BufferedReader in;
		private StringBuffer response;
		private String translationToReturn = "Perdón. Había un error.";

		@Override
		protected String doInBackground(String... strings) {
			try {
				append = strings[0];

				URI uri = new URI(
						"https",
						"translate.yandex.net",
						"/api/v1.5/tr.json/detect",
						"key=trnsl.1.1.20190308T015611Z.bb6bbcdd8a9023a8.d084dbbdfd5e4f4df4fcc3fbc9e40e2f19616afe&hint=es,en&text=" + append,
						null);

				URL url = uri.toURL();
				System.out.println(url.toString());

				Log.d(ChatFragment.CHAT_FRAGMENT_TAG, "Creating the connection");
				HttpURLConnection con = (HttpURLConnection) url.openConnection();

				Log.d(ChatFragment.CHAT_FRAGMENT_TAG, "Response Code: " + con.getResponseCode());

				in = new BufferedReader(new InputStreamReader(con.getInputStream()));

				String inputLine;
				response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}

				System.out.println("Response");
				System.out.println(response);

				in.close();

				obj = new JSONParser().parse(response.toString());

				JSONObject jo = (JSONObject) obj;

				String lang = ((String) jo.get("lang"));

				System.out.println(lang);

				String translateLang;
				if (lang.equals("es")) {
					translateLang = "en";
					tts.setLanguage(Locale.US);
				}
				else
					translateLang = "es";

				//Getting the translation
				URI translateURI = new URI(
						"https",
						"translate.yandex.net",
						"/api/v1.5/tr.json/translate",
						"key=trnsl.1.1.20190308T015611Z.bb6bbcdd8a9023a8.d084dbbdfd5e4f4df4fcc3fbc9e40e2f19616afe&lang=" + translateLang + "&text=" + append,
						null
				);

				URL translateURL = translateURI.toURL();

				System.out.println(translateURL.toString());

				HttpURLConnection translateCon = (HttpURLConnection) translateURL.openConnection();

				BufferedReader br = new BufferedReader(new InputStreamReader(translateCon.getInputStream()));

				StringBuffer translateResponse = new StringBuffer();

				String translateInputLine;

				while ((translateInputLine = br.readLine()) != null) {
					translateResponse.append(translateInputLine);
				}

				Object translationObj = new JSONParser().parse(translateResponse.toString());

				JSONObject translationJSONObject = (JSONObject) translationObj;

				System.out.println("Translation");

				translationToReturn = translationJSONObject.get("text").toString();
				System.out.println(translationToReturn);

			} catch (Exception e) {
				Log.e(ChatFragment.CHAT_FRAGMENT_TAG, e.toString());
			}

			return translationToReturn;
		}

		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);

			mMessages.add(new Message(s + "\n\n" + getResources().getString(R.string.yandex), Message.TRANSLATION_MESSAGE, mMessagePosition));
			tts.speak(s, TextToSpeech.QUEUE_FLUSH, null); //Speak the last message.
			tts.setLanguage(new Locale("es", "ES"));
			updateUI(mMessagePosition++);
		}
	}
}
