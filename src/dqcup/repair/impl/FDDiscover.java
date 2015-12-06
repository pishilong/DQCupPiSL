package dqcup.repair.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import dqcup.repair.Tuple;

public class FDDiscover {
	//数据立方体
	public static LinkedList<List<Set<String>>> levels;
	
	//FD信息
	public static HashMap<String, Set<Set<String>>> FD;
	
	//Right Hand Sides plus
	public static HashMap<Set<String>, Set<String>> rhs;
	//顶点key
	public static final Set<String> emptyRhs = new HashSet<String>(Arrays.asList("empty"));
	
	//Partition -> {A}, {{1, {1,2,3}}, {2, {4, 5}}}对值进行区分，并且保存CUID
	public static HashMap<Set<String>, HashMap<Integer, Set<Integer>>> partition;
	
	//总数
	public static int tuplesAmount;
	
	//epsilon
	public static final float eps = (float) 0.0012;
	
	//stop level
	private static final int stopLevel = 4;
	
	//fields
	public static final String[] FIELDS = new String[]{"MINIT", "FNAME", "LNAME", "STNUM", "STADD",
				"APMT", "CITY", "STATE", "ZIP"};
	
	//e(X->A)记录对于每个A的FD的e最小值
	public static HashMap<String, Float> rhsE;
	

	public static HashMap<String, Set<Set<String>>> performance(HashMap<String, Tuple> truthTuples) {
		initFDDiscover(truthTuples);
		
		List<Set<String>> currentLevel = levels.getLast();
		
		while(!currentLevel.isEmpty() && levels.size() <= stopLevel){
			//System.out.println("currentLevel:" + currentLevel.toString());
			computeDependencies(currentLevel);
			prune(currentLevel);
			levels.addLast(generateNextLevel(currentLevel));
			currentLevel = levels.getLast();
		}
		
		return FD;	
	}


	private static List<Set<String>> generateNextLevel(List<Set<String>> currentLevel) {
		List<Set<String>> nextLevel = new LinkedList<Set<String>>();
		List<List<Set<String>>> prefixBlocks = findPrefixBlocks(currentLevel);
		
		for(List<Set<String>> prefixBlock : prefixBlocks){
			//X=Y并Z
			Set<String> Y = prefixBlock.get(0);
			Set<String> Z = prefixBlock.get(1);
			Set<String> X = new HashSet<String>(Y);
			X.addAll(Z);
			//计算partition[X] = product(partition[Y],partition[Z])
			partition.put(X, 
					strippedProduct(
							partition.get(Y),
							partition.get(Z)
					));
			
			boolean flag = true;
			for(String element : X){
				Set<String> dummy = new HashSet<String>(X);
				dummy.remove(element);
				if(!currentLevel.contains(dummy)){
					flag = false;
				}	
			}
			if(flag && !nextLevel.contains(X)){
				nextLevel.add(X);
			}
		}
		
		return nextLevel;
	}




	private static HashMap<Integer, Set<Integer>> strippedProduct(HashMap<Integer, Set<Integer>> partition_1,
			HashMap<Integer, Set<Integer>> partition_2) {
		
		HashMap<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();
		HashMap<Integer, Integer> T = new HashMap<Integer, Integer>();
		HashMap<Integer, Set<Integer>> S = new HashMap<Integer, Set<Integer>>();
		int index = 1;
		
		LinkedList<Set<Integer>> p1Values = new LinkedList<Set<Integer>>(partition_1.values());
		LinkedList<Set<Integer>> p2Values = new LinkedList<Set<Integer>>(partition_2.values());
		
		for(int i = 0; i < p1Values.size(); i++){		
			for(int cuid : p1Values.get(i)){
				T.put(cuid, i);
			}
		}
		
		for(int i = 0; i < p2Values.size(); i++){
			for(int cuid : p2Values.get(i)){
				if (T.containsKey(cuid)){
					int klass = T.get(cuid);
					if (S.containsKey(klass)){
						S.get(klass).addAll(Arrays.asList(cuid));
					}else{
						S.put(klass, new HashSet<Integer>(Arrays.asList(cuid)));
					}
				}
			}
			
			for(int cuid : p2Values.get(i)){
				Set<Integer> block = S.get(T.get(cuid));
				if (block != null && block.size() > 1) {
					result.put(index, block);
					index += 1;
				}
				S.remove(T.get(cuid));
			}
		}
		return result;
	}


