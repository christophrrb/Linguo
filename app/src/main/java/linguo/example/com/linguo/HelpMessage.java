package linguo.example.com.linguo;

import java.util.Random;

public class HelpMessage {

	public static final int HELP_MESSAGE = 3;

	public static String[] helpMessageArray = {
			"Quiero ayuda con el pretérito.",
			"Necesito ayuda con el pretérito.",
			"Quiero ayuda con el imperfecto.",
			"Necesito ayuda con el imperfecto.",
			"Quiero ayuda con por y para.",
			"Necesito ayuda con por y para."
	};

	public static String getRandomMessage() {
		Random random = new Random();
		return helpMessageArray[random.nextInt(helpMessageArray.length)];
	}
}
