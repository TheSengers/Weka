package weka.comparitizer;
import java.util.ArrayList;

import weka.core.Instance;

public class ClusterHomogeneity {

	  static double euclideanDistance(Instance a, Instance b)
	  {
		  double distance = 0;
		  
		  for(int m = 0; m < a.numAttributes(); m++)
			  distance += Math.pow( (a.toDoubleArray()[m] - b.toDoubleArray()[m]), 2 );
		  
		  return Math.sqrt(distance);
	  }
	  
	  static double manhattanDistance(Instance a, Instance b)
	  {
		  double distance = 0;
		  
		  for(int m = 0; m < a.numAttributes(); m++)
			  distance += Math.abs(a.toDoubleArray()[m] - b.toDoubleArray()[m]);
		  
		  return distance;
	  }
	  
	  public static double calculateHomogeneityUsingEuclidean(ArrayList<Instance> inst)
	  {
		  int numOfInstance = inst.size();
		  double total = 0;
		  
		  for(int i = 0; i < numOfInstance; i++)
			  for(int j = 0; j < numOfInstance; j++)		
				  //if(i != j)
					  total += euclideanDistance(inst.get(i), inst.get(j));
					 					 
		  return total / (numOfInstance* (numOfInstance - 1));
	  }
	  
	  public static double calculateHomogeneityUsingManhattan(ArrayList<Instance> inst)
	  {
		  int numOfInstance = inst.size();
		  double total = 0;
		  
		  for(int i = 0; i < numOfInstance; i++)
			  for(int j = 0; j < numOfInstance; j++)		
				  //if(i != j)
					  total += manhattanDistance(inst.get(i), inst.get(j));
					 					 
		  return total / (numOfInstance* (numOfInstance - 1));
	  }
	  
	  public static double calculateHomogeneityWithRespectToCentroidUsingEuclidean(ArrayList<Instance> inst, Instance centroid)
	  {
		  int numOfInstance = inst.size();
		  double total = 0;
		  
		  for(int i = 0; i < numOfInstance; i++)
					  total += euclideanDistance(inst.get(i), centroid);
		
		  return total / numOfInstance;
	  }
	  
	  public static double calculateHomogeneityWithRespectToCentroidUsingManhattan(ArrayList<Instance> inst, Instance centroid)
	  {
		  int numOfInstance = inst.size();
		  double total = 0;
		  
		  for(int i = 0; i < numOfInstance; i++)
					  total += manhattanDistance(inst.get(i), centroid);
		
		  return total / numOfInstance;
	  }
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
