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

package pl.edu.icm.coansys.disambiguation.author.normalizers;

public class ToLowerCase implements PigNormalizer {

	@Override
	public String normalize(String text) {

		if (text == null) {
			return null;
		}
		String tmp;

		if (text instanceof String) {
			tmp = (String) text;
		} else {
			tmp = text.toString();
		}

		if (tmp.isEmpty()) {
			return null;
		}

		tmp = tmp.toLowerCase();

		if (tmp.isEmpty()) {
			return null;
		}

		return tmp;
	}
}
