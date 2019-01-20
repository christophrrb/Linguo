package linguo.example.com.linguo;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Sound {
	public static int easyId = 0, mediumId = 0, hardId = 0;
	private int id;
	private final int resId;
	private final String difficulty, spelling;

	Sound(int resId, String difficulty, String spelling) {
		switch (difficulty) {
			case "fácil":
				this.id = easyId;
				easyId++;
				break;

			case "intermedio":
				this.id = mediumId;
				mediumId++;
				break;

			case "difícil":
				this.id = hardId;
				hardId++;
				break;
		}

		this.resId = resId;
		this.difficulty = difficulty;
		this.spelling = spelling;
	}

	public Sound getSoundById() {
		return this;
	}

	public int getResId() {
		return resId;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public String getSpelling() {
		return spelling;
	}

	public static int getEasyId() {
		return easyId;
	}

	public static int getMediumId() {
		return mediumId;
	}

	public static int getHardId() {
		return hardId;
	}

	public int getId() {
		return id;
	}
}
