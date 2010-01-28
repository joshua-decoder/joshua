package joshua.discriminative.semiring_parsing;

import java.util.HashMap;

import joshua.decoder.hypergraph.HGNode;
import joshua.decoder.hypergraph.HyperEdge;
import joshua.decoder.hypergraph.HyperGraph;

/** semiring parsing on a Hypergraph, 
 *  
 */

/*This calss does not require any correctness of the HG scores, but it does rely on the topology of the HG
 * All that matters is the getHyperedgeSemiringWeight() function, which should return the right value that we want to do expectation*/

public abstract class DefaultSemiringParser {

	private HashMap<HGNode, CompositeSemiring> semiringMembersTable =  new HashMap<HGNode, CompositeSemiring>();
	AtomicSemiring atomicSemiring;
	double scale = 1;
	

	protected HyperGraph hg;
	
	public DefaultSemiringParser(int semiring, int add_mode, double scale){
		if(semiring!=1 || add_mode !=0){
			System.out.println("We only suport log-sum atomic semiring now");
			System.exit(1);
		}
		this.scale = scale;
		atomicSemiring = new AtomicSemiring(semiring, add_mode);
	}

	//score specific to the HG and the expectation that we want to compute
	protected abstract CompositeSemiring getHyperedgeSemiringWeight(HyperEdge dt, HGNode parent_item, double scale, AtomicSemiring p_atomic_semiring);
	
	protected abstract CompositeSemiring createNewSemiringMember();

	public final void setScale(double scale){
		this.scale = scale;
	}
	
	

	public void setHyperGraph(HyperGraph hg){
		semiringMembersTable.clear(); 	
		this.hg = hg;
	}
	
//	############ bottomn-up insdide estimation ##########################	
	public void insideEstimationOverHG(HyperGraph hg){
		semiringMembersTable.clear(); 		
		insideEstimationOverItem(hg.goalNode);
	}
	
	
	public  CompositeSemiring getGoalSemiringMember(HyperGraph hg){
		return semiringMembersTable.get(hg.goalNode);	
	}
	
	private CompositeSemiring insideEstimationOverItem(HGNode it){		
		if(semiringMembersTable.containsKey(it))
			return (CompositeSemiring) semiringMembersTable.get(it);
		
		CompositeSemiring res = createNewSemiringMember();
		res.setToZero(atomicSemiring);
		
		//### recursive call on each hyperedge
		for(HyperEdge dt : it.hyperedges){
			CompositeSemiring hyperedgeWeight = insideEstimationOverHyperedge(dt, it);//deduction-specifc operation
			res.add(hyperedgeWeight, atomicSemiring);
		}		
		//### item-specific operation, but all the prob should be factored into each deduction
		//res.normalizeFactors();		
		semiringMembersTable.put(it,res);
		return res;
	}

	private CompositeSemiring insideEstimationOverHyperedge(HyperEdge dt, HGNode parent_item){
		CompositeSemiring res = createNewSemiringMember();
		res.setToOne(atomicSemiring);
		 
		//### recursive call on each ant item
		if(dt.getAntNodes()!=null)
			for(HGNode ant_it : dt.getAntNodes()){
				CompositeSemiring sem_item = insideEstimationOverItem(ant_it);
				res.multi(sem_item, atomicSemiring);				
			}
				
		//### deduction operation
		CompositeSemiring deduct_prob = getHyperedgeSemiringWeight(dt, parent_item, scale, atomicSemiring);//feature-set specific	
		res.multi(deduct_prob, atomicSemiring);	
		return res;
	}
//	########### end inside estimation	

			
}
