package linguo.example.com.linguo;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Creates Sound objects and is a random retriever for them also.
 */
public class SoundMessage {
	public static final int SOUND_MESSAGE = 4;
	private static Random r = new Random();

	static List<Sound> soundList = new LinkedList<Sound>() {{
		add(new Sound(R.raw.supermercado, "fácil", "dónde está el supermercado"));
	}}; //Why two curly braces?

	public static Sound getRandomSound(String difficulty) {
		switch (difficulty) {
			case "fácil":
				return soundList.get(r.nextInt(Sound.easyId));

			case "intermedio":
				return soundList.get(r.nextInt(Sound.mediumId));

			case "difícil":
				return soundList.get(r.nextInt(Sound.hardId));

			default:
				return soundList.get(r.nextInt(Sound.easyId));
		}
	}
}
