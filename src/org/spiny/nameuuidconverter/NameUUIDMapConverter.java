/**
 * 
 */
/**
 * @author Elijah
 *
 */
package org.spiny.nameuuidconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

import com.spiny.util.ObjectIOStreamUtil;

public class NameUUIDMapConverter<T> {
	
	//Class for converting name-indexed player maps into 1.7.8 compatible UUID ones; Hopefully the derivative method and field names will compensate for the lack of comments.
	
	private File originalNameFile;
	private File newUUIDFile;
	private Map<UUID, T> uuidMap;
	private Map<String, T> nameMap;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public NameUUIDMapConverter(Map<UUID, T> map, File i, File o, Class<? extends Map> defaultMap) throws NoConvertablesException {
		originalNameFile = i;
		newUUIDFile = o;
		uuidMap = map;
		try {
			nameMap = (Map<String, T>) ObjectIOStreamUtil.load(originalNameFile);
		} catch (FileNotFoundException e) {
			throw new NoConvertablesException();
		}
		if(nameMap == null) {
			try {
				nameMap = defaultMap.newInstance();
			} catch (InstantiationException | IllegalAccessException e1) {
				e1.printStackTrace();
			}
		} else {
			for(String name : nameMap.keySet()) System.out.println(name);
		}
		try {
			newUUIDFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void update(OfflinePlayer p) {
		String name = p.getName();
		
		for(Entry<String, T> entry : nameMap.entrySet()) {
			if(entry.getKey() == name) {
				uuidMap.put(p.getUniqueId(), entry.getValue());
				nameMap.remove(name);
			}
		}
		
		if(nameMap.isEmpty() ) {
			originalNameFile.delete();
		}
	}
	
	public void save() {
		if(originalNameFile.exists()) ObjectIOStreamUtil.save(originalNameFile, nameMap);
	}
	
	public static class NoConvertablesException extends Exception {

		private static final long serialVersionUID = 1L;
		
	}
}