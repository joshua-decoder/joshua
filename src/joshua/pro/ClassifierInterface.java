package joshua.pro;

import java.util.Vector;

public interface ClassifierInterface 
{
	/* Arguments required to train a binary linear classifier:
	 * Vector<String> samples: all training samples should use sparse feature value representation.
		  					   Format: feat_id1:feat_val1 feat_id2:feat_val2 ... label (1 or -1)  
							   Example: 3:0.2 6:2 8:0.5 -1 (only enumerate firing features)
							   Note feat_id should start from 1
	 * double[] initialLambda: the initial weight vector(doesn't have to be used, depending on the classifier -
	 * 						   just ignore the array if not to be used). The length of the vector should be the
	 * 						   same as feature dimension. Note the 0^th entry is not used, so array should have
	 * 						   length featDim+1 (to be consistent with Z-MERT)
	 * int featDim:		   	   feature vector dimension
	
	 * Return value:
	 * double[]:	a vector containing weights for all features after training(also should have length featDim+1)
	 */
	double[] runClassifier( Vector<String> samples, double[] initialLambda, int featDim );
	
	//Set classifier-specific parameters, like config file path, num of iterations, command line... 
	void setClassifierParam( String[] param );
}
