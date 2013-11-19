package com.studiovision.loungefm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.analytics.tracking.android.EasyTracker;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.LayoutParams;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.perm.kate.api.Api;
import com.studiovision.loungefm.MusicService.MusicServiceBinder;
import com.studiovision.loungefm.MusicService.State;




public class MainActivity extends FragmentActivity implements OnClickListener, LMediaPlayerServiceClient{
	PagerAdapter pagerAdapter;
	private ViewPager mViewPager;
	MusicService mService;
	private boolean mBound = false;
	private boolean isFirst = true;
	//vk api
	public final int REQUEST_LOGIN = 1;
	VkAccount account = new VkAccount();
	Api api;
	
	//facebook
	
	private static final String FACEBOOK_APPID = "417758631649860";
	private static final String FACEBOOK_PERMISSION = "publish_stream";
	private static final String TAG = "Lounge Fm";
	private static final String MSG = "Рекомендую скачать приложение Lounge Fm для Android: http://loungefm.com.ua";
	
	private final Handler mFacebookHandler = new Handler();
	private FacebookConnector facebookConnector;
	
    final Runnable mUpdateFacebookNotification = new Runnable() {
        public void run() {
        	Toast toast = new Toast(getApplicationContext());
			ImageView imageView = new ImageView(getApplicationContext());
			imageView.setImageResource(R.drawable.checkmark);
			imageView.setBackgroundColor(Color.BLACK);
			toast.setGravity ( Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0 , 0 );
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setView(imageView);
			toast.show();
        }
    };
	
    ImageView circle;
	Button mPlayPauseButton;
	Button mShareButton;
	ImageButton mOnAirButton;
	Button m128kbButton;
	Button m32kbButton;
	Button btnChange;
	
	//share group
	Button mVkontakte;
	Button mFacebook;
	Button mEmail;
	Button mSMS;
	Button mCancelButton;
	
	//on air group
	TextView firstSongTextView;
	TextView firstSongTime;
	TextView secondSongTextView;
	TextView secondSongTime;
	TextView thirdSongTextView;
	TextView thirdSongTime;
	Button mOnAirBackButton;
	
	//progress on play button
	ImageView mProgressImageView;
	
	ImageView mainLogo;
	int page;

	
	//track title
	TextView currentTrackTitle;
	
	//tracks containers
	ArrayList<ArrayList<String>> tracksInfoList = null;
	
	boolean isPlaying = false;
	
	//volume control
	private SeekBar volumeSeekBar = null;
	private AudioManager audioManager = null;
	
	public void SetBackdroundRes(int resId)
	{
		m128kbButton.setBackgroundResource(resId);
		m32kbButton.setBackgroundResource(resId);
		mShareButton.setBackgroundResource(resId);
		mOnAirButton.setBackgroundResource(resId);
	}
	
	public void SetVolumeBg(int resId)
	{
		
		int curProgress = volumeSeekBar.getProgress();
		Rect bounds = volumeSeekBar.getProgressDrawable().getBounds();
        Drawable progressDrawable = getResources().getDrawable(resId);

        volumeSeekBar.setProgress(0);
		volumeSeekBar.setProgressDrawable(progressDrawable);
		volumeSeekBar.getProgressDrawable().setBounds(bounds);

        volumeSeekBar.setProgress(curProgress);
		//volumeSeekBar = s;
		updateUI();
	}
	
	@Override
	  public void onStart() {
	    super.onStart();
	    EasyTracker.getInstance(this).activityStart(this); // Add this method.
	  }
	
	@Override
	  public void onStop() {
	    super.onStop();
	    EasyTracker.getInstance(this).activityStop(this); // Add this method.
	  }
	
    /**
     * Called when the activity is first created. Here, we simply set the event listeners and
     * start the background service ({@link MusicService}) that will handle the actual media
     * playback.
     */	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	
		
		//restore saved vk session
		account.restore(this);
		
		if(account.access_token!=null)
            api=new Api(account.access_token, Constants.API_ID);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	//	hideActionBar();
		
