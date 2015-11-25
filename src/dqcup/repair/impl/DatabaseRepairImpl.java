package dqcup.repair.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import dqcup.repair.DatabaseRepair;
import dqcup.repair.DbFileReader;
import dqcup.repair.RepairedCell;
import dqcup.repair.Tuple;

public class DatabaseRepairImpl implements DatabaseRepair {

	@Override
	public Set<RepairedCell> repair(String fileRoute) {
		//Please implement your own repairing methods here.
		LinkedList<Tuple> tuples = DbFileReader.readFile(fileRoute);
		
		HashSet<RepairedCell> result = new HashSet<RepairedCell>();
		
		HashMap<String, Tuple> truthTuples = new HashMap<String, Tuple>();
		
		HashMap<String, Set<Set<String>>> FDs;
		
		//Data Profolling
		truthTuples = DataProfolling.performance(tuples);
		FDs = FDDiscover.performance(truthTuples);
		truthTuples = FDScanner.performance(FDs, truthTuples);
		//truthTuples = FunctionDependency.performance(truthTuples);
		/*
		 * 遍历一次tuples
		 * 进行单行单列处理（对比metadata进行检查和修复）
		 */
		for (Tuple tuple: tuples){
			Tuple truthTuple = truthTuples.get(tuple.getValue("CUID"));
			for (String field : DataProfolling.FIELDS) {
				if (!tuple.getValue(field).equals(truthTuple.getValue(field))) {
					result.add(new RepairedCell(Integer.parseInt(tuple.getValue("RUID")), 
							field, 
							truthTuple.getValue(field)));
				}
			}
		}
		
		for (RepairedCell rc : result){
			System.out.println(rc.toString());
		}
		
		return result;
	}

}
