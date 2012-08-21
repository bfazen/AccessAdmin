package com.alphabetbloc.chvsettings.data;

import java.util.Random;

public class StringGenerator {

	private final Random random = new Random();
	private final char[] buf;
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
		buf = new char[length];
	}

	public String getRandomAlphaNumericString() {
		for (int idx = 0; idx < buf.length; ++idx)
			buf[idx] = symbols[random.nextInt(symbols.length)];
		return new String(buf);
	}

	public String getRandomNumericString() {
		for (int idx = 0; idx < buf.length; ++idx)
			buf[idx] = digits[random.nextInt(digits.length)];
		return new String(buf);
	}

	public String getRandomAlphaString() {
		for (int idx = 0; idx < buf.length; ++idx)
			buf[idx] = characters[random.nextInt(characters.length)];
		return new String(buf);
	}

	public String getDefaultAlphaNumericString() {
		for (int idx = 0; idx < buf.length; ++idx){
			buf[idx] = (char) ('a');
		}
		for (int idx = 0; idx < buf.length; ++idx){
			buf[idx] = (char) ('0');
		}
		return new String(buf);
	}
	
	public String getDefaultAlphaString() {
		for (int idx = 0; idx < buf.length; ++idx){
			buf[idx] = (char) ('a');
		}
		return new String(buf);
	}
	
	public String getDefaultNumericString() {
		for (int idx = 0; idx < buf.length; ++idx){
			buf[idx] = (char) ('0');
		}
		return new String(buf);
	}

}
