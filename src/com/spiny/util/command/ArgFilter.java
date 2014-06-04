package com.spiny.util.command;

public interface ArgFilter {
	
	Object convert(String arg) throws ArgNotConvertableException;

	String cannotConvert(String arg);
	
}
