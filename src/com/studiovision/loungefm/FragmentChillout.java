package com.studiovision.loungefm;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentChillout extends Fragment {	
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {	    
	    
        View myView = inflater.inflate(R.layout.choice_chillout, container, false);
        
      
        return myView;
    }
}
