package sssj.index;

public abstract class AbstractIndex implements Index {
  protected int size;
  protected int maxLength;
  protected long numCandidates;
  protected long numSimilarities;
  protected long numPostingEntries;

  @Override
  public IndexStatistics stats() {
    return new IndexStatistics() {

      @Override
      public int size() {
        return size;
      }

      @Override
      public int maxLength() {
        return maxLength;
      }

      @Override
      public long numCandidates() {
        return numCandidates;
      }

      @Override
      public long numSimilarities() {
        return numSimilarities;
      }

      @Override
      public long numPostingEntries() {
        return numPostingEntries;
      }
    };
  }

}