		mPlayPauseButton = (Button) findViewById(R.id.playPauseButton);
		mShareButton = (Button) findViewById(R.id.shareButton);
		mOnAirButton = (ImageButton) findViewById(R.id.onAirButton);
		m128kbButton = (Button)findViewById(R.id.button_128kb);
		m32kbButton = (Button)findViewById(R.id.button_32kb);
		mProgressImageView = (ImageView)findViewById(R.id.progressImageView);
		mProgressImageView.setVisibility(View.GONE);
		btnChange = (Button) findViewById(R.id.btnChangeRadio);
		mainLogo  = (ImageView) findViewById(R.id.logoImageView);
		
		mPlayPauseButton.setOnClickListener(this);
		mShareButton.setOnClickListener(this);
		mOnAirButton.setOnClickListener(this);
		m128kbButton.setOnClickListener(this);
		m128kbButton.setSelected(true);
		m32kbButton.setOnClickListener(this);
		m32kbButton.setSelected(false);
		btnChange.setOnClickListener(this);
		currentTrackTitle = (TextView)findViewById(R.id.track_title);
		currentTrackTitle.setSelected(true);
		
		
		initVolumeControl();
		//facebook		
		this.facebookConnector = new FacebookConnector(FACEBOOK_APPID, this, getApplicationContext(), new String[] {FACEBOOK_PERMISSION});
		
		bindToService();
		
		mViewPager = (ViewPager) findViewById(R.id.pager);
		  TitleAdapter titleAdapter = new TitleAdapter(getSupportFragmentManager());
		  try
			{
		       
	        mViewPager.setAdapter(titleAdapter);

		}
		catch(Exception ex)
		{
			Log.e("mylog",ex.getLocalizedMessage());
		}  
	        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

