package com.studiovision.loungefm;

public interface LMediaPlayerServiceClient {
	 
    /**
     * A callback made by a MediaPlayerService onto its clients to indicate that a player is initializing.
     * @param message A message to propagate to the client
     */
    public void onInitializePlayerStart();
 
    /**
     * A callback made by a MediaPlayerService onto its clients to indicate that a player was successfully initialized.
     */
    public void onInitializePlayerSuccess();
    
    public void onPlayerPreparing();
    
    public void onPlayerPrepared();
    
    public void onPlayerPause(); 
    
    /**
     * A callback made by a MediaPlayerService onto its clients to indicate that a player update buffer.
     */
    
    public void onMediaPlayerUpdateBuffer();

 
    /**
     *  A callback made by a MediaPlayerService onto its clients to indicate that a player encountered an error.
     */
    public void onMediaPlayerError();
}
