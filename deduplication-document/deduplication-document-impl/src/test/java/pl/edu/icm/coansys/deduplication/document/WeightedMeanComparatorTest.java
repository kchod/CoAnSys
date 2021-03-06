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

package pl.edu.icm.coansys.deduplication.document;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import pl.edu.icm.coansys.deduplication.document.comparator.WeightedMeanComparator;
import pl.edu.icm.coansys.deduplication.document.tool.MockDocumentMetadataFactory;
import pl.edu.icm.coansys.deduplication.document.voter.AuthorsVoter;
import pl.edu.icm.coansys.deduplication.document.voter.SimilarityVoter;
import pl.edu.icm.coansys.deduplication.document.voter.TitleVoter;
import pl.edu.icm.coansys.deduplication.document.voter.Vote;
import pl.edu.icm.coansys.deduplication.document.voter.YearVoter;
import pl.edu.icm.coansys.models.DocumentProtos;

public class WeightedMeanComparatorTest {

    
    private WeightedMeanComparator duplicateWorkComparator;
    private TitleVoter workTitleVoter;
    private AuthorsVoter workAuthorVoter;
    private YearVoter workYearVoter;
    
    private DocumentProtos.DocumentMetadata doc1 = MockDocumentMetadataFactory.createDocumentMetadata("a");
    private DocumentProtos.DocumentMetadata doc2 = MockDocumentMetadataFactory.createDocumentMetadata("b");
    
    
    @BeforeMethod
    public void setUp() throws Exception {
        List<SimilarityVoter> voters = new ArrayList<SimilarityVoter>();
        workTitleVoter = mock(TitleVoter.class);
        workAuthorVoter = mock(AuthorsVoter.class);
        workYearVoter = mock(YearVoter.class);
        voters.add(workTitleVoter);
        voters.add(workAuthorVoter);
        voters.add(workYearVoter);
        
        duplicateWorkComparator = new WeightedMeanComparator();
        duplicateWorkComparator.setSimilarityVoters(voters);
    }

    
    @Test
    public void testSameTitleSameAuthorsSameYear() {
        when(workTitleVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.PROBABILITY, 1.0f));
        when(workTitleVoter.getWeight()).thenReturn(1.0f);

        when(workAuthorVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.PROBABILITY, 0.9f));
        when(workAuthorVoter.getWeight()).thenReturn(1.0f);
        
        when(workYearVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.PROBABILITY, 1.0f));
        when(workYearVoter.getWeight()).thenReturn(0.5f);
        
        Assert.assertTrue(duplicateWorkComparator.isDuplicate(doc1, doc2, null));
        
        verify(workTitleVoter, times(1)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
        verify(workAuthorVoter, times(1)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
        verify(workYearVoter, times(1)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
    }
    
    
    @Test
    public void testSameTitleSameAuthorsDifferentYear() {
        when(workTitleVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.PROBABILITY, 1.0f));
        when(workTitleVoter.getWeight()).thenReturn(1.0f);

        when(workAuthorVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.PROBABILITY, 1.0f));
        when(workAuthorVoter.getWeight()).thenReturn(1.0f);
        
        when(workYearVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.NOT_EQUALS));
        when(workYearVoter.getWeight()).thenReturn(0.5f);
        
        Assert.assertFalse(duplicateWorkComparator.isDuplicate(doc1, doc2, null));
        
        verify(workTitleVoter, times(1)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
        verify(workAuthorVoter, times(1)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
        verify(workYearVoter, times(1)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
    }
    
    @Test
    public void testSameTitleDifferentAuthorsSameYear() {
        when(workTitleVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.PROBABILITY, 1.0f));
        when(workTitleVoter.getWeight()).thenReturn(1.0f);

        when(workAuthorVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.NOT_EQUALS));
        when(workAuthorVoter.getWeight()).thenReturn(1.0f);
        
        when(workYearVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.PROBABILITY, 1.0f));
        when(workYearVoter.getWeight()).thenReturn(0.5f);
        
        Assert.assertFalse(duplicateWorkComparator.isDuplicate(doc1, doc2, null));
        
        verify(workTitleVoter, times(1)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
        verify(workAuthorVoter, times(1)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
        verify(workYearVoter, times(0)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
    }
    
    
    @Test
    public void testDifferentTitleSameAuthorsSameYear() {

        when(workTitleVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.NOT_EQUALS));
        when(workTitleVoter.getWeight()).thenReturn(1.0f);

        when(workAuthorVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.PROBABILITY, 1.0f));
        when(workAuthorVoter.getWeight()).thenReturn(1.0f);
        
        when(workYearVoter.vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class)))
                .thenReturn(new Vote(Vote.VoteStatus.PROBABILITY, 1.0f));
        when(workYearVoter.getWeight()).thenReturn(0.5f);
        
        Assert.assertFalse(duplicateWorkComparator.isDuplicate(doc1, doc2, null));
        
        verify(workTitleVoter, times(1)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
        verify(workAuthorVoter, times(0)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
        verify(workYearVoter, times(0)).vote(any(DocumentProtos.DocumentMetadata.class), any(DocumentProtos.DocumentMetadata.class));
    }
}