package main;

import android.util.Log;
import android.widget.EditText;

import com.rivescript.RiveScript;

import java.util.Arrays;

import linguo.example.com.linguo.ChatFragment;
import linguo.example.com.linguo.Message;

public abstract class Conversation {
	/**
	 * Attempts to run the validResponse() loop before stopping if a valid response isn't retrieved.
	 */
	public static final int DEFAULT_ATTEMPTS = 1000;

	/**
	 * Method to start conversation.
	 */
//	public abstract void converse(); I was using this method to extend the functionality to other classes, but the project can do without it for now.
	static RiveScript mBot = ChatFragment.mBot;



	private static boolean mCallOtherClass = false;



	//TODO Add respondLoop() function for when the bot wants to send multiple messages.

	/**
	 * Continues to ask user for valid input if the input is not valid.
	 * @param editTextCompare User's Input
	 * @param invalidInput What Rivescript says if the input is invalid.
	 * @param attempts Times to run the loop
	 * @param printOutput Print intermediate messages
	 * @return A string of the user's valid response
	 */ //TODO Make this work.
	public String validResponse (EditText editTextCompare, String invalidInput, int attempts, boolean printOutput) {
		String compare = editTextCompare.getText().toString();
		String initial_reply = mBot.reply("user", compare);

		if (initial_reply.equals(invalidInput)) System.out.println(initial_reply); //And go into the loop.
		else {
			if (printOutput) System.out.println(initial_reply);
			return initial_reply;
		}

		String response = invalidInput;

		for (int i = 0;
			 (i < attempts) && (response.equals(invalidInput));
			 i++) {
//			compare = respond();
			response = mBot.reply("user", compare);
			if (printOutput) System.out.println(response);
		}

		if (response.equals(invalidInput)) return "`invalid`";
		else return response;
	}
}
