package sssj.index;

import static sssj.Commons.forgetFactor;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssj.Commons;
import sssj.Commons.ResidualList;
import sssj.Vector;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.common.primitives.Doubles;

public class StreamingIndex implements Index {
  private static final Logger log = LoggerFactory.getLogger(StreamingIndex.class);
  private Int2ReferenceMap<StreamingPostingList> idx = new Int2ReferenceOpenHashMap<>();
  private ResidualList resList = new ResidualList();
  private int size = 0;
  private final double theta;
  private final double lambda;
  private final double tau;
  private final Vector maxVector; // \hat{y}

  public StreamingIndex(double theta, double lambda) {
    this.theta = theta;
    this.lambda = lambda;
    this.tau = Commons.tau(theta, lambda);
    this.maxVector = new Vector();
    log.info("Tau = {}", tau);
  }

  @Override
  public Map<Long, Double> queryWith(final Vector v) {
    Vector updates = maxVector.updateMaxByDimension(v);
    Long2DoubleMap accumulator = new Long2DoubleOpenHashMap(size);
    for (Entry e : v.int2DoubleEntrySet()) {
      int dimension = e.getIntKey();
      if (!idx.containsKey(dimension))
        continue;
      StreamingPostingList list = idx.get(dimension);
      double queryWeight = e.getDoubleValue();
      for (Iterator<StreamingPostingEntry> it = list.iterator(); it.hasNext();) {
        StreamingPostingEntry pe = it.next();
        long targetID = pe.getLongKey();

        // time filtering
        long deltaT = v.timestamp() - targetID;
        if (Doubles.compare(deltaT, tau) > 0) {
          it.remove();
          continue;
        }

        double targetWeight = pe.getDoubleValue();
        double currentSimilarity = accumulator.get(targetID);
        double additionalSimilarity = queryWeight * targetWeight * forgetFactor(lambda, deltaT); // add forgetting factor e^(-lambda*delta_T)
        accumulator.put(targetID, currentSimilarity + additionalSimilarity);
      }
    }

    // filter candidates < theta
    Map<Long, Double> results = Maps.filterValues(accumulator, new Predicate<Double>() {
      @Override
      public boolean apply(Double input) {
        return input.compareTo(theta) >= 0;
      }
    });
    return results;
  }

  @Override
  public Vector addVector(Vector v) {
    size++;
    for (Entry e : v.int2DoubleEntrySet()) {
      int dimension = e.getIntKey();
      double weight = e.getDoubleValue();
      if (!idx.containsKey(dimension))
        idx.put(dimension, new StreamingPostingList());
      idx.get(dimension).add(v.timestamp(), weight);
    }
    return Vector.EMPTY_VECTOR;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public String toString() {
    return "StreamingIndex [idx=" + idx + "]";
  }

  public static class StreamingPostingList implements Iterable<StreamingPostingEntry> {
    private LongArrayList ids = new LongArrayList();
    private DoubleArrayList weights = new DoubleArrayList();

    public void add(long vectorID, double weight) {
      ids.add(vectorID);
      weights.add(weight);
    }

    @Override
    public String toString() {
      return "[ids=" + ids + ", weights=" + weights + "]";
    }

    @Override
    public Iterator<StreamingPostingEntry> iterator() {
      return new Iterator<StreamingPostingEntry>() {
        private int i = 0;
        private StreamingPostingEntry entry = new StreamingPostingEntry();

        @Override
        public boolean hasNext() {
          return i < ids.size();
        }

        @Override
        public StreamingPostingEntry next() {
          entry.setKey(ids.getLong(i));
          entry.setValue(weights.getDouble(i));
          i++;
          return entry;
        }

        @Override
        public void remove() { // TODO optimize to avoid too many system.arraycopy() calls
          i--;
          ids.removeLong(i);
          weights.removeDouble(i);
        }
      };
    }
  }

  public static class StreamingPostingEntry {
    protected long key;
    protected double value;

    public StreamingPostingEntry() {
      this(0, 0);
    }

    public StreamingPostingEntry(long key, double value) {
      this.key = key;
      this.value = value;
    }

    public void setKey(long key) {
      this.key = key;
    }

    public void setValue(double value) {
      this.value = value;
    }

    public long getLongKey() {
      return key;
    }

    public double getDoubleValue() {
      return value;
    }

    @Override
    public String toString() {
      return "[" + key + " -> " + value + "]";
    }
  }
}
