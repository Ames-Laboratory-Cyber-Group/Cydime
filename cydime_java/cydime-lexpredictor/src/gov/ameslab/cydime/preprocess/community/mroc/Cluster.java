package gov.ameslab.cydime.preprocess.community.mroc;

import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.MapSet;

import java.util.Set;

public class Cluster<T> {

	private Set<T> mMembers;
	private Set<T> mNeighbors;

	public Cluster(Set<T> members, Set<T> neighbors) {
		mMembers = members;
		mNeighbors = neighbors;
	}

	public Set<T> getMembers() {
		return mMembers;
	}

	public Set<T> getNeighbors() {
		return mNeighbors;
	}

	public void addToRawClusterMap(MapSet<T, Cluster<T>> map) {
		for (T i : getMembers()) {
			map.add(i, this);
		}
	}

	public void removeFromRawClusterMap(MapSet<T, Cluster<T>> map) {
		for (T i : getMembers()) {
			map.remove(i, this);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Cluster)) return false;
		Cluster<T> o = (Cluster<T>) obj;
		return mMembers.equals(o.mMembers);
	}
	
	@Override
	public int hashCode() {
		return mMembers.hashCode();
	}
	
	public static <T> Cluster<T> merge(Cluster<T> c1, Cluster<T> c2) {
		Set<T> members = CUtil.makeSet();
		members.addAll(c1.mMembers);
		members.addAll(c2.mMembers);
		Set<T> neighbors = CUtil.makeSet();
		neighbors.addAll(c1.mNeighbors);
		neighbors.addAll(c2.mNeighbors);
		neighbors.removeAll(members);
		return new Cluster<T>(members, neighbors);
	}

	public static <T> double getJaccard(Cluster<T> c1, Cluster<T> c2) {
		Set<T> and = CUtil.makeSet();
		and.addAll(c1.mMembers);
		and.retainAll(c2.mMembers);
		
		int orSize = c1.mMembers.size() + c2.mMembers.size() - and.size();		
		if (orSize == 0) return Double.NaN;
		return 1.0 * and.size() / orSize;
	}

}
