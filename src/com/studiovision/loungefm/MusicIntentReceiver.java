package com.studiovision.loungefm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * Receives broadcasted intents. In particular, we are interested in the
 * android.media.AUDIO_BECOMING_NOISY and android.intent.action.MEDIA_BUTTON intents, which is
 * broadcast, for example, when the user disconnects the headphones. This class works because we are
 * declaring it in a receiver tag in AndroidManifest.xml.
 */

public class MusicIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) 
		{
			Toast.makeText(context, "Headphones disconnected", Toast.LENGTH_SHORT).show();
			
			//send an intent to our MusicService to telling it to pause the audio
			context.startService(new Intent(MusicService.ACTION_PAUSE));			
		} 
		else if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) 
		{
			KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
			if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) 
			{
				return;
			}
			
			switch (keyEvent.getKeyCode()) 
			{
			case KeyEvent.KEYCODE_HEADSETHOOK:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				context.startService(new Intent(MusicService.ACTION_TOGGLE_PLAYBACK));
				break;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				context.startService(new Intent(MusicService.ACTION_STOP));
				break;
			}
		}
		else if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG))
		{
			int state = intent.getIntExtra("state", -1);
            switch (state) 
            {
            case 0:
            	context.startService(new Intent(MusicService.ACTION_PAUSE));
                break;
            case 1:
               // Log.d(TAG, "Headset is plugged");
                break;
            default:
               // Log.d(TAG, "I have no idea what the headset state is");
            }	
		}
	}
}