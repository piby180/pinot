/**
 * Copyright (C) 2014-2015 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.core.operator.aggregation.function;

import com.google.common.base.Preconditions;
import com.linkedin.pinot.core.operator.groupby.ResultHolder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.List;


/**
 * Class to implement the 'distinctcount' aggregation function.
 */
public class DistinctCountAggregationFunction implements AggregationFunction {
  private static final String FUNCTION_NAME = AggregationFunctionFactory.DISTINCTCOUNT_AGGREGATION_FUNCTION;
  private static final ResultDataType RESULT_DATA_TYPE = ResultDataType.DISTINCTCOUNT_SET;

  /**
   * Performs 'distinctcount' aggregation on the input array.
   *
   * {@inheritDoc}
   *
   * @param valueArray
   * @return
   */
  @Override
  public void aggregate(int length, ResultHolder resultHolders, double[]... valueArray) {
    throw new RuntimeException("Unsupported method aggregate(double[] values) for class " + getClass().getName());
  }

  /**
   * {@inheritDoc}
   *
   * While the interface allows for variable number of valueArrays, we do not support
   * multiple columns within one aggregation function right now.
   *
   * @param length
   * @param groupKeys
   * @param resultHolder
   * @param valueArray
   */
  @Override
  public void aggregateGroupBySV(int length, int[] groupKeys, ResultHolder resultHolder, double[]... valueArray) {
    Preconditions.checkArgument(valueArray.length == 1);
    Preconditions.checkState(length <= valueArray[0].length);

    for (int i = 0; i < length; i++) {
      int groupKey = groupKeys[i];
      IntOpenHashSet valueSet = (IntOpenHashSet) resultHolder.getResult(groupKey);
      if (valueSet == null) {
        valueSet = new IntOpenHashSet();
        resultHolder.setValueForKey(groupKey, valueSet);
      }
      valueSet.add((int) valueArray[0][i]);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param length
   * @param docIdToGroupKeys
   * @param resultHolder
   * @param valueArray
   */
  @Override
  public void aggregateGroupByMV(int length, int[][] docIdToGroupKeys, ResultHolder resultHolder,
      double[]... valueArray) {
    Preconditions.checkArgument(valueArray.length == 1);
    Preconditions.checkState(length <= valueArray[0].length);

    for (int i = 0; i < length; i++) {
      int value = (int) valueArray[0][i];
      for (int groupKey : docIdToGroupKeys[i]) {
        IntOpenHashSet valueSet = (IntOpenHashSet) resultHolder.getResult(groupKey);
        if (valueSet == null) {
          valueSet = new IntOpenHashSet();
          resultHolder.setValueForKey(groupKey, valueSet);
        }
        valueSet.add(value);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @Override
  public double getDefaultValue() {
    throw new RuntimeException("Unsupported method getDefaultValue() for class " + getClass().getName());
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public ResultDataType getResultDataType() {
    return RESULT_DATA_TYPE;
  }

  @Override
  public String getName() {
    return FUNCTION_NAME;
  }

  /**
   * {@inheritDoc}
   *
   * @param combinedResult
   * @return
   */
  @Override
  public Double reduce(List<Object> combinedResult) {
    throw new RuntimeException(
        "Unsupported method reduce(List<Object> combinedResult for class " + getClass().getName());
  }
}
