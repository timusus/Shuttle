package com.simplecity.amp_library.utils;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.simplecity.amp_library.model.Song;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This is a separate from {@link ShuttleUtilsPowerMockTest} for the time being as PowerMock and Robolectric
 * can't work together until Robolectric 3.3 is released:
 * https://github.com/robolectric/robolectric/wiki/Using-PowerMock
 * <p>
 * Use the devDebug build variant to run.
 */
@Config(sdk = 23)
@RunWith(RobolectricTestRunner.class)
public class ShuttleUtilsTest {
    @Test
    public void testOpenShuttleLinkNullInput() throws Exception {
        Activity mockActivity = mock(Activity.class);
        String fakePackage = "fake.package.name";

        ShuttleUtils.openShuttleLink(null, null);
        verify(mockActivity, never()).startActivity(any(Intent.class));

        ShuttleUtils.openShuttleLink(mockActivity, null);
        verify(mockActivity, never()).startActivity(any(Intent.class));

        ShuttleUtils.openShuttleLink(null, fakePackage);
        verify(mockActivity, never()).startActivity(any(Intent.class));
    }

    @Test
    public void testOpenShuttleLinkMarketLink() throws Exception {
        Activity mockActivity = mock(Activity.class);
        String fakePackage = "fake.package.name";

        // Call the method and capture the "fired" intent
        ShuttleUtils.openShuttleLink(mockActivity, fakePackage);
        ArgumentCaptor<Intent> intentCaptor = new ArgumentCaptor<>();
        verify(mockActivity).startActivity(intentCaptor.capture());
        Intent intent = intentCaptor.getValue();

        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);

        // Todo:
        // Is this the correct approach, or should this be run under two different tests?
        // Also, I guess ShuttleUtils.isAmazonBuild() needs to be tested or we can't trust this test
        if (ShuttleUtils.isAmazonBuild()) {
            assertThat(intent.getData()).isEqualTo(Uri.parse("amzn://apps/android?p=" + fakePackage));
        } else {
            assertThat(intent.getData()).isEqualTo(Uri.parse("market://details?id=" + fakePackage));
        }
    }

    @Test
    public void testOpenShuttleLinkWebLink() throws Exception {
        Activity mockActivity = mock(Activity.class);
        String fakePackage = "fake.package.name";

        // Setup the mock to throw an ActivityNotFoundException only the first time startActivity is called
        doThrow(new ActivityNotFoundException()).doNothing().when(mockActivity).startActivity(any(Intent.class));

        // Call the method and capture the "fired" intent
        ShuttleUtils.openShuttleLink(mockActivity, fakePackage);
        ArgumentCaptor<Intent> intentCaptor = new ArgumentCaptor<>();
        verify(mockActivity, times(2)).startActivity(intentCaptor.capture());
        Intent intent = intentCaptor.getValue();

        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);

        // Todo:
        // Is this the correct approach, or should this be run under two different tests?
        // Also, I guess ShuttleUtils.isAmazonBuild() needs to be tested or we can't trust this test
        if (ShuttleUtils.isAmazonBuild()) {
            assertThat(intent.getData()).isEqualTo(Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=" + fakePackage));
        } else {
            assertThat(intent.getData()).isEqualTo(Uri.parse("https://play.google.com/store/apps/details?id=" + fakePackage));
        }
    }

    @Test
    public void testIncrementPlayCount() throws Exception {
        Context mockContext = mock(Context.class);
        ContentResolver mockContentResolver = mock(ContentResolver.class);
        Song fakeSong = new Song(mock(Cursor.class));
        fakeSong.id = 100L;
        fakeSong.playCount = 50;

        // getPlayCount has extra logic to conduct SQL Queries, which would be a pain to mock in a test
        // so, we can use a Spy here in order to use both fake data (above) plus control return behaviors (below)
        Song spySong = spy(fakeSong);
        doReturn(50).when(spySong).getPlayCount(any(Context.class));

        when(mockContext.getContentResolver()).thenReturn(mockContentResolver);

        // Setup to perform the method call on a song with no play counts
        when(mockContentResolver.update(any(Uri.class), any(ContentValues.class), anyString(), any(String[].class))).thenReturn(0);

        // Call the method and capture the ContentValues object
        ArgumentCaptor<ContentValues> contentValuesCaptor = new ArgumentCaptor<>();
        ShuttleUtils.incrementPlayCount(mockContext, spySong);
        verify(mockContentResolver).update(any(Uri.class), contentValuesCaptor.capture(), anyString(), any(String[].class));

        // Check that the values being updated or inserted match the actual song
        ContentValues values = contentValuesCaptor.getValue();
        assertThat(values.get("_id")).isEqualTo(100L);
        assertThat(values.get("play_count")).isEqualTo(51);
        verify(mockContentResolver).insert(any(Uri.class), eq(values));

        // Next, test what happens with a song that already had play counts (should not cause an insert operation)
        reset(mockContentResolver);
        when(mockContentResolver.update(any(Uri.class), contentValuesCaptor.capture(), anyString(), any(String[].class))).thenReturn(1);
        ShuttleUtils.incrementPlayCount(mockContext, spySong);
        verify(mockContentResolver, never()).insert(any(Uri.class), any(ContentValues.class));
    }

}
