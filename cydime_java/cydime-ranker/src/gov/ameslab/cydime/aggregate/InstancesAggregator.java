package gov.ameslab.cydime.aggregate;

import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.ReplaceMissingValues;
import static gov.ameslab.cydime.preprocess.WekaPreprocess.UnsupervisedFilter.StringToNominal;
import gov.ameslab.cydime.aggregate.aggregator.Aggregator;
import gov.ameslab.cydime.model.InstanceDatabase;
import gov.ameslab.cydime.preprocess.WekaPreprocess;
import gov.ameslab.cydime.util.CUtil;
import gov.ameslab.cydime.util.Config;
import gov.ameslab.cydime.util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class InstancesAggregator {
	
	private static final Logger Log = Logger.getLogger(InstancesAggregator.class.getName());
	
	private List<String> mIDs;
	private Instances mSchema;
	private List<? extends Aggregator> mAgg;

	private Map<String, List<Instance>> mInstancesMap;
	
	private Instance cZeroInstance;
	
	public InstancesAggregator(List<String> ids, Instances schema, List<? extends Aggregator> agg) {
		mIDs = ids;
		mSchema = schema;
		mAgg = agg;
		mInstancesMap = CUtil.makeMap();
	}
		
	public void addInstance(String id, Instance inst) {
		List<Instance> insts = mInstancesMap.get(id);
		if (insts == null) {
			//Aggregation for an IP only starts from the first day of occurrence
			if (inst == cZeroInstance) return;
			
			insts = CUtil.makeList();
			mInstancesMap.put(id, insts);
		}
		
		insts.add(inst);
	}
	
	public void addZeroInstance(String id) {
		addInstance(id, getZeroInstance());
	}
	
	private Instance getZeroInstance() {
		if (cZeroInstance == null) {
			cZeroInstance = new DenseInstance(mSchema.numAttributes());
			cZeroInstance.setDataset(mSchema);
			
			for (int i = 0; i < mSchema.numAttributes(); i++) {
				if (i == mSchema.classIndex()) continue;
				
				if (i >= 13 && i <= 15) {
					//avg_sim, var_sim, max_sim
					cZeroInstance.setMissing(i);
					continue;
				}
				
				Attribute a = mSchema.attribute(i);
				if (a.isNumeric()) {
					cZeroInstance.setValue(i, 0.0);
				} else {
					cZeroInstance.setMissing(i);
				}
			}
			
			cZeroInstance.setClassMissing();
		}
		return cZeroInstance;
	}

	public InstanceDatabase aggregate() throws IOException {
		InstanceDatabase[] instDBs = new InstanceDatabase[mAgg.size()];
		for (int i = 0; i < instDBs.length; i++) {
			Aggregator agg = mAgg.get(i);
			Instances aggSchema = makeSchema(agg);
			Instances instances = new Instances(aggSchema, 0);
			
			Log.log(Level.INFO, "Aggregating {0}...", agg.getName());
			
			for (String id : mIDs) {
				List<Instance> insts = mInstancesMap.get(id);
				
				Instance inst = aggregate(insts, agg, instances);
				instances.add(inst);
			}
			
			String outPath = Config.INSTANCE.getAggregatedPath() + "." + agg.getName();
			WekaPreprocess.save(instances, outPath + WekaPreprocess.ALL_SUFFIX);
			
			WekaPreprocess.filterUnsuperivsed(outPath + WekaPreprocess.ALL_SUFFIX,
					StringToNominal,
					ReplaceMissingValues);
			
			FileUtil.copy(outPath + WekaPreprocess.ALL_SUFFIX, outPath + WekaPreprocess.REPORT_SUFFIX);
			
			instDBs[i] = new InstanceDatabase(outPath, mIDs);
		}
		
		Log.log(Level.INFO, "Merging all aggregates...");
		
		InstanceDatabase allAgg = InstanceDatabase.mergeFeatures(Config.INSTANCE.getAggregatedPath(), instDBs);
		allAgg.saveIPs();
		allAgg.writeReport();
		return allAgg;
	}

	private Instances makeSchema(Aggregator agg) throws IOException {
		Instances aggSchema = new Instances(agg.getName(), new ArrayList<Attribute>(), 0);
		
		for (int i = 0, ia = 0; i < mSchema.numAttributes(); i++) {
			if (i == mSchema.classIndex()) continue;
			
			Attribute a = mSchema.attribute(i);
			if (a.isNumeric() == agg.isNumeric()) {
				aggSchema.insertAttributeAt(agg.makeAttribute(a.name()), ia);
				ia++;
			}
		}
		
		Attribute classAtt = new Attribute("class");
		aggSchema.insertAttributeAt(classAtt, aggSchema.numAttributes());
		aggSchema.setClassIndex(aggSchema.numAttributes() - 1);
		
		return aggSchema;
	}
	
	private Instance aggregate(List<Instance> insts, Aggregator agg, Instances aggInsts) {
		Instance inst = new DenseInstance(aggInsts.numAttributes());
		inst.setDataset(aggInsts);
		for (int i = 0, ia = 0; i < mSchema.numAttributes(); i++) {
			if (i == mSchema.classIndex()) continue;
			
			Attribute a = mSchema.attribute(i);
			if (a.isNumeric() == agg.isNumeric()) {
				agg.aggregate(insts, i, inst, ia);
				ia++;
			}
		}
		inst.setClassMissing();		
		return inst;
	}
	
}
