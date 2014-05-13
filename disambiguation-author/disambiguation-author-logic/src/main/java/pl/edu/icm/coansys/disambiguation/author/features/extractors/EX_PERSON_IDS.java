/*
 * This file is part of CoAnSys project.
 * Copyright (c) 2012-2013 ICM-UW
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
package pl.edu.icm.coansys.disambiguation.author.features.extractors;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.pig.data.DataBag;
import org.apache.pig.data.DefaultDataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import pl.edu.icm.coansys.disambiguation.author.features.extractors.indicators.DisambiguationExtractorAuthor;
import pl.edu.icm.coansys.disambiguation.author.normalizers.PigNormalizer;
import pl.edu.icm.coansys.models.DocumentProtos.Author;
import pl.edu.icm.coansys.models.DocumentProtos.DocumentMetadata;
import pl.edu.icm.coansys.models.DocumentProtos.KeyValue;

public class EX_PERSON_IDS extends DisambiguationExtractorAuthor {

	// put here id names which should not be extracted to disambiguation
	// e.g. "pbnPersonId"
	public static final String SKIPPED_PERSON_ID_KIND[] = {

	};
	private HashSet<String> skip_id_set = new HashSet<String>(
			Arrays.asList(SKIPPED_PERSON_ID_KIND));

	public EX_PERSON_IDS() {
		super();
	}

	public EX_PERSON_IDS(PigNormalizer[] new_normalizers) {
		super(new_normalizers);
	}

	@Override
	public DataBag extract(Object o, int fakeIndex, String lang) {
		TupleFactory tf = TupleFactory.getInstance();
		DocumentMetadata dm = (DocumentMetadata) o;
		DataBag db = new DefaultDataBag();
		Tuple t = tf.newTuple();

		Author a = dm.getBasicMetadata().getAuthor(fakeIndex);
		for (KeyValue kv : a.getExtIdList()) {
			String id_name = kv.getKey();
			if (!skip_id_set.contains(id_name)) {
				String id_value = kv.getValue();
				Object normalized = normalizeExtracted(id_name + "|" + id_value);
				t.append(normalized);
				db.add(t);
			}
		}
		return db;
	}

	@Override
	public String getId() {
		return "E";
	}
}