	/**
	 * 寻找prefix blocks
	 * @param currentLevel
	 * @return
	 */
	private static List<List<Set<String>>> findPrefixBlocks(List<Set<String>> currentLevel) {
		List<List<Set<String>>> result = new LinkedList<List<Set<String>>>();
		for(int i = 0; i < currentLevel.size() - 1; i ++){
			Set<String> ele1 = currentLevel.get(i);
			for(int j = i + 1; j < currentLevel.size(); j ++){
				Set<String> tempSet = new HashSet<String>(ele1);
				Set<String> ele2 = currentLevel.get(j);
				
				
				tempSet.retainAll(ele2);
				//ele1, ele2属于同一个PREFIX BLOCK
				if (tempSet.size() == ele1.size() - 1){
					List<Set<String>> prefixBlock = new LinkedList<Set<String>>();
					prefixBlock.add(ele1);
					prefixBlock.add(ele2);
					result.add(prefixBlock);
				}
				
			}
		}
		return result;
	}


	private static void prune(List<Set<String>> currentLevel) {
		Iterator<Set<String>> it = currentLevel.iterator();
		while(it.hasNext()){
			Set<String> X = it.next();
			if(rhs.get(X).isEmpty()){
				it.remove();
			} else {
				// X is a superKey
				if (caculateE(X) == 0) {
					Set<String> _set = new HashSet<String>(rhs.get(X));
					_set.removeAll(X);
					for (String A : _set) {
						Set<String> XA = new HashSet<String>(X);
						Set<String> interSet = new HashSet<String>();
						XA.add(A);
						for (String B : X) {
							XA.remove(B);
							if (rhs.containsKey(XA)) {
								interSet.retainAll(rhs.get(XA));		
							}
						}
						if (interSet.contains(A)) {
							addFD(A,X);
						}
					}
					it.remove();
				}
			}
		}
		//superKey Logic, X 的分区中所有等价类的size都是1，对于stripped partition来说，就是空
		
	}


	private static void addFD(String a, Set<String> x) {
		if(FD.containsKey(a)){
			FD.get(a).add(x);
			//System.out.println("Function Dependency" + X.toString() + "->" + A);
		}else{
			Set<Set<String>> fdSet = new HashSet<Set<String>>();
			fdSet.add(x);
			FD.put(a, fdSet);
		}
		
	}


	private static void computeDependencies(List<Set<String>> currentLevel) {
		caculateLevelRHS(currentLevel);
		validateDependency(currentLevel);
	}


	private static void validateDependency(List<Set<String>> currentLevel) {
		for(Set<String> elements : currentLevel){
			//X并rhs[X]
			Set<String> candidates = new HashSet<String>(elements);
			candidates.retainAll(rhs.get(elements));
			//R-X
			Set<String> lastSet = new HashSet<String>(rhs.get(emptyRhs));
			lastSet.removeAll(elements);
			
			for(String A : candidates){
				Set<String> X = new HashSet<String>(elements);
				X.remove(A);
				Set<String> XA = new HashSet<String>(X);
				XA.add(A);
				if (!X.isEmpty() && checkValid(X, A)){
					addFD(A,X);
					rhs.get(elements).remove(A);
					if(caculateE(X) == caculateE(XA)){
						rhs.get(elements).removeAll(lastSet);
					}
				}
			}
		}
		
	}


	//e(X-{A} -> A) < threshold
	private static boolean checkValid(Set<String> X, String A) {
		// X-{A}
		Set<String> XA = new HashSet<String>(X);
		XA.add(A);
		float e_x = caculateE(X);
		float e_w = caculateE(XA);
		
		if (e_x - e_w > eps && e_x < eps){
			return true;
		}else{
			float e = computeFDE(X, A, XA);	
			
	        return e < eps && e > 0.0005;
		}
	}


