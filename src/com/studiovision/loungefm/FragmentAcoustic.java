package com.studiovision.loungefm;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class FragmentAcoustic extends Fragment {	
	
     public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {	    
	    
        View myView = inflater.inflate(R.layout.choice_acoustic, container, false);
        
        return myView;
    }
    
}
