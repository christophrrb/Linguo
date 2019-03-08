package linguo.example.com.linguo;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * Created by christophrrb on 12/24/18.
 */

public class Message {
	public static final int USER_MESSAGE = 1;
	public static final int BOT_MESSAGE = 2;
	public static final int HELP_MESSAGE = 3; //Same as HelpMessage.HELP_MESSAGE
	public static final int SOUND_MESSAGE = 4;
	public static final int IMAGE_MESSAGE = 5;
	public static final int TRANSLATION_MESSAGE = 6;


	private int mType;
	private int mPosition;
	private String mText;
	private int mResId;

	public Message(int resId, int type, int position) {
		mText = "Non-text Message";
		mType = type;
		mPosition = position;
		mResId = resId;
	}

	public Message(String text, int type, int position) {
		mText = text;
		mType = type;
		mPosition = position;
	}

	public int getType() {
		return mType;
	}

	public void setType(int type) {
		mType = type;
	}

	public int getPosition() {
		return mPosition;
	}

	public void setPosition(int position) {
		mPosition = position;
	}

	public String getText() {
		return mText;
	}

	public void setText(String text) {
		mText = text;
	}

	public int getResId() {
		return mResId;
	}
}
