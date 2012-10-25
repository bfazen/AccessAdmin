package com.alphabetbloc.chvsettings.data;

import java.util.Random;

public class StringGenerator {

	private Random random;
	private int mPwdLength;
	private static final char[] symbols = new char[36];
	static {
		for (int idx = 0; idx < 10; ++idx)
			symbols[idx] = (char) ('0' + idx);
		for (int idx = 10; idx < 36; ++idx)
			symbols[idx] = (char) ('a' + idx - 10);
	}

	private static final char[] digits = new char[10];
	static {
		for (int idx = 0; idx < 10; ++idx)
			digits[idx] = (char) ('0' + idx);
	}

	private static final char[] characters = new char[26];
	static {
		for (int idx = 0; idx < 26; ++idx)
			characters[idx] = (char) ('a' + idx);
	}

	public StringGenerator(int length) {
		if (length < 1)
			throw new IllegalArgumentException("length < 1: " + length);
		mPwdLength = length;
		random = new Random();
	}
	
	public String getRandomAlphaNumericString() {
		int randomPwdLength = mPwdLength < 15 ? 15 : mPwdLength;
		char[] buf = new char[randomPwdLength];
		for (int idx = 0; idx < buf.length; ++idx) {
			buf[idx] = symbols[random.nextInt(symbols.length)];
		}

		boolean alpha = false;
		boolean num = false;
		for (char c : buf) {
			if (Character.isLetter(c))
				alpha = true;
			if (Character.isDigit(c))
				num = true;
		}
		if (alpha && num)
			return new String(buf);
		else
			return getRandomAlphaNumericString();
	}
}