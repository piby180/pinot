/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pinot.queries;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.pinot.core.common.Operator;
import org.apache.pinot.core.operator.query.AggregationGroupByOperator;
import org.apache.pinot.core.operator.query.AggregationOperator;
import org.apache.pinot.core.operator.query.SelectionOnlyOperator;
import org.apache.pinot.core.query.aggregation.groupby.AggregationGroupByResult;
import org.apache.pinot.core.query.aggregation.groupby.GroupKeyGenerator;
import org.apache.pinot.segment.local.indexsegment.immutable.ImmutableSegmentLoader;
import org.apache.pinot.segment.local.segment.creator.impl.SegmentIndexCreationDriverImpl;
import org.apache.pinot.segment.local.segment.readers.GenericRowRecordReader;
import org.apache.pinot.segment.spi.ImmutableSegment;
import org.apache.pinot.segment.spi.IndexSegment;
import org.apache.pinot.segment.spi.creator.SegmentGeneratorConfig;
import org.apache.pinot.spi.config.table.TableConfig;
import org.apache.pinot.spi.config.table.TableType;
import org.apache.pinot.spi.data.FieldSpec;
import org.apache.pinot.spi.data.Schema;
import org.apache.pinot.spi.data.readers.GenericRow;
import org.apache.pinot.spi.utils.ReadMode;
import org.apache.pinot.spi.utils.builder.TableConfigBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


public class CastQueriesTest extends BaseQueriesTest {

  private static final File INDEX_DIR = new File(FileUtils.getTempDirectory(), "CastQueriesTest");
  private static final String RAW_TABLE_NAME = "testTable";
  private static final String SEGMENT_NAME = "testSegment";

  private static final int NUM_RECORDS = 1000;
  private static final int BUCKET_SIZE = 8;
  private static final String CLASSIFICATION_COLUMN = "class";
  private static final String X_COL = "x";
  private static final String Y_COL = "y";

  private static final Schema SCHEMA = new Schema.SchemaBuilder()
      .addSingleValueDimension(X_COL, FieldSpec.DataType.DOUBLE)
      .addSingleValueDimension(Y_COL, FieldSpec.DataType.DOUBLE)
      .addSingleValueDimension(CLASSIFICATION_COLUMN, FieldSpec.DataType.STRING)
      .build();

  private static final TableConfig TABLE_CONFIG =
      new TableConfigBuilder(TableType.OFFLINE).setTableName(RAW_TABLE_NAME).build();

  private IndexSegment _indexSegment;
  private List<IndexSegment> _indexSegments;

  @Override
  protected String getFilter() {
    return "";
  }

  @Override
  protected IndexSegment getIndexSegment() {
    return _indexSegment;
  }

  @Override
  protected List<IndexSegment> getIndexSegments() {
    return _indexSegments;
  }

  @BeforeClass
  public void setUp()
      throws Exception {
    FileUtils.deleteQuietly(INDEX_DIR);

    List<GenericRow> records = new ArrayList<>(NUM_RECORDS);
    for (int i = 0; i < NUM_RECORDS; i++) {
      GenericRow record = new GenericRow();
      record.putValue(X_COL, 0.5);
      record.putValue(Y_COL, 0.25);
      record.putValue(CLASSIFICATION_COLUMN, "" + (i % BUCKET_SIZE));
      records.add(record);
    }

    SegmentGeneratorConfig segmentGeneratorConfig = new SegmentGeneratorConfig(TABLE_CONFIG, SCHEMA);
    segmentGeneratorConfig.setTableName(RAW_TABLE_NAME);
    segmentGeneratorConfig.setSegmentName(SEGMENT_NAME);
    segmentGeneratorConfig.setOutDir(INDEX_DIR.getPath());

    SegmentIndexCreationDriverImpl driver = new SegmentIndexCreationDriverImpl();
    driver.init(segmentGeneratorConfig, new GenericRowRecordReader(records));
    driver.build();

    ImmutableSegment immutableSegment = ImmutableSegmentLoader.load(new File(INDEX_DIR, SEGMENT_NAME), ReadMode.mmap);
    _indexSegment = immutableSegment;
    _indexSegments = Arrays.asList(immutableSegment, immutableSegment);
  }

  @Test
  public void testCastSum() {
    String query = "select cast(sum(" + X_COL + ") as int), "
        + "cast(sum(" + Y_COL + ") as int) "
        + "from " + RAW_TABLE_NAME;
    Operator<?> operator = getOperatorForSqlQuery(query);
    assertTrue(operator instanceof AggregationOperator);
    List<Object> aggregationResult = ((AggregationOperator) operator).nextBlock().getAggregationResult();
    assertNotNull(aggregationResult);
    assertEquals(aggregationResult.size(), 2);
    assertEquals(((Number) aggregationResult.get(0)).intValue(), NUM_RECORDS / 2);
    assertEquals(((Number) aggregationResult.get(1)).intValue(), NUM_RECORDS / 4);
  }

  @Test
  public void testCastSumGroupBy() {
    String query = "select cast(sum(" + X_COL + ") as int), "
        + "cast(sum(" + Y_COL + ") as int) "
        + "from " + RAW_TABLE_NAME + " "
        + "group by " + CLASSIFICATION_COLUMN;
    Operator<?> operator = getOperatorForSqlQuery(query);
    assertTrue(operator instanceof AggregationGroupByOperator);
    AggregationGroupByResult result = ((AggregationGroupByOperator) operator).nextBlock().getAggregationGroupByResult();
    assertNotNull(result);
    Iterator<GroupKeyGenerator.GroupKey> it = result.getGroupKeyIterator();
    while (it.hasNext()) {
      GroupKeyGenerator.GroupKey groupKey = it.next();
      Object aggregate = result.getResultForGroupId(0, groupKey._groupId);
      assertEquals(((Number) aggregate).intValue(), NUM_RECORDS / (2 * BUCKET_SIZE));
      aggregate = result.getResultForGroupId(1, groupKey._groupId);
      assertEquals(((Number) aggregate).intValue(), NUM_RECORDS / (4 * BUCKET_SIZE));
    }
  }

  @Test
  public void testCastFilterAndProject() {
    String query = "select cast(" + CLASSIFICATION_COLUMN + " as int)"
        + " from " + RAW_TABLE_NAME
        + " where " + CLASSIFICATION_COLUMN + " = cast(0 as string) limit " + NUM_RECORDS;
    Operator<?> operator = getOperatorForSqlQuery(query);
    assertTrue(operator instanceof SelectionOnlyOperator);
    Collection<Object[]> result = ((SelectionOnlyOperator) operator).nextBlock().getSelectionResult();
    assertNotNull(result);
    assertEquals(result.size(), NUM_RECORDS / BUCKET_SIZE);
    for (Object[] row : result) {
      assertEquals(row.length, 1);
      assertEquals(row[0], 0);
    }
  }
}