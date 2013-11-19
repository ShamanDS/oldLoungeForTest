package com.studiovision.loungefm;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentMain extends Fragment {	
	
     public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {	    
	    
        View myView = inflater.inflate(R.layout.choice_main, container, false);
        
      
        return myView;
    }
}
