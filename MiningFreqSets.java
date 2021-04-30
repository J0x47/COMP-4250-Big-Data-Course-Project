// package uw.cs.datamining;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MiningFreqSets {
	
	public static class Pair<K, V> implements Comparable<Pair> {
		private K k;
		private V v;
		private int count = 0;
		
		public K getK() {
			return k;
		}
		public void setK(K k) {
			this.k = k;
		}
		public V getV() {
			return v;
		}
		public void setV(V v) {
			this.v = v;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		public void incCount() {
			this.count++;
		}
		@Override
		public int compareTo(Pair o) {
			return this.count - o.count;
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "(" + this.k +"," + this.v + "," +this.count +")";
		}
	}

	
	/**
	 * implementation of A-priori algorithm
	 * @param filePath
	 * @throws IOException
	 * 
	 */
	public static void apriori(String filePath, float dataSize, float st) throws IOException {
		long startTime = System.nanoTime();
		
		List<String> baskets = Files.readAllLines(Paths.get(filePath));
		int i=0;
		int chunkSize = (int)(baskets.size()*dataSize); // 1%, 5%, 10%
		int supportThresholds = (int)(st*chunkSize); // 1%, 5%, 10%
		System.out.println("basket_size:"+baskets.size());
		System.out.println("chunk_size:"+chunkSize);
		baskets=baskets.subList(0, chunkSize);
		System.out.println("sub basket_size:"+baskets.size());
		System.out.println("supportThresholds:"+supportThresholds);
		
		// build baskets from an input dataset file
		Map<Integer, List<Integer>> sm = new LinkedHashMap<Integer, List<Integer>>();
		for(;i < chunkSize; i++) {
			List<Integer> x = Arrays.stream(baskets.get(i).split("\\s"))
					.map(Integer::valueOf)
					.collect(Collectors.toList());
			sm.put(i, x);
		}

		System.out.println("sm:"+sm.keySet().size());
		
		/* Pass 1: Read baskets and count in main memory the occurrences of each individual item.
		 * Items that appear ≥ s (supportThreasholds) times are the frequent items
		 * 
		 * Pass 2: Read baskets again and count in main memory only those pairs where both
		 * elements are frequent (from Pass 1)
		 * 
		 * */
		
		// apriori Pass 1
		// count the items
		Map<Integer, Integer> itemCount = new HashMap<Integer, Integer>();
		for(Map.Entry<Integer, List<Integer>> entry: sm.entrySet()) {
			List<Integer> values = entry.getValue();
			for(int j = 0; j < values.size(); j++) {
				Integer value = values.get(j);
				int count = itemCount.containsKey(value) ? itemCount.get(value) : 0;
				itemCount.put(value, count + 1);			
			}
		}
		
		// get frequent items
		Set<Integer> freqItems = new HashSet<Integer>();
		itemCount.forEach((k,v)-> {if(v>=supportThresholds) freqItems.add(k);});		
		List<Pair<Integer, Integer>> candidatePairs = new ArrayList<MiningFreqSets.Pair<Integer,Integer>>();
		Integer p1, p2;
		boolean isExist;
		Pair<Integer, Integer> p;
		
		// apriori pass 2
		for(Map.Entry<Integer, List<Integer>> entry: sm.entrySet()) {
			List<Integer> items = entry.getValue();
			for(int i1 = 0; i1 < items.size(); i1++) {
				p1 = items.get(i1);
				if(!freqItems.contains(p1)) continue; // skip non-frequent item
				for(int i2=i1+1; i2 < items.size(); i2++) {
					p2 = items.get(i2);
					isExist = false;
					if(!freqItems.contains(p2)) continue; // skip non-frequent item
					p = new Pair<Integer, Integer>(); 
					p.setK(p1);
					p.setV(p2);
					for(int ip=0; ip < candidatePairs.size(); ip++) {
						Pair<Integer, Integer> fp = candidatePairs.get(ip); 
						if(fp.getK() == p1 && fp.getV() == p2) {
							isExist = true;
							fp.incCount();
							break;
						}
					}
					if(!isExist) {
						p.incCount();
						candidatePairs.add(p);
					}						
				}
			}
		}
		// find frequent pairs
		List<Pair<Integer, Integer>> result = candidatePairs.stream()
	    		.filter(m->m.getCount() >= supportThresholds)
	    		.sorted(Comparator.reverseOrder())
	    		.collect(Collectors.toList());
//		System.out.println(result);
	    long stopTime = System.nanoTime();
		System.out.println("[Aprior supportThresholds:" + st + ",dataSize:"+dataSize+"] Time elapsed is:"+TimeUnit.MILLISECONDS.convert((stopTime - startTime), TimeUnit.NANOSECONDS)+"ms");
	}
	
	/**
	 * implementation of PCY algorithm
	 * @param filePath
	 * @throws IOException
	 */
	public static void Pcy(String filePath, float dataSize, float st, int bucketSize) throws IOException {
		long startTime = System.nanoTime();
		int[] buckets = new int[bucketSize];
		BitSet bitVector = new BitSet(bucketSize);
		List<String> baskets = Files.readAllLines(Paths.get(filePath));
		int chunkSize = (int)(baskets.size()*dataSize); // 1%, 5%, 10%
		int supportThresholds = (int)(st*chunkSize); // 1%, 5%, 10%
		System.out.println("basket_size:"+baskets.size());
		System.out.println("chunk_size:"+chunkSize);
		baskets=baskets.subList(0, chunkSize);
		System.out.println("sub basket_size:"+baskets.size());
		System.out.println("supportThresholds:"+supportThresholds);

		int i=0;
		// build baskets from an input dataset file
		Map<Integer, List<Integer>> sm = new LinkedHashMap<Integer, List<Integer>>();
		for(;i < chunkSize; i++) {
			List<Integer> x = Arrays.stream(baskets.get(i).split("\\s"))
					.map(Integer::valueOf)
		            .collect(Collectors.toList());
			sm.put(i, x);
		}
		
		System.out.println("sm:"+sm.keySet().size());
		
		// PCY Pass 1
		// count the items and the pairs and Keep a count for each bucket into 
		// which pairs of items are hashed
		int hv;
		Map<Integer, Integer> itemCount = new HashMap<Integer, Integer>();
		for(Map.Entry<Integer, List<Integer>> entry: sm.entrySet()) {
			List<Integer> values = entry.getValue();
			for(int j = 0; j < values.size(); j++) {
				Integer value = values.get(j);
				int count = itemCount.containsKey(value) ? itemCount.get(value) : 0;
				itemCount.put(value, count + 1);
				for(int k=j+1; k < values.size(); k++) {
					hv = (value + values.get(k)) % bucketSize;
					buckets[hv] += 1;					
					
				}
			}
		}
		
		// get frequent items
		Set<Integer> freqItems = new HashSet<Integer>();
		itemCount.forEach((k,v)-> {if(v>=supportThresholds) freqItems.add(k);});
		
		/**
		 * Replace the buckets by a bit-vector 1 means the bucket 
		 * count exceeded the support (call it a frequent bucket); 0 means it did not
		 */
		for(int bi=0; bi<bucketSize; bi++) {
			if(buckets[bi]>=supportThresholds) bitVector.set(bi); 
		}
		
		// Pass 2
		/** count the pair(i,j) which satisfies the two conditions:
		 * (a). Both i and j are frequent items
		 * (b). The pair {i, j} hashes to a bucket whose bit in the bit
		 * vector is 1 (i.e., a frequent bucket)
		 */
		List<Pair<Integer, Integer>> candidatePairs = new ArrayList<MiningFreqSets.Pair<Integer,Integer>>();
		Integer p1, p2;
		boolean isExist;
		Pair<Integer, Integer> p;
		for(Map.Entry<Integer, List<Integer>> entry: sm.entrySet()) {
			List<Integer> items = entry.getValue();
			for(int i1 = 0; i1 < items.size(); i1++) {
				p1 = items.get(i1);
				if(!freqItems.contains(p1)) continue; // skip non-frequent item
				for(int i2=i1+1; i2 < items.size(); i2++) {
					p2 = items.get(i2);
					isExist = false;
					if(!freqItems.contains(p2)) continue; // skip non-frequent item
					if(bitVector.get((p1 + p2) % bucketSize)) {
						p = new Pair<Integer, Integer>();
						p.setK(p1);
						p.setV(p2);
						for(int ip=0; ip < candidatePairs.size(); ip++) {
							Pair<Integer, Integer> fp = candidatePairs.get(ip); 
							if(fp.getK() == p1 && fp.getV() == p2) {
								isExist = true;
								fp.incCount();
								break;
							}
						}
						if(!isExist) {
							p.incCount();
							candidatePairs.add(p);
						}						
					}
				}
			}
		}
//		System.out.println("candidatePairs:"+candidatePairs.size());
		List<Pair<Integer, Integer>> result = candidatePairs.stream()
	    		.filter(m->m.getCount() >= supportThresholds)
	    		.sorted(Comparator.reverseOrder())
	    		.collect(Collectors.toList());
//		System.out.println(result);
		long stopTime = System.nanoTime();
		System.out.println("[PCY supportThresholds:" + st + ",dataSize:"+dataSize+"] Time elapsed is:"+TimeUnit.MILLISECONDS.convert((stopTime - startTime), TimeUnit.NANOSECONDS)+"ms");
	}
	
	/**
	 * implementation of Multistage algorithm
	 * @param filePath
	 * @throws IOException
	 */
	public static void multistage(String filePath, float dataSize, float st, int bucketSize) throws IOException {
		long startTime = System.nanoTime();
		int[] buckets = new int[bucketSize];
		int[] buckets2 = new int[bucketSize];
		BitSet bitVector = new BitSet(bucketSize);
		BitSet bitVector2 = new BitSet(bucketSize);
		List<String> baskets = Files.readAllLines(Paths.get(filePath));
		int chunkSize = (int)(baskets.size()*dataSize); // 1%, 5%, 10%
		int supportThresholds = (int)(st*chunkSize); // 1%, 5%, 10%
		System.out.println("basket_size:"+baskets.size());
		System.out.println("chunk_size:"+chunkSize);
		baskets=baskets.subList(0, chunkSize);
		System.out.println("sub basket_size:"+baskets.size());
		System.out.println("supportThresholds:"+supportThresholds);

		int i=0;
		// build baskets from an input dataset file
		Map<Integer, List<Integer>> sm = new LinkedHashMap<Integer, List<Integer>>();
		for(;i < chunkSize; i++) {
			List<Integer> x = Arrays.stream(baskets.get(i).split("\\s"))
					.map(Integer::valueOf)
		            .collect(Collectors.toList());
			sm.put(i, x);
		}
		
		System.out.println("sm:"+sm.keySet().size());
		
		// Multistage Pass 1
		// count the items and the pairs and Keep a count for each bucket into 
		// which pairs of items are hashed
		int hv;
		Map<Integer, Integer> itemCount = new HashMap<Integer, Integer>();
		for(Map.Entry<Integer, List<Integer>> entry: sm.entrySet()) {
			List<Integer> values = entry.getValue();
			for(int j = 0; j < values.size(); j++) {
				Integer value = values.get(j);
				int count = itemCount.containsKey(value) ? itemCount.get(value) : 0;
				itemCount.put(value, count + 1);
				for(int k=j+1; k < values.size(); k++) {
					hv = (value + values.get(k)) % bucketSize;
					buckets[hv] += 1;					
					
				}
			}
		}
		// Multistage Pass 2
		// get frequent items
		Set<Integer> freqItems = new HashSet<Integer>();
		itemCount.forEach((k,v)-> {if(v>=supportThresholds) freqItems.add(k);});
		
		/**
		 * Replace the buckets (first hash table) by a bit-vector 1 means the bucket 
		 * count exceeded the support (call it a frequent bucket); 0 means it did not
		 */
		for(int bi=0; bi<bucketSize; bi++) {
			if(buckets[bi]>=supportThresholds) bitVector.set(bi); 
		}
		
		// Multistage Pass 2
		// Pass 2
		/** hash the pair(i,j) into second hash table, and it satisfies the two conditions:
		 * (a). Both i and j are frequent items
		 * (b). The pair {i, j} hashes to a frequent bucket whose bit in the bit
		 * vector is 1 (i.e., a frequent bucket) in Pass 1
		 */
		Integer v1, v2;
		int hv2;
		for(Map.Entry<Integer, List<Integer>> entry: sm.entrySet()) {
			List<Integer> items = entry.getValue();
			for(int s1 = 0; s1 < items.size(); s1++) {
				v1 = items.get(s1);
				if(!freqItems.contains(v1)) continue; // skip non-frequent item
				for(int s2=s1+1; s2 < items.size(); s2++) {
					v2 = items.get(s2);
					if(!freqItems.contains(v2)) continue; // skip non-frequent item
					if(bitVector.get((v1 + v2) % bucketSize)) {
						hv2 = (v1 + v2) % bucketSize;
						buckets2[hv2] += 1;							
					}
				}
			}
		}
		
		/**
		 * Replace the buckets2 (second hash table) by a bit-vector 1 means the bucket 
		 * count exceeded the support (call it a frequent bucket); 0 means it did not
		 */
		for(int bi2=0; bi2<bucketSize; bi2++) {
			if(buckets2[bi2]>=supportThresholds) bitVector2.set(bi2); 
		}
		
		// Pass 3
		/** count the pair(i,j) which satisfies the two conditions:
		 * (a). Both i and j are frequent items
		 * (b). The pair {i, j} hashes to a frequent bucket whose bit in the bit
		 * vector is 1 (i.e., a frequent bucket) in Pass 1
		 * (b). The pair {i, j} hashes to a frequent bucket whose bit in the bit
		 * vector is 1 (i.e., a frequent bucket) in Pass 2
		 */
		List<Pair<Integer, Integer>> candidatePairs = new ArrayList<MiningFreqSets.Pair<Integer,Integer>>();
		Integer p1, p2;
		boolean isExist;
		Pair<Integer, Integer> p;
		for(Map.Entry<Integer, List<Integer>> entry: sm.entrySet()) {
			List<Integer> items = entry.getValue();
			for(int i1 = 0; i1 < items.size(); i1++) {
				p1 = items.get(i1);
				if(!freqItems.contains(p1)) continue; // skip non-frequent item
				for(int i2=i1+1; i2 < items.size(); i2++) {
					p2 = items.get(i2);
					isExist = false;
					if(!freqItems.contains(p2)) continue; // skip non-frequent item
					if(bitVector.get((p1 + p2) % bucketSize) && bitVector2.get((p1 + p2) % bucketSize)) {
						p = new Pair<Integer, Integer>();
						p.setK(p1);
						p.setV(p2);
						for(int ip=0; ip < candidatePairs.size(); ip++) {
							Pair<Integer, Integer> fp = candidatePairs.get(ip); 
							if(fp.getK() == p1 && fp.getV() == p2) {
								isExist = true;
								fp.incCount();
								break;
							}
						}
						if(!isExist) {
							p.incCount();
							candidatePairs.add(p);
						}						
					}
				}
			}
		}
		
		List<Pair<Integer, Integer>> result = candidatePairs.stream()
	    		.filter(m->m.getCount() >= supportThresholds)
	    		.sorted(Comparator.reverseOrder())
	    		.collect(Collectors.toList());
//		System.out.println(result);
		long stopTime = System.nanoTime();
		System.out.println("[multistage supportThresholds:" + st + ",dataSize:"+dataSize+"] Time elapsed is:"+TimeUnit.MILLISECONDS.convert((stopTime - startTime), TimeUnit.NANOSECONDS)+"ms");
	}
	/**
	 * implementation of multihash algorithm
	 * @param filePath
	 * @throws IOException
	 */
	public static void multihash(String filePath, float dataSize, float st, int bucketSize) throws IOException {
		long startTime = System.nanoTime();
		int[] buckets = new int[bucketSize]; // hash table 1
		int[] buckets2 = new int[bucketSize]; // hash table 2
		BitSet bitVector = new BitSet(bucketSize); // bitmap 1
		BitSet bitVector2 = new BitSet(bucketSize); // bitmap 2
		List<String> baskets = Files.readAllLines(Paths.get(filePath)); // baskets (transactions)
		int chunkSize = (int)(baskets.size()*dataSize); // 1%, 5%, 10%
		int supportThresholds = (int)(st*chunkSize); // 1%, 5%, 10%
		System.out.println("basket_size:"+baskets.size());
		System.out.println("chunk_size:"+chunkSize);
		baskets=baskets.subList(0, chunkSize);
		System.out.println("sub basket_size:"+baskets.size());
		System.out.println("supportThresholds:"+supportThresholds);

		int i=0;
		// build baskets from an input dataset file
		Map<Integer, List<Integer>> sm = new LinkedHashMap<Integer, List<Integer>>();
		for(;i < chunkSize; i++) {
			List<Integer> x = Arrays.stream(baskets.get(i).split("\\s"))
					.map(Integer::valueOf)
		            .collect(Collectors.toList());
			sm.put(i, x);
		}
		
		System.out.println("sm:"+sm.keySet().size());
		
		// multihash Pass 1 (using two hash tables with two hash functions in this pass)
		// count the items and the pairs and Keep a count for each bucket into 
		// which pairs of items are hashed
		int hv, hv2;
		Map<Integer, Integer> itemCount = new HashMap<Integer, Integer>();
		for(Map.Entry<Integer, List<Integer>> entry: sm.entrySet()) {
			List<Integer> values = entry.getValue();
			for(int j = 0; j < values.size(); j++) {
				Integer value = values.get(j);
				int count = itemCount.containsKey(value) ? itemCount.get(value) : 0;
				itemCount.put(value, count + 1);
				for(int k=j+1; k < values.size(); k++) {
					hv = (value + values.get(k)) % bucketSize; // hash function 1
					hv2 = (value + (values.get(k))*2 -1) % bucketSize; // hash function 2 (using different hash function)
					buckets[hv] += 1;	
					buckets2[hv2] += 1;
				}
			}
		}
		
		// get frequent items
		Set<Integer> freqItems = new HashSet<Integer>();
		itemCount.forEach((k,v)-> {if(v>=supportThresholds) freqItems.add(k);});

		/**
		 * Replace the buckets (hash table 1) and hash table 2 by a bit-vector 1 means the bucket 
		 * count exceeded the support (call it a frequent bucket); 0 means it did not
		 */
		for(int bi=0; bi<bucketSize; bi++) {
			if(buckets[bi]>=supportThresholds) bitVector.set(bi); 
			if(buckets2[bi]>=supportThresholds) bitVector2.set(bi); 
		}

		// Pass 2
		/** count the pair(i,j) which satisfies the two conditions:
		 * (a). Both i and j are frequent items
		 * (b). The pair {i, j} hashes to the both buckets whose bit in the bit
		 * vector are 1 (i.e., a frequent bucket)
		 */
		List<Pair<Integer, Integer>> candidatePairs = new ArrayList<MiningFreqSets.Pair<Integer,Integer>>();
		Integer p1, p2;
		boolean isExist;
		Pair<Integer, Integer> p;
		for(Map.Entry<Integer, List<Integer>> entry: sm.entrySet()) {
			List<Integer> items = entry.getValue();
			for(int i1 = 0; i1 < items.size(); i1++) {
				p1 = items.get(i1);
				if(!freqItems.contains(p1)) continue; // skip non-frequent item
				for(int i2=i1+1; i2 < items.size(); i2++) {
					p2 = items.get(i2);
					isExist = false;
					if(!freqItems.contains(p2)) continue; // skip non-frequent item
					if(bitVector.get((p1 + p2) % bucketSize) && bitVector2.get((p1 + p2*2 -1) % bucketSize)) { // in both frequent bucket from pass 1
						p = new Pair<Integer, Integer>();
						p.setK(p1);
						p.setV(p2);
						for(int ip=0; ip < candidatePairs.size(); ip++) {
							Pair<Integer, Integer> fp = candidatePairs.get(ip); 
							if(fp.getK() == p1 && fp.getV() == p2) {
								isExist = true;
								fp.incCount();
								break;
							}
						}
						if(!isExist) {
							p.incCount();
							candidatePairs.add(p);
						}						
					}
				}
			}
		}
		List<Pair<Integer, Integer>> result = candidatePairs.stream()
	    		.filter(m->m.getCount() >= supportThresholds)
	    		.sorted(Comparator.reverseOrder())
	    		.collect(Collectors.toList());
//		System.out.println(result);
		long stopTime = System.nanoTime();
		System.out.println("[multihash supportThresholds:" + st + ",dataSize:"+dataSize+"] Time elapsed is:"+TimeUnit.MILLISECONDS.convert((stopTime - startTime), TimeUnit.NANOSECONDS)+"ms");
	}
	
	public static void main(String[] args) throws IOException {
		int bucketSize = 30000;
		String dataFile = "/tmp/retail.txt";
		if(args.length > 0) dataFile = args[0]; 
		float[] dataSizeValues = {0.01f,0.02f,0.03f,0.04f,0.05f,0.06f,0.07f,0.10f,0.11f,0.12f,0.13f,
								  0.14f,0.15f,0.20f,0.23f,0.25f,0.27f,0.30f,0.32f,0.35f,0.37f,0.40f,
								  0.42f,0.43f,0.45f,0.48f,0.50f,0.52f,0.55f,0.57f,0.60f,0.63f,0.65f,
								  0.68f,0.70f,0.71f,0.72f,0.74f,0.75f,0.78f,0.80f,0.85f,0.90f,0.91f,
								  0.92f,0.93f,0.95f,1.0f};
		float[] st = {0.01f, 0.05f, 0.10f};
		boolean isTest = false;
		// test
		if(isTest) {
			/** file: testcase.txt
			 *  1 2 3
				1 4 5
				1 3
				2 5
				1 3 4
				1 2 3 5
				2 3 5
				2 3
			 */
			bucketSize = 5;
			String testFile = "/tmp/testcase.txt";
			float testDataSize = 1.0f; // 1.0f
			float testSupportThresholds = 0.375f; // 0.375f
			System.out.println("----------------------------------------------------------------------");
			System.out.println("#supportThresholds:"+testSupportThresholds +",dataSize:"+testDataSize);
			System.out.println("################################# PCY #############################");
			Pcy(testFile, testDataSize, testSupportThresholds, bucketSize);
			
			System.out.println("\n############################### Apriori ##########################");
			apriori(testFile, testDataSize, testSupportThresholds);
			System.out.println("\n");
			
			System.out.println("\n############################### multistage ##########################");
			multistage(testFile, testDataSize, testSupportThresholds, bucketSize);
			System.out.println("\n");
			
			System.out.println("\n############################### multihash ##########################");
			multihash(testFile, testDataSize, testSupportThresholds, 3);
			System.out.println("\n");
		} else {
			for(int i = 0; i < st.length; i++) {
				for(int j = 0; j < dataSizeValues.length; j++) {
					System.out.println("----------------------------------------------------------------------");
					System.out.println("#supportThresholds:"+st[i] +",dataSize:"+dataSizeValues[j]);
					System.out.println("###################################### PCY #############################");
					Pcy(dataFile, dataSizeValues[j], st[i], bucketSize);
					
					System.out.println("\n############################### Apriori ##########################");
					apriori(dataFile, dataSizeValues[j], st[i]);
					System.out.println("\n");
					
					System.out.println("\n############################### multistage ##########################");
					multistage(dataFile, dataSizeValues[j], st[i], bucketSize);
					System.out.println("\n");
					
					System.out.println("\n############################### multihash ##########################");
					multihash(dataFile, dataSizeValues[j], st[i], bucketSize);
					System.out.println("\n");
				}
			}
		}
	}
}
