package dqcup.repair.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import dqcup.repair.Tuple;

public class FDScanner {

	public static HashMap<String, Tuple> performance(HashMap<String, Set<String>> FDs,
			HashMap<String, Tuple> truthTuples) {
		
		for(Entry<String, Set<String>> fd : FDs.entrySet()){
			System.out.println(fd.getValue().toString() + "->" + fd.getKey());
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

	private static String checkValue(String field, Tuple tuple, HashMap<String, Set<String>> FDs, HashMap<String, Tuple> truthTuples) {
		Set<String> dependencies = FDs.get(field);
		Collection<Set<Integer>> partitions = FDDiscover.partition.get(dependencies).values();
		Set<Integer> partition = new HashSet<Integer>();
		
		int cuid = Integer.parseInt(tuple.getValue("CUID"));
		for(Set<Integer> _p: partitions){
			if(_p.contains(cuid)){
				partition = _p;
				break;
			}
		}
		LinkedList<Tuple> tuples = new LinkedList<Tuple>();
		for(int _cuid : partition){
			tuples.add(truthTuples.get(Integer.toString(_cuid)));
		}
		Iterator<Tuple> it = tuples.iterator();
		while(it.hasNext()){
			Tuple t = it.next();
			if(t.getValue(field).matches("NeedRepair")){
				it.remove();
			}
		}
		if(tuples.size() > 2){
			return DataProfolling.voteTruthValue(tuples, field);	
		}else{	
			return tuple.getValue(field);
		}	
	}

}
