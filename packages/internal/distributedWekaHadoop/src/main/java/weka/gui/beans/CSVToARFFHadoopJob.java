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
 *    CSVToARFFHadoopJob
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.util.ArrayList;
import java.util.List;

import weka.core.Instances;
import weka.distributed.hadoop.ArffHeaderHadoopJob;

/**
 * Knowledge Flow step for the CSVToARFFHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 10076 $
 */
@KFStep(category = "Hadoop", toolTipText = "Makes a unified ARFF header for a data set")
public class CSVToARFFHadoopJob extends AbstractHadoopJob {

  /** For serialization */
  private static final long serialVersionUID = -8029477841981163952L;

  /** Downstream listeners for data set output */
  protected List<DataSourceListener> m_dsListeners = new ArrayList<DataSourceListener>();

  /**
   * Constructor
   */
  public CSVToARFFHadoopJob() {
    super();

    m_job = new ArffHeaderHadoopJob();
    m_visual.setText("CSVToARFFHeaderHadoopJob");
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "ARFFHeaderHadoopJob.gif",
      BeanVisual.ICON_PATH + "ARFFHeaderHadoopJob.gif");
  }

  /**
   * Help info for this KF step
   * 
   * @return help info
   */
  public String globalInfo() {
    return "Creates a unified ARFF header for a data set by "
      + "determining column types (if not supplied by "
      + "user) and all nominal values";
  }

  @Override
  protected void notifyJobOutputListeners() {

    Instances finalHeader = ((ArffHeaderHadoopJob) m_runningJob)
      .getFinalHeader();
    if (finalHeader != null) {
      DataSetEvent de = new DataSetEvent(this, finalHeader);
      for (DataSourceListener d : m_dsListeners) {
        d.acceptDataSet(de);
      }
    }
  }

  /**
   * Add a data source listener
   * 
   * @param dsl a data source listener
   */
  public synchronized void addDataSourceListener(DataSourceListener dsl) {
    m_dsListeners.add(dsl);
  }

  /**
   * Remove a data source listener
   * 
   * @param dsl a data source listener
   */
  public synchronized void removeDataSourceListener(DataSourceListener dsl) {
    m_dsListeners.remove(dsl);
  }
}
