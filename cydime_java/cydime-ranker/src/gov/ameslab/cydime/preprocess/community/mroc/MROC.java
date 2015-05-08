package gov.ameslab.cydime.preprocess.community.mroc;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import gov.ameslab.cydime.preprocess.community.Edge;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.MapSet;

public class MROC {
	
	private static final Logger Log = Logger.getLogger(MROC.class.getName());
	
	private Graph<Integer, ? extends Edge> mRawGraph;
	private DirectedGraph<Cluster<Integer>, Edge> mForest;
	private Queue<Cluster<Integer>> mQueue;
	private Set<Cluster<Integer>> mQueueSet;
	private MapSet<Integer, Cluster<Integer>> mRawClusterMap;
	
	public DirectedGraph<Cluster<Integer>, Edge> run(Graph<Integer, ? extends Edge> graph) {
		mRawGraph = graph;
		mForest = new DirectedSparseGraph<Cluster<Integer>, Edge>();
		mQueue = new LinkedList<Cluster<Integer>>();
		mQueueSet = CUtil.makeSet();
		mRawClusterMap = new MapSet<Integer, Cluster<Integer>>();
		
		initQueue();
		
		while (!mQueue.isEmpty()) {
			Cluster<Integer> c = mQueue.remove();
			if (!mQueueSet.remove(c)) continue;
			
			Set<Cluster<Integer>> neighbors = getNeighbors(c);
			if (neighbors.isEmpty()) continue;
			
			Cluster<Integer> best = findBest(c, neighbors);
			Cluster<Integer> merged = Cluster.merge(c, best);
			
			mQueue.add(merged);
			mQueueSet.add(merged);
			mQueueSet.remove(best);
			
			mForest.addVertex(merged);
			mForest.addEdge(new Edge(), c, merged);
			mForest.addEdge(new Edge(), best, merged);
			
			c.removeFromRawClusterMap(mRawClusterMap);
			best.removeFromRawClusterMap(mRawClusterMap);
			merged.addToRawClusterMap(mRawClusterMap);
//			System.out.print(".");
		}		
		
		return mForest;
	}

	private void initQueue() {
		for (Integer v : mRawGraph.getVertices()) {
			Set<Integer> base = CUtil.makeSet(mRawGraph.getNeighbors(v));
			base.add(v);
			
			Set<Integer> baseNeighbors = CUtil.makeSet();
			for (Integer b : base) {
				baseNeighbors.addAll(mRawGraph.getNeighbors(b));
			}
			baseNeighbors.removeAll(base);
			
			Cluster<Integer> c = new Cluster<Integer>(base, baseNeighbors);
			mQueueSet.add(c);
		}
		
		mQueue.addAll(mQueueSet);
		
		for (Cluster<Integer> c : mQueueSet) {
			mForest.addVertex(c);
			c.addToRawClusterMap(mRawClusterMap);
		}
		
		Log.log(Level.INFO, "MROC leaf size = {0}", mQueueSet.size());
	}

	private Set<Cluster<Integer>> getNeighbors(Cluster<Integer> c) {
		Set<Cluster<Integer>> nClusters = CUtil.makeSet();
		for (Integer n : c.getNeighbors()) {
			Set<Cluster<Integer>> nCluster = mRawClusterMap.get(n);
			nClusters.addAll(nCluster);
		}
		return nClusters;
	}	

	private Cluster<Integer> findBest(Cluster<Integer> c, Set<Cluster<Integer>> neighbors) {
		double max = -1.0;
		Cluster<Integer> best = null;
		for (Cluster<Integer> n : neighbors) {
			double dist = Cluster.getJaccard(c, n);
			if (dist > max) {
				best = n;
				max = dist;
			}
		}
		return best;
	}
		
}
