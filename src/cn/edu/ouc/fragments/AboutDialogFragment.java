/*
 * Copyright 2012 Ocean University of China.
 *
 */

package cn.edu.ouc.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import cn.edu.ouc.R;

/**
 * A DialogFragment to show information about Indoor Tracks.
 * 
 * @author Chu Hongwei, Hong Feng
 */
public class AboutDialogFragment extends DialogFragment {
	
	public static final String ABOUT_DIALOG_TAG = "aboutDialog";
	
	private FragmentActivity activity;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		activity = getActivity();
		View view = activity.getLayoutInflater().inflate(R.layout.about, null);
		return new AlertDialog.Builder(activity)
			.setPositiveButton(R.string.generic_ok, null)
			.setTitle(R.string.help_about)
			.setView(view)
			.create();
	}
	
	

}
