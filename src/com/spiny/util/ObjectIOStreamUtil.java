package com.spiny.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectIOStreamUtil {
	
	public static Object load(File file) throws FileNotFoundException {
		Object o = null;
		try {
			ObjectInputStream stream = new ObjectInputStream(new FileInputStream(file));
			o = stream.readObject();
			stream.close();
		} catch(FileNotFoundException e) {
			throw e;
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return o;
	}
	
	public static void save(File file, Object o) {
		try {
			file.createNewFile();
			ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
			stream.writeObject(o);
			stream.flush();
			stream.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
