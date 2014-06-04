package com.spiny.util.grouping;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class FakeMap<K, V extends Identifiable<K>> {
	
	private Collection<V> values;
	private Comparator<K> defaultComparator = new Comparator<K>(){
		public int compare(K o1, K o2) {
			return (o1.equals(o2)) ? 0 : 1;
		}
	};
	
	public FakeMap(Collection<V> values) {
		this.values = values;
	}
	
	public void put(V v) {
		values.add(v);
	}
	
	public Set<V> get(K k, Comparator<K> c) {
		Set<V> s = new HashSet<V>();
		for(V v : values) {
			if(c.compare(k, v.getKey()) == 0) s.add(v);
		}
		return s;
	}
	
	public Set<V> get(K k) {
		return get(k, defaultComparator);
	}
	
}
