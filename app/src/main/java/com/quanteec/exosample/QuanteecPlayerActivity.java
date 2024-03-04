package com.quanteec.exosample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.example.quanteecexoplugin.QuanteecBaseDataSource;
import com.example.quanteecexoplugin.adapters.exoplayer.QuanteecBandwidthMeters;
import com.example.quanteecexoplugin.adapters.exoplayer.QuanteecExoCore;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.quanteec.plugin.manifest.DASH.parser.C;
import com.quanteec.plugin.settings.QuanteecConfig;

public class QuanteecPlayerActivity extends AppCompatActivity {

    protected ExoPlayer player;
    protected StyledPlayerView playerView;
    private TrackSelectionParameters trackSelectionParameters;
    private int startItemIndex;
    private long startPosition;
    Uri simpleUri = Uri.parse("https://vz-7c6a4993-54a.b-cdn.net/007973c8-7f42-4bb1-aa4d-3170ebe2fb85/playlist.m3u8");

    private QuanteecConfig quanteecConfig = null;
    private QuanteecExoCore quanteecExoCore = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quanteec_player);
        playerView = findViewById(R.id.player_view);

        trackSelectionParameters = new TrackSelectionParameters.Builder(/* context= */ this).build();
    }


    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT <= 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
            releaseQuanteec();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT > 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
            releaseQuanteec();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            // Empty results are triggered if a permission is requested while another request was already
            // pending and can be safely ignored in this case.
            return;
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer();
        } else {
            showToast("Permissions denied");
            finish();
        }
    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * @return Whether initialization was successful.
     */
    protected boolean initializePlayer() {

        if(quanteecExoCore == null)
        {
            // 1 - Create a quanteecConfig with a videoID and a quanteecKey.
            //Useful if the videosource url contains a token which is different for every users. If an empty String is used : the source url is used.

            quanteecConfig = new QuanteecConfig.Builder("<your-quanteec-key>").setVideoID("Quanteec Exoplayer Sample Application").build();
            // 2 - Create a quanteecCore by passing a Context and the QuanteecConfig
            quanteecExoCore = new QuanteecExoCore(/* context= */this, quanteecConfig);
        }

        if (player == null) {

            // 3 - Create a Factory for the QuanteecBaseDataSource
            QuanteecBaseDataSource.Factory quanteecDataSourceFatory = new QuanteecBaseDataSource.Factory(quanteecExoCore);

            ExoPlayer.Builder playerBuilder =
                    new ExoPlayer.Builder(/* context= */ this);

            player = playerBuilder.setMediaSourceFactory(
                    // 4 - Register the quanteecDataSourceFactory in Exoplayer
                    new DefaultMediaSourceFactory(this).setDataSourceFactory(
                            quanteecDataSourceFatory
                    )
            // 5 - Register a QuanteecBandwithMeters in Exoplayer
            ).setBandwidthMeter(
                    new QuanteecBandwidthMeters.Builder(this).build()
            ).build();

            player.setTrackSelectionParameters(trackSelectionParameters);
            player.setPlayWhenReady(true);
            //6 - Register the ExoPlayer instance inside the quanteecPlugin
            quanteecExoCore.setPlayer(player);
            playerView.setPlayer(player);
        }


        boolean haveStartPosition = startItemIndex != C.INDEX_UNSET;
        if (haveStartPosition) {
            player.seekTo(startItemIndex, startPosition);
        }
        
        MediaItem mediaItems = new MediaItem.Builder().setUri(simpleUri).build();
        player.setMediaItem(mediaItems, /* resetPosition= */ !haveStartPosition);
        player.prepare();
        return true;
    }

    protected void releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters();
            updateStartPosition();
            player.release();
            player = null;
            playerView.setPlayer(/* player= */ null);
        }
    }

    protected void releaseQuanteec()
    {
        if(this.quanteecExoCore!=null)
        {
            this.quanteecExoCore.release();
            this.quanteecExoCore = null;
        }
    }

    private void updateTrackSelectorParameters() {
        if (player != null) {
            trackSelectionParameters = player.getTrackSelectionParameters();
        }
    }

    private void updateStartPosition() {
        if (player != null) {
            startItemIndex = player.getCurrentMediaItemIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }
}
