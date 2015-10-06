package sssj.index;

import java.util.Map;

import sssj.base.Vector;

public interface Index {

  /**
   * Queries the current index with the vector passed as parameter.
   * 
   * @param v
   *          the query vector
   * @param addToIndex
   *          a boolean indicating whether to add the query vector to the index
   * @return the matchings between the previously indexed vectors and the query
   */
  public Map<Long, Double> queryWith(final Vector v, final boolean addToIndex);

  /**
   * Returns the size of the index in number of entries (dimension-value pairs).
   * 
   * @return the number of entries indexed
   */
  public int size();

  /**
   * Returns the maximum length of the posting lists in the index.
   * 
   * @return the maximum posting list length
   */
  public int maxLength();
}