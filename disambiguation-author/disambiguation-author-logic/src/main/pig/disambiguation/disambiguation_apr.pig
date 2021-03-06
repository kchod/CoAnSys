--
-- This file is part of CoAnSys project.
-- Copyright (c) 2012-2015 ICM-UW
--
-- CoAnSys is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.

-- CoAnSys is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with CoAnSys. If not, see <http://www.gnu.org/licenses/>.
--
-- -----------------------------------------------------
-- -----------------------------------------------------
-- default section
-- -----------------------------------------------------
-- -----------------------------------------------------
%DEFAULT and_inputDocsData workflows/pl.edu.icm.coansys-disambiguation-author-workflow/results/splitted/apr-no-sim
%DEFAULT and_time ''
%DEFAULT and_outputContribs disambiguation/outputContribs$and_time
%DEFAULT and_failedContribs disambiguation/failedContribs$and_time
%DEFAULT and_feature_info 'IntersectionPerMaxval#EX_DOC_AUTHS_SNAMES#1.0#1'
%DEFAULT and_threshold '-0.8'
%DEFAULT and_aproximate_remember_sim 'false'
%DEFAULT and_use_extractor_id_instead_name 'true'
%DEFAULT and_statistics 'false'
%DEFAULT and_exhaustive_limit 6627

DEFINE exhaustiveAND pl.edu.icm.coansys.disambiguation.author.pig.ExhaustiveAND('$and_threshold','$and_feature_info','$and_use_extractor_id_instead_name','$and_statistics');
DEFINE aproximateAND pl.edu.icm.coansys.disambiguation.author.pig.AproximateAND_BFS('$and_threshold', '$and_feature_info','$and_aproximate_remember_sim','$and_use_extractor_id_instead_name','$and_statistics');
DEFINE GenUUID pl.edu.icm.coansys.disambiguation.author.pig.GenUUID();

-- -----------------------------------------------------
-- -----------------------------------------------------
-- set section
-- -----------------------------------------------------
-- -----------------------------------------------------
%DEFAULT and_sample 1.0
%DEFAULT and_parallel_param 16
%DEFAULT pig_tmpfilecompression_param true
%DEFAULT pig_tmpfilecompression_codec_param gz
%DEFAULT job_priority normal
%DEFAULT pig_cachedbag_mem_usage 0.1
%DEFAULT pig_skewedjoin_reduce_memusage 0.3
%DEFAULT mapredChildJavaOpts -Xmx8000m

set default_parallel $and_parallel_param
set pig.tmpfilecompression $pig_tmpfilecompression_param
set pig.tmpfilecompression.codec $pig_tmpfilecompression_codec_param
set job.priority $job_priority
set pig.cachedbag.memusage $pig_cachedbag_mem_usage
set pig.skewedjoin.reduce.memusage $pig_skewedjoin_reduce_memusage
set mapred.child.java.opts $mapredChildJavaOpts
-- ulimit must be more than two times the heap size value !
-- set mapred.child.ulimit unlimited
set dfs.client.socket-timeout 60000
%DEFAULT and_scheduler default
SET mapred.fairscheduler.pool $and_scheduler
-- -----------------------------------------------------
-- -----------------------------------------------------
-- code section
-- -----------------------------------------------------
-- -----------------------------------------------------
D0 = LOAD '$and_inputDocsData' as (sname:int, datagroup:{(cId:chararray, sname:int, data:map[{(int)}])}, count:long);
D1 = foreach D0 generate *, COUNT(datagroup) as cnt;                  
D2 = filter D1 by (cnt>0);
D = foreach D2 generate sname, datagroup, count;
-- -----------------------------------------------------
-- BIG GRUPS OF CONTRIBUTORS 
-- -----------------------------------------------------

-- D1000A: {datagroup: NULL,simTriples: NULL}
E1 = foreach D generate flatten( aproximateAND( datagroup ) ) as (datagroup:{ ( cId:chararray, sname:int, data:map[{(int)}] ) }, simTriples:{});
E2 = foreach E1 generate datagroup, simTriples, COUNT( datagroup ) as count;

split E2 into
	ESINGLE if count <= 2,
	EEXH if ( count > 2 and count <= $and_exhaustive_limit ),
	EBIG if count > $and_exhaustive_limit;

-- -----------------------------------------------------
-- TOO BIG CLUSTERS FOR EXHAUSTIVE
-- -----------------------------------------------------
-- TODO maybe MagicAND for such big clusters in future
-- then storing data below and add new node in workflow after aproximates:
-- store EBIG into '$and_failedContribs';
--
-- For now: each contributor from too big cluster is going to get his own UUID
-- so we need to "ungroup by sname".

I = foreach EBIG generate flatten(datagroup);
BIG = foreach I generate cId as cId, GenUUID( TOBAG(cId) ) as uuid;

-- -----------------------------------------------------
-- CLUSTERS WITH ONE CONTRIBUTOR
-- -----------------------------------------------------

F = foreach ESINGLE generate datagroup.cId as cIds, GenUUID( datagroup.cId ) as uuid;
SINGLE = foreach F generate flatten( cIds ) as cId, uuid as uuid;

-- -----------------------------------------------------
-- CLUSTERS FOR EXHAUSTIVE
-- -----------------------------------------------------

G1 = foreach EEXH generate flatten( exhaustiveAND( datagroup, simTriples ) ) as (uuid:chararray, cIds:{(chararray)});
G2 = foreach G1 generate *, COUNT(cIds) as cnt;
G3 = filter G2 by (uuid is not null and cnt>0);
-- H: {cId: chararray,uuid: chararray}
H = foreach G3 generate flatten( cIds ) as cId, uuid;

-- -----------------------------------------------------
-- STORING RESULTS
-- -----------------------------------------------------

R = union SINGLE, BIG, H;
store R into '$and_outputContribs';
