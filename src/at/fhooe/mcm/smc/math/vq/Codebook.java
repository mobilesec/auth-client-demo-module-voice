package at.fhooe.mcm.smc.math.vq;

import java.io.Serializable;

import at.fhooe.mcm.smc.math.matrix.Matrix;

/** Represents a codebook: a collection of cluster centers basically. */
public class Codebook implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2854040747139238024L;
	private int length;
	private Matrix[] centroids;
	
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public Matrix[] getCentroids() {
		return centroids;
	}
	public void setCentroids(Matrix[] centroids) {
		this.centroids = centroids;
	}
}