	private static float computeFDE(Set<String> X, String A, Set<String> XA) {
		int e = 0;

		HashMap<Integer, Integer> T = new HashMap<Integer, Integer>();
		
		for (Set<Integer> c : partition.get(XA).values()) {
			T.put(c.iterator().next(), c.size());
		}
		for (Set<Integer> c : partition.get(X).values()) {
			int m = 1;
			for (int cuid : c) {
				if (T.containsKey(cuid) && T.get(cuid) > m)
					m = T.get(cuid);
			}
			e += c.size() - m;
		}
		
		float result = (float) e / (float) tuplesAmount;
		
		
		/*if(A == "ZIP"){
			System.out.println("e(" + X + "->" + A + "): " + result);
			//System.out.println(partition.get(X).toString());
		}*/
		
		return result;
	}


	//在stripped partition的前提下，e(X) = (partion(X)中所有等价类的size总和 - partion(X)的等价类个数)/总的记录数
	//stripped partition是在分区时，移除size 为 1的等价类
	private static float caculateE(Set<String> X) {
		HashMap<Integer, Set<Integer>> strippedPartition = partition.get(X);
		int sumSize = 0;
		for(Set<Integer> eqClass : strippedPartition.values()){
			sumSize += eqClass.size();
		}
		return (float)(sumSize - strippedPartition.size()) / (float)tuplesAmount;	
	}


	//判断是否子集
	private static boolean isSubset(Set<Integer> src, Set<Integer> tar) {
		return src.containsAll(tar);
	}


	private static void caculateLevelRHS(List<Set<String>> currentLevel) {
		for(Set<String> elements: currentLevel){
			Set<String> result = new HashSet<String>();
			for(String element : elements){
				//X-{A}
				Set<String> _elements = new HashSet<String>(elements);
				_elements.remove(element);
				//对每个rhsElement进行并集操作
				if(_elements.isEmpty()) _elements = emptyRhs;
				Set<String> rhsElement = rhs.get(_elements);
				if(result.isEmpty()){
					result.addAll(rhsElement);
				}else{
					result.retainAll(rhsElement);
				}
			}
			rhs.put(elements, result);
		}
	}

	private static void initFDDiscover(HashMap<String, Tuple> truthTuples) {
		rhsE = new HashMap<String, Float>();
		levels = new LinkedList<List<Set<String>>>();
		List<Set<String>> firstLevel = new LinkedList<Set<String>>();
		for(String field : FIELDS){
			Set<String> _set = new HashSet<String>();
			_set.add(field);
			firstLevel.add(_set);
		}
		levels.addLast(firstLevel);
		
		FD = new HashMap<String, Set<Set<String>>>();
		
		rhs = new HashMap<Set<String>, Set<String>>();
		//顶点
		rhs.put(emptyRhs, 
				new HashSet<String>(Arrays.asList(FIELDS)));
		
		//分区
		//Stripped partition需要移除size 为1的等价类
		partition = new HashMap<Set<String>, HashMap<Integer, Set<Integer>>>();
		
		Collection<Tuple> tuples = truthTuples.values();
		for(Set<String> element : firstLevel){
			String field = (String) element.toArray()[0];
			HashMap<String, Integer> valueDict = new HashMap<String, Integer>();
			int valueClass = 1;
			HashMap<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();
			for (Tuple tuple : tuples) {
				String value = tuple.getValue(field);
				int cuid = Integer.parseInt(tuple.getValue("CUID"));
				int klass;
				if (!value.isEmpty()) {
					if (valueDict.containsKey(value)) {
						klass = valueDict.get(value);
					} else {
						klass = valueClass;
						valueDict.put(value, valueClass);
						valueClass += 1;
					}

					if (result.containsKey(klass)) {
						result.get(klass).add(cuid);
					} else {
						Set<Integer> cuidSet = new HashSet<Integer>();
						cuidSet.add(cuid);
						result.put(klass, cuidSet);
					}
				}
			}
			
			result = stripSet(result);
			
			partition.put(element, result);
		}
		
		tuplesAmount = truthTuples.size();
		
	}


	private static HashMap<Integer, Set<Integer>> stripSet(HashMap<Integer, Set<Integer>> result) {
		Iterator<Entry<Integer, Set<Integer>>>  it = result.entrySet().iterator();
		while(it.hasNext()){
			Entry<Integer, Set<Integer>> entry = it.next();
			if(entry.getValue().size() == 1){
				it.remove();
			}
		}
		return result;
	}
	
}
