package org.emoflon.ibex.common.collections.fastutil;

import java.util.Collection;
import java.util.Set;

import org.emoflon.ibex.common.collections.IntToObjectMap;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class FastUtilIntToObjectMap<T> extends IntToObjectMap<T> {
	private Int2ObjectOpenHashMap<T> internal = new Int2ObjectOpenHashMap<>();

	@Override
	public T get(int i) {
		return internal.get(i);
	}

	@Override
	public void put(int i, T o) {
		internal.put(i, o);
	}

	@Override
	public Set<Integer> keySet() {
		return internal.keySet();
	}

	@Override
	public int size() {
		return internal.size();
	}

	@Override
	public Collection<T> values() {
		return internal.values();
	}

	@Override
	public boolean containsKey(int i) {
		return internal.containsKey(i);
	}

	@Override
	public boolean containsValue(T o) {
		return internal.containsValue(o);
	}
}