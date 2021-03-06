/*
 * This file is part of CoAnSys project.
 * Copyright (c) 2012-2015 ICM-UW
 * 
 * CoAnSys is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * CoAnSys is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with CoAnSys. If not, see <http://www.gnu.org/licenses/>.
 */

package pl.edu.icm.coansys.classification.documents.pig.proceeders;

import java.io.IOException;
import java.util.Arrays;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.edu.icm.coansys.commons.java.StackTraceExtractor;

/**
 *
 * @author pdendek
 */
public class POS_NEG extends EvalFunc<Tuple> {

    private static final Logger logger = LoggerFactory.getLogger(POS_NEG.class);

    @Override
    public Schema outputSchema(Schema p_input) {
        try {
            return Schema.generateNestedSchema(DataType.TUPLE,
                    DataType.CHARARRAY, DataType.CHARARRAY, DataType.INTEGER, DataType.INTEGER);
        } catch (FrontendException e) {
            logger.error("Error in creating output schema:", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Tuple exec(Tuple input) throws IOException {
        if (input == null || input.size() == 0) {
            return null;
        }
        try {
            String keyA = (String) input.get(0);
            String keyB = (String) input.get(1);
            DataBag categsA = (DataBag) input.get(2);
            DataBag categsB = (DataBag) input.get(3);
            String categQ = (String) input.get(4);

            if (keyA == null || keyB == null || categsA == null || categsB == null || categQ == null) {
                return null;
            }

            boolean inA = false;
            boolean inB = false;

            for (Tuple t : categsA) {
                if (categQ.equals((String) (t.get(0)))) {
                    inA = true;
                    break;
                }
            }

            for (Tuple t : categsB) {
                if (categQ.equals((String) (t.get(0)))) {
                    inB = true;
                    break;
                }
            }

            if (!inB) {
                Object[] to = new Object[]{keyA, categQ, 0, 0};
                return TupleFactory.getInstance().newTuple(Arrays.asList(to));
            } else {
                Object[] to = new Object[]{keyA, categQ, inA ? 1 : 0, (!inA) ? 1 : 0};
                return TupleFactory.getInstance().newTuple(Arrays.asList(to));
            }

        } catch (Exception e) {
            logger.error("Error in processing input row:", e);
            throw new IOException("Caught exception processing input row:\t"
                    + StackTraceExtractor.getStackTrace(e).replace("\n", "\t"));
        }
    }
}
