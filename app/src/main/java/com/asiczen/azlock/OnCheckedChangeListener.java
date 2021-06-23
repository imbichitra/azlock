package com.asiczen.azlock;

import android.widget.CompoundButton;

/**
 * Created by Somnath on 1/10/2017.
 */

public interface OnCheckedChangeListener {

    int REQUEST_TAMPER_NOTIFICATION=10021;
    int REQUEST_ASK_PIN=10022;
    int REQUEST_PLAY_SOUND=10033;
    int REQUEST_AJAR = 10023;
    int REQUEST_AUTO_LOCK = 10024;

    void onCheckedChanged(CompoundButton buttonView, boolean isChecked, int requestCode);
}
