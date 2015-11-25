package dqcup.repair.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import dqcup.repair.Tuple;

public class FDScanner {

	public static HashMap<String, Tuple> performance(HashMap<String, Set<Set<String>>> FDs,
			HashMap<String, Tuple> truthTuples) {
		
		for(Entry<String, Set<Set<String>>> fds : FDs.entrySet()){
			for(Set<String> fd : fds.getValue()){
				System.out.println(fd.toString() + "->" + fds.getKey());
			}
		}
		LinkedList<String> FDKeys = new LinkedList<String>(FDs.keySet());
		
		for (Tuple tuple : truthTuples.values()) {
			StringBuffer sb = new StringBuffer();
			for (String _field : DataProfolling.FIELDS){
				if (FDKeys.contains(_field)){
					sb.append(checkValue(_field, tuple, FDs, truthTuples)).append(":");
				} else {
					sb.append(tuple.getValue(_field)).append(":");
				}
			}
			sb.deleteCharAt(sb.length() - 1);
			truthTuples.put(tuple.getValue("CUID"), 
					new Tuple(DataProfolling.COLUMNNAMES, sb.toString()));
			truthTuples.remove(tuple);
		}
		return truthTuples;
	}

	private static String checkValue(String field, Tuple tuple, HashMap<String, Set<Set<String>>> FDs, HashMap<String, Tuple> truthTuples) {
		Map<String, Integer> voteBox = new HashMap<String, Integer>();
		for(Set<String> _dependency : FDs.get(field)){
			String result;
			Set<String> dependencies = _dependency;
			Collection<Set<Integer>> partitions = FDDiscover.partition.get(dependencies).values();
			Set<Integer> partition = new HashSet<Integer>();

			int cuid = Integer.parseInt(tuple.getValue("CUID"));
			for (Set<Integer> _p : partitions) {
				if (_p.contains(cuid)) {
					partition = _p;
					break;
				}
			}
			LinkedList<Tuple> tuples = new LinkedList<Tuple>();
			for (int _cuid : partition) {
				tuples.add(truthTuples.get(Integer.toString(_cuid)));
			}
			Iterator<Tuple> it = tuples.iterator();
			while (it.hasNext()) {
				Tuple t = it.next();
				if (t.getValue(field).matches("NeedRepair")) {
					it.remove();
				}
			}
		
			if (tuples.size() > 2) {
				result = DataProfolling.voteTruthValue(tuples, field);
			} else {
				result = tuple.getValue(field);
			}
			
			if(!result.matches("NeedRepair")){
				if (voteBox.containsKey(result)) {
					voteBox.put(result, voteBox.get(result) + 1);
				} else {
					voteBox.put(result, 1);
				}
			}
				
		}

		int most = Integer.MIN_VALUE;
		String result = tuple.getValue(field);
		for(Entry<String, Integer> vote : voteBox.entrySet()){
			if(vote.getValue() > most){
				most = vote.getValue();
				result = vote.getKey();
			}
		}
		return result;
	}

}
