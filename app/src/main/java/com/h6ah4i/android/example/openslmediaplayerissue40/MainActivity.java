package com.h6ah4i.android.example.openslmediaplayerissue40;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContentResolverCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.h6ah4i.android.media.IBasicMediaPlayer;
import com.h6ah4i.android.media.opensl.OpenSLMediaPlayer;
import com.h6ah4i.android.media.opensl.OpenSLMediaPlayerContext;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private customPlayer customPlayer;
    private ArrayList<Song> actual_listSong = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOnClickButton();
            }
        });

        this.customPlayer = new customPlayer(this);

        Cursor c = ContentResolverCompat.query(
                getContentResolver(),
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.AudioColumns._ID,
                        MediaStore.Audio.AudioColumns.TITLE,
                }, null, null, null, null);

        if (c.moveToFirst()) {
            final int limit = 100;
            do {
                long id = c.getLong(c.getColumnIndex(MediaStore.Audio.AudioColumns._ID));
                String title = c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
                actual_listSong.add(new Song(id, title));
            } while (c.moveToNext() && c.getPosition() < limit);
        }
    }

    private void handleOnClickButton() {
        Song song = actual_listSong.remove(0);
        if (song != null) {
            this.customPlayer.playSong(this, song);
        }
    }

    static class Settings {
        public static final int OPTION_USE_FADE = OpenSLMediaPlayer.OPTION_USE_FADE;
    }

    static class Song {
        final long id;
        final String title;

        public Song(long id, String title) {
            this.id = id;
            this.title = title;
        }

        public long getID() {
            return this.id;
        }

        public String getTitle() {
            return this.title;
        }
    }

    static class customPlayer {
        private static final String LOG_TAG = "customPlayer";

        private static final boolean USE_PREPARE_ASYNC = true;

        public OpenSLMediaPlayer player;
        private OpenSLMediaPlayerContext openSLMediaPlayerContext;
        public Song nextSong;
        private Song actual_playing;

        public customPlayer(Context context) {
            OpenSLMediaPlayerContext.Parameters parameters = new OpenSLMediaPlayerContext.Parameters();
            parameters.streamType = AudioManager.STREAM_MUSIC;
            openSLMediaPlayerContext = new OpenSLMediaPlayerContext(context, parameters);
            player = new OpenSLMediaPlayer(openSLMediaPlayerContext, Settings.OPTION_USE_FADE);
            player.setOnPreparedListener(new OnPreparedListener());                                     //I add one single time the prepared listener

            nextSong = null;
        }

        class OnPreparedListener implements IBasicMediaPlayer.OnPreparedListener {
            @Override
            public void onPrepared(IBasicMediaPlayer mp) {
                mp.start();
            }
        }

        private void playSong(final Context context, final Song song) {
//            if (player == null) {   //create the player if it is null
//                player = new customPlayer(context);
//            } else {
            player.reset(); //reset the player anyway. If it is playing, or has finished to play, setdatasource wan't crash without the reset of mediaplayer
            //player.setOnPreparedListener(new OnPreparedListener()); //I tried here to reset the player,but didn't work
//            }
            Uri contentUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getID());
            if (contentUri == null) {
                Log.i(LOG_TAG, "Error finding the media Id : " + song.getTitle() + " - " + song.getID());
                return;
            }

            setNextSong();  //update the next song

            try {
                player.setDataSource(context, contentUri);
                if (USE_PREPARE_ASYNC) {
                    player.prepareAsync();
                } else {
                    player.prepare();
                    player.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, e.getMessage() + " (" + actual_playing.getTitle() + ")", Toast.LENGTH_LONG).show();
            }
        }

        private void setNextSong() {

        }
    }
}
