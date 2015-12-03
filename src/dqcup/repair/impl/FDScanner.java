package dqcup.repair.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import dqcup.repair.Tuple;

public class FDScanner {

	public static HashMap<String, Tuple> performance(HashMap<String, Set<Set<String>>> FDs,
			HashMap<String, Tuple> truthTuples) {
		
		/*
		for(Entry<String, Set<Set<String>>> fds : FDs.entrySet()){
			for(Set<String> fd : fds.getValue()){
				System.out.println(fd.toString() + "->" + fds.getKey());
			}
		}
		*/
		LinkedList<String> FDKeys = new LinkedList<String>(FDs.keySet());
		
		for (Tuple tuple : truthTuples.values()) {
			StringBuffer sb = new StringBuffer();
			for (String _field : DataProfolling.FIELDS){
				if (FDKeys.contains(_field)){
//					if(tuple.getValue("CUID").equals("9718") && _field.equals("ZIP")){
//						System.out.println("hahahahah");
//					}
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
			// 只有存在等价类时，才把其放入voteBox，否则视为对这个tuple无效的FD
			if (!partition.isEmpty()) {
				LinkedList<Tuple> tuples = new LinkedList<Tuple>();
				for (int _cuid : partition) {
					tuples.add(truthTuples.get(Integer.toString(_cuid)));
				}
				Iterator<Tuple> it = tuples.iterator();
				while (it.hasNext()) {
					Tuple t = it.next();
					if (isNeedRepair(t.getValue(field))) {
						it.remove();
					}
				}

				result = tuple.getValue(field);
				if (isNeedRepair(result)) {
					result = voteMostValue(tuples, field).get("value");
				} else {
					if (tuples.size() > 2) {
						Map<String, String> voteResult = voteMostValue(tuples, field);
						if (Integer.parseInt(voteResult.get("count")) > 1)
							result = voteResult.get("value");
					}
				}

				if (!result.isEmpty()) {
					if (voteBox.containsKey(result)) {
						voteBox.put(result, voteBox.get(result) + 1);
					} else {
						voteBox.put(result, 1);
					}
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

	private static boolean isNeedRepair(String value) {
		return value.endsWith("NeedRepair");
	}

	private static Map<String, String> voteMostValue(LinkedList<Tuple> tuples, String field) {
		String truthValue = "";
		int maxVote = -1;
		HashMap<String, Integer> voteBox = new HashMap<String, Integer>();
		for (Tuple tuple : tuples) {
			String value = tuple.getValue(field);
			if (voteBox.containsKey(value)) {
				voteBox.put(value, voteBox.get(value) + 1);
			} else {
				voteBox.put(value, 1);
			}
		}
		for (Entry voteEntry : voteBox.entrySet()) {
			int voteCount = (Integer) voteEntry.getValue();
			String value = (String) voteEntry.getKey();
			if (voteCount > maxVote) {
				truthValue = value;
				maxVote = voteCount;
			}
		}
		Map<String, String> result = new HashMap<String, String>();
		result.put("value", truthValue);
		result.put("count", String.valueOf(maxVote));
		return result;
	}

}
