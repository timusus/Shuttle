package com.simplecity.amp_library.utils;


import android.database.Cursor;

import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.search.SearchUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * This is a separate from {@link ShuttleUtilsPowerMockTest} for the time being as PowerMock and Robolectric
 * can't work together until Robolectric 3.3 is released:
 * https://github.com/robolectric/robolectric/wiki/Using-PowerMock
 * <p>
 * Use the devDebug build variant to run.
 */
@Config(sdk = 23, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)

public class SearchUtilsUnitTest {

    @Test
    public void testJaroWinklerObject() throws Exception {

        //These are kind of vague tests, just ensuring that various JaroWinkler search distance
        //queries fall roughly in the expected range..

        //Todo:
        //I'm not really sure if this is the correct use of unit tests. This particular set is less about
        //testing whether the code is sound and working as expected, and more about testing whether the logic
        //behind that code gives us the results we want..

        double SCORE_THRESHOLD = 0.70;

        //We expect this to do well, as 'words' is one of the words in the song name. (Score should be 1.0)
        Song song = new Song(mock(Cursor.class));
        song.name = "The lots of words song";
        SearchUtils.JaroWinklerObject jaroWinklerObject = new SearchUtils.JaroWinklerObject<>(song, "words", song.name);
        assertThat(jaroWinklerObject.score).isGreaterThan(SCORE_THRESHOLD);

        //This was a user-submitted example of a previously failing search - before adjusting the Jaro-Winkler
        //to split the song/album/artist name at whitespace, and return the best score for each substring.
        song = new Song(mock(Cursor.class));
        song.artistName = "Aby Wolf";
        jaroWinklerObject = new SearchUtils.JaroWinklerObject<>(song, "aby wolf", song.artistName);
        assertThat(jaroWinklerObject.score).isGreaterThan(SCORE_THRESHOLD);

        //'Baby worm' isn't similar enough to 'aby wolf' to pass our threshold.
        song = new Song(mock(Cursor.class));
        song.artistName = "Bad Worms";
        jaroWinklerObject = new SearchUtils.JaroWinklerObject<>(song, "aby wolf", song.artistName);
        assertThat(jaroWinklerObject.score).isLessThan(SCORE_THRESHOLD);

    }
}