	        public void onPageSelected(int pageNumber) {
	          // Just define a callback method in your fragment and call it like this! 
	        	page = pageNumber;
	        	final Handler handler = new Handler();
	          	 
	        	if (!isFirst)
	        	{
	        		
	    	        	isPlaying = false; 
	    		        mService.mState = State.Stopped;
	    		        hideAnimation();
	    		        updateUI();
	    		      
	    			    if (mService != null)
	    				{
	    			    	mService.onDestroy();
	    				
	    			    	String stream;
	    					switch	(page)
	    					{
	    					case (0):
	    						stream = "http://cast.loungefm.com.ua/lfm_chillout.ogg";
	    					SetBackdroundRes(R.drawable.small_button_chill);
	    					SetVolumeBg(R.drawable.styled_progress_chill);
	    					currentTrackTitle.setText("Lounge FM Chill-out");	
	    					mainLogo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo_small));
	    					updateTrackList();
	    					handler.postDelayed(new Runnable() {
	    			            @Override
	    			            public void run() {
	    			            	mViewPager.setCurrentItem(4,false);

	    			            }
	    			        }, 250);

	    	            	break;
	    					case (1):
	    						stream = "http://cast.loungefm.com.ua/lfm_terrace.ogg";
	    						SetBackdroundRes(R.drawable.small_button_ter);
	    						SetVolumeBg(R.drawable.styled_progress_ter);	
	    						mainLogo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo_small));
	    						updateTrackList();
	    						break;
	    					case (2):
	    						stream = "http://cast.loungefm.com.ua/loungefm";
	    						SetBackdroundRes(R.drawable.small_button);
	    						SetVolumeBg(R.drawable.styled_progress);
	    						mainLogo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo_small2));
	    						updateTrackList();

	    						break;
	    					case (3):
	    						stream = "http://cast.loungefm.com.ua/lfm_acoustic.ogg";
	    						SetBackdroundRes(R.drawable.small_button_acc);
	    						SetVolumeBg(R.drawable.styled_progress_acc);
	    						mainLogo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo_small));
	    						updateTrackList();
	    						break;
	    					case (4):
	    						stream = "http://cast.loungefm.com.ua/lfm_chillout.ogg";
	    						SetBackdroundRes(R.drawable.small_button_chill);
	    						SetVolumeBg(R.drawable.styled_progress_chill);
	    						mainLogo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo_small));
	    						updateTrackList();
	    						break;
	    					case (5):
	    						stream = "http://cast.loungefm.com.ua/lfm_terrace.ogg";
	    					SetBackdroundRes(R.drawable.small_button_ter);
	    					SetVolumeBg(R.drawable.styled_progress_ter);	
	    					updateTrackList();
	    					mainLogo.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.logo_small));
	    				        handler.postDelayed(new Runnable() {
	    				            @Override
	    				            public void run() {
	    				            	mViewPager.setCurrentItem(1,false);
	    				            }
	    				        }, 250);
	    				        break;
	    						
	    					default:
	    						stream = "http://cast.loungefm.com.ua/loungefm";
	    						SetBackdroundRes(R.drawable.small_button);
	    						SetVolumeBg(R.drawable.styled_progress);
	    						updateTrackList();
	    					}
	    					
	    					MusicService.AUDIO_URL_128 = stream;
	    					MusicService.AUDIO_URL_32 = stream;
	    					bindToService();
	    					//currentTrackTitle.setText(buildTrackTilte(0, false));	

	    				}
	        		
	        	}
	        	else
	        	{
	    			SetVolumeBg(R.drawable.styled_progress);

	        	}
	        }
	        
	        

	        public void onPageScrolled(int arg0, float arg1, int arg2) {

	        }

	        public void onPageScrollStateChanged(int state) {
	        	
	        }
	      });

	        
	        //SetVolumeBg(R.drawable.styled_progress_ter);	        
	        mViewPager.setCurrentItem(2);
	        isFirst = false;
	        



	}
	

	
	
	@Override
	protected void onResume() {
		updateTrackList();
		updateUI();
		super.onResume();
	}
	
	
    /**
     * Binds to the instance of MediaPlayerService. If no instance of MediaPlayerService exists, it first starts
     * a new instance of the service.
     */
    public void bindToService() {
        Intent intent = new Intent(MusicService.ACTION_STOP);
 
        if (MusicServiceRunning()) {
            // Bind to LocalService
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        else {
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }
    
    /** Determines if the MediaPlayerService is already running.
     * @return true if the service is running, false otherwise.
     */
    private boolean MusicServiceRunning() {
 
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
 
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.studiovision.loungefm.MusicService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            Log.d("MainActivity","service connected");
 
            //bound with Service. get Service instance
            MusicServiceBinder binder = (MusicServiceBinder) serviceBinder;
            mService = binder.getService();
 
          //send this instance to the service, so it can make callbacks on this instance as a client
            mService.setClient(MainActivity.this);
            
            mBound = true;
        }
 
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    public void onInitializePlayerStart() {
    	showAnimation();
    };
    
    @Override
    public void onInitializePlayerSuccess() {
    	hideAnimation();
    }
    
    @Override
    public void onPlayerPreparing() {
    	showAnimation();
    	
    }
    
    @Override
    public void onPlayerPrepared() {
    	hideAnimation();
    	
    }
    
    @Override
    public void onMediaPlayerUpdateBuffer() {
    	showAnimation();
    	
    }
    
    @Override
    public void onMediaPlayerError() {
    	hideAnimation();
    	mPlayPauseButton.setBackgroundResource(R.drawable.play);
    	isPlaying = false;
    }
    
    @Override
    public void onPlayerPause() {
			isPlaying = false;
			updateUI();
    }
    	
	private void initVolumeControl() {
		
		try {
			volumeSeekBar = (SeekBar)findViewById(R.id.volumeControl);
			
			audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			volumeSeekBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
			volumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
			
			volumeSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private void updateUI() 
	{
		if (isPlaying) {
			mPlayPauseButton.setBackgroundResource(R.drawable.pause);
		}
		else {
			mPlayPauseButton.setBackgroundResource(R.drawable.play);
		}
		volumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
	}
	

	@Override
	public void onClick(View target) {
		// Send the correct intent to the MusicService, according to the button that was clicked		
		if (target == mPlayPauseButton) {
			isPlaying = !isPlaying; 
		    updateUI();
			startService(new Intent (MusicService.ACTION_TOGGLE_PLAYBACK));
		}
		else if (target == mShareButton) {
			clickShare();
		}
		else if (target == mOnAirButton) {
			clickOnAir();
		}
		else if (target == m128kbButton) {
			click128kb();	
		}
		else if (target == m32kbButton){
			click32kb();
		}
		else if (target == btnChange){
			
			SetVolumeBg(R.drawable.styled_progress);
			updateUI();
		}
	}
	private void showAnimation() 
	{
		mProgressImageView.setVisibility(View.VISIBLE);
		mProgressImageView.startAnimation(AnimationUtils.loadAnimation(this, R.animator.rotate_indefinitely));
	}
	
	private void hideAnimation() 
	{
		mProgressImageView.setVisibility(View.GONE);
		mProgressImageView.clearAnimation();
		
	}
	
	private void clickShare() {
		
		//popupShare = new PopupWindow(this);
		mShareButton.setSelected(true);
	
		LayoutInflater layoutInflater = (LayoutInflater)getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = layoutInflater.inflate(R.layout.share, null);
		
		final PopupWindow popupShare = new PopupWindow(layout, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, true);
		
		popupShare.setAnimationStyle(R.style.Animation);
		
		/*
		popupShare.setBackgroundDrawable(new BitmapDrawable());
		popupShare.setOutsideTouchable(true);
		 
		
		 
		 popupShare.setTouchInterceptor(new OnTouchListener() {
	            public boolean onTouch(View v, MotionEvent event) {
	            	mShareButton.setSelected(false);
	                if(event.getAction() == MotionEvent.ACTION_OUTSIDE) {
	                    popupShare.dismiss();	                    
	                    return true;
	                }
	                return false;
	                
	            }
	        });
	        */
		 
		 
		 mVkontakte = (Button)layout.findViewById(R.id.vkontakteButton);
		 mFacebook = (Button)layout.findViewById(R.id.facebookButton);
		 mEmail = (Button)layout.findViewById(R.id.emailButton);
		 mSMS = (Button)layout.findViewById(R.id.smsButton);
		 mCancelButton = (Button)layout.findViewById(R.id.cancelButton);
		
		 mVkontakte.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mShareButton.setSelected(false);				
				popupShare.dismiss();
				if (account.access_token != null) {
					postMessageToWall();
				}
				else {
					startLoginActivity();
				}
			}
		});
		 
		 mCancelButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mShareButton.setSelected(false);
					popupShare.dismiss();
				}
			});
		 
		
		 mFacebook.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				mShareButton.setSelected(false);
				popupShare.dismiss();
				postMessage();
			}
		});
		 
		 //compose sms
		 mSMS.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mShareButton.setSelected(false);
				Intent smsIntent = new Intent(Intent.ACTION_VIEW);
				smsIntent.putExtra("sms_body", "Рекомендую скачать приложение Lounge Fm для Android: http://loungefm.com.ua");
				smsIntent.setType("vnd.android-dir/mms-sms");
				popupShare.dismiss();
				startActivity(smsIntent);
				
			}
		});
		 
		 //compose email
		 mEmail.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mShareButton.setSelected(false);
				Intent emailIntent = new Intent(Intent.ACTION_SEND);    	
		    	emailIntent.setType("text/plain");
		    	String[] recipients = new String[]{"my@email.com", ""};
		    	emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);
		    	emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Lounge Fm");
		    	emailIntent.putExtra(Intent.EXTRA_TEXT, "Рекомендую скачать приложение Lounge Fm для Android: http://loungefm.com.ua");
		    	
		    	//check if we have such intent
		    	PackageManager packageManager = getPackageManager();
		    	List<ResolveInfo>activities = packageManager.queryIntentActivities(emailIntent, 0);
		    	boolean isIntentSafe = activities.size() > 0;
		    	if (isIntentSafe) {
		    	//	String title = (String) getResources().getText(R.string.chooser_title);
		    		Intent chooser = Intent.createChooser(emailIntent, "1234");		    		
		    		startActivity(chooser);
		    	} 
		    	popupShare.dismiss();
			}
		});
		 
		 popupShare.showAtLocation(this.findViewById(android.R.id.content).getRootView(), Gravity.BOTTOM, 0, 0);		 
	}
	
	
	private String getFacebookMsg() {
		return MSG + " at " + new Date().toLocaleString();
	}	
	
		
	public void postMessage() {
		
		if (facebookConnector.getFacebook().isSessionValid()) {
			postMessageInThread();
		} else {
			SessionEvents.AuthListener listener = new SessionEvents.AuthListener() {
				
				@Override
				public void onAuthSucceed() {
					postMessageInThread();
				}
				
				@Override
				public void onAuthFail(String error) {
					
				}
			};
			SessionEvents.addAuthListener(listener);
			facebookConnector.login();
		}
	}

	private void postMessageInThread() {
		Thread t = new Thread() {
			public void run() {
		    	
		    	try {
		    		facebookConnector.postMessageOnWall(getFacebookMsg());
					mFacebookHandler.post(mUpdateFacebookNotification);
				} catch (Exception ex) {
					Log.e(TAG, "Error sending msg",ex);
				}
		    }
		};
		t.start();
	}
	
	
	//on air button is clicked	
	private void clickOnAir() {
		//popupOnAir = new PopupWindow(this);
		
		try
		{
		
		mOnAirButton.setSelected(true);
		
		LayoutInflater layoutInflater = (LayoutInflater)getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = layoutInflater.inflate(R.layout.on_air, null);
		
	

		final PopupWindow popupOnAir = new PopupWindow(layout, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, true);
		
		popupOnAir.setAnimationStyle(R.style.Animation);
		
		/*
		popupOnAir.setBackgroundDrawable(new BitmapDrawable());
		popupOnAir.setOutsideTouchable(true);
	
		 popupOnAir.setTouchInterceptor(new OnTouchListener() {
	            public boolean onTouch(View v, MotionEvent event) {
	            	mOnAirButton.setSelected(false);
	                if(event.getAction() == MotionEvent.ACTION_OUTSIDE) {
	                	popupOnAir.dismiss();
	                    return true;
	                }
	                return false;
	            }
	        });
	        */
		circle = (ImageView)layout.findViewById(R.id.firstTrackCircle);
		int circleID;
		switch(mViewPager.getCurrentItem())
		{
			case 1:
				circleID = R.drawable.circle_ter;
				break;
			case 2:
				circleID = R.drawable.circle_purple;
				break;
			case 3:
				circleID = R.drawable.circle_acc;
				break;
			case 4:
				circleID = R.drawable.circle_chill;
				break;
			default:
				circleID = R.drawable.circle_purple;
				break;
		}
		circle.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), circleID));

		
		firstSongTextView = (TextView)layout.findViewById(R.id.first_track);
		firstSongTime = (TextView)layout.findViewById(R.id.firstTrackTime);
		
		secondSongTextView = (TextView)layout.findViewById(R.id.second_track);
		secondSongTime = (TextView)layout.findViewById(R.id.secondTrackTime);
		
		thirdSongTextView = (TextView)layout.findViewById(R.id.third_track);
		thirdSongTime = (TextView)layout.findViewById(R.id.thirdTrackTime);
		
		firstSongTextView.setText(buildTrackTilte(1, true));
		firstSongTime.setText(buildTrackTime(1));
		secondSongTextView.setText(buildTrackTilte(2, true));
		secondSongTime.setText(buildTrackTime(2));
		thirdSongTextView.setText(buildTrackTilte(3, true));
		thirdSongTime.setText(buildTrackTime(3));
		
		
		mOnAirBackButton = (Button)layout.findViewById(R.id.onAirBackButton);

		mOnAirBackButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mOnAirButton.setSelected(false);
				popupOnAir.dismiss();
			}
		});
		
		popupOnAir.showAtLocation(this.findViewById(android.R.id.content).getRootView(), Gravity.BOTTOM, 0, 0);
		}
		catch(Exception ex)
		{
			Log.e("mylog",ex.getMessage().toString());
			mOnAirButton.setSelected(false);

		}
		
	
	}
	
	//change bitrate	
	private void click128kb() { 		
		m32kbButton.setSelected(false);
		m128kbButton.setSelected(true);
		Intent intent = new Intent(MusicService.ACTION_CHANGE_BITRATE);
		intent.putExtra("Quality", 1);
		startService(intent);
	}
	
    private void click32kb() {
    	m128kbButton.setSelected(false);
    	m32kbButton.setSelected(true);
    	Intent intent = new Intent(MusicService.ACTION_CHANGE_BITRATE);
		intent.putExtra("Quality", 0);
		startService(intent);
	}
	
	
	private void startLoginActivity() {
		Intent intent = new Intent();
		intent.setClass(this, VkLoginActivity.class);
		startActivityForResult(intent, REQUEST_LOGIN);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_LOGIN) {
			if (resultCode == RESULT_OK) {
				//authorization succesfull
				account.access_token = data.getStringExtra("token");
				account.user_id = data.getLongExtra("user_id", 0);
				account.save(MainActivity.this);
				api = new Api(account.access_token, Constants.API_ID);
				postMessageToWall();
				
			}
		}
		this.facebookConnector.getFacebook().authorizeCallback(requestCode, resultCode, data);
	}
	
	//post message to wall
	private void postMessageToWall() {
		new Thread() {
			@Override
			public void run() {
				try {
					ArrayList<String> atachments = new ArrayList<String>();
					atachments.add("http://loungefm.com.ua");
					api.createWallPost(account.user_id, MSG, atachments, null, false, false, false, null, null, null, null);
					runOnUiThread(successRunnable);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	Runnable successRunnable = new Runnable() {
		@Override
		public void run() {
			Toast toast = new Toast(getApplicationContext());
			ImageView imageView = new ImageView(getApplicationContext());
			imageView.setImageResource(R.drawable.checkmark);
			imageView.setBackgroundColor(Color.BLACK);
			toast.setGravity ( Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0 , 0 );
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setView(imageView);
			toast.show();
		}
		
	};
	
	public void updateTrackList() {
		
		ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		String steam;
		switch(page)
		{
			case(0):	
				steam="http://loungefm.com.ua/stream/chillout.json";
				break;
			case(1):
				steam="http://loungefm.com.ua/stream/terrace.json";
				break;
			case(2):
				steam="http://loungefm.com.ua/stream/playlist_iphone.json";
				break;
			case(3):
				steam="http://loungefm.com.ua/stream/acoustic.json";
				break;
			case(4):
				steam="http://loungefm.com.ua/stream/chillout.json";
				break;
			case(5):
				steam="http://loungefm.com.ua/stream/terrace.json";
				break;
			default:
				steam="http://loungefm.com.ua/stream/playlist_iphone.json";
				break;
		}
		
		if (networkInfo != null && networkInfo.isConnected()) {
			new NetworkTask().execute(steam);			
		}
		else {
			Toast.makeText(getApplicationContext(), R.string.network_is_offline, Toast.LENGTH_LONG).show();
		}
	}
	
	//build track title as Artist - Title
	private String buildTrackTilte(int trackNumber, boolean onAir) {
		String trackTitleString = "";
		if (tracksInfoList.size() > 0) {
			StringBuilder sbBuilder = new StringBuilder();
			if (onAir) {
				trackTitleString = sbBuilder.append(tracksInfoList.get(trackNumber).get(0)).append("\n").append(tracksInfoList.get(trackNumber).get(1)).toString();					
			}
			else {
				trackTitleString = sbBuilder.append(tracksInfoList.get(trackNumber).get(0)).append(" - ").append(tracksInfoList.get(trackNumber).get(1)).toString();
			}		
		}
		else {
			trackTitleString = "Artist - Track";
		}
		return trackTitleString;
	}	
	
	
	//build track time as hh:mm
	private String buildTrackTime(int trackNumber) {
		String trackTimeString = "";
		if (tracksInfoList.size() > 0) {
			StringBuilder sbBuilder = new StringBuilder();
			trackTimeString = sbBuilder.append(tracksInfoList.get(trackNumber).get(2)).toString();	
		}
		else {
			trackTimeString = "";
		}
		return trackTimeString;
	}	
	
	
	private class NetworkTask extends AsyncTask<String, Void, JSONArray>{
		@Override
		protected JSONArray doInBackground(String... params) {
			
			//holds
			JSONArray tracksList = null;
			
			try {
				tracksList = downloadUrl(params[0]);
			} catch (Exception e) {
				Log.d(Constants.TAG, "Unable to retrieve data");
			}
			return tracksList;
		}
		
		@Override
		protected void onPostExecute(JSONArray result) {
			if (result != null) {
				
				tracksInfoList = new ArrayList<ArrayList<String>>();
				
				JSONObject track = null;
				String tempString = null;
				ArrayList<String>tempArrayList = null;
				StringBuilder sbBuilder = new StringBuilder();
				
				for (int i = 0; i < result.length(); i++) {
					tempArrayList = new ArrayList<String>();					
					sbBuilder.setLength( 0 );					
					try {
						track = (JSONObject)result.get(i);
						tempString = track.getString("ARTIST");
						tempArrayList.add(tempString);
						tempString = track.getString("NAME");
						tempArrayList.add(tempString);
						tempString = track.getString("START_TIME");
						StringTokenizer tokens = new StringTokenizer(tempString, ":");
						
						tempString = sbBuilder.append(tokens.nextToken()).append(":").append(tokens.nextToken()).toString();
						
						tempArrayList.add(tempString);
						
						tracksInfoList.add(tempArrayList);					
						
					} catch (Exception e) {
						// TODO: handle exception
					}					
				}
				//set the currentTrackTextView to first song from container
				currentTrackTitle.setText(buildTrackTilte(0, false));				
			}
		}
		
		private JSONArray downloadUrl(String urlStr) throws IOException {
			
			JSONArray tracksList = null;
			
			InputStream inputStream = null;
			
			try {
				URL url = new URL(urlStr);
				HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
				httpURLConnection.setRequestMethod("GET");
				httpURLConnection.setDoInput(true);
				
				//starts the queury
				httpURLConnection.connect();
				int response = httpURLConnection.getResponseCode();
				Log.d(Constants.TAG, "the response is: " +response);
				inputStream = httpURLConnection.getInputStream();
								
				tracksList = new JSONArray(convertStreamToString(inputStream));
				
			} catch (Exception e) {
				Log.d(Constants.TAG, "exception - " +e.getMessage());
			}			
			finally{
				if (inputStream != null) {
					inputStream.close();					
				}
			}
			return tracksList;
		}
		
		public String convertStreamToString(InputStream is) throws Exception {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		    StringBuilder sb = new StringBuilder();
		    String string = null;

		    while ((string = reader.readLine()) != null) {
		        sb.append(string);
		    }
		    return sb.toString();
		}
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                startService(new Intent(MusicService.ACTION_TOGGLE_PLAYBACK));
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
            	audioManager.adjustVolume(AudioManager.ADJUST_RAISE, 0);
            	volumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            	return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            	audioManager.adjustVolume(AudioManager.ADJUST_LOWER, 0);
            	volumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            	return true;
            case KeyEvent.KEYCODE_BACK:
            	moveTaskToBack(true);
        }
        return super.onKeyDown(keyCode, event);
    }	
	
	
	public class TitleAdapter extends FragmentPagerAdapter
	{
		private final String titles[] = getResources().getStringArray(R.array.data); 
		
	    private final Fragment frags[] = new Fragment[titles.length];
	 
	    public TitleAdapter(FragmentManager fm) {
	        super(fm);
	        frags[0] = new FragmentChillout();
	        frags[1] = new FragmentTrance();
	        frags[2] = new FragmentMain();
	        frags[3] = new FragmentAcoustic();
	        frags[4] = new FragmentChillout();
	        frags[5] = new FragmentTrance();
		    
	    }
	 
	    @Override
	    public CharSequence getPageTitle(int position) {
	        return titles[position];
	    }
	 
	    @Override
	    public Fragment getItem(int position) {
	        return frags[position];
	    }
	 
	    @Override
	    public int getCount() {
	        return frags.length;
	    }
	    
	}
	
}
