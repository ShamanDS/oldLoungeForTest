package com.studiovision.loungefm;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;



/**
* Service that handles media playback. This is the Service through which we perform all the media
* handling in our application. Upon initialization, it starts a {@link MusicRetriever} to scan
* the user's media. Then, it waits for Intents (which come from our main activity,
* {@link MainActivity}, which signal the service to perform specific operations: Play, Pause,
* Rewind, Skip, etc.
*/
public class MusicService extends Service implements OnCompletionListener,
		OnPreparedListener, OnErrorListener, OnBufferingUpdateListener, MusicFocusable {
 
	private final Binder mBinder = new MusicServiceBinder();
	
	private LMediaPlayerServiceClient mClient;
	
	   /**
     * A class for clients binding to this service. The client will be passed an object of this class
     * via its onServiceConnected(ComponentName, IBinder) callback.
     */
    public class MusicServiceBinder extends Binder {
        /**
         * Returns the instance of this service for a client to make method calls on it.
         * @return the instance of this service.
         */
        public MusicService getService() {
            return MusicService.this;
        }
 
    }
	
	static String AUDIO_URL_128 = "http://cast.loungefm.com.ua/loungefm";
    static String AUDIO_URL_32 = "http://cast.loungefm.com.ua/loungefm2.ogg";
    
    
	
	// These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
	
	public static final String ACTION_TOGGLE_PLAYBACK = "com.studiovision.loungefm.action.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY = "com.studiovision.loungefm.action.PLAY";
	public static final String ACTION_PAUSE = "com.studiovision.loungefm.action.PAUSE";
	public static final String ACTION_STOP = "com.studiovision.loungefm.action.STOP";
	public static final String ACTION_CHANGE_BITRATE = "com.studiovision.loungefm.action.CHANGE_BITRATE";
	
	// The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
	public static final float DUCK_VOLUME = 0.1f;
	
	// our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null.
	AudioFocusHelper mAudioFocusHelper = null;
	
	//our media player
	MediaPlayer mPlayer = null;
	
	// indicates the state our service:
	enum State {
		Stopped,  	// media player is stopped and not prepared to play
		Preparing,	// media player is preparing...
		Playing, 	// playback active (media player ready!). (but the media player may actually be
					// paused in this state if we don't have audio focus. But we stay in this state
					// so that we know we have to resume playback once we get focus back)
		Paused		//playback paused (media player ready)
	}
	
	State mState = State.Stopped;
	
	// do we have audio focus?
	enum AudioFocus {
		NoFocusNoDuck,	// we don't have audio focus, and can't duck
		NoFocusCanDack,	// we don't have focus, but can play at a low volume ("ducking")
		Focused			// we have full audio focus
	}
	
	AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
	
    //stream quality
	enum Quality {
 	   lowQuality,
 	   highQuality
 	   
 	}
    
    Quality mQuality = Quality.highQuality;
	 
	// title of the song we are currently playing
	String mSongTitle = "";
	
	// Wifi lock that we hold when streaming files from the internet, in order to prevent the
    // device from shutting off the Wifi radio
	WifiLock mWifiLock;
	
	// The ID we use for the notification (the onscreen alert that appears at the notification
    // area at the top of the screen as an icon -- and as text as well if the user expands the
    // notification area).
    final int NOTIFICATION_ID = 1;
	
	// our RemoteControlClient object, which will use remote control APIs available in
    // SDK level >= 14, if they're available.
    RemoteControlClientCompat mRemoteControlClientCompat;
	
	// The component name of MusicIntentReceiver, for use with media button and remote control
    // APIs
	ComponentName mMediaButtonReceiverComponent;
	
	AudioManager mAudioManager;
	NotificationManager mNotificationManager;
	
	Notification mNotification = null;
	
	/**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
	void createMediaPlayerIfNeeded() {
		if (mPlayer == null) {
			mClient.onInitializePlayerStart();
			mPlayer = new MediaPlayer();
			
			// Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
			mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			
			// we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnCompletionListener(this);
			mPlayer.setOnErrorListener(this);
			mPlayer.setOnBufferingUpdateListener(this);
		}
		else 
			mPlayer.reset();
	}
	
	@Override
	public void onCreate() {
		Log.i(Constants.TAG, "debug: Creating service");
		
		//create the wifi lock (this does not acquire the lock, this just creates it)
		mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "myLock");
		
		mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
		
		//create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
		if (android.os.Build.VERSION.SDK_INT >= 8) 
			mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
		else 
			mAudioFocus = AudioFocus.Focused;
		
		mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);
		
		super.onCreate();
	}
	
	/**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (action.equals(ACTION_TOGGLE_PLAYBACK)) 
			processTogglePlaybackRequest();
		else if (action.equals(ACTION_PLAY)) 
			processPlayRequest();
		else if (action.equals(ACTION_PAUSE)) 
			processPauseRequest();
		else if (action.equals(ACTION_STOP)) 
			processStopRequest();
		else if (action.equals(ACTION_CHANGE_BITRATE))
		{
			int quality = intent.getIntExtra("Quality", 1);
			mQuality = (quality == 1) ? Quality.highQuality : Quality.lowQuality;
			processChangeBitrate();
		}
			
		return START_NOT_STICKY; // Means we started the service, but don't want it to								 								 // restart in case it's killed.
	}
	
	
    /**
     * Sets the client using this service.
     * @param client The client of this service, which implements the IMediaPlayerServiceClient interface
     */
    public void setClient(LMediaPlayerServiceClient client) {
        this.mClient = client;
    }
	
	void processTogglePlaybackRequest() {
		if (mState == State.Paused || mState == State.Stopped) {
			processPlayRequest();
		} 
		else {
			processPauseRequest();			
		}		
	}
	
	void processPlayRequest() {
		
		tryToGetAudioFocus();
		
		if (mState == State.Stopped || mState == State.Preparing) {
            // If we're stopped, just go ahead to the next song and start playing
            playSong();
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mSongTitle + " (playing)");
            configAndStartMediaPlayer();
        }
		
		// Tell any remote controls that our playback state is 'playing'.
		if (mRemoteControlClientCompat != null) {
			mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			
		}
	
	}
	
	void processPauseRequest() {

        if (mState == State.Playing || mState == State.Preparing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mPlayer.pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
            mClient.onPlayerPause();
        }

        // Tell any remote controls that our playback state is 'paused'.
        if (mRemoteControlClientCompat != null) {
            mRemoteControlClientCompat
                    .setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }
	
	void processStopRequest() {
        processStopRequest(false);
    }
	
	void processStopRequest(boolean force) {
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // Tell any remote controls that our playback state is 'paused'.
            if (mRemoteControlClientCompat != null) {
                mRemoteControlClientCompat
                        .setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }
	
	void processChangeBitrate(){
		if (mState == State.Playing || mState == State.Preparing) 
		{
			processStopRequest(true);
			mState = State.Preparing;
			processPlayRequest();
		}
		else 
		{
			processStopRequest(false);
			
		}
	}
	
	/**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }
    
    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }
    
    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mPlayer.isPlaying()) mPlayer.pause();
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDack)
            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        else
            mPlayer.setVolume(1.0f, 1.0f); // we can be loud

        if (!mPlayer.isPlaying()) mPlayer.start();
    }
    
    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                        && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }
    
    /**
     * Starts playing the song.
     */
    void playSong() {
    	mClient.onPlayerPreparing();
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer

        try {
        	
        	// set the source of the media player to a manual URL or path
            createMediaPlayerIfNeeded();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            
            if (mQuality == Quality.highQuality) 
            {
            	mPlayer.setDataSource(AUDIO_URL_128);
			}
            else 
            {
            	mPlayer.setDataSource(AUDIO_URL_32);
			}
            
            

           // mSongTitle = playingItem.getTitle();

            mState = State.Preparing;
            setUpAsForeground(mSongTitle + " (loading)");

            // Use the media button APIs (if available) to register ourselves for media button
            // events

            MediaButtonHelper.registerMediaButtonEventReceiverCompat(
                    mAudioManager, mMediaButtonReceiverComponent);

            // Use the remote control APIs (if available) to set the playback state

            if (mRemoteControlClientCompat == null) {
                Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                intent.setComponent(mMediaButtonReceiverComponent);
                mRemoteControlClientCompat = new RemoteControlClientCompat(
                        PendingIntent.getBroadcast(this /*context*/,
                                0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));
                RemoteControlHelper.registerRemoteControlClient(mAudioManager,
                        mRemoteControlClientCompat);
            }

            mRemoteControlClientCompat.setPlaybackState(
                    RemoteControlClient.PLAYSTATE_PLAYING);

            mRemoteControlClientCompat.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                    RemoteControlClient.FLAG_KEY_MEDIA_STOP);

           /*
            // Update the remote controls
            mRemoteControlClientCompat.editMetadata(true)
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, playingItem.getArtist())
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, playingItem.getAlbum())
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, playingItem.getTitle())
                    .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                            playingItem.getDuration())
                    // TODO: fetch real item artwork
                    .putBitmap(
                            RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                            mDummyAlbumArt)
                    .apply();
                    */

            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mPlayer.prepareAsync();

            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
            // we are *not* streaming, we want to release the lock if we were holding it before.
            mWifiLock.acquire();
        }
        catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /** Called when media player is done playing current song. */
    public void onCompletion(MediaPlayer player) {
        // The media player finished playing the current song, so we go ahead and start the next.
    }
    
    
    /** Called when media player is done preparing. */
    @Override
    public void onPrepared(MediaPlayer player) {
        // The media player is done preparing. That means we can start playing!
        mClient.onInitializePlayerSuccess();
    	mState = State.Playing;
        updateNotification(mSongTitle + " (playing)");
        configAndStartMediaPlayer();
        
    }
    
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    	
    	
    }
    
    /** Updates the notification. */
    void updateNotification(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification.setLatestEventInfo(getApplicationContext(), "RandomMusicPlayer", text, pi);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }
    
    
    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification = new Notification();
        mNotification.tickerText = text;
       // mNotification.icon = R.drawable.ic_stat_playing;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
       
        mNotification.setLatestEventInfo(getApplicationContext(), "Lounge FM",
                text, pi);
        
        startForeground(NOTIFICATION_ID, mNotification);
    }

    /**
     * Called when there's an error playing media. When this happens, the media player goes to
     * the Error state. We warn the user about the error and reset the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mClient.onMediaPlayerError();
    	/*Toast.makeText(getApplicationContext(), "Media player error! Resetting.",
            Toast.LENGTH_SHORT).show();*/
        Log.e(Constants.TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }

    public void onGainedAudioFocus() {
       // Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
        mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    public void onLostAudioFocus(boolean canDuck) {
        /*
    	Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
            "no duck"), Toast.LENGTH_SHORT).show();
            */
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDack : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (mPlayer != null && mPlayer.isPlaying())
            configAndStartMediaPlayer();
    }



    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

}
