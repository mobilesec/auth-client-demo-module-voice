package at.fhooe.mcm.smc.math.vq;

import at.fhooe.mcm.smc.math.matrix.Matrix;

/** Represents a single cluster for the k-means algorithm. */
public class Cluster {
	private int elementCount; 
	private Matrix center; 

	private Matrix sum; 
	private Matrix squaredSum; 

	/** Creates a single cluster with a new center */
	public Cluster(Matrix mean) {
		this.center = mean;
		this.elementCount = 0;
		this.sum = new Matrix(center.getRowDimension(), center
				.getColumnDimension());
		this.squaredSum = new Matrix(center.getRowDimension(), center
				.getColumnDimension());
	}

	/**
	 * Returns the euclidean distance of a point x to the cluster center.
	 * 
	 */
	public double getDistanceFromCenter(Matrix x) {
		Matrix diff = center.minus(x);
		return diff.transpose().times(diff).get(0, 0);
	}

	/**
	 * Adds a point x to this cluster.
	 * 
	 */
	public void add(Matrix x) {
		sum.plusEquals(x);
		squaredSum.plusEquals(x.pow(2.0d));
		elementCount++;
	}

	/**
	 * Returns the mean of all the elements in this cluster.
	 * 
	 */
	public Matrix getMean() {
		return sum.times(1.0d / elementCount);
	}


	/**
	 * Returns the cluster center.
	 */ 
	public Matrix getCenter() {
		return center;
	}
	
	/** Resets this cluster. */
	public void reset(Matrix newCenter) {
		this.center = newCenter;
		this.elementCount = 0;
		this.sum = new Matrix(center.getRowDimension(), center
				.getColumnDimension());
		this.squaredSum = new Matrix(center.getRowDimension(), center
				.getColumnDimension());
	}


}