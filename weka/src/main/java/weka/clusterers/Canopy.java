/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    Canopy.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import weka.classifiers.rules.DecisionTableHashKey;
import weka.core.AttributeStats;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SparseInstance;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 * <!-- globalinfo-start --> Cluster data using the capopy clustering algorithm,
 * which requires just one pass over the data. Can run in eitherbatch or
 * incremental mode. Results are generally not as good when running
 * incrementally as the min/max for each numeric attribute is not known in
 * advance. Has a heuristic (based on attribute std. deviations), that can be
 * used in batch mode, for setting the T2 distance. The T2 distance determines
 * how many canopies (clusters) are formed. When the user specifies a specific
 * number (N) of clusters to generate, the algorithm will return the top N
 * canopies (as determined by T2 density) when N &lt; number of canopies (this
 * applies to both batch and incremental learning); when N &gt; number of
 * canopies, the difference is made up by selecting training instances randomly
 * (this can only be done when batch training). For more information see:<br/>
 * <br/>
 * A. McCallum, K. Nigam, L.H. Ungar: Efficient Clustering of High Dimensional
 * Data Sets with Application to Reference Matching. In: Proceedings of the
 * sixth ACM SIGKDD internation conference on knowledge discovery and data
 * mining ACM-SIAM symposium on Discrete algorithms, 169-178, 2000.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;inproceedings{McCallum2000,
 *    author = {A. McCallum and K. Nigam and L.H. Ungar},
 *    booktitle = {Proceedings of the sixth ACM SIGKDD internation conference on knowledge discovery and data mining ACM-SIAM symposium on Discrete algorithms},
 *    pages = {169-178},
 *    title = {Efficient Clustering of High Dimensional Data Sets with Application to Reference Matching},
 *    year = {2000}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -N &lt;num&gt;
 *  Number of clusters.
 *  (default 2).
 * </pre>
 * 
 * <pre>
 * -t2
 *  The T2 distance to use. Values &lt; 0 indicate that
 *  a heuristic based on attribute std. deviation should be used to set this.
 *  Note that this heuristic can only be used when batch training
 *  (default = -1.0)
 * </pre>
 * 
 * <pre>
 * -t1
 *  The T1 distance to use. A value &lt; 0 is taken as a
 *  positive multiplier for T2. (default = -1.5)
 * </pre>
 * 
 * <pre>
 * -M
 *  Don't replace missing values with mean/mode when running in batch mode.
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 10458 $
 */
public class Canopy extends RandomizableClusterer implements
  UpdateableClusterer, NumberOfClustersRequestable, OptionHandler,
  TechnicalInformationHandler {

  /** For serialization */
  private static final long serialVersionUID = 2067574593448223334L;

  /** The canopy centers */
  protected Instances m_canopies;

  /** The T2 density of each canopy */
  protected List<double[]> m_canopyT2Density;
  protected List<double[][]> m_canopyCenters;
  protected List<double[]> m_canopyNumMissingForNumerics;

  /**
   * The list of canopies that each canopy is a member of (according to the T1
   * radius, which can overlap). Each bit position in the long values
   * corresponds to one canopy. Outer list order corresponds to the order of the
   * instances that store the actual canopy centers
   */
  protected List<long[]> m_clusterCanopies;

  public static final double DEFAULT_T2 = -1.0;
  public static final double DEFAULT_T1 = -1.25;

  /** < 0 means use the heuristic based on std. dev. to set the t2 radius */
  protected double m_userT2 = DEFAULT_T2;

  /**
   * < 0 indicates the multiplier to use for T2 when setting T1, otherwise the
   * value is take as is
   */
  protected double m_userT1 = DEFAULT_T1;

  /** Outer radius */
  protected double m_t1 = m_userT1;

  /** Inner radius */
  protected double m_t2 = m_userT2;

  /**
   * Default is to let the t2 radius determine how many canopies/clusters are
   * formed
   */
  protected int m_numClustersRequested = -1;

  /**
   * If not null, then this is expected to be a filter that can replace missing
   * values immediately (at training and testing time)
   */
  protected Filter m_missingValuesReplacer;

  /**
   * Replace missing values globally when running in batch mode?
   */
  protected boolean m_dontReplaceMissing = false;

  /** The distance function to use */
  protected NormalizableDistance m_distanceFunction = new EuclideanDistance();

  /**
   * Used to pad out number of cluster centers if fewer canopies are generated
   * than the number of requested clusters and we are running in batch mode.
   */
  protected Instances m_trainingData;

  /**
   * Returns a string describing this clusterer.
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Cluster data using the capopy clustering algorithm, which requires just "
      + "one pass over the data. Can run in either"
      + "batch or incremental mode. Results are generally not as good when "
      + "running incrementally as the min/max for each numeric attribute is not "
      + "known in advance. Has a heuristic (based on attribute std. deviations), "
      + "that can be used in batch mode, for setting the T2 distance. The T2 distance "
      + "determines how many canopies (clusters) are formed. When the user specifies "
      + "a specific number (N) of clusters to generate, the algorithm will return the "
      + "top N canopies (as determined by T2 density) when N < number of canopies "
      + "(this applies to both batch and incremental learning); "
      + "when N > number of canopies, the difference is made up by selecting training "
      + "instances randomly (this can only be done when batch training). For more "
      + "information see:\n\n" + getTechnicalInformation().toString();

  }

  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "A. McCallum and K. Nigam and L.H. Ungar");
    result
      .setValue(
        Field.TITLE,
        "Efficient Clustering of High Dimensional Data Sets with Application to Reference Matching");
    result.setValue(Field.BOOKTITLE,
      "Proceedings of the sixth ACM SIGKDD internation conference on "
        + "knowledge discovery and data mining "
        + "ACM-SIAM symposium on Discrete algorithms");
    result.setValue(Field.YEAR, "2000");
    result.setValue(Field.PAGES, "169-178");

    return result;
  }

  /**
   * Returns default capabilities of the clusterer.
   * 
   * @return the capabilities of this clusterer
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();
    result.enable(Capability.NO_CLASS);

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tNumber of clusters.\n" + "\t(default 2).",
      "N", 1, "-N <num>"));

    result
      .addElement(new Option(
        "\tThe T2 distance to use. Values < 0 indicate that\n\t"
          + "a heuristic based on attribute std. deviation should be used to set this.\n\t"
          + "Note that this heuristic can only be used when batch training\n\t"
          + "(default = -1.0)", "t2", 1, "-t2"));

    result.addElement(new Option(
      "\tThe T1 distance to use. A value < 0 is taken as a\n\t"
        + "positive multiplier for T2. (default = -1.5)", "t1", 1, "-t1"));

    result.addElement(new Option(
      "\tDon't replace missing values with mean/mode when "
        + "running in batch mode.\n", "M", 0, "-M"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -N &lt;num&gt;
   *  Number of clusters.
   *  (default 2).
   * </pre>
   * 
   * <pre>
   * -t2
   *  The T2 distance to use. Values &lt; 0 indicate that
   *  a heuristic based on attribute std. deviation should be used to set this.
   *  Note that this heuristic can only be used when batch training
   *  (default = -1.0)
   * </pre>
   * 
   * <pre>
   * -t1
   *  The T1 distance to use. A value &lt; 0 is taken as a
   *  positive multiplier for T2. (default = -1.5)
   * </pre>
   * 
   * <pre>
   * -M
   *  Don't replace missing values with mean/mode when running in batch mode.
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 1)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings throws Exception
   *          if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String temp = Utils.getOption('N', options);
    if (temp.length() > 0) {
      setNumClusters(Integer.parseInt(temp));
    }

    temp = Utils.getOption("t2", options);
    if (temp.length() > 0) {
      setT2(Double.parseDouble(temp));
    }

    temp = Utils.getOption("t1", options);
    if (temp.length() > 0) {
      setT1(Double.parseDouble(temp));
    }

    setDontReplaceMissingValues(Utils.getFlag('M', options));

    super.setOptions(options);
  }

  /**
   * Gets the current settings of Canopy.
   * 
   * @return an array of strings suitable for passing to setOptions()
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-N");
    result.add("" + getNumClusters());
    result.add("-t2");
    result.add("" + getT2());
    result.add("-t1");
    result.add("" + getT1());

    if (getDontReplaceMissingValues()) {
      result.add("-M");
    }

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Tests if two sets of canopies have a non-empty intersection
   * 
   * @param first the first canopy set
   * @param second the second canopy set
   * @return true if the intersection is non-empty
   * @throws Exception if a problem occurs
   */
  public static boolean nonEmptyCanopySetIntersection(long[] first,
    long[] second) throws Exception {
    if (first.length != second.length) {
      throw new Exception("Canopy lists need to be the same length");
    }

    if (first.length == 0 || second.length == 0) {
      return false;
    }

    for (int i = 0; i < first.length; i++) {
      long firstBlock = first[i];
      long secondBlock = second[i];

      if ((firstBlock & secondBlock) != 0L) {
        return true;
      }
    }

    return false;
  }

  private static void updateCanopyAssignment(long[] assigned, int toAssign) {
    int whichLong = toAssign / 64;
    int whichBitPosition = toAssign % 64;
    long mask = 1L << whichBitPosition;

    assigned[whichLong] |= mask;
  }

  /**
   * Uses T1 distance to assign canopies to the supplied instance. If the
   * instance does not fall within T1 distance of any canopies then the instance
   * has the closest canopy assigned to it.
   * 
   * @param inst the instance to find covering canopies for
   * @return a set of canopies that contain this instance according to T1
   *         distance
   * @throws Exception if a problem occurs
   */
  public long[] assignCanopies(Instance inst) throws Exception {
    if (m_missingValuesReplacer != null) {
      m_missingValuesReplacer.input(inst);
      inst = m_missingValuesReplacer.output();
    }

    int numLongs = m_canopies.size() / 64 + 1;
    long[] assigned = new long[numLongs];
    // for (int i = 0; i < numLongs; i++) {
    // assigned.add(new long[1]);
    // }

    double minDist = Double.MAX_VALUE;
    double bitsSet = 0;
    int index = -1;
    for (int i = 0; i < m_canopies.numInstances(); i++) {
      double dist = m_distanceFunction.distance(inst, m_canopies.instance(i));
      if (dist < minDist) {
        minDist = dist;
        index = i;
      }

      if (dist < m_t1) {
        updateCanopyAssignment(assigned, i);
        bitsSet++;
        // assigned.add(i);
      }
    }

    // this won't be necessary (for the training data) unless canopies have been
    // pruned due to the user requesting fewer canopies than are created
    // naturally according to the t2 radius
    if (bitsSet == 0) {
      // add the closest canopy
      updateCanopyAssignment(assigned, index);
    }

    return assigned;
  }

  protected void updateCanopyCenter(Instance newInstance, double[][] center,
    double[] numMissingNumerics) {
    for (int i = 0; i < newInstance.numAttributes(); i++) {
      if (newInstance.attribute(i).isNumeric()) {
        if (center[i].length == 0) {
          center[i] = new double[1];
        }

        if (!newInstance.isMissing(i)) {
          center[i][0] += newInstance.value(i);
        } else {
          numMissingNumerics[i]++;
        }
      } else if (newInstance.attribute(i).isNominal()) {
        if (center[i].length == 0) {
          // +1 for missing
          center[i] = new double[newInstance.attribute(i).numValues() + 1];
        }
        if (newInstance.isMissing(i)) {
          center[i][center[i].length - 1]++;
        } else {
          center[i][(int) newInstance.value(i)]++;
        }
      }
    }
  }

  @Override
  public void updateClusterer(Instance newInstance) throws Exception {

    if (m_missingValuesReplacer != null) {
      m_missingValuesReplacer.input(newInstance);
      newInstance = m_missingValuesReplacer.output();
    }

    m_distanceFunction.update(newInstance);
    boolean addPoint = true;

    for (int i = 0; i < m_canopies.numInstances(); i++) {
      if (m_distanceFunction.distance(newInstance, m_canopies.instance(i)) < m_t2) {
        double[] density = m_canopyT2Density.get(i);
        density[0]++;
        addPoint = false;

        double[][] center = m_canopyCenters.get(i);
        double[] numMissingNumerics = m_canopyNumMissingForNumerics.get(i);
        updateCanopyCenter(newInstance, center, numMissingNumerics);

        break;
      }
    }

    if (addPoint) {
      m_canopies.add(newInstance);
      double[] density = new double[1];
      density[0] = 1.0;
      m_canopyT2Density.add(density);

      double[][] center = new double[newInstance.numAttributes()][0];
      double[] numMissingNumerics = new double[newInstance.numAttributes()];
      updateCanopyCenter(newInstance, center, numMissingNumerics);
      m_canopyCenters.add(center);
      m_canopyNumMissingForNumerics.add(numMissingNumerics);
    }
  }

  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {
    double[] d = new double[numberOfClusters()];

    if (m_missingValuesReplacer != null) {
      m_missingValuesReplacer.input(instance);
      instance = m_missingValuesReplacer.output();
    }

    for (int i = 0; i < m_canopies.numInstances(); i++) {
      double distance = m_distanceFunction.distance(instance,
        m_canopies.instance(i));

      d[i] = 1.0 / (1.0 + distance);
    }

    Utils.normalize(d);
    return d;
  }

  private void assignCanopiesToCanopyCenters() {
    // assign canopies to each canopy center
    m_clusterCanopies = new ArrayList<long[]>();
    for (int i = 0; i < m_canopies.size(); i++) {
      Instance inst = m_canopies.instance(i);
      try {
        long[] assignments = assignCanopies(inst);
        m_clusterCanopies.add(assignments);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  @Override
  public void updateFinished() {
    if (m_canopies == null || m_canopies.numInstances() == 0) {
      return;
    }

    // set the final canopy centers and weights
    double[] densities = new double[m_canopies.size()];
    for (int i = 0; i < m_canopies.numInstances(); i++) {
      double[] density = m_canopyT2Density.get(i);
      double[][] centerSums = m_canopyCenters.get(i);
      double[] numMissingForNumerics = m_canopyNumMissingForNumerics.get(i);
      double[] finalCenter = new double[m_canopies.numAttributes()];
      for (int j = 0; j < m_canopies.numAttributes(); j++) {
        if (m_canopies.attribute(j).isNumeric()) {
          if (numMissingForNumerics[j] == density[0]) {
            finalCenter[j] = Utils.missingValue();
          } else {
            finalCenter[j] = centerSums[j][0]
              / (density[0] - numMissingForNumerics[j]);
          }
        } else if (m_canopies.attribute(j).isNominal()) {
          int mode = Utils.maxIndex(centerSums[j]);
          if (mode == centerSums[j].length - 1) {
            finalCenter[j] = Utils.missingValue();
          } else {
            finalCenter[j] = mode;
          }
        }
      }

      Instance finalCenterInst = m_canopies.instance(i) instanceof SparseInstance ? new SparseInstance(
        1.0, finalCenter) : new DenseInstance(1.0, finalCenter);
      m_canopies.set(i, finalCenterInst);

      m_canopies.instance(i).setWeight(density[0]);
      densities[i] = density[0];
    }

    if (m_numClustersRequested < 0) {
      assignCanopiesToCanopyCenters();

      m_trainingData = new Instances(m_canopies, 0);
      return;
    }

    // more canopies than requested?
    if (m_canopies.numInstances() > m_numClustersRequested) {
      int[] sortedIndexes = Utils.stableSort(densities);

      Instances finalCanopies = new Instances(m_canopies, 0);
      int count = 0;
      for (int i = sortedIndexes.length - 1; count < m_numClustersRequested; i--) {
        finalCanopies.add(m_canopies.instance(sortedIndexes[i]));
        count++;
      }

      m_canopies = finalCanopies;
    } else if (m_canopies.numInstances() < m_numClustersRequested
      && m_trainingData != null && m_trainingData.numInstances() > 0) {

      // make up the difference with randomly selected instances (if possible)
      Random r = new Random(getSeed());
      for (int i = 0; i < 10; i++) {
        r.nextInt();
      }
      HashMap<DecisionTableHashKey, Integer> initC = new HashMap<DecisionTableHashKey, Integer>();
      DecisionTableHashKey hk = null;

      // put the existing canopies in the lookup
      for (int i = 0; i < m_canopies.numInstances(); i++) {
        try {
          hk = new DecisionTableHashKey(m_canopies.instance(i),
            m_canopies.numAttributes(), true);

          initC.put(hk, null);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      for (int j = m_trainingData.numInstances() - 1; j >= 0; j--) {
        int instIndex = r.nextInt(j + 1);
        try {
          hk = new DecisionTableHashKey(m_trainingData.instance(instIndex),
            m_trainingData.numAttributes(), true);
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (!initC.containsKey(hk)) {
          m_canopies.add(m_trainingData.instance(instIndex));
          initC.put(hk, null);
        }
        m_trainingData.swap(j, instIndex);

        if (m_canopies.numInstances() == m_numClustersRequested) {
          break;
        }
      }
    }

    assignCanopiesToCanopyCenters();

    // save memory
    m_trainingData = new Instances(m_canopies, 0);
    m_canopyNumMissingForNumerics = null;
    m_canopyT2Density = null;
  }

  /**
   * Initialize the distance function (i.e set min/max values for numeric
   * attributes) with the supplied instances.
   * 
   * @param init the instances to initialize with
   * @throws Exception if a problem occurs
   */
  public void initializeDistanceFunction(Instances init) throws Exception {
    if (m_missingValuesReplacer != null) {
      init = Filter.useFilter(init, m_missingValuesReplacer);
    }

    m_distanceFunction.setInstances(init);
  }

  /**
   * Pretty hokey heuristic to try and set t2 distance automatically based on
   * standard deviation
   * 
   * @param trainingBatch the training instances
   * @throws Exception if a problem occurs
   */
  protected void setT2T1BasedOnStdDev(Instances trainingBatch) throws Exception {
    double normalizedStdDevSum = 0;

    for (int i = 0; i < trainingBatch.numAttributes(); i++) {
      if (trainingBatch.attribute(i).isNominal()) {
        normalizedStdDevSum += 0.25;
      } else if (trainingBatch.attribute(i).isNumeric()) {
        AttributeStats stats = trainingBatch.attributeStats(i);
        if (trainingBatch.numInstances() - stats.missingCount > 2) {
          double stdDev = stats.numericStats.stdDev;
          double min = stats.numericStats.min;
          double max = stats.numericStats.max;
          if (!Utils.isMissingValue(stdDev) && max - min > 0) {
            stdDev = 0.5 * stdDev / (max - min);
            normalizedStdDevSum += stdDev;
          }
        }
      }
    }

    normalizedStdDevSum = Math.sqrt(normalizedStdDevSum);
    if (normalizedStdDevSum > 0) {
      m_t2 = normalizedStdDevSum;
    }
  }

  @Override
  public void buildClusterer(Instances data) throws Exception {
    m_t1 = m_userT1;
    m_t2 = m_userT2;

    if (data.numInstances() == 0 && m_userT2 < 0) {
      System.err
        .println("The heuristic for setting T2 based on std. dev. can't be used when "
          + "running in incremental mode. Using default of 1.0.");
      m_t2 = 1.0;
    }

    m_canopyT2Density = new ArrayList<double[]>();
    m_canopyCenters = new ArrayList<double[][]>();
    m_canopyNumMissingForNumerics = new ArrayList<double[]>();

    if (data.numInstances() > 0) {
      if (!m_dontReplaceMissing) {
        m_missingValuesReplacer = new ReplaceMissingValues();
        m_missingValuesReplacer.setInputFormat(data);
        data = Filter.useFilter(data, m_missingValuesReplacer);
      }
      Random r = new Random(getSeed());
      for (int i = 0; i < 10; i++) {
        r.nextInt();
      }
      data.randomize(r);

      if (m_userT2 < 0) {
        setT2T1BasedOnStdDev(data);
      }
    }
    m_t1 = m_userT1 > 0 ? m_userT1 : -m_userT1 * m_t2;
    if (m_t1 < m_t2) {
      throw new Exception("T1 can't be less than T2. Computed T2 as " + m_t2
        + " T1 is requested to be " + m_t1);
    }

    m_distanceFunction.setInstances(data);

    m_canopies = new Instances(data, 0);
    if (data.numInstances() > 0) {
      m_trainingData = new Instances(data);
    }

    for (int i = 0; i < data.numInstances(); i++) {
      updateClusterer(data.instance(i));
    }

    updateFinished();
  }

  @Override
  public int numberOfClusters() throws Exception {
    return m_canopies.numInstances();
  }

  /**
   * Set a ready-to-use missing values replacement filter
   * 
   * @param missingReplacer the missing values replacement filter to use
   */
  public void setMissingValuesReplacer(Filter missingReplacer) {
    m_missingValuesReplacer = missingReplacer;
  }

  /**
   * Get the canopies (cluster centers).
   * 
   * @return the canopies
   */
  public Instances getCanopies() {
    return m_canopies;
  }

  /**
   * Set the canopies to use (replaces any learned by this clusterer already)
   * 
   * @param canopies the canopies to use
   */
  public void setCanopies(Instances canopies) {
    m_canopies = canopies;
  }

  /**
   * Get the canopies that each canopy (cluster center) is within T1 distance of
   * 
   * @return a list of canopies for each cluster center
   */
  public List<long[]> getClusterCanopyAssignments() {
    return m_clusterCanopies;
  }

  /**
   * Set the canopies that each canopy (cluster center) is within T1 distance of
   * 
   * @param clusterCanopies the list canopies for each cluster center
   */
  public void setClusterCanopyAssignments(List<long[]> clusterCanopies) {
    m_clusterCanopies = clusterCanopies;
  }

  /**
   * Get the actual value of T2 (which may be different from the initial value
   * if the heuristic is used)
   * 
   * @return the actual value of T2
   */
  public double getActualT2() {
    return m_t2;
  }

  /**
   * Get the actual value of T1 (which may be different from the initial value
   * if the heuristic is used)
   * 
   * @return the actual value of T1
   */
  public double getActualT1() {
    return m_t1;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String t1TipText() {
    return "The T1 distance to use. Values < 0 are taken as a positive "
      + "multiplier for the T2 distance";
  }

  /**
   * Set the T1 distance. Values < 0 are taken as a positive multiplier for the
   * T2 distance - e.g. T1_actual = Math.abs(t1) * t2;
   * 
   * @param t1 the T1 distance to use
   */
  public void setT1(double t1) {
    m_userT1 = t1;
  }

  /**
   * Get the T1 distance. Values < 0 are taken as a positive multiplier for the
   * T2 distance - e.g. T1_actual = Math.abs(t1) * t2;
   * 
   * @return the T1 distance to use
   */
  public double getT1() {
    return m_userT1;
  }

  /**
   * Tip text for this property
   * 
   * @return the tip text for this property
   */
  public String t2TipText() {
    return "The T2 distance to use. Values < 0 indicate that this should be set using "
      + "a heuristic based on attribute standard deviation (note that this only"
      + "works when batch training)";
  }

  /**
   * Set the T2 distance to use. Values < 0 indicate that a heuristic based on
   * attribute standard deviation should be used to set this (note that the
   * heuristic is only applicable when batch training).
   * 
   * @param t2 the T2 distance to use
   */
  public void setT2(double t2) {
    m_userT2 = t2;
  }

  /**
   * Get the T2 distance to use. Values < 0 indicate that a heuristic based on
   * attribute standard deviation should be used to set this (note that the
   * heuristic is only applicable when batch training).
   * 
   * @return the T2 distance to use
   */
  public double getT2() {
    return m_userT2;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numClustersTipText() {
    return "Set number of clusters. -1 means number of clusters is determined by "
      + "T2 distance";
  }

  @Override
  public void setNumClusters(int numClusters) throws Exception {
    m_numClustersRequested = numClusters;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String dontReplaceMissingValuesTipText() {
    return "Replace missing values globally with mean/mode.";
  }

  /**
   * Sets whether missing values are to be replaced.
   * 
   * @param r true if missing values are to be replaced
   */
  public void setDontReplaceMissingValues(boolean r) {
    m_dontReplaceMissing = r;
  }

  /**
   * Gets whether missing values are to be replaced.
   * 
   * @return true if missing values are to be replaced
   */
  public boolean getDontReplaceMissingValues() {
    return m_dontReplaceMissing;
  }

  /**
   * Get the number of clusters to generate
   * 
   * @return the number of clusters to generate
   */
  public int getNumClusters() {
    return m_numClustersRequested;
  }

  public static String printSingleAssignment(long[] assignments) {
    StringBuilder temp = new StringBuilder();

    boolean first = true;
    temp.append(" <");
    for (int j = 0; j < assignments.length; j++) {
      long block = assignments[j];
      int offset = j * 64;

      for (int k = 0; k < 64; k++) {
        long mask = 1L << k;

        if ((mask & block) != 0L) {
          temp.append("" + (!first ? "," : "") + (offset + k));
          if (first) {
            first = false;
          }
        }
      }
    }

    temp.append(">");
    return temp.toString();
  }

  /**
   * Print the supplied instances and their canopies
   * 
   * @param dataPoints the instances to print
   * @param canopyAssignments the canopy assignments, one assignment array for
   *          each instance
   * @return a string containing the printed assignments
   */
  public static String printCanopyAssignments(Instances dataPoints,
    List<long[]> canopyAssignments) {
    StringBuilder temp = new StringBuilder();

    for (int i = 0; i < dataPoints.size(); i++) {
      temp.append("Cluster " + i + ": ");
      temp.append(dataPoints.instance(i));
      if (canopyAssignments != null
        && canopyAssignments.size() == dataPoints.size()) {
        long[] assignments = canopyAssignments.get(i);
        temp.append(printSingleAssignment(assignments));
      }
      temp.append("\n");
    }

    return temp.toString();
  }

  /**
   * Return a textual description of this clusterer
   * 
   * @param header true if the header should be printed
   * @return a string describing the result of the clustering
   */
  public String toString(boolean header) {
    StringBuffer temp = new StringBuffer();

    if (m_canopies == null) {
      return "No clusterer built yet";
    }

    if (header) {
      temp.append("\nCanopy clustering\n=================\n");
      temp.append("\nNumber of canopies (cluster centers) found: "
        + m_canopies.numInstances());
    }

    temp.append("\nT2 radius: " + String.format("%-10.3f", m_t2));
    temp.append("\nT1 radius: " + String.format("%-10.3f", m_t1));
    temp.append("\n\n");

    temp.append(printCanopyAssignments(m_canopies, m_clusterCanopies));

    temp.append("\n");

    return temp.toString();
  }

  @Override
  public String toString() {
    return toString(true);
  }

  public static void main(String[] args) {
    runClusterer(new Canopy(), args);
  }
}
