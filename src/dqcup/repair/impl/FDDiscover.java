package dqcup.repair.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dqcup.repair.Tuple;

public class FDDiscover {
	//数据立方体
	public static LinkedList<List<Set<String>>> levels;
	
	//FD信息
	public static HashMap<String, Set<String>> FD;
	
	//Right Hand Sides plus
	public static HashMap<Set<String>, Set<String>> rhs;
	//顶点key
	public static final Set<String> topRhs = new HashSet<String>(Arrays.asList("top"));
	

	public static void performance(HashMap<String, Tuple> truthTuples) {
		initFDDiscover();
		
		List<Set<String>> currentLevel = levels.getLast();
		
		while(!currentLevel.isEmpty()){
			computeDependencies(currentLevel);
			prune(currentLevel);
			levels.addLast(generateNextLevel(currentLevel));
			currentLevel = levels.getLast();
		}
		
	}


	private static List<Set<String>> generateNextLevel(List<Set<String>> currentLevel) {
		List<Set<String>> nextLevel = new LinkedList<Set<String>>();
		List<List<Set<String>>> prefixBlocks = findPrefixBlocks(currentLevel);
		
		return null;
	}


	/**
	 * 寻找prefix blocks
	 * @param currentLevel
	 * @return
	 */
	private static List<List<Set<String>>> findPrefixBlocks(List<Set<String>> currentLevel) {
		List<List<Set<String>>> result = new LinkedList<List<Set<String>>>();
		for(int i = 0; i < currentLevel.size(); i ++){
			Set<String> ele1 = currentLevel.get(i);
			for(int j = 0; j < currentLevel.size(); j ++){
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
		// TODO Auto-generated method stub
		
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
			Set<String> lastSet = new HashSet<String>(rhs.get(topRhs));
			lastSet.removeAll(elements);
			
			for(String candidate : candidates){
				Set<String> fromSet = new HashSet<String>(elements);
				fromSet.remove(candidate);
				if (checkValid(fromSet, candidate)){
					FD.put(candidate, fromSet);
					rhs.get(elements).remove(candidate);
					rhs.get(elements).removeAll(lastSet);
				}
			}
		}
		
	}


	private static boolean checkValid(Set<String> fromSet, String candidate) {
		// TODO Auto-generated method stub
		return false;
	}


	private static void caculateLevelRHS(List<Set<String>> currentLevel) {
		for(Set<String> elements: currentLevel){
			Set<String> result = new HashSet<String>();
			for(String element : elements){
				//X-{A}
				Set<String> _elements = new HashSet<String>(elements);
				_elements.remove(element);
				//对每个rhsElement进行并集操作
				Set<String> rhsElement = caculateRHS(_elements);
				if(result.isEmpty()){
					result.addAll(rhsElement);
				}else{
					result.retainAll(rhsElement);
				}
			}
			rhs.put(elements, result);
		}
	}


	private static Set<String> caculateRHS(Set<String> elements) {
		// TODO Auto-generated method stub
		return null;
	}


	private static void initFDDiscover() {
		levels = new LinkedList<List<Set<String>>>();
		List<Set<String>> firstLevel = new LinkedList<Set<String>>();
		for(String field : DataProfolling.FIELDS){
			Set<String> _set = new HashSet<String>();
			_set.add(field);
			firstLevel.add(_set);
		}
		levels.addLast(firstLevel);
		
		FD = new HashMap<String, Set<String>>();
		
		rhs = new HashMap<Set<String>, Set<String>>();
		//顶点
		rhs.put(topRhs, 
				new HashSet<String>(Arrays.asList(DataProfolling.FIELDS)));
	}

}
