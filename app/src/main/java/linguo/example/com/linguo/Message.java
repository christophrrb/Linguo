package linguo.example.com.linguo;

/**
 * Created by christophrrb on 12/24/18.
 */

public class Message {
	public static final int USER_MESSAGE = 1;
	public static final int BOT_MESSAGE = 2;

	private int mType;
	private int mPosition;
	private String mText;

	public Message(int type, int position) {
		mText = "Non-text Message";
		mType = type;
		mPosition = position;
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
}
