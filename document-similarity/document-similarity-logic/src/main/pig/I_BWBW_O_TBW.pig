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

REGISTER 'lib/*.jar'

%default input 'input'
%default output 'output'

A = load '$input' 
	using pl.edu.icm.coansys.commons.pig.udf.RichSequenceFileLoader
	('org.apache.hadoop.io.BytesWritable',
	'org.apache.hadoop.io.BytesWritable') 
	as (k:bytearray,v:bytearray);

B = foreach A generate 
	FLATTEN(pl.edu.icm.coansys.commons.pig.udf.ByteArrayToText(k))
	as (k:chararray),v;
C = filter B by
	(
	k is not null 
	and k!=''
	and v is not null 
	and v!=''
	);

store C into '$output' using pl.edu.icm.coansys.commons.pig.udf.RichSequenceFileLoader('org.apache.hadoop.io.Text','org.apache.hadoop.io.BytesWritable');

