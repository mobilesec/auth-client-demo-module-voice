package at.fhooe.mcm.smc.math.mfcc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import at.fhooe.mcm.smc.math.matrix.Matrix;

/** Represents a vector of features, in this case MFCCs. */
public class FeatureVector implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -152381381588586374L;
	private int dimension;
	private int numberElements;
	private List<Matrix> data;


	/**
	 * Creates a new point list with default capacity and a dimension.
	 * 
	 */
	public FeatureVector(int dimension) {
		this(dimension, 16384);
	}

	/**
	 * Creates a new point list.
	 * 
	 * @param dimension
	 *            int dimension of the vector space; must be at least one
	 */
	public FeatureVector(int dimension, int capacity) {
		if (dimension < 1 || capacity < 1)
			throw new IllegalArgumentException(
					"capacity and dimension must be >= 1");
		this.data = new ArrayList<Matrix>(capacity);
		this.dimension = dimension;
	}

	/**
	 * Adds a new point.
	 * 
	 */
	public void add(double[] point) {
		if (point == null || point.length != dimension)
			throw new IllegalArgumentException(
					"data point must not be a null value and dimension must agree");

		Matrix x = new Matrix(point, dimension);

		// add the point to the list
		data.add(x);
		numberElements++;

	}

	public int getDimension() {
		return dimension;
	}

	public int size() {
		return numberElements;
	}

	public Matrix get(int i) {
		return data.get(i);
	}
}
