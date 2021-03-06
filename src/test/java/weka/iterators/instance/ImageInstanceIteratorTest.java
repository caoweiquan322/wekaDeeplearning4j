/*
 * WekaDeeplearning4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WekaDeeplearning4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WekaDeeplearning4j.  If not, see <https://www.gnu.org/licenses/>.
 *
 * ImageInstanceIteratorTest.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.iterators.instance;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.datavec.image.recordreader.ImageRecordReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.InvalidInputDataException;
import weka.dl4j.iterators.instance.ImageInstanceIterator;
import weka.util.DatasetLoader;

/**
 * JUnit tests for the ImageInstanceIterator {@link ImageInstanceIterator}
 *
 * @author Steven Lang
 */
public class ImageInstanceIteratorTest {

  /**
   * Seed
   */
  private static final int SEED = 42;
  /**
   * ImageInstanceIterator object
   */
  private ImageInstanceIterator idi;

  /**
   * Initialize iterator
   */
  @Before
  public void init() {
    this.idi = new ImageInstanceIterator();
    this.idi.setImagesLocation(new File("datasets/nominal/mnist-minimal"));
    this.idi.setNumChannels(1);
    this.idi.setTrainBatchSize(1);
    this.idi.setWidth(28);
    this.idi.setHeight(28);
  }

  /**
   * Test validate method with valid data
   *
   * @throws Exception Could not load mnist meta data
   */
  @Test
  public void testValidateValidData() throws Exception {
    // Test valid setup
    final Instances metaData = DatasetLoader.loadMiniMnistMeta();
    this.idi.validate(metaData);
  }

  /**
   * Test validate method with invalid data
   *
   * @throws Exception Could not load mnist meta data
   */
  @Test(expected = InvalidInputDataException.class)
  public void testValidateInvalidLocation() throws Exception {
    final Instances metaData = DatasetLoader.loadMiniMnistMeta();
    final String invalidPath = "foo/bar/baz";
    this.idi.setImagesLocation(new File(invalidPath));
    this.idi.validate(metaData);
  }

  /**
   * Test validate method with invalid data
   *
   * @throws Exception Could not load mnist meta data
   */
  @Test(expected = InvalidInputDataException.class)
  public void testValidateInvalidInstances() throws Exception {
    ArrayList<Attribute> invalidAttributes = new ArrayList<>();
    final Attribute f = new Attribute("file");
    invalidAttributes.add(f);
    final Instances metaData = new Instances("invalidMetaData", invalidAttributes, 0);
    this.idi.setImagesLocation(new File("datasets/nominal/mnist-minimal"));
    this.idi.validate(metaData);
  }

  /**
   * Test
   */
  @Test
  public void testGetImageRecordReader() throws Exception {
    final Instances metaData = DatasetLoader.loadMiniMnistMeta();
    Method method =
        ImageInstanceIterator.class.getDeclaredMethod("getImageRecordReader", Instances.class);
    method.setAccessible(true);
    this.idi.setTrainBatchSize(1);
    final ImageRecordReader irr = (ImageRecordReader) method.invoke(this.idi, metaData);

    Set<String> labels = new HashSet<>();
    for (Instance inst : metaData) {
      String label = inst.stringValue(1);
      String itLabel = irr.next().get(1).toString();
      Assert.assertEquals(label, itLabel);
      labels.add(label);
    }
    Assert.assertEquals(10, labels.size());
    Assert.assertTrue(labels.containsAll(irr.getLabels()));
    Assert.assertTrue(irr.getLabels().containsAll(labels));
  }

  /**
   * Test getDataSetIterator
   */
  @Test
  public void testGetIterator() throws Exception {
    final Instances metaData = DatasetLoader.loadMiniMnistMeta();
    this.idi.setImagesLocation(new File("datasets/nominal/mnist-minimal"));
    final int batchSize = 1;
    final DataSetIterator it = this.idi.getDataSetIterator(metaData, SEED, batchSize);

    Set<Integer> labels = new HashSet<>();
    for (Instance inst : metaData) {
      int label = Integer.parseInt(inst.stringValue(1));
      final DataSet next = it.next();
      int itLabel = next.getLabels().argMax().getInt(0);
      Assert.assertEquals(label, itLabel);
      labels.add(label);
    }
    final List<Integer> collect =
        it.getLabels().stream().map(Integer::valueOf).collect(Collectors.toList());
    Assert.assertEquals(10, labels.size());
    Assert.assertTrue(labels.containsAll(collect));
    Assert.assertTrue(collect.containsAll(labels));
  }

  /**
   * Test image instance iterator mnist.
   *
   * @throws Exception IO error.
   */
  @Test
  public void testImageInstanceIteratorMnist() throws Exception {

    // Data
    Instances data = DatasetLoader.loadMiniMnistMeta();
    data.setClassIndex(data.numAttributes() - 1);
    ImageInstanceIterator imgIter = DatasetLoader.loadMiniMnistImageIterator();

    final int seed = 1;
    for (int batchSize : new int[]{1, 2, 5, 10}) {
      final int actual = countIterations(data, imgIter, seed, batchSize);
      final int expected = data.numInstances() / batchSize;
      Assert.assertEquals(expected, actual);
    }
  }

  /**
   * Counts the number of iterations an {@see ImageInstanceIterator}
   *
   * @param data Instances to iterate
   * @param imgIter ImageInstanceIterator to be tested
   * @param seed Seed
   * @param batchsize Size of the batch which is returned in {@see DataSetIterator#next}
   * @return Number of iterations
   */
  private int countIterations(
      Instances data, ImageInstanceIterator imgIter, int seed, int batchsize) throws Exception {
    DataSetIterator it = imgIter.getDataSetIterator(data, seed, batchsize);
    int count = 0;
    while (it.hasNext()) {
      count++;
      DataSet dataset = it.next();
    }
    return count;
  }
}
