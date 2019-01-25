package linguo.example.com.linguo;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Creates Sound objects and is a random retriever for them also.
 */
public class SoundMessage {
	private static Random r = new Random();

	private static int lastEasySound = -1;
	private static int lastMediumSound = -1;
	private static int lastHardSound = -1;

	static List<Sound> easySoundList = new LinkedList<Sound>() {{
		add(new Sound(R.raw.supermercado, "fácil", "dónde está el supermercado"));
		add(new Sound(R.raw.bicicleta_easy, "fácil", "yo monto en bicicleta los lunes"));
		add(new Sound(R.raw.camisa_easy, "fácil", "me gusta la camisa"));
	}}; //Why two curly braces?

	static List<Sound> mediumSoundList = new LinkedList<Sound>() {{
		add(new Sound(R.raw.amigos_medium, "intermedio", "voy a pasar un rato con mis amigos"));
		add(new Sound(R.raw.escuela_medium, "intermedio", "conducí a la escuela ayer"));
		add(new Sound(R.raw.proyecto_medium, "intermedio", "cómo terminaremos el proyecto"));
	}};

	static List<Sound> hardSoundList = new LinkedList<Sound>() {{
		add(new Sound(R.raw.exhibicion_hard, "difícil", "el profesor quiere que ustedes vayan a la exhibición"));
		add(new Sound(R.raw.paraguas_hard, "difícil", "lleva un paraguas no vaya a ser que te mojes"));
		add(new Sound(R.raw.trafico_hard, "difícil", "si yo hubiera sabido que había tráfico pues lo habría evitado"));
	}};

	public static Sound getRandomSound(String difficulty) {
		switch (difficulty) {
			case "fácil":
				int easyNumber = randomNumber(Sound.easyId);
				while (randomNumber(Sound.easyId) == lastEasySound) {
					easyNumber = randomNumber(Sound.easyId);
				}

				lastEasySound = easyNumber;
				return easySoundList.get(easyNumber);

			case "intermedio":
				int mediumNumber = randomNumber(Sound.mediumId);
				while (randomNumber(Sound.mediumId) == lastMediumSound) {
					mediumNumber = randomNumber(Sound.mediumId);
				}

				lastMediumSound = mediumNumber;
				return mediumSoundList.get(mediumNumber);

			case "difícil":
				int hardNumber = randomNumber(Sound.hardId);
				while (randomNumber(Sound.hardId) == lastHardSound) {
					hardNumber = randomNumber(Sound.hardId);
				}

				lastHardSound = hardNumber;
				return hardSoundList.get(hardNumber);

			default:
				int defaultNumber = randomNumber(Sound.easyId);
				while (randomNumber(Sound.easyId) == lastEasySound) {
					defaultNumber = randomNumber(Sound.easyId);
				}

				return easySoundList.get(defaultNumber);
		}
	}

	private static int randomNumber(int size) {
		return r.nextInt(size);
	}
